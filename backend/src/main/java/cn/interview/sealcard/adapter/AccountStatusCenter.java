package cn.interview.sealcard.adapter;

/**
 * 可信账户中心的当前状态查询边界。
 *
 * <p>实现必须对网络调用设置超时；查询失败、数据缺失或未知状态均不能冒充CLOSED确认。 本系统不维护核心账户主数据，也不通过此接口批准新的印鉴适用范围。
 */
public interface AccountStatusCenter {
  AccountState lookupAccount(String accountNo);

  final class AccountState {
    public final String accountNo;
    public final String customerNo;
    public final String branchNo;
    public final String status;

    public AccountState(String accountNo, String customerNo, String branchNo, String status) {
      this.accountNo = accountNo;
      this.customerNo = customerNo;
      this.branchNo = branchNo;
      this.status = status;
    }
  }
}
