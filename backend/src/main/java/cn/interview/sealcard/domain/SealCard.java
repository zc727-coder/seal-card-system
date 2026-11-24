package cn.interview.sealcard.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 印鉴卡主表
 *
 * @author zc
 * @since 2026-11-24
 */
@ApiModel(value = "SealCard", description = "客户归属、当前生效版本与停用信息")
@Getter
@Setter
@TableName("seal_card")
public class SealCard implements Serializable {

  private static final long serialVersionUID = 1L;

  /** 内部主键 */
  @TableId(value = "id", type = IdType.AUTO)
  @ApiModelProperty(value = "内部主键")
  private Long id;

  /** 业务卡号 */
  @TableField("card_no")
  @ApiModelProperty(value = "印鉴卡业务编号")
  private String cardNo;

  /** 外部客户中心CIF */
  @TableField("customer_no")
  @ApiModelProperty(value = "外部客户中心客户号（CIF）")
  private String customerNo;

  /** 管理机构 */
  @TableField("branch_no")
  @ApiModelProperty(value = "操作机构号")
  private String branchNo;

  /** 当前生效版本，须属于本卡 */
  @TableField("current_version_id")
  @ApiModelProperty(value = "当前生效版本ID，未生效或停用时为空")
  private Long currentVersionId;

  @TableField("status")
  @ApiModelProperty(value = "卡片状态：ENABLED允许使用、DISABLED已停用；ENABLED不等于已有生效版本")
  private String status;

  /** 乐观锁版本 */
  @TableField("row_version")
  @Version
  @ApiModelProperty(value = "乐观锁版本号，从0递增")
  private Integer rowVersion;

  @TableField("created_by")
  @ApiModelProperty(value = "录入人员编号")
  private String createdBy;

  @TableField("disabled_by")
  @ApiModelProperty(value = "停用操作人编号")
  private String disabledBy;

  @TableField("disabled_at")
  @ApiModelProperty(value = "停用时间")
  private LocalDateTime disabledAt;

  @TableField("disable_reason")
  @ApiModelProperty(value = "停用原因")
  private String disableReason;

  @TableField("remark")
  @ApiModelProperty(value = "备注")
  private String remark;

  @TableField("created_at")
  @ApiModelProperty(value = "创建时间（Asia/Shanghai）")
  private LocalDateTime createdAt;

  @TableField("updated_at")
  @ApiModelProperty(value = "更新时间（Asia/Shanghai）")
  private LocalDateTime updatedAt;
}
