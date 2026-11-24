package cn.interview.sealcard.service;

import static cn.interview.sealcard.service.BusinessStore.*;

import cn.interview.sealcard.adapter.*;
import cn.interview.sealcard.exception.BusinessException;
import cn.interview.sealcard.param.*;
import cn.interview.sealcard.security.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

/** 以数据库行锁串行化每张卡的状态转换；公开方法不直接暴露持久化实体。 */
@Service
public class SealBusinessService {
  private final BusinessStore db;
  private final AuditService audit;
  private final CustomerAccountAdapter customers;
  private final ImageValidator images;
  private final ObjectMapper json;
  private final TransactionTemplate tx;
  private UploadTaskWorker worker;
  private final ScopeAuthorization scopes;

  public SealBusinessService(
      BusinessStore db,
      AuditService audit,
      CustomerAccountAdapter customers,
      ImageValidator images,
      ObjectMapper json,
      PlatformTransactionManager manager,
      ScopeAuthorization scopes) {
    this.scopes = scopes;
    this.db = db;
    this.audit = audit;
    this.customers = customers;
    this.images = images;
    this.json = json;
    tx = new TransactionTemplate(manager);
  }

  @org.springframework.beans.factory.annotation.Autowired
  public void setWorker(@org.springframework.context.annotation.Lazy UploadTaskWorker worker) {
    this.worker = worker;
  }

  public static LocalDateTime now() {
    return LocalDateTime.now(ZoneId.of("Asia/Shanghai")).truncatedTo(ChronoUnit.MILLIS);
  }

  public static LocalDateTime time(Object value) {
    if (value == null) return null;
    if (value instanceof LocalDateTime) return (LocalDateTime) value;
    if (value instanceof java.sql.Timestamp) return ((java.sql.Timestamp) value).toLocalDateTime();
    return LocalDateTime.parse(value.toString().replace(' ', 'T'));
  }

  private String sid(Object value) {
    return value == null ? null : value.toString();
  }

  private void branch(Map<String, Object> card) {
    Identity u = RequestContext.user();
    if (!u.has("AUDITOR") && !u.has("ADMIN") && !scopes.canRead(u, text(card,"branch_no")))
      throw BusinessException.forbidden();
  }

  private Map<String, Object> card(long id, boolean lock) {
    Map<String, Object> c =
        lock ? db.lockCard(id) : db.one("seal_card", "id=#{args.id}", m("id", id));
    if (c == null) throw BusinessException.missing();
    if (lock && !RequestContext.user().branchNo.equals(c.get("branch_no")))
      throw BusinessException.forbidden();
    branch(c);
    return c;
  }

  private Map<String, Object> version(long cardId, long versionId) {
    Map<String, Object> v =
        db.one(
            "seal_card_version",
            "id=#{args.id} AND seal_card_id=#{args.card}",
            m("id", versionId, "card", cardId));
    if (v == null) throw BusinessException.missing();
    return v;
  }

  public Map<String, Object> customer(String cif) {
    RequestContext.role("MAKER", "CHECKER", "TELLER");
    Map<String, Object> c = customers.customer(cif);
    if (!scopes.canRead(RequestContext.user(),text(c,"branchNo")))
      throw BusinessException.forbidden();
    return c;
  }

  private void validateOwner(UploadMetadata metadata) {
    Map<String, Object> c =
        customers.validate(metadata.customerNo, metadata.accountNos, metadata.authorizationRef);
    if (!RequestContext.user().branchNo.equals(c.get("branchNo")))
      throw BusinessException.forbidden();
  }

  private String serialize(Object o) {
    try {
      return json.writeValueAsString(o);
    } catch (Exception e) {
      throw new IllegalStateException("序列化失败");
    }
  }

  private String requestHash(UploadMetadata data, List<Map<String, Object>> pages, Long card) {
    List<Map<String, Object>> safe = new ArrayList<>();
    for (Map<String, Object> p : pages)
      safe.add(
          m(
              "sha256",
              p.get("sha256"),
              "pageNo",
              p.get("pageNo"),
              "mimeType",
              p.get("mimeType"),
              "fileSize",
              p.get("fileSize")));
    return LocalPrivateImageStorage.hash(
        serialize(
                m(
                    "cardId",
                    card,
                    "customerNo",
                    data.customerNo,
                    "accountNos",
                    data.accountNos,
                    "remark",
                    data.remark,
                    "authorizationRef",
                    data.authorizationRef,
                    "changeReason",
                    data.changeReason,
                    "pages",
                    safe))
            .getBytes(StandardCharsets.UTF_8));
  }

