package cn.interview.sealcard.service;

import static cn.interview.sealcard.service.BusinessStore.*;
import static cn.interview.sealcard.service.SealBusinessService.*;

import cn.interview.sealcard.adapter.*;
import cn.interview.sealcard.exception.BusinessException;
import cn.interview.sealcard.security.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 上传任务的持久化租约、幂等平台确认与有界重试。 */
@Service
public class UploadTaskWorker {
  private final BusinessStore db;
  private final ImageStorage storage;
  private final ObjectMapper json;
  private final AuditService audit;
  private final DemoIdentityAdapter users;
  private final TransactionTemplate tx;
  private final int limit, lease, delay;
  private final String instance = UUID.randomUUID().toString();

  public UploadTaskWorker(
      BusinessStore db,
      ImageStorage storage,
      ObjectMapper json,
      AuditService audit,
      DemoIdentityAdapter users,
      PlatformTransactionManager manager,
      @Value("${seal.retry-limit}") int limit,
      @Value("${seal.lease-seconds}") int lease,
      @Value("${seal.retry-seconds}") int delay) {
    this.db = db;
    this.storage = storage;
    this.json = json;
    this.audit = audit;
    this.users = users;
    tx = new TransactionTemplate(manager);
    tx.setPropagationBehavior(3);
    this.limit = limit;
    this.lease = lease;
    this.delay = delay;
  }

