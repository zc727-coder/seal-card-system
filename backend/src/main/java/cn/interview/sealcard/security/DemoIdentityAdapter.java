package cn.interview.sealcard.security;

import cn.interview.sealcard.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 显式演示认证适配器。生产替换为银行统一认证，不是用户管理系统。 */
@Component
public class DemoIdentityAdapter {
  private final Map<String, Identity> users = new LinkedHashMap<>();
  private final byte[] password;
  private final ScopeAuthorization scopes;

  public DemoIdentityAdapter(@Value("${seal.demo-password}") String password, ScopeAuthorization scopes) {
    this.scopes = scopes;
    if (password.length() < 8) throw new IllegalStateException("DEMO_PASSWORD至少8位");
    this.password = password.getBytes(StandardCharsets.UTF_8);
    add("maker01", "录入柜员", "B001", "MAKER");
    add("checker01", "复核主管", "B001", "CHECKER");
    add("teller01", "交易柜员", "B001", "TELLER");
    add("auditor01", "审计人员", "B001", "AUDITOR");
    add("admin01", "系统管理员", "B001", "ADMIN");
    add("maker02", "分行录入柜员", "B002", "MAKER");
    add("checker02", "分行复核主管", "B002", "CHECKER");
  }

  private void add(String id, String name, String branch, String role) {
    users.put(id, new Identity(id, name, branch, role));
  }

  public Identity find(String id) {
    Identity user = users.get(id);
    return user == null ? null : scopes.snapshot(user);
  }

  public Identity login(String user, String pass) {
    if (pass == null
        || !MessageDigest.isEqual(password, pass.getBytes(StandardCharsets.UTF_8))
        || !users.containsKey(user))
      throw new BusinessException(401, "INVALID_CREDENTIALS", "账号或密码错误");
    return find(user);
  }

  public List<Map<String, Object>> list() {
    List<Map<String, Object>> list = new ArrayList<>();
    for (Identity i : users.values()) {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("username", i.operatorId);
      m.put("displayName", i.displayName);
      m.put("branchNo", i.branchNo);
      m.put("roles", i.roles);
      m.put("queryBranchNos", scopes.branches(i));
      list.add(m);
    }
    return list;
  }
}