  private Map<String, Object> replay(String key, String hash) {
    Map<String, Object> task =
        db.one("seal_upload_task", "idempotency_key=#{args.key}", m("key", key));
    if (task == null) return null;
    Identity u = RequestContext.user();
    if (!u.operatorId.equals(task.get("operator_id")) || !u.branchNo.equals(task.get("branch_no")))
      throw BusinessException.conflict("幂等键已使用");
    if (!hash.equals(task.get("request_hash")))
      throw new BusinessException(409, "IDEMPOTENCY_CONFLICT", "相同幂等键不能用于不同请求内容");
    return task;
  }

  public Map<String, Object> upload(
      Long existing, UploadMetadata metadata, List<MultipartFile> files, String key) {
    RequestContext.role("MAKER");
    if (key == null || !key.matches("[a-zA-Z0-9_-]{8,64}"))
      throw BusinessException.bad("须提供8至64位安全幂等键");
    validateOwner(metadata);
    List<Map<String, Object>> pages = images.stage(files);
    String hash = requestHash(metadata, pages, existing);
    Map<String, Object> found = replay(key, hash);
    if (found != null) return uploadResult(found);
    final Identity user = RequestContext.user();
    Map<String, Object> task;
    try {
      task =
          tx.execute(
              s -> {
                Map<String, Object> again = replay(key, hash);
                if (again != null) return again;
                long cardId;
                if (existing == null) {
                  cardId =
                      db.insert(
                          "seal_card",
                          m(
                              "card_no",
                              "YJ" + UUID.randomUUID().toString().replace("-", "").substring(0, 28),
                              "customer_no",
                              metadata.customerNo,
                              "branch_no",
                              user.branchNo,
                              "created_by",
                              user.operatorId,
                              "remark",
                              metadata.remark));
                } else {
                  Map<String, Object> c = card(existing, true);
                  if (!metadata.customerNo.equals(c.get("customer_no")))
                    throw BusinessException.bad("版本客户号与原卡不一致");
                  assertChangeAllowed(c);
                  cardId = existing;
                }
                int number = nextVersion(cardId);
                long versionId =
                    db.insert(
                        "seal_card_version",
                        m(
                            "seal_card_id",
                            cardId,
                            "version_no",
                            number,
                            "created_by",
                            user.operatorId,
                            "expected_image_count",
                            pages.size(),
                            "authorization_ref",
                            metadata.authorizationRef,
                            "change_reason",
                            metadata.changeReason));
                bindings(
                    cardId,
                    versionId,
                    metadata.accountNos,
                    metadata.authorizationRef,
                    user.operatorId);
                Map<String, Object> row =
                    m(
                        "idempotency_key",
                        key,
                        "request_hash",
                        hash,
                        "version_id",
                        versionId,
                        "operator_id",
                        user.operatorId,
                        "branch_no",
                        user.branchNo,
                        "request_id",
                        RequestContext.requestId(),
                        "payload",
                        serialize(pages));
                db.insert("seal_upload_task", row);
                audit.write("UPLOAD", cardId, versionId, null, "SUCCESS", null);
                return row;
              });
    } catch (DuplicateKeyException e) {
      task = replay(key, hash);
      if (task == null) throw BusinessException.conflict("并发提交冲突，请重试");
    }
    worker.process(id(task, "id"));
    return uploadResult(db.one("seal_upload_task", "id=#{args.id}", m("id", task.get("id"))));
  }

  private Map<String, Object> uploadResult(Map<String, Object> task) {
    Map<String, Object> v =
        db.one("seal_card_version", "id=#{args.id}", m("id", task.get("version_id")));
    return m(
        "cardId",
        sid(v.get("seal_card_id")),
        "versionId",
        sid(v.get("id")),
        "taskId",
        sid(task.get("id")),
        "status",
        v.get("status"),
        "imageCount",
        v.get("expected_image_count"));
  }

  private void assertChangeAllowed(Map<String, Object> c) {
    if (!"ENABLED".equals(c.get("status"))) throw BusinessException.conflict("已停用卡片不能变更");
    if (db.count(
            "seal_card_version",
            "seal_card_id=#{args.id} AND status IN ('UPLOADING','PENDING_APPROVAL','APPROVED')",
            m("id", c.get("id")))
        > 0) throw BusinessException.conflict("此卡已有待完成申请");
  }

