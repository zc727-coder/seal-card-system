package cn.interview.sealcard.controller;

import cn.interview.sealcard.response.ApiResponse;
import cn.interview.sealcard.security.*;
import io.swagger.annotations.*;
import java.util.*;
import javax.servlet.http.*;
import org.springframework.web.bind.annotation.*;

@Api(tags = "演示身份认证")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private final DemoIdentityAdapter users;
  private final SessionTokens tokens;

  public AuthController(DemoIdentityAdapter users, SessionTokens tokens) {
    this.users = users;
    this.tokens = tokens;
  }

  @ApiOperation("演示账号登录；生产需替换银行认证")
  @PostMapping("/login")
  @cn.interview.sealcard.service.AuditAction("LOGIN")
  public ApiResponse<Identity> login(
      @RequestBody Map<String, String> body, HttpServletRequest req, HttpServletResponse res) {
    Identity i;
    try {
      i = users.login(body.get("username"), body.get("password"));
    } catch (cn.interview.sealcard.exception.BusinessException failure) {
      // 认证失败按匿名身份审计。
      RequestContext.set(null, RequestContext.requestId());
      throw failure;
    }
    // 记录已认证身份。
    RequestContext.set(i, RequestContext.requestId());
    cookie(res, tokens.issue(i), tokens.ttl(), req.isSecure());
    return ApiResponse.ok(i);
  }

  @ApiOperation("当前可信身份")
  @GetMapping("/me")
  public ApiResponse<Identity> me() {
    return ApiResponse.ok(RequestContext.user());
  }

  @ApiOperation("退出当前浏览器会话")
  @PostMapping("/logout")
  public ApiResponse<Object> logout(HttpServletRequest req, HttpServletResponse res) {
    cookie(res, "", 0, req.isSecure());
    return ApiResponse.ok(null);
  }

  @ApiOperation("演示身份清单，不含密码")
  @GetMapping("/demo-users")
  public ApiResponse<?> demo() {
    return ApiResponse.ok(users.list());
  }

  private void cookie(HttpServletResponse res, String token, long age, boolean secure) {
    res.addHeader(
        "Set-Cookie",
        "SEAL_SESSION="
            + token
            + "; Path=/; HttpOnly; SameSite=Strict; Max-Age="
            + age
            + (secure ? "; Secure" : ""));
  }
}
