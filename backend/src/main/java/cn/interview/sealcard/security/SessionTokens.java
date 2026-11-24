package cn.interview.sealcard.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SessionTokens {
  private final byte[] secret;
  private final long seconds;
  private final DemoIdentityAdapter users;

  public SessionTokens(
      @Value("${seal.auth-secret}") String secret,
      @Value("${seal.session-seconds:28800}") long seconds,
      DemoIdentityAdapter users) {
    if (secret.length() < 32) throw new IllegalStateException("APP_AUTH_SECRET至少32位");
    this.secret = secret.getBytes(StandardCharsets.UTF_8);
    this.seconds = seconds;
    this.users = users;
  }

  private byte[] sign(String body) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret, "HmacSHA256"));
    return mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
  }

  public String issue(Identity i) {
    try {
      String body =
          Base64.getUrlEncoder()
              .withoutPadding()
              .encodeToString(
                  (i.operatorId
                          + ":"
                          + (System.currentTimeMillis() / 1000 + seconds)
                          + ":"
                          + UUID.randomUUID())
                      .getBytes(StandardCharsets.UTF_8));
      return body + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(sign(body));
    } catch (Exception e) {
      throw new IllegalStateException("认证签名失败");
    }
  }

  public Identity verify(String token) {
    try {
      if (token == null || token.length() > 512) return null;
      String[] p = token.split("\\.");
      if (p.length != 2 || !MessageDigest.isEqual(sign(p[0]), Base64.getUrlDecoder().decode(p[1])))
        return null;
      String[] data =
          new String(Base64.getUrlDecoder().decode(p[0]), StandardCharsets.UTF_8).split(":");
      if (data.length != 3 || Long.parseLong(data[1]) <= System.currentTimeMillis() / 1000)
        return null;
      return users.find(data[0]);
    } catch (Exception e) {
      return null;
    }
  }

  public long ttl() {
    return seconds;
  }
}