  private int nextVersion(long cardId) {
    List<Map<String, Object>> list =
        db.rows(
            "seal_card_version",
            "seal_card_id=#{args.id}",
            "ORDER BY version_no DESC LIMIT 1",
            m("id", cardId));
    return list.isEmpty() ? 1 : ((Number) list.get(0).get("version_no")).intValue() + 1;
  }

  private void bindings(long card, long version, List<String> accounts, String ref, String maker) {
    for (String account : accounts)
      db.insert(
          "seal_card_account",
          m(
              "seal_card_id",
              card,
              "version_id",
              version,
              "account_no",
              account,
              "created_by",
              maker,
              "authorization_ref",
              ref));
  }

  public Map<String, Object> accounts(long cardId, AccountChangeRequest request) {
    RequestContext.role("MAKER");
    return tx.execute(
        s -> {
          Map<String, Object> c = card(cardId, true);
          assertChangeAllowed(c);
          customers.validate(text(c, "customer_no"), request.accountNos, request.authorizationRef);
          long old = id(c, "current_version_id");
          if (old == 0) throw BusinessException.conflict("请先完成首次建档审批");
          List<Map<String, Object>> source =
              db.rows(
                  "seal_card_image",
                  "version_id=#{args.id} AND storage_status='READY'",
                  "ORDER BY page_no",
                  m("id", old));
          if (source.isEmpty()) throw BusinessException.conflict("当前版本没有可复用影像");
          long v =
              db.insert(
                  "seal_card_version",
                  m(
                      "seal_card_id",
                      cardId,
                      "version_no",
                      nextVersion(cardId),
                      "status",
                      "PENDING_APPROVAL",
                      "created_by",
                      RequestContext.user().operatorId,
                      "expected_image_count",
                      source.size(),
                      "authorization_ref",
                      request.authorizationRef,
                      "change_reason",
                      request.changeReason,
                      "submitted_at",
                      now()));
          bindings(
              cardId,
              v,
              request.accountNos,
              request.authorizationRef,
              RequestContext.user().operatorId);
          for (Map<String, Object> image : source)
            db.insert(
                "seal_card_image",
                m(
                    "version_id",
                    v,
                    "page_no",
                    image.get("page_no"),
                    "image_id",
                    image.get("image_id"),
                    "sha256",
                    image.get("sha256"),
                    "mime_type",
                    image.get("mime_type"),
                    "file_size",
                    image.get("file_size")));
          audit.write("BIND", cardId, v, request.changeReason, "SUCCESS", null);
          return versionView(version(cardId, v));
        });
  }

  public Map<String, Object> approve(long cardId, long versionId, ApprovalRequest request) {
    RequestContext.role("CHECKER");
    return tx.execute(
        s -> {
          Map<String, Object> c = card(cardId, true), v = version(cardId, versionId);
          if (!"ENABLED".equals(c.get("status"))) throw BusinessException.conflict("卡片已停用");
          if (!"PENDING_APPROVAL".equals(v.get("status")))
            throw BusinessException.conflict("版本不处于待复核状态");
          if (RequestContext.user().operatorId.equals(v.get("created_by")))
            throw new BusinessException(403, "MAKER_CHECKER_CONFLICT", "录入人与复核人必须分离");
          List<String> accounts = accountNos(versionId);
          customers.validate(text(c, "customer_no"), accounts, text(v, "authorization_ref"));
          long count =
              db.count(
                  "seal_card_image",
                  "version_id=#{args.id} AND storage_status='READY'",
                  m("id", versionId));
          if (count != id(v, "expected_image_count")) throw BusinessException.conflict("影像未完整就绪");
          LocalDateTime stamp = now();
          LocalDateTime effective =
              request.effectiveFrom == null
                  ? stamp
                  : request.effectiveFrom.truncatedTo(ChronoUnit.MILLIS);
          if (effective.isBefore(stamp.minusSeconds(2))) throw BusinessException.bad("不能回溯生效时间");
          if (!effective.isAfter(stamp)) effective = stamp;
          db.update(
              "seal_card_version",
              m(
                  "status",
                  "APPROVED",
                  "approved_by",
                  RequestContext.user().operatorId,
                  "approved_at",
                  stamp,
                  "effective_from",
                  effective),
              "id=#{args.id}",
              m("id", versionId));
          if (!effective.isAfter(stamp)) activateLocked(c, version(cardId, versionId), false);
          audit.write("APPROVE", cardId, versionId, null, "SUCCESS", null);
          return versionView(version(cardId, versionId));
        });
  }

