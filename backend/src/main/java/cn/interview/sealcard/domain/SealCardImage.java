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
 * 影像索引，不存图片正文
 *
 * @author zc
 * @since 2026-11-24
 */
@ApiModel(value = "SealCardImage", description = "影像索引及完整性信息，不包含影像正文")
@Getter
@Setter
@TableName("seal_card_image")
public class SealCardImage implements Serializable {

  private static final long serialVersionUID = 1L;

  @TableId(value = "id", type = IdType.AUTO)
  @ApiModelProperty(value = "内部主键")
  private Long id;

  @TableField("version_id")
  @ApiModelProperty(value = "所属印鉴版本ID")
  private Long versionId;

  @TableField("page_no")
  @ApiModelProperty(value = "影像页码，从1开始")
  private Integer pageNo;

  /** 私有平台对象ID，非公网URL */
  @TableField("image_id")
  @ApiModelProperty(value = "私有影像平台对象ID，不是永久公网URL")
  private String imageId;

  @TableField("sha256")
  @ApiModelProperty(value = "文件SHA-256摘要，64位小写十六进制字符串")
  private String sha256;

  @TableField("mime_type")
  @ApiModelProperty(value = "经过内容校验的文件媒体类型")
  private String mimeType;

  @TableField("file_size")
  @ApiModelProperty(value = "文件大小，单位字节，必须大于0")
  private Long fileSize;

  @TableField("storage_status")
  @ApiModelProperty(value = "存储状态：READY已就绪、FAILED失败")
  private String storageStatus;

  @TableField("created_at")
  @ApiModelProperty(value = "创建时间（Asia/Shanghai）")
  private LocalDateTime createdAt;
}
