package cn.interview.sealcard.job;

import cn.interview.sealcard.service.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = "seal.scheduling-enabled",
    havingValue = "true",
    matchIfMissing = true)
public class CompensationJob {
  private final UploadTaskWorker worker;
  private final SealBusinessService seals;
  private final AccountBindingReconciler accountBindings;

  public CompensationJob(
      UploadTaskWorker worker,
      SealBusinessService seals,
      AccountBindingReconciler accountBindings) {
    this.worker = worker;
    this.seals = seals;
    this.accountBindings = accountBindings;
  }

  @Scheduled(fixedDelay = 1000)
  public void recover() {
    try {
      worker.recover();
      seals.activateDue();
    } catch (Exception e) {
      org.slf4j.LoggerFactory.getLogger(getClass())
          .warn("Compensation cycle failed: {}", e.getClass().getSimpleName());
    }
  }

  @Scheduled(fixedDelay = 3600000, initialDelay = 60000)
  public void reconcile() {
    worker.cleanup();
  }

  @Scheduled(fixedDelayString = "${seal.account-reconcile-millis:30000}")
  public void reconcileAccounts() {
    try {
      accountBindings.reconcile();
    } catch (Exception e) {
      org.slf4j.LoggerFactory.getLogger(getClass())
          .warn("Account reconciliation failed: {}", e.getClass().getSimpleName());
    }
  }
}