  public Map<String, Object> reject(long cardId, long versionId, String reason) {
    RequestContext.role("CHECKER");
    return tx.execute(
        s -> {
          card(cardId, true);
          Map<String, Object> v = version(cardId, versionId);
          if (!"PENDING_APPROVAL".equals(v.get("status")))
            throw BusinessException.conflict("只能驳回待复核版本");
          if (RequestContext.user().operatorId.equals(v.get("created_by")))
            throw new BusinessException(403, "MAKER_CHECKER_CONFLICT", "录入人不能驳回自己的申请");
          db.update(
              "seal_card_version",
              m(
                  "status",
                  "REJECTED",
                  "rejected_by",
                  RequestContext.user().operatorId,
                  "rejected_at",
                  now(),
                  "reject_reason",
                  reason),
              "id=#{args.id}",
              m("id", versionId));
          db.update(
              "seal_card_account",
              m("binding_status", "REJECTED"),
              "version_id=#{args.id}",
              m("id", versionId));
          audit.write("REJECT", cardId, versionId, reason, "SUCCESS", null);
          return versionView(version(cardId, versionId));
        });
  }

  private void activateLocked(Map<String, Object> c, Map<String, Object> v, boolean system) {
    long cardId = id(c, "id"), versionId = id(v, "id"), old = id(c, "current_version_id");
    LocalDateTime effective = time(v.get("effective_from"));
    if (old != 0) {
      Map<String, Object> before = version(cardId, old);
      LocalDateTime start = time(before.get("effective_from"));
      if (!effective.isAfter(start)) effective = start.plusNanos(1000000);
      db.update(
          "seal_card_version",
          m("status", "ARCHIVED", "effective_to", effective),
          "id=#{args.id}",
          m("id", old));
      db.update(
          "seal_card_account",
          m("binding_status", "ENDED", "effective_to", effective),
          "version_id=#{args.id} AND binding_status='ACTIVE'",
          m("id", old));
    }
    db.update(
        "seal_card_version",
        m("status", "ACTIVE", "effective_from", effective),
        "id=#{args.id}",
        m("id", versionId));
    db.update(
        "seal_card_account",
        m(
            "binding_status",
            "ACTIVE",
            "effective_from",
            effective,
            "approved_by",
            v.get("approved_by"),
            "approved_at",
            v.get("approved_at")),
        "version_id=#{args.id} AND binding_status='PENDING'",
        m("id", versionId));
    db.update(
        "seal_card",
        m("current_version_id", versionId, "row_version", id(c, "row_version") + 1),
        "id=#{args.id}",
        m("id", cardId));
    if (system) audit.system("ACTIVATE", cardId, versionId, text(c, "branch_no"));
    else audit.write("ACTIVATE", cardId, versionId, null, "SUCCESS", null);
  }

  /** 到期时再次核验外部账户状态；未知/超时延后，明确失效拒绝激活。 */
  public void activateDue() {
    long cursor = 0;
    while (true) {
      List<Map<String, Object>> due =
          db.rows(
              "seal_card_version",
              "status='APPROVED' AND effective_from<=#{args.now} AND id>#{args.cursor}",
              "ORDER BY id LIMIT 200",
              m("now", now(), "cursor", cursor));
      if (due.isEmpty()) return;
      for (Map<String, Object> entry : due) {
        cursor = id(entry, "id");
        List<String> accounts = accountNos(id(entry, "id"));
        Map<String, AccountStatusCenter.AccountState> states = new LinkedHashMap<>();
        boolean confirmed = !accounts.isEmpty();
        try {
          for (String account : accounts) {
            AccountStatusCenter.AccountState state = customers.lookupAccount(account);
            if (state == null
                || !account.equals(state.accountNo)
                || state.customerNo == null
                || state.customerNo.trim().isEmpty()
                || state.branchNo == null
                || state.branchNo.trim().isEmpty()
                || !("ACTIVE".equals(state.status) || "CLOSED".equals(state.status))) {
              confirmed = false;
              break;
            }
            states.put(account, state);
          }
        } catch (RuntimeException unavailable) {
          confirmed = false;
        }
        if (!confirmed) {
          org.slf4j.LoggerFactory.getLogger(getClass())
              .warn(
                  "Scheduled activation deferred: version={}, account state unconfirmed",
                  entry.get("id"));
          continue;
        }
        tx.execute(
            s -> {
              Map<String, Object> c = db.lockCard(id(entry, "seal_card_id"));
              Map<String, Object> v = version(id(entry, "seal_card_id"), id(entry, "id"));
              if (!"ENABLED".equals(c.get("status"))
                  || !"APPROVED".equals(v.get("status"))
                  || time(v.get("effective_from")).isAfter(now())) return null;
              String rejection = null;
              for (AccountStatusCenter.AccountState state : states.values()) {
                if ("CLOSED".equals(state.status)) {
                  rejection = "预约生效重新核验失败：账户中心已确认适用账户销户";
                  break;
                }
                if (!text(c, "customer_no").equals(state.customerNo)
                    || !text(c, "branch_no").equals(state.branchNo)) {
                  rejection = "预约生效重新核验失败：账户中心已确认客户归属或管理机构迁移";
                  break;
                }
              }
              if (rejection == null) {
                activateLocked(c, v, true);
              } else {
                db.update(
                    "seal_card_version",
                    m(
                        "status",
                        "REJECTED",
                        "rejected_by",
                        "SYSTEM",
                        "rejected_at",
                        now(),
                        "reject_reason",
                        rejection),
                    "id=#{args.id}",
                    m("id", v.get("id")));
                db.update(
                    "seal_card_account",
                    m("binding_status", "REJECTED"),
                    "version_id=#{args.id} AND binding_status='PENDING'",
                    m("id", v.get("id")));
                db.insert(
                    "seal_card_audit_log",
                    m(
                        "seal_card_id",
                        c.get("id"),
                        "version_id",
                        v.get("id"),
                        "action",
                        "REJECT",
                        "operator_id",
                        "SYSTEM",
                        "branch_no",
                        c.get("branch_no"),
                        "request_id",
                        "activation-" + UUID.randomUUID(),
                        "result",
                        "SUCCESS",
                        "purpose",
                        rejection));
              }
              return null;
            });
      }
    }
  }

