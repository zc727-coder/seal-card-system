package cn.interview.sealcard.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 上传幂等与可恢复补偿任务
 *
 * @author zc
 * @since 2026-11-24
 */
@ApiModel(value = "SealUploadTask", description = "内部幂等任务及重试租约信息，不作为公共请求对象")
@Getter
@Setter
@TableName("seal_upload_task")
public class SealUploadTask implements Serializable {

  private static final long serialVersionUID = 1L;

  @TableId(value = "id", type = IdType.AUTO)
  @ApiModelProperty(value = "内部主键")
  private Long id;

  @TableField("idempotency_key")
  @ApiModelProperty(value = "上传请求幂等键，重复请求须核对身份及请求摘要")
  private String idempotencyKey;

  @TableField("request_hash")
  @ApiModelProperty(value = "请求内容摘要，64位小写十六进制字符串")
  private String requestHash;

  @TableField("version_id")
  @ApiModelProperty(value = "所属印鉴版本ID")
  private Long versionId;

  @TableField("operator_id")
  @ApiModelProperty(value = "可信身份中的操作人员编号")
  private String operatorId;

  @TableField("branch_no")
  @ApiModelProperty(value = "操作机构号")
  private String branchNo;

  @TableField("request_id")
  @ApiModelProperty(value = "请求追踪编号")
  private String requestId;

  /** 私有暂存对象及页摘要清单，不含图片字节/凭证 */
  @TableField("payload")
  @ApiModelProperty(value = "内部恢复清单JSON，仅保存暂存对象与页摘要等元数据，禁止存图片正文或凭证")
  private String payload;

  @TableField("task_status")
  @ApiModelProperty(value = "任务状态：PENDING待处理、PROCESSING处理中、SUCCESS成功、RETRY待重试、FAILED失败")
  private String taskStatus;

  @TableField("retry_count")
  @ApiModelProperty(value = "已重试次数，非负整数")
  private Integer retryCount;

  @TableField("next_retry_at")
  @ApiModelProperty(value = "下次允许重试时间")
  private LocalDateTime nextRetryAt;

  @TableField("lock_owner")
  @ApiModelProperty(value = "持有租约的任务执行实例标识")
  private String lockOwner;

  @TableField("lock_until")
  @ApiModelProperty(value = "任务租约到期时间")
  private LocalDateTime lockUntil;

  /** 递增抢占令牌，防过期执行者写入 */
  @TableField("lock_token")
  @ApiModelProperty(value = "递增抢占令牌，防止过期执行者提交结果")
  private Long lockToken;

  @TableField("last_error_code")
  @ApiModelProperty(value = "最近一次失败的错误码")
  private String lastErrorCode;

  /** 脱敏摘要 */
  @TableField("last_error_message")
  @ApiModelProperty(value = "最近一次失败的脱敏说明")
  private String lastErrorMessage;

  @TableField("completed_at")
  @ApiModelProperty(value = "任务完成时间")
  private LocalDateTime completedAt;

  @TableField("created_at")
  @ApiModelProperty(value = "创建时间（Asia/Shanghai）")
  private LocalDateTime createdAt;

  @TableField("updated_at")
  @ApiModelProperty(value = "更新时间（Asia/Shanghai）")
  private LocalDateTime updatedAt;
}