  @SuppressWarnings("unchecked")
  public void process(long taskId) {
    Map<String, Object> current = db.one("seal_upload_task", "id=#{args.id}", m("id", taskId));
    if (current == null) return;
    long token = id(current, "lock_token") + 1;
    LocalDateTime stamp = now();
    int claimed =
        db.update(
            "seal_upload_task",
            m(
                "task_status",
                "PROCESSING",
                "lock_owner",
                instance,
                "lock_until",
                stamp.plusSeconds(lease),
                "lock_token",
                token),
            "id=#{args.id} AND lock_token=#{args.old} AND ((task_status IN ('PENDING','RETRY') AND (next_retry_at IS NULL OR next_retry_at<=#{args.now})) OR (task_status='PROCESSING' AND lock_until<#{args.now}))",
            m("id", taskId, "old", token - 1, "now", stamp));
    if (claimed != 1) return;
    Identity prior = RequestContext.optional();
    String request = RequestContext.requestId();
    RequestContext.set(users.find(text(current, "operator_id")), text(current, "request_id"));
    try {
      List<Map<String, Object>> pages = json.readValue(text(current, "payload"), List.class);
      List<Map<String, Object>> ready = new ArrayList<>();
      long version = id(current, "version_id");
      for (Map<String, Object> p : pages) {
        renew(taskId, token);
        String object = "v" + version + "p" + p.get("pageNo") + "-" + p.get("sha256");
        storage.promote(p.get("stagedId").toString(), object, p.get("sha256").toString());
        ready.add(
            m(
                "version_id",
                version,
                "page_no",
                p.get("pageNo"),
                "image_id",
                object,
                "sha256",
                p.get("sha256"),
                "mime_type",
                p.get("mimeType"),
                "file_size",
                p.get("fileSize")));
      }
      tx.execute(
          s -> {
            Map<String, Object> v = db.one("seal_card_version", "id=#{args.id}", m("id", version));
            Map<String, Object> card = db.lockCard(id(v, "seal_card_id"));
            Map<String, Object> t =
                db.one(
                    "seal_upload_task",
                    "id=#{args.id} AND task_status='PROCESSING' AND lock_token=#{args.token} AND lock_owner=#{args.owner} AND lock_until>#{args.now}",
                    m("id", taskId, "token", token, "owner", instance, "now", now()));
            if (t == null) return null;
            if (!"ENABLED".equals(card.get("status")) || !"UPLOADING".equals(v.get("status"))) {
              db.update(
                  "seal_upload_task",
                  m(
                      "task_status",
                      "FAILED",
                      "last_error_code",
                      "VERSION_UNAVAILABLE",
                      "last_error_message",
                      "卡片或版本状态已变化",
                      "lock_owner",
                      null,
                      "lock_until",
                      null),
                  "id=#{args.id} AND lock_token=#{args.token}",
                  m("id", taskId, "token", token));
              return null;
            }
            if (ready.size() != id(v, "expected_image_count"))
              throw BusinessException.conflict("影像页数不完整");
            for (Map<String, Object> image : ready) {
              Map<String, Object> before =
                  db.one(
                      "seal_card_image",
                      "version_id=#{args.v} AND page_no=#{args.page}",
                      m("v", version, "page", image.get("page_no")));
              if (before == null) db.insert("seal_card_image", image);
              else if (!before.get("sha256").equals(image.get("sha256")))
                throw BusinessException.conflict("影像摘要冲突");
            }
            db.update(
                "seal_card_version",
                m("status", "PENDING_APPROVAL", "submitted_at", now()),
                "id=#{args.id}",
                m("id", version));
            db.update(
                "seal_upload_task",
                m(
                    "task_status",
                    "SUCCESS",
                    "completed_at",
                    now(),
                    "last_error_code",
                    null,
                    "last_error_message",
                    null,
                    "lock_owner",
                    null,
                    "lock_until",
                    null),
                "id=#{args.id} AND lock_token=#{args.token}",
                m("id", taskId, "token", token));
            audit.write("SUBMIT", id(card, "id"), version, null, "SUCCESS", null);
            return null;
          });
    } catch (Exception e) {
      tx.execute(
          s -> {
            Map<String, Object> task =
                db.one(
                    "seal_upload_task",
                    "id=#{args.id} AND lock_token=#{args.token} AND task_status='PROCESSING' AND lock_owner=#{args.owner}",
                    m("id", taskId, "token", token, "owner", instance));
            if (task == null) return null;
            int count = ((Number) task.get("retry_count")).intValue() + 1;
            Map<String, Object> v =
                db.one("seal_card_version", "id=#{args.id}", m("id", task.get("version_id")));
            db.update(
                "seal_upload_task",
                m(
                    "task_status",
                    count >= limit ? "FAILED" : "RETRY",
                    "retry_count",
                    count,
                    "next_retry_at",
                    now().plusSeconds(delay * Math.min(count, 10)),
                    "last_error_code",
                    "IMAGE_STORAGE_UNAVAILABLE",
                    "last_error_message",
                    "影像确认失败，等待恢复或人工核对",
                    "lock_owner",
                    null,
                    "lock_until",
                    null),
                "id=#{args.id} AND lock_token=#{args.token}",
                m("id", taskId, "token", token));
            if (count >= limit)
              db.update(
                  "seal_card_version",
                  m("status", "FAILED"),
                  "id=#{args.id} AND status='UPLOADING'",
                  m("id", task.get("version_id")));
            audit.write(
                "UPLOAD",
                id(v, "seal_card_id"),
                id(v, "id"),
                null,
                "FAIL",
                "IMAGE_STORAGE_UNAVAILABLE");
            return null;
          });
    } finally {
      RequestContext.set(prior, request);
    }
  }

  private void renew(long id, long token) {
    if (db.update(
            "seal_upload_task",
            m("lock_until", now().plusSeconds(lease)),
            "id=#{args.id} AND task_status='PROCESSING' AND lock_token=#{args.token} AND lock_owner=#{args.owner} AND lock_until>#{args.now}",
            m("id", id, "token", token, "owner", instance, "now", now()))
        != 1) throw BusinessException.conflict("任务租约已失效");
  }

  public void recover() {
    for (Map<String, Object> t :
        db.rows(
            "seal_upload_task",
            "(task_status IN ('PENDING','RETRY') AND (next_retry_at IS NULL OR next_retry_at<=#{args.now})) OR (task_status='PROCESSING' AND lock_until<#{args.now})",
            "ORDER BY id LIMIT 20",
            m("now", now()))) process(id(t, "id"));
  }

  public Map<String, Object> view(Map<String, Object> t) {
    Map<String, Object> v =
        db.one("seal_card_version", "id=#{args.id}", m("id", t.get("version_id")));
    return m(
        "taskId",
        t.get("id").toString(),
        "cardId",
        v.get("seal_card_id").toString(),
        "versionId",
        t.get("version_id").toString(),
        "status",
        t.get("task_status"),
        "retryCount",
        t.get("retry_count"),
        "nextRetryAt",
        date(t.get("next_retry_at")),
        "lastErrorCode",
        t.get("last_error_code"),
        "lastErrorMessage",
        t.get("last_error_message"),
        "createdAt",
        date(t.get("created_at")),
        "updatedAt",
        date(t.get("updated_at")));
  }