  public Map<String, Object> disable(long cardId, String reason) {
    RequestContext.role("CHECKER");
    tx.execute(
        s -> {
          Map<String, Object> c = card(cardId, true);
          if (!"ENABLED".equals(c.get("status"))) throw BusinessException.conflict("卡片已停用");
          LocalDateTime stamp = now();
          long current = id(c, "current_version_id");
          if (current != 0) {
            Map<String, Object> v = version(cardId, current);
            LocalDateTime from = time(v.get("effective_from"));
            if (!stamp.isAfter(from)) stamp = from.plusNanos(1000000);
            db.update(
                "seal_card_version",
                m("status", "ARCHIVED", "effective_to", stamp),
                "id=#{args.id}",
                m("id", current));
            db.update(
                "seal_card_account",
                m("binding_status", "ENDED", "effective_to", stamp),
                "seal_card_id=#{args.id} AND binding_status='ACTIVE'",
                m("id", cardId));
          }
          db.update(
              "seal_card_version",
              m("status", "FAILED"),
              "seal_card_id=#{args.id} AND status IN ('UPLOADING','PENDING_APPROVAL','APPROVED')",
              m("id", cardId));
          db.update(
              "seal_card_account",
              m("binding_status", "REJECTED"),
              "seal_card_id=#{args.id} AND binding_status='PENDING'",
              m("id", cardId));
          db.update(
              "seal_card",
              m(
                  "status",
                  "DISABLED",
                  "current_version_id",
                  null,
                  "disabled_by",
                  RequestContext.user().operatorId,
                  "disabled_at",
                  stamp,
                  "disable_reason",
                  reason,
                  "row_version",
                  id(c, "row_version") + 1),
              "id=#{args.id}",
              m("id", cardId));
          audit.write("DISABLE", cardId, null, reason, "SUCCESS", null);
          return null;
        });
    return detail(cardId);
  }

  private List<String> accountNos(long versionId) {
    List<String> out = new ArrayList<>();
    for (Map<String, Object> a :
        db.rows("seal_card_account", "version_id=#{args.id}", "ORDER BY id", m("id", versionId)))
      out.add(text(a, "account_no"));
    return out;
  }

  private Object date(Object value) {
    LocalDateTime t = time(value);
    return t == null ? null : t.toString();
  }

