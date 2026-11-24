package cn.interview.sealcard.exception;

public class BusinessException extends RuntimeException {
  private final int status;
  private final String code;

  public BusinessException(int status, String code, String message) {
    super(message);
    this.status = status;
    this.code = code;
  }

  public int getStatus() {
    return status;
  }

  public String getCode() {
    return code;
  }

  public static BusinessException bad(String message) {
    return new BusinessException(400, "INVALID_REQUEST", message);
  }

  public static BusinessException forbidden() {
    return new BusinessException(403, "ACCESS_DENIED", "无权执行此操作");
  }

  public static BusinessException missing() {
    return new BusinessException(404, "NOT_FOUND", "目标不存在");
  }

  public static BusinessException conflict(String message) {
    return new BusinessException(409, "STATE_CONFLICT", message);
  }
}
