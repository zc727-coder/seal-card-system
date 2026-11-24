package cn.interview.sealcard.service;

import cn.interview.sealcard.exception.BusinessException;
import org.aspectj.lang.*;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.*;

@Aspect
@Component
public class OperationAuditAspect {
  private final AuditService audit;

  public OperationAuditAspect(AuditService audit) {
    this.audit = audit;
  }

  @Around("@annotation(action)")
  public Object around(ProceedingJoinPoint p, AuditAction action) throws Throwable {
    Long card = null;
    ServletRequestAttributes a =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (a != null) {
      String uri = a.getRequest().getRequestURI();
      java.util.regex.Matcher m =
          java.util.regex.Pattern.compile("/seal-cards/(\\d+)").matcher(uri);
      if (m.find()) card = Long.valueOf(m.group(1));
    }
    try {
      Object value = p.proceed();
      if (action.success()) audit.write(action.value(), card, null, null, "SUCCESS", null);
      return value;
    } catch (Throwable e) {
      try {
        audit.failure(
            action.value(),
            card,
            e instanceof BusinessException
                ? ((BusinessException) e).getCode()
                : "OPERATION_FAILED");
      } catch (Exception ignored) {
        org.slf4j.LoggerFactory.getLogger(getClass())
            .error(
                "Audit persistence unavailable for request {}",
                cn.interview.sealcard.security.RequestContext.requestId());
      }
      throw e;
    }
  }
}