  public Map<String, Object> versionView(Map<String, Object> v) {
    long id = id(v, "id");
    Map<String, Object> out =
        m(
            "versionId",
            sid(v.get("id")),
            "versionNo",
            v.get("version_no"),
            "status",
            v.get("status"),
            "createdBy",
            v.get("created_by"),
            "approvedBy",
            v.get("approved_by"),
            "approvedAt",
            date(v.get("approved_at")),
            "effectiveFrom",
            date(v.get("effective_from")),
            "effectiveTo",
            date(v.get("effective_to")),
            "changeReason",
            v.get("change_reason"),
            "rejectReason",
            v.get("reject_reason"),
            "authorizationRef",
            v.get("authorization_ref"),
            "accountNos",
            accountNos(id),
            "createdAt",
            date(v.get("created_at")));
    List<Map<String, Object>> list = new ArrayList<>();
    for (Map<String, Object> image :
        db.rows("seal_card_image", "version_id=#{args.id}", "ORDER BY page_no", m("id", id)))
      list.add(
          m(
              "pageNo",
              image.get("page_no"),
              "mimeType",
              image.get("mime_type"),
              "fileSize",
              image.get("file_size"),
              "sha256",
              image.get("sha256")));
    List<Map<String, Object>> bindings = new ArrayList<>();
    for (Map<String, Object> binding : db.rows("seal_card_account", "version_id=#{args.id}", "ORDER BY id", m("id", id))) {
      bindings.add(m("accountNo", binding.get("account_no"), "bindingStatus", binding.get("binding_status"),
          "effectiveFrom", date(binding.get("effective_from")), "effectiveTo", date(binding.get("effective_to")),
          "approvedBy", binding.get("approved_by")));
    }
    out.put("bindings", bindings);
    out.put("images", list);
    return out;
  }

  private Map<String, Object> latest(long cardId) {
    List<Map<String, Object>> versions =
        db.rows(
            "seal_card_version",
            "seal_card_id=#{args.id}",
            "ORDER BY version_no DESC LIMIT 1",
            m("id", cardId));
    return versions.isEmpty() ? null : versions.get(0);
  }

  private Map<String, Object> summary(Map<String, Object> c, Map<String, Object> selected) {
    Map<String, Object> v = selected == null ? latest(id(c, "id")) : selected;
    return m(
        "cardId",
        sid(c.get("id")),
        "cardNo",
        c.get("card_no"),
        "customerNo",
        c.get("customer_no"),
        "customerName",
        customers.customerName(text(c, "customer_no")),
        "branchNo",
        c.get("branch_no"),
        "status",
        "DISABLED".equals(c.get("status")) ? "DISABLED" : v == null ? "UPLOADING" : v.get("status"),
        "currentVersionId",
        sid(c.get("current_version_id")),
        "versionId",
        v == null ? null : sid(v.get("id")),
        "versionNo",
        v == null ? null : v.get("version_no"),
        "accountNos",
        v == null ? Collections.emptyList() : accountNos(id(v, "id")),
        "createdAt",
        date(c.get("created_at")),
        "effectiveFrom",
        v == null ? null : date(v.get("effective_from")),
        "effectiveTo",
        v == null ? null : date(v.get("effective_to")));
  }

  public Map<String, Object> detail(long id) {
    activateDue();
    Map<String, Object> c = card(id, false);
    Map<String, Object> out = summary(c, null);
    out.putAll(
        m(
            "remark",
            c.get("remark"),
            "createdBy",
            c.get("created_by"),
            "rowVersion",
            c.get("row_version"),
            "disabledAt",
            date(c.get("disabled_at")),
            "disableReason",
            c.get("disable_reason")));
    List<Map<String, Object>> list = new ArrayList<>();
    for (Map<String, Object> v :
        db.rows(
            "seal_card_version",
            "seal_card_id=#{args.id}",
            "ORDER BY version_no DESC",
            m("id", id))) list.add(versionView(v));
    out.put("versions", list);
    return out;
  }

  public Map<String, Object> list(String cif, String account, String status, int page, int size) {
    RequestContext.role("MAKER", "CHECKER", "TELLER", "ADMIN");
    activateDue();
    paging(page, size);
    Identity u = RequestContext.user();
    Map<String, Object> a =
        m(
            "branch",
            u.branchNo,
            "cif",
            cif,
            "account",
            account,
            "status",
            status,
            "offset",
            (page - 1) * size,
            "limit",
            size);
    String where = u.has("ADMIN") ? "1=1" : readableBranches(u,a);
    if (cif != null && !cif.isEmpty()) where += " AND customer_no=#{args.cif}";
    if (account != null && !account.isEmpty())
      where +=
          " AND EXISTS(SELECT 1 FROM seal_card_account a WHERE a.seal_card_id=seal_card.id AND a.account_no=#{args.account})";
    if (status != null && !status.isEmpty()) {
      if ("DISABLED".equals(status)) where += " AND status='DISABLED'";
      else
        where +=
            " AND status='ENABLED' AND EXISTS(SELECT 1 FROM seal_card_version v WHERE v.seal_card_id=seal_card.id AND v.status=#{args.status})";
    }
    long total = db.count("seal_card", where, a);
    List<Map<String, Object>> out = new ArrayList<>();
    for (Map<String, Object> c :
        db.rows(
            "seal_card", where, "ORDER BY id DESC LIMIT #{args.limit} OFFSET #{args.offset}", a)) {
      Map<String, Object> selected = null;
      if (status != null && !status.isEmpty() && !"DISABLED".equals(status))
        selected =
            db.one(
                "seal_card_version",
                "seal_card_id=#{args.id} AND status=#{args.status}",
                m("id", c.get("id"), "status", status));
      out.add(summary(c, selected));
    }
    return page(out, total, page, size);
  }

