package cn.interview.sealcard.service;

import static cn.interview.sealcard.service.BusinessStore.m;

import cn.interview.sealcard.security.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AuditService {
  private final BusinessStore db;
  private final TransactionTemplate independent;

  public AuditService(BusinessStore db, PlatformTransactionManager tx) {
    this.db = db;
    independent = new TransactionTemplate(tx);
    independent.setPropagationBehavior(3);
  }

  public void write(
      String action, Long card, Long version, String purpose, String result, String error) {
    Identity u = RequestContext.optional();
    db.insert(
        "seal_card_audit_log",
        m(
            "seal_card_id",
            card,
            "version_id",
            version,
            "action",
            action,
            "operator_id",
            u == null ? "anonymous" : u.operatorId,
            "branch_no",
            u == null ? "UNKNOWN" : u.branchNo,
            "request_id",
            RequestContext.requestId(),
            "purpose",
            purpose == null ? null : purpose.substring(0, Math.min(128, purpose.length())),
            "result",
            result,
            "error_code",
            error));
  }

  public void system(String action, Long card, Long version, String branch) {
    db.insert(
        "seal_card_audit_log",
        m(
            "seal_card_id",
            card,
            "version_id",
            version,
            "action",
            action,
            "operator_id",
            "SYSTEM",
            "branch_no",
            branch,
            "request_id",
            "job-" + java.util.UUID.randomUUID(),
            "result",
            "SUCCESS"));
  }

  public void failure(String action, Long card, String code) {
    independent.execute(
        s -> {
          write(action, card, null, null, "FAIL", code);
          return null;
        });
  }
}