  private Object date(Object t) {
    return t == null ? null : time(t).toString();
  }

  private Map<String, Object> task(long id) {
    RequestContext.role("MAKER", "ADMIN");
    Map<String, Object> t = db.one("seal_upload_task", "id=#{args.id}", m("id", id));
    if (t == null) throw BusinessException.missing();
    if (!RequestContext.user().has("ADMIN")
        && !RequestContext.user().operatorId.equals(t.get("operator_id")))
      throw BusinessException.forbidden();
    return t;
  }

  public Map<String, Object> get(long id) {
    return view(task(id));
  }

  public Map<String, Object> list(String status, int page, int size) {
    RequestContext.role("MAKER", "ADMIN");
    paging(page, size);
    String where = RequestContext.user().has("ADMIN") ? "1=1" : "operator_id=#{args.user}";
    if (status != null && !status.isEmpty()) where += " AND task_status=#{args.status}";
    Map<String, Object> a =
        m(
            "user",
            RequestContext.user().operatorId,
            "status",
            status,
            "limit",
            size,
            "offset",
            (page - 1) * size);
    List<Map<String, Object>> list = new ArrayList<>();
    for (Map<String, Object> t :
        db.rows(
            "seal_upload_task",
            where,
            "ORDER BY id DESC LIMIT #{args.limit} OFFSET #{args.offset}",
            a)) list.add(view(t));
    return page(list, db.count("seal_upload_task", where, a), page, size);
  }

  public Map<String, Object> retry(long id) {
    Map<String, Object> t = task(id);
    tx.execute(
        s -> {
          Map<String, Object> v =
              db.one("seal_card_version", "id=#{args.id}", m("id", t.get("version_id")));
          Map<String, Object> c = db.lockCard(id(v, "seal_card_id"));
          if (!"ENABLED".equals(c.get("status"))) throw BusinessException.conflict("卡片已停用");
          if (!"RETRY".equals(t.get("task_status")) && !"FAILED".equals(t.get("task_status")))
            throw BusinessException.conflict("任务不处于可重试状态");
          if (db.count(
                  "seal_card_version",
                  "seal_card_id=#{args.card} AND id<>#{args.id} AND status IN ('UPLOADING','PENDING_APPROVAL','APPROVED')",
                  m("card", c.get("id"), "id", v.get("id")))
              > 0) throw BusinessException.conflict("已有其他待完成版本");
          db.update(
              "seal_card_version",
              m("status", "UPLOADING"),
              "id=#{args.id} AND status IN ('FAILED','UPLOADING')",
              m("id", v.get("id")));
          int changed =
              db.update(
                  "seal_upload_task",
                  m("task_status", "PENDING", "retry_count", 0, "next_retry_at", null),
                  "id=#{args.id} AND task_status IN ('RETRY','FAILED')",
                  m("id", id));
          if (changed != 1) throw BusinessException.conflict("任务已被其他执行者处理");
          audit.write("RETRY_UPLOAD", id(c, "id"), id(v, "id"), null, "SUCCESS", null);
          return null;
        });
    process(id);
    return get(id);
  }

  @SuppressWarnings("unchecked")
  public void cleanup() {
    try {
      Set<String> keep = new HashSet<>();
      for (Map<String, Object> i : db.rows("seal_card_image", "1=1", m()))
        keep.add(text(i, "image_id"));
      for (Map<String, Object> t : db.rows("seal_upload_task", "task_status<>'SUCCESS'", m())) {
        for (Map<String, Object> p :
            (List<Map<String, Object>>) json.readValue(text(t, "payload"), List.class)) {
          keep.add(p.get("stagedId").toString());
          keep.add("v" + t.get("version_id") + "p" + p.get("pageNo") + "-" + p.get("sha256"));
        }
      }
      storage.cleanupUnreferenced(keep, System.currentTimeMillis() - 86400000L);
    } catch (Exception e) {
      org.slf4j.LoggerFactory.getLogger(getClass()).warn("Orphan-image reconciliation failed");
    }
  }
}
