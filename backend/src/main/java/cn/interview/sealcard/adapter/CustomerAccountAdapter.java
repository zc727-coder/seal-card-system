package cn.interview.sealcard.adapter;

import static cn.interview.sealcard.service.BusinessStore.*;

import cn.interview.sealcard.exception.BusinessException;
import cn.interview.sealcard.security.*;
import java.util.*;
import org.springframework.stereotype.Component;

/** 显式模拟账户中心；不在业务库重复保存主数据。 */
@Component
public class CustomerAccountAdapter implements AccountStatusCenter {
  private volatile boolean available = true;
  private final Map<String, AccountState> overrides =
      new java.util.concurrent.ConcurrentHashMap<>();

  public void setAvailableForTest(boolean value) {
    available = value;
  }

  /** 仅供独立集成测试模拟可信账户中心变更，不通过HTTP暴露。 */
  public void setAccountStateForTest(
      String accountNo, String customerNo, String branchNo, String status) {
    overrides.put(accountNo, new AccountState(accountNo, customerNo, branchNo, status));
  }

  public void resetAccountStatesForTest() {
    overrides.clear();
    available = true;
  }

  @Override
  public AccountState lookupAccount(String accountNo) {
    if (!available) throw new BusinessException(503, "ACCOUNT_CENTER_UNAVAILABLE", "客户账户中心暂不可用");
    AccountState override = overrides.get(accountNo);
    if (override != null) return override;
    if ("ACC00000123".equals(accountNo) || "ACC00000124".equals(accountNo))
      return new AccountState(accountNo, "C10001", "B001", "ACTIVE");
    if ("ACC00000125".equals(accountNo))
      return new AccountState(accountNo, "C10001", "B001", "CLOSED");
    if ("ACC00000223".equals(accountNo))
      return new AccountState(accountNo, "C10002", "B001", "ACTIVE");
    if ("ACC00000323".equals(accountNo))
      return new AccountState(accountNo, "C20001", "B002", "ACTIVE");
    throw new BusinessException(404, "ACCOUNT_NOT_FOUND", "账户中心未确认该单位结算账户");
  }

  public Map<String, Object> customer(String cif) {
    if (!available) throw new BusinessException(503, "ACCOUNT_CENTER_UNAVAILABLE", "客户账户中心暂不可用");
    String name, branch;
    List<Map<String, Object>> accounts = new ArrayList<>();
    if ("C10001".equals(cif)) {
      name = "示例科技有限公司";
      branch = "B001";
      accounts.add(account("ACC00000123", name, "ACTIVE"));
      accounts.add(account("ACC00000124", name, "ACTIVE"));
      accounts.add(account("ACC00000125", name, "CLOSED"));
    } else if ("C10002".equals(cif)) {
      name = "示例商贸有限公司";
      branch = "B001";
      accounts.add(account("ACC00000223", name, "ACTIVE"));
    } else if ("C20001".equals(cif)) {
      name = "分行示例公司";
      branch = "B002";
      accounts.add(account("ACC00000323", name, "ACTIVE"));
    } else throw new BusinessException(404, "CUSTOMER_NOT_FOUND", "客户号不存在于模拟客户中心");
    return m(
        "customerNo",
        cif,
        "customerName",
        name,
        "branchNo",
        branch,
        "status",
        "ACTIVE",
        "accounts",
        accounts);
  }

  private Map<String, Object> account(String number, String name, String status) {
    AccountState current = lookupAccount(number);
    return m("accountNo", number, "accountName", name, "status", current.status);
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> validate(String cif, List<String> numbers, String authorization) {
    Map<String, Object> c = customer(cif);
    if (numbers == null || numbers.isEmpty() || numbers.size() > 20)
      throw BusinessException.bad("请选择1至20个单位结算账户");
    if (new HashSet<>(numbers).size() != numbers.size()) throw BusinessException.bad("账户不能重复");
    if (numbers.size() > 1 && (authorization == null || authorization.trim().isEmpty()))
      throw BusinessException.bad("共用印鉴须填写授权依据");
    for (String n : numbers) {
      boolean found = false;
      for (Map<String, Object> a : (List<Map<String, Object>>) c.get("accounts"))
        if (n.equals(a.get("accountNo"))) {
          found = true;
          AccountState state = lookupAccount(n);
          if (!cif.equals(state.customerNo))
            throw new BusinessException(400, "ACCOUNT_OWNER_MISMATCH", "账户当前归属与申请客户不一致");
          if (!c.get("branchNo").equals(state.branchNo))
            throw new BusinessException(409, "ACCOUNT_BRANCH_CHANGED", "账户管理机构已迁移，请重新核实适用范围");
          if (!"ACTIVE".equals(state.status))
            throw new BusinessException(409, "ACCOUNT_INACTIVE", "单位结算账户已销户或不可用");
        }
      if (!found) throw new BusinessException(400, "ACCOUNT_OWNER_MISMATCH", "结算账户不属于该客户");
    }
    return c;
  }

  public String customerName(String cif) {
    return text(customer(cif), "customerName");
  }
}
