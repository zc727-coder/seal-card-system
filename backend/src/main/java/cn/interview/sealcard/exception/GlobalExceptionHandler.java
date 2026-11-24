package cn.interview.sealcard.exception;

import cn.interview.sealcard.response.ApiResponse;
import org.slf4j.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private final cn.interview.sealcard.service.AuditService audit;

  public GlobalExceptionHandler(cn.interview.sealcard.service.AuditService audit) {
    this.audit = audit;
  }

  private void invalidAudit(String code) {
    try {
      audit.failure("INVALID_REQUEST", null, code);
    } catch (Exception ignored) {
      log.error("Request validation audit unavailable");
    }
  }

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Object>> validation(
      org.springframework.web.bind.MethodArgumentNotValidException e) {
    invalidAudit("INVALID_REQUEST");
    java.util.Map<String, String> fields = new java.util.LinkedHashMap<>();
    e.getBindingResult()
        .getFieldErrors()
        .forEach(f -> fields.put(f.getField(), f.getDefaultMessage()));
    return ResponseEntity.badRequest()
        .body(
            new ApiResponse<>(
                "INVALID_REQUEST",
                "参数校验失败",
                cn.interview.sealcard.service.BusinessStore.m("fieldErrors", fields)));
  }

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResponse<Object>> business(BusinessException e) {
    return ResponseEntity.status(e.getStatus())
        .body(new ApiResponse<>(e.getCode(), e.getMessage(), null));
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ApiResponse<Object>> large(Exception e) {
    invalidAudit("FILE_TOO_LARGE");
    return ResponseEntity.status(413).body(new ApiResponse<>("FILE_TOO_LARGE", "上传内容超过允许大小", null));
  }

  @ExceptionHandler({
    IllegalArgumentException.class,
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
    HttpMessageNotReadableException.class,
    org.springframework.web.bind.MissingServletRequestParameterException.class,
    org.springframework.web.multipart.support.MissingServletRequestPartException.class
  })
  public ResponseEntity<ApiResponse<Object>> invalid(Exception e) {
    invalidAudit("INVALID_REQUEST");
    return ResponseEntity.badRequest()
        .body(new ApiResponse<>("INVALID_REQUEST", "请求参数或数据格式错误", null));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiResponse<Object>> conflict(Exception e) {
    return ResponseEntity.status(409)
        .body(new ApiResponse<>("DATA_CONFLICT", "数据状态冲突，请刷新后重试", null));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Object>> unknown(Exception e) {
    log.error("Request failed: {}", e.getClass().getSimpleName());
    return ResponseEntity.status(500)
        .body(new ApiResponse<>("INTERNAL_ERROR", "处理失败，请凭请求号联系管理员", null));
  }
}
