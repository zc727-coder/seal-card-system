package cn.interview.sealcard.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;

/** Swagger响应契约。描述并转换公开传输字段，不暴露数据库列、影像对象键或任务恢复清单。 */
public final class ApiModels {
  private ApiModels() {}
  private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = new com.fasterxml.jackson.databind.ObjectMapper();
  public static <T> ApiResponse<T> response(Object source, Class<T> type) {
    return ApiResponse.ok(MAPPER.convertValue(source, type));
  }

  @ApiModel(description = "印鉴候选摘要；CIF检索结果不能单独作为付款验印依据")
  public static class CardSummary {
    @ApiModelProperty(value = "内部卡片ID，使用十进制字符串避免JS精度损失", example = "1001")
    public String cardId;
    @ApiModelProperty("服务端生成的印鉴卡业务编号") public String cardNo;
    @ApiModelProperty("客户中心CIF") public String customerNo;
    @ApiModelProperty("客户中心单位名称") public String customerName;
    @ApiModelProperty("卡片管理机构") public String branchNo;
    @ApiModelProperty("展示版本状态；卡片停用时固定DISABLED") public String status;
    @ApiModelProperty("当前生效版本ID；尚未生效或停用时为空") public String currentVersionId;
    @ApiModelProperty("本条候选对应版本ID，可能是历史版本或待复核版本") public String versionId;
    @ApiModelProperty("本卡从1递增的版本号，不是数据库ID") public Integer versionNo;
    @ApiModelProperty("本版本审批账户范围快照；不表示这些账户当前都能交易") public List<String> accountNos;
    @ApiModelProperty("建档时间，Asia/Shanghai本地时间") public String createdAt;
    @ApiModelProperty("版本生效起点，包含；未生效时为空") public String effectiveFrom;
    @ApiModelProperty("版本失效终点，不包含；未终止时为空") public String effectiveTo;
  }

  @ApiModel(description = "影像页元数据；原图只能通过再次鉴权的读图接口取得")
  public static class ImagePage {
    @ApiModelProperty("从1开始的页码") public Integer pageNo;
    @ApiModelProperty(value = "实际验证后的文件类型", allowableValues = "image/png,image/jpeg") public String mimeType;
    @ApiModelProperty("文件字节数") public Long fileSize;
    @ApiModelProperty("完整性SHA-256摘要") public String sha256;
  }

  @ApiModel(description = "不可覆盖的版本审批快照；有效区间为左闭右开")
  public static class Version {
    @ApiModelProperty("版本内部ID字符串") public String versionId;
    @ApiModelProperty("本卡版本序号") public Integer versionNo;
    @ApiModelProperty(value = "版本状态", allowableValues = "UPLOADING,PENDING_APPROVAL,APPROVED,ACTIVE,ARCHIVED,FAILED,REJECTED") public String status;
    @ApiModelProperty("该版本录入人员") public String createdBy;
    @ApiModelProperty("独立复核人员；未复核为空") public String approvedBy;
    @ApiModelProperty("复核时间") public String approvedAt;
    @ApiModelProperty("实际生效起点，包含") public String effectiveFrom;
    @ApiModelProperty("实际失效终点，不包含") public String effectiveTo;
    @ApiModelProperty("变更申请原因") public String changeReason;
    @ApiModelProperty("驳回或到期核验失败原因") public String rejectReason;
    @ApiModelProperty("多账户共用授权依据") public String authorizationRef;
    @ApiModelProperty("经该版本审批的账户范围快照") public List<String> accountNos;
    @ApiModelProperty("版本创建时间") public String createdAt;
    @ApiModelProperty("已持久化且完整的影像页元数据，按页码排序") public List<ImagePage> images;
    @ApiModelProperty("逐账户真实适用关系及其独立有效区间") public List<Binding> bindings;
  }

  @ApiModel(description = "档案详情，包括全部版本；当前版本须通过currentVersionId识别")
  public static class CardDetail extends CardSummary {
    @ApiModelProperty("建档备注") public String remark;
    @ApiModelProperty("建档录入人员") public String createdBy;
    @ApiModelProperty("内部并发修订序号") public Integer rowVersion;
    @ApiModelProperty("停用时间") public String disabledAt;
    @ApiModelProperty("停用或挂失原因") public String disableReason;
    @ApiModelProperty("按版本号倒序排列的审批及历史版本") public List<Version> versions;
  }

