package cn.interview.sealcard.service;

import static cn.interview.sealcard.service.BusinessStore.*;
import static cn.interview.sealcard.service.SealBusinessService.now;
import static cn.interview.sealcard.service.SealBusinessService.time;

import cn.interview.sealcard.adapter.AccountStatusCenter;
import cn.interview.sealcard.adapter.AccountStatusCenter.AccountState;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 外部账户状态与当前适用关系的对账。
 *
 * <p>仅终止经可信中心确认已销户/迁移的当前关系；不删除历史、不启用新范围、不改变版本审批。 多实例先锁同一卡片，再条件更新ACTIVE关系，只有更新成功者追加审计。
 */
@Service
public class AccountBindingReconciler {
  private static final Logger log = LoggerFactory.getLogger(AccountBindingReconciler.class);
  private final AccountStatusCenter center;
  private final BusinessStore db;
  private final TransactionTemplate tx;

  public AccountBindingReconciler(
      AccountStatusCenter center, BusinessStore db, PlatformTransactionManager manager) {
    this.center = center;
    this.db = db;
    this.tx = new TransactionTemplate(manager);
  }

  /** 返回本轮实际终止的关系数；依赖异常时保留原关系，由交易侧实时校验拒绝使用。 */
  public int reconcile() {
    long cursor = 0;
    int ended = 0;
    while (true) {
      List<Map<String, Object>> batch =
          db.rows(
              "seal_card_account",
              "binding_status='ACTIVE' AND id>#{args.cursor}",
              "ORDER BY id LIMIT 200",
              m("cursor", cursor));
      if (batch.isEmpty()) return ended;
      for (Map<String, Object> candidate : batch) {
        cursor = id(candidate, "id");
        final AccountState confirmed;
        try {
          // 网络查询在数据库事务之外；未得到权威确认时绝不终止关系。
          confirmed = center.lookupAccount(text(candidate, "account_no"));
        } catch (RuntimeException unavailable) {
          log.warn(
              "Account reconciliation deferred: binding={}, reason={}",
              cursor,
              unavailable.getClass().getSimpleName());
          continue;
        }
        if (!validConfirmation(candidate, confirmed)) continue;
        Boolean changed = tx.execute(status -> endIfConfirmed(candidate, confirmed));
        if (Boolean.TRUE.equals(changed)) ended++;
      }
    }
  }

  private boolean validConfirmation(Map<String, Object> candidate, AccountState state) {
    return state != null
        && text(candidate, "account_no").equals(state.accountNo)
        && state.customerNo != null
        && !state.customerNo.trim().isEmpty()
        && state.branchNo != null
        && !state.branchNo.trim().isEmpty()
        && ("ACTIVE".equals(state.status) || "CLOSED".equals(state.status));
  }

  private boolean endIfConfirmed(Map<String, Object> candidate, AccountState confirmed) {
    Map<String, Object> card = db.lockCard(id(candidate, "seal_card_id"));
    if (card == null) return false;
    Map<String, Object> current =
        db.one("seal_card_account", "id=#{args.id}", m("id", candidate.get("id")));
    if (current == null || !"ACTIVE".equals(current.get("binding_status"))) return false;
    if (!text(current, "account_no").equals(confirmed.accountNo)) return false;
    boolean closed = "CLOSED".equals(confirmed.status);
    boolean moved =
        !text(card, "customer_no").equals(confirmed.customerNo)
            || !text(card, "branch_no").equals(confirmed.branchNo);
    if (!closed && !moved) return false;

    // 以实际核对确认时刻终止当前关系，不倒写已归档版本的历史有效区间。
    LocalDateTime end = now();
    LocalDateTime from = time(current.get("effective_from"));
    if (from == null || !end.isAfter(from)) return false;
    LocalDateTime previousEnd = time(current.get("effective_to"));
    if (previousEnd != null && !previousEnd.isAfter(end)) return false;
    int changed =
        db.update(
            "seal_card_account",
            m("binding_status", "ENDED", "effective_to", end),
            "id=#{args.id} AND binding_status='ACTIVE' AND (effective_to IS NULL OR effective_to>#{args.end})",
            m("id", current.get("id"), "end", end));
    if (changed != 1) return false;
    db.insert(
        "seal_card_audit_log",
        m(
            "seal_card_id",
            card.get("id"),
            "version_id",
            current.get("version_id"),
            "target_ref",
            "binding:" + current.get("id"),
            "action",
            "UNBIND",
            "operator_id",
            "SYSTEM",
            "branch_no",
            card.get("branch_no"),
            "request_id",
            "account-sync-" + UUID.randomUUID(),
            "result",
            "SUCCESS",
            "purpose",
            closed ? "账户中心确认销户，终止当前适用关系" : "账户中心确认归属或机构迁移，终止原适用关系"));
    return true;
  }
}
