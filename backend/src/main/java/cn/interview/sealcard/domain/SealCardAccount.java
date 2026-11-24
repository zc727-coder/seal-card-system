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
 * 按审批版本保存的账户适用范围
 *
 * @author zc
 * @since 2026-11-24
 */
@ApiModel(value = "SealCardAccount", description = "按审批版本保存的单位结算账户范围")
@Getter
@Setter
@TableName("seal_card_account")
public class SealCardAccount implements Serializable {

  private static final long serialVersionUID = 1L;

  @TableId(value = "id", type = IdType.AUTO)
  @ApiModelProperty(value = "内部主键")
  private Long id;

  @TableField("seal_card_id")
  @ApiModelProperty(value = "所属印鉴卡ID")
  private Long sealCardId;

  /** 账户范围所属审批版本快照 */
  @TableField("version_id")
  @ApiModelProperty(value = "所属印鉴版本ID")
  private Long versionId;

  /** 单位结算账号，外部引用 */
  @TableField("account_no")
  @ApiModelProperty(value = "外部核心系统单位结算账号，不是银行卡号")
  private String accountNo;

  @TableField("binding_status")
  @ApiModelProperty(value = "适用状态：PENDING待复核、ACTIVE生效、ENDED已终止、REJECTED已驳回")
  private String bindingStatus;

  /** 待审核时为空，生效时必填 */
  @TableField("effective_from")
  @ApiModelProperty(value = "生效起始时间，包含该时刻；待审核时可为空")
  private LocalDateTime effectiveFrom;

  @TableField("effective_to")
  @ApiModelProperty(value = "生效截止时间，不包含该时刻；为空表示未限定截止时间")
  private LocalDateTime effectiveTo;

  @TableField("approved_by")
  @ApiModelProperty(value = "复核人员编号，必须与录入人不同")
  private String approvedBy;

  @TableField("approved_at")
  @ApiModelProperty(value = "复核通过时间")
  private LocalDateTime approvedAt;

  @TableField("authorization_ref")
  @ApiModelProperty(value = "共用印鉴或变更授权依据的内部引用")
  private String authorizationRef;

  @TableField("created_by")
  @ApiModelProperty(value = "录入人员编号")
  private String createdBy;

  @TableField("created_at")
  @ApiModelProperty(value = "创建时间（Asia/Shanghai）")
  private LocalDateTime createdAt;
}
