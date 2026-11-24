package cn.interview.sealcard.security;

import cn.interview.sealcard.exception.BusinessException;

public final class RequestContext {
  private static final ThreadLocal<Identity> USER = new ThreadLocal<>();
  private static final ThreadLocal<String> ID = new ThreadLocal<>();

  public static void set(Identity identity, String id) {
    USER.set(identity);
    ID.set(id);
  }

  public static Identity user() {
    Identity i = USER.get();
    if (i == null) throw new BusinessException(401, "UNAUTHENTICATED", "请先登录");
    return i;
  }

  public static Identity optional() {
    return USER.get();
  }

  public static String requestId() {
    return ID.get() == null ? "background" : ID.get();
  }

  public static void clear() {
    USER.remove();
    ID.remove();
  }

  public static void role(String... roles) {
    Identity i = user();
    for (String r : roles) if (i.has(r)) return;
    throw BusinessException.forbidden();
  }
}