  private String readableBranches(Identity identity,Map<String,Object> args) {
    List<String> branches=scopes.branches(identity);
    StringJoiner placeholders=new StringJoiner(",");
    for(int i=0;i<branches.size();i++) {
      String name="scope"+i;args.put(name,branches.get(i));placeholders.add("#{args."+name+"}");
    }
    return "branch_no IN ("+placeholders+")";
  }

  public static void paging(int page, int size) {
    if (page < 1 || page > 1000000 || size < 1 || size > 100)
      throw BusinessException.bad("分页参数超出范围");
  }

  public static Map<String, Object> page(List<?> items, long total, int page, int size) {
    return m("items", items, "total", total, "page", page, "pageSize", size);
  }

  public Map<String, Object> dashboard() {
    Identity u = RequestContext.user();
    Map<String, Object> a = m("branch", u.branchNo);
    String where = u.has("ADMIN") || u.has("AUDITOR") ? "1=1" : readableBranches(u,a);
    long cards = db.count("seal_card", where, a);
    String related = "seal_card_id IN(SELECT id FROM seal_card WHERE " + where + ")";
    return m(
        "totalCards",
        cards,
        "pendingApproval",
        db.count("seal_card_version", related + " AND status='PENDING_APPROVAL'", a),
        "activeVersions",
        db.count("seal_card_version", related + " AND status='ACTIVE'", a),
        "failedTasks",
        db.count(
            "seal_upload_task",
            (u.has("ADMIN") ? "1=1" : readableBranches(u,a)) + " AND task_status='FAILED'",
            a),
        "recentAudits",
        recentAudits(u));
  }

  private List<Map<String, Object>> recentAudits(Identity user) {
    List<Map<String, Object>> result = new ArrayList<>();
    Map<String,Object> args=m();
    String visible=user.has("AUDITOR")?"1=1":readableBranches(user,args);
    for (Map<String, Object> a :
        db.rows(
            "seal_card_audit_log",
            visible,
            "ORDER BY id DESC LIMIT 8",
            args))
      result.add(
          m(
              "id",
              sid(a.get("id")),
              "action",
              a.get("action"),
              "operatorId",
              a.get("operator_id"),
              "branchNo",
              a.get("branch_no"),
              "result",
              a.get("result"),
              "createdAt",
              date(a.get("created_at"))));
    return result;
  }

  public Map<String, Object> audits(
      Long cardId, String action, String result, String requestId, int page, int size) {
    RequestContext.role("AUDITOR");
    paging(page, size);
    Map<String, Object> a =
        m(
            "card",
            cardId,
            "action",
            action,
            "result",
            result,
            "request",
            requestId,
            "limit",
            size,
            "offset",
            (page - 1) * size);
    String where = "1=1";
    if (cardId != null) where += " AND seal_card_id=#{args.card}";
    if (action != null && !action.isEmpty()) where += " AND action=#{args.action}";
    if (result != null && !result.isEmpty()) where += " AND result=#{args.result}";
    if (requestId != null && !requestId.isEmpty()) where += " AND request_id=#{args.request}";
    List<Map<String, Object>> out = new ArrayList<>();
    for (Map<String, Object> row :
        db.rows(
            "seal_card_audit_log",
            where,
            "ORDER BY id DESC LIMIT #{args.limit} OFFSET #{args.offset}",
            a))
      out.add(
          m(
              "id",
              sid(row.get("id")),
              "sealCardId",
              sid(row.get("seal_card_id")),
              "versionId",
              sid(row.get("version_id")),
              "action",
              row.get("action"),
              "operatorId",
              row.get("operator_id"),
              "branchNo",
              row.get("branch_no"),
              "requestId",
              row.get("request_id"),
              "result",
              row.get("result"),
              "errorCode",
              row.get("error_code"),
              "purpose",
              row.get("purpose"),
              "createdAt",
              date(row.get("created_at"))));
    return page(out, db.count("seal_card_audit_log", where, a), page, size);
  }

