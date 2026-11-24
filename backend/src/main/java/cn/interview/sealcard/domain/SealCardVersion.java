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
 * 印鉴卡版本与复核记录
 *
 * @author zc
 * @since 2026-11-24
 */
@ApiModel(value = "SealCardVersion", description = "影像版本、复核记录与生效区间")
@Getter
@Setter
@TableName("seal_card_version")
public class SealCardVersion implements Serializable {

  private static final long serialVersionUID = 1L;

  @TableId(value = "id", type = IdType.AUTO)
  @ApiModelProperty(value = "内部主键")
  private Long id;

  @TableField("seal_card_id")
  @ApiModelProperty(value = "所属印鉴卡ID")
  private Long sealCardId;

  @TableField("version_no")
  @ApiModelProperty(value = "同一印鉴卡内的版本序号，从1递增")
  private Integer versionNo;

  @TableField("status")
  @ApiModelProperty(
      value =
          "版本状态：UPLOADING上传中、PENDING_APPROVAL待复核、APPROVED待生效、ACTIVE生效、ARCHIVED归档、FAILED失败、REJECTED驳回")
  private String status;

  @TableField("effective_from")
  @ApiModelProperty(value = "生效起始时间，包含该时刻；待审核时可为空")
  private LocalDateTime effectiveFrom;

  @TableField("effective_to")
  @ApiModelProperty(value = "生效截止时间，不包含该时刻；为空表示未限定截止时间")
  private LocalDateTime effectiveTo;

  @TableField("created_by")
  @ApiModelProperty(value = "录入人员编号")
  private String createdBy;

  @TableField("approved_by")
  @ApiModelProperty(value = "复核人员编号，必须与录入人不同")
  private String approvedBy;

  @TableField("approved_at")
  @ApiModelProperty(value = "复核通过时间")
  private LocalDateTime approvedAt;

  @TableField("submitted_at")
  @ApiModelProperty(value = "提交复核时间")
  private LocalDateTime submittedAt;

  @TableField("rejected_by")
  @ApiModelProperty(value = "驳回人员编号")
  private String rejectedBy;

  @TableField("rejected_at")
  @ApiModelProperty(value = "驳回时间")
  private LocalDateTime rejectedAt;

  @TableField("reject_reason")
  @ApiModelProperty(value = "驳回原因")
  private String rejectReason;

  /** 申请上传页数，完成时须核对实际READY页数 */
  @TableField("expected_image_count")
  @ApiModelProperty(value = "预期影像页数，提交复核前须与实际就绪页数一致")
  private Integer expectedImageCount;

  /** 共用/变更授权依据索引，不存原文 */
  @TableField("authorization_ref")
  @ApiModelProperty(value = "共用印鉴或变更授权依据的内部引用")
  private String authorizationRef;

  @TableField("change_reason")
  @ApiModelProperty(value = "版本或账户适用范围变更原因")
  private String changeReason;

  @TableField("created_at")
  @ApiModelProperty(value = "创建时间（Asia/Shanghai）")
  private LocalDateTime createdAt;

  @TableField("updated_at")
  @ApiModelProperty(value = "更新时间（Asia/Shanghai）")
  private LocalDateTime updatedAt;
}
