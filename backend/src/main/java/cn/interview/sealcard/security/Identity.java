package cn.interview.sealcard.security;

import java.util.*;

public class Identity {
  public String operatorId, displayName, branchNo;
  public List<String> roles;
  /** 服务端计算的只读查询范围展示，不作为客户端提交的授权依据。 */
  public List<String> queryBranchNos;

  public Identity() {}

  public Identity(String id, String name, String branch, String role) {
    operatorId = id;
    displayName = name;
    branchNo = branch;
    roles = Collections.singletonList(role);
  }

  public boolean has(String role) {
    return roles != null && roles.contains(role);
  }
}
