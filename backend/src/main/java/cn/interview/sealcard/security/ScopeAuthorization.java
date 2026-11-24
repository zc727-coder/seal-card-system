package cn.interview.sealcard.security;

import cn.interview.sealcard.exception.BusinessException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 当前查询/调阅机构权限策略。仅TELLER可以从受信任配置获得额外读取范围。
 * 写操作仍必须按身份原机构校验；ADMIN/AUDITOR不会因这里的配置自动获得读图权。
 */
@Component
public class ScopeAuthorization {
  private final QueryScopeProperties properties;

  public ScopeAuthorization(QueryScopeProperties properties) {
    this.properties = properties;
  }

  public List<String> branches(Identity identity) {
    Set<String> result = new LinkedHashSet<>();
    result.add(identity.branchNo);
    if (identity.has("TELLER") && !identity.has("ADMIN") && !identity.has("AUDITOR")) {
      List<String> configured = properties.getQueryScopes().get(identity.operatorId);
      if (configured != null) {
        for (String branch : configured) {
          if (branch == null || !branch.matches("[a-zA-Z0-9_-]{1,32}")) {
            throw new IllegalStateException("Invalid trusted query-scope configuration");
          }
          result.add(branch);
        }
      }
    }
    return new ArrayList<>(result);
  }

  public boolean canRead(Identity identity, String branch) {
    return branches(identity).contains(branch);
  }

  public Identity snapshot(Identity source) {
    Identity result = new Identity();
    result.operatorId = source.operatorId;
    result.displayName = source.displayName;
    result.branchNo = source.branchNo;
    result.roles = new ArrayList<>(source.roles);
    result.queryBranchNos = branches(source);
    return result;
  }
}