  public Map<String, Object> transactions(
      String cif, String account, String type, LocalDate date, String purpose) {
    RequestContext.role("TELLER");
    if (cif == null
        || account == null
        || purpose == null
        || purpose.trim().isEmpty()
        || purpose.length() > 128) throw BusinessException.bad("交易查询须填写客户、付款账号和业务用途");
    if (!"PAYMENT".equals(type) && !"CHECK".equals(type))
      throw BusinessException.bad("未接入该交易类型的适用规则");
    if (date != null && !"CHECK".equals(type)) throw BusinessException.bad("只有票据流程允许按签发日期查询历史");
    if (date != null && date.isAfter(now().toLocalDate())) throw BusinessException.bad("签发日期不能在未来");
    if (date == null) {
      Map<String, Object> cust = customers.validate(cif, Collections.singletonList(account), null);
      if (!scopes.canRead(RequestContext.user(),text(cust,"branchNo")))
        throw BusinessException.forbidden();
    } else {
      // 历史CHECK只能按当时已复核的卡片/账户关系区间与原机构授权查询。
      // 当前销户/迁移不能抹去旧审批快照；中心不可用时仍拒绝，不静默降级。
      customers.customer(cif);
    }
    activateDue();
    LocalDateTime point = now(),
        start = date == null ? null : date.atStartOfDay(),
        end = date == null ? null : date.plusDays(1).atStartOfDay();
    List<Map<String, Object>> out = new ArrayList<>();
    Map<String,Object> visibleArgs=m("cif",cif);
    String visible=readableBranches(RequestContext.user(),visibleArgs);
    for (Map<String, Object> c :
        db.rows(
            "seal_card",
            "customer_no=#{args.cif} AND "+visible,
            visibleArgs)) {
      if (date == null && !"ENABLED".equals(c.get("status"))) continue;
      String interval =
          date == null
              ? "effective_from<=#{args.point} AND (effective_to IS NULL OR effective_to>#{args.point})"
              : "effective_from<#{args.end} AND (effective_to IS NULL OR effective_to>#{args.start})";
      Map<String, Object> args = m("card", c.get("id"), "point", point, "start", start, "end", end);
      for (Map<String, Object> v :
          db.rows(
              "seal_card_version",
              "seal_card_id=#{args.card} AND status IN ('ACTIVE','ARCHIVED') AND " + interval,
              args)) {
        if (date == null && id(c, "current_version_id") != id(v, "id")) continue;
        args.put("version", v.get("id"));
        args.put("account", account);
        if (db.count(
                "seal_card_account",
                "version_id=#{args.version} AND account_no=#{args.account} AND binding_status IN ('ACTIVE','ENDED') AND "
                    + interval,
                args)
            > 0) out.add(summary(c, v));
      }
    }
    return m(
        "items",
        out,
        "total",
        out.size(),
        "ambiguous",
        out.size() > 1,
        "selectionNotice",
        date == null
            ? "返回候选资料，不执行自动验印；签章组合及票据有效性须由银行业务流程确认"
            : "签发日期只有天精度，返回该日与生效区间相交的全部候选，不能自动认定其中一版；须按银行制度核实实际签发时刻和适用组合");
  }

  public Map<String, Object> image(
      long cardId,
      long versionId,
      int page,
      String account,
      String type,
      LocalDate date,
      String purpose) {
    Map<String, Object> c = card(cardId, false);
    Map<String, Object> v = version(cardId, versionId);
    Identity u = RequestContext.user();
    if (u.has("ADMIN") || u.has("AUDITOR")) throw BusinessException.forbidden();
    if (u.has("MAKER")) {
      if (!u.operatorId.equals(v.get("created_by"))) throw BusinessException.forbidden();
    } else if (u.has("CHECKER")) {
    } else if (u.has("TELLER")) {
      Map<String, Object> matches =
          transactions(text(c, "customer_no"), account, type, date, purpose);
      boolean valid = false;
      for (Object o : (List<?>) matches.get("items")) {
        Map<?, ?> item = (Map<?, ?>) o;
        if (sid(v.get("id")).equals(item.get("versionId"))) valid = true;
      }
      if (!valid) throw new BusinessException(403, "IMAGE_NOT_APPLICABLE", "该影像不适用于当前交易上下文");
    } else throw BusinessException.forbidden();
    Map<String, Object> image =
        db.one(
            "seal_card_image",
            "version_id=#{args.version} AND page_no=#{args.page} AND storage_status='READY'",
            m("version", versionId, "page", page));
    if (image == null) throw BusinessException.missing();
    return image;
  }
}
