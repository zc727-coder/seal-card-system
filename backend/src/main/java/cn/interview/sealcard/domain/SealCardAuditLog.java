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
 * 追加审计；无外键以允许失败/未找到目标留痕
 *
 * @author zc
 * @since 2026-11-24
 */
@ApiModel(value = "SealCardAuditLog", description = "上传、复核、查询和调阅等操作的追加审计记录")
@Getter
@Setter
@TableName("seal_card_audit_log")
public class SealCardAuditLog implements Serializable {

  private static final long serialVersionUID = 1L;

  @TableId(value = "id", type = IdType.AUTO)
  @ApiModelProperty(value = "内部主键")
  private Long id;

  /** 失败或无候选查询允许为空 */
  @TableField("seal_card_id")
  @ApiModelProperty(value = "所属印鉴卡ID")
  private Long sealCardId;

  @TableField("version_id")
  @ApiModelProperty(value = "所属印鉴版本ID")
  private Long versionId;

  /** 未找到的目标等非敏感引用 */
  @TableField("target_ref")
  @ApiModelProperty(value = "审计目标引用，未找到卡片时也可留痕")
  private String targetRef;

  /** UPLOAD/SUBMIT/QUERY/APPROVE/REJECT/ACTIVATE/DISABLE/BIND/UNBIND/VIEW_IMAGE */
  @TableField("action")
  @ApiModelProperty(
      value = "操作类型：UPLOAD、SUBMIT、QUERY、APPROVE、REJECT、ACTIVATE、DISABLE、BIND、UNBIND、VIEW_IMAGE")
  private String action;

  @TableField("operator_id")
  @ApiModelProperty(value = "可信身份中的操作人员编号")
  private String operatorId;

  @TableField("branch_no")
  @ApiModelProperty(value = "操作机构号")
  private String branchNo;

  @TableField("request_id")
  @ApiModelProperty(value = "请求追踪编号")
  private String requestId;

  @TableField("result")
  @ApiModelProperty(value = "执行结果：SUCCESS成功、FAIL失败")
  private String result;

  /** 失败码，不写原始敏感报错 */
  @TableField("error_code")
  @ApiModelProperty(value = "脱敏业务错误码，成功时可为空")
  private String errorCode;

  /** 业务用途/流程引用 */
  @TableField("purpose")
  @ApiModelProperty(value = "业务用途或流程引用")
  private String purpose;

  @TableField("created_at")
  @ApiModelProperty(value = "创建时间（Asia/Shanghai）")
  private LocalDateTime createdAt;
}