  @ApiModel(description = "版本内账户适用关系，销户时可早于版本终止")
  public static class Binding {
    @ApiModelProperty("结算账号") public String accountNo;
    @ApiModelProperty("PENDING/ACTIVE/ENDED/REJECTED") public String bindingStatus;
    @ApiModelProperty("关系生效起点，包含") public String effectiveFrom;
    @ApiModelProperty("关系终止时刻，不包含") public String effectiveTo;
    @ApiModelProperty("复核人员") public String approvedBy;
  }

  @ApiModel(description = "服务端分页，page从1开始，pageSize范围1至100")
  public static class Page<T> {
    @ApiModelProperty("满足过滤条件的总记录数") public Long total;
    @ApiModelProperty("当前页数据") public List<T> items;
    @ApiModelProperty("页码") public Integer page;
    @ApiModelProperty("每页条数") public Integer pageSize;
  }

  @ApiModel(description = "印鉴候选分页") public static class CardPage extends Page<CardSummary> {}
  @ApiModel(description = "上传任务分页") public static class TaskPage extends Page<Task> {}
  @ApiModel(description = "审计记录分页") public static class AuditPage extends Page<Audit> {}

  @ApiModel(description = "创建受理结果；201表示进入待复核，202表示持久化后仍在处理")
  public static class UploadResult {
    @ApiModelProperty("卡片ID字符串") public String cardId;
    @ApiModelProperty("版本ID字符串") public String versionId;
    @ApiModelProperty("可查询进度的上传任务ID字符串") public String taskId;
    @ApiModelProperty("版本当前状态，不代表自动生效") public String status;
    @ApiModelProperty("本次应上传的影像页数") public Integer imageCount;
  }

  @ApiModel(description = "公开上传任务状态；不返回payload、stagedId、锁或存储路径")
  public static class Task {
    @ApiModelProperty("任务ID字符串") public String taskId;
    @ApiModelProperty("卡片ID字符串") public String cardId;
    @ApiModelProperty("版本ID字符串") public String versionId;
    @ApiModelProperty(value = "任务状态", allowableValues = "PENDING,PROCESSING,SUCCESS,RETRY,FAILED") public String status;
    @ApiModelProperty("当前自动重试次数") public Integer retryCount;
    @ApiModelProperty("下一次重试时间，未安排时为空") public String nextRetryAt;
    @ApiModelProperty("最近失败的稳定错误码") public String lastErrorCode;
    @ApiModelProperty("经脱敏的失败说明") public String lastErrorMessage;
    @ApiModelProperty("创建时间") public String createdAt;
    @ApiModelProperty("最近更新时间") public String updatedAt;
  }

  @ApiModel(description = "追加式审计记录；失败或无候选操作允许目标ID为空")
  public static class Audit {
    @ApiModelProperty("审计记录ID字符串") public String id;
    @ApiModelProperty("目标卡片ID字符串，可空") public String sealCardId;
    @ApiModelProperty("目标版本ID字符串，可空") public String versionId;
    @ApiModelProperty("稳定操作码，例如UPLOAD、APPROVE、VIEW_IMAGE、UNBIND") public String action;
    @ApiModelProperty("可信操作人或SYSTEM；匿名拒绝为anonymous") public String operatorId;
    @ApiModelProperty("操作人机构") public String branchNo;
    @ApiModelProperty("请求追踪编号") public String requestId;
    @ApiModelProperty(value = "执行结果", allowableValues = "SUCCESS,FAIL") public String result;
    @ApiModelProperty("失败错误码；成功为空") public String errorCode;
    @ApiModelProperty("业务用途或变更原因") public String purpose;
    @ApiModelProperty("发生时间") public String createdAt;
  }

  @ApiModel(description = "客户中心确认的单位结算账户")
  public static class Account {
    @ApiModelProperty("单位银行结算账号，不是银行卡号") public String accountNo;
    @ApiModelProperty("账户中心确认的账户名称") public String accountName;
    @ApiModelProperty("中心当前状态，ACTIVE或CLOSED") public String status;
  }

