package cn.interview.sealcard.security;

import cn.interview.sealcard.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.net.URI;
import java.util.UUID;
import javax.servlet.*;
import javax.servlet.http.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(1)
public class AuthenticationFilter extends OncePerRequestFilter {
  private final SessionTokens tokens;
  private final ObjectMapper json;
  private final cn.interview.sealcard.service.AuditService audit;

  public AuthenticationFilter(
      SessionTokens tokens, ObjectMapper json, cn.interview.sealcard.service.AuditService audit) {
    this.tokens = tokens;
    this.json = json;
    this.audit = audit;
  }

  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    String id = UUID.randomUUID().toString();
    String path = req.getServletPath();
    String token = null;
    if (req.getCookies() != null)
      for (Cookie c : req.getCookies())
        if ("SEAL_SESSION".equals(c.getName())) token = c.getValue();
    RequestContext.set(tokens.verify(token), id);
    res.setHeader("X-Request-Id", id);
    res.setHeader("X-Content-Type-Options", "nosniff");
    try {
      if (path.startsWith("/api/v1/")
          && !"GET".equals(req.getMethod())
          && !"HEAD".equals(req.getMethod())
          && !sameOrigin(req)) {
        deny(res, 403, "ORIGIN_REJECTED", "请求来源不匹配");
        return;
      }
      boolean open =
          path.equals("/api/v1/auth/login")
              || path.equals("/api/v1/auth/demo-users")
              || path.equals("/api/v1/system/status");
      if (path.startsWith("/api/v1/") && !open && RequestContext.optional() == null) {
        deny(res, 401, "UNAUTHENTICATED", "请先登录");
        return;
      }
      chain.doFilter(req, res);
    } finally {
      RequestContext.clear();
    }
  }

  private boolean sameOrigin(HttpServletRequest r) {
    String origin = r.getHeader("Origin");
    if (origin == null) origin = r.getHeader("Referer");
    if (origin == null) return true;
    try {
      URI u = URI.create(origin);
      String host = r.getHeader("Host");
      return ("http".equals(u.getScheme()) || "https".equals(u.getScheme()))
          && u.getRawAuthority() != null
          && u.getRawAuthority().equalsIgnoreCase(host);
    } catch (Exception e) {
      return false;
    }
  }

  private void deny(HttpServletResponse res, int status, String code, String msg)
      throws IOException {
    try {
      audit.failure("ACCESS_DENIED", null, code);
    } catch (RuntimeException failure) {
      org.slf4j.LoggerFactory.getLogger(getClass())
          .error("Access denial audit unavailable for request {}", RequestContext.requestId());
    }
    res.setStatus(status);
    res.setContentType("application/json;charset=UTF-8");
    json.writeValue(res.getWriter(), new ApiResponse<>(code, msg, null));
  }
}