  @ApiModel(description = "演示客户中心响应；生产通过银行适配器替换")
  public static class Customer {
    @ApiModelProperty("客户号CIF") public String customerNo;
    @ApiModelProperty("单位客户名称") public String customerName;
    @ApiModelProperty("客户管理机构") public String branchNo;
    @ApiModelProperty("客户状态") public String status;
    @ApiModelProperty("中心账户清单，实际绑定仍在服务端再次核验") public List<Account> accounts;
  }

  @ApiModel(description = "交易候选；返回候选不表示已完成电子验印")
  public static class TransactionCandidates {
    @ApiModelProperty("全部适用候选，历史日期可能含同日多个版本") public List<CardSummary> items;
    @ApiModelProperty("候选数量") public Long total;
    @ApiModelProperty("候选多于一项时为true，需业务流程确认") public Boolean ambiguous;
    @ApiModelProperty("当前或历史流程适用限制") public String selectionNotice;
  }

  @ApiModel(description = "当前身份可见的工作台统计")
  public static class Dashboard {
    @ApiModelProperty("可见档案总数") public Long totalCards;
    @ApiModelProperty("待复核申请数") public Long pendingApproval;
    @ApiModelProperty("ACTIVE版本数，不等同于可付款账户数") public Long activeVersions;
    @ApiModelProperty("失败上传任务数") public Long failedTasks;
    @ApiModelProperty("最近可见操作摘要") public List<RecentAudit> recentAudits;
  }

  @ApiModel(description = "工作台最近操作摘要")
  public static class RecentAudit {
    @ApiModelProperty("审计记录ID字符串") public String id;
    @ApiModelProperty("动作码") public String action;
    @ApiModelProperty("操作人员") public String operatorId;
    @ApiModelProperty("机构") public String branchNo;
    @ApiModelProperty("SUCCESS或FAIL") public String result;
    @ApiModelProperty("发生时间") public String createdAt;
  }

  @ApiModel(description = "运行状态，不含数据库凭证或存储路径")
  public static class SystemStatus {
    @ApiModelProperty("服务标识") public String service;
    @ApiModelProperty("数据库连通性UP或DOWN") public String database;
    @ApiModelProperty("应用进程实例标识，用于本地故障切换演示") public String instanceId;
    @ApiModelProperty("私有存储可用性UP或DOWN") public String storage;
    @ApiModelProperty("当前为DEMO") public String mode;
  }

  @ApiModel(description = "印鉴列表响应") public static class CardPageResponse extends ApiResponse<CardPage> { public CardPageResponse() { super(null, null, null); } }
  @ApiModel(description = "档案详情响应") public static class CardDetailResponse extends ApiResponse<CardDetail> { public CardDetailResponse() { super(null, null, null); } }
  @ApiModel(description = "版本详情响应") public static class VersionResponse extends ApiResponse<Version> { public VersionResponse() { super(null, null, null); } }
  @ApiModel(description = "上传受理响应") public static class UploadResponse extends ApiResponse<UploadResult> { public UploadResponse() { super(null, null, null); } }
  @ApiModel(description = "任务列表响应") public static class TaskPageResponse extends ApiResponse<TaskPage> { public TaskPageResponse() { super(null, null, null); } }
  @ApiModel(description = "任务详情响应") public static class TaskResponse extends ApiResponse<Task> { public TaskResponse() { super(null, null, null); } }
  @ApiModel(description = "审计分页响应") public static class AuditPageResponse extends ApiResponse<AuditPage> { public AuditPageResponse() { super(null, null, null); } }
  @ApiModel(description = "客户核验响应") public static class CustomerResponse extends ApiResponse<Customer> { public CustomerResponse() { super(null, null, null); } }
  @ApiModel(description = "交易候选响应") public static class TransactionResponse extends ApiResponse<TransactionCandidates> { public TransactionResponse() { super(null, null, null); } }
  @ApiModel(description = "工作台响应") public static class DashboardResponse extends ApiResponse<Dashboard> { public DashboardResponse() { super(null, null, null); } }
  @ApiModel(description = "系统状态响应") public static class SystemStatusResponse extends ApiResponse<SystemStatus> { public SystemStatusResponse() { super(null, null, null); } }
}
