package cn.interview.sealcard.param;

import io.swagger.annotations.*;
import java.util.*;
import javax.validation.constraints.*;

@ApiModel(description = "建档或新版本申请；机构与录入人从认证身份取得")
public class UploadMetadata {
  @ApiModelProperty(value = "客户号", example = "C10001", required = true)
  @NotBlank
  @Size(max = 32)
  public String customerNo;

  @ApiModelProperty(value = "完整适用结算账户集合", required = true)
  @NotEmpty
  @Size(max = 20)
  public List<@NotBlank @Size(max = 32) String> accountNos;

  @ApiModelProperty("备注")
  @Size(max = 500)
  public String remark;

  @ApiModelProperty("多账户共用授权引用")
  @Size(max = 128)
  public String authorizationRef;

  @ApiModelProperty("变更原因")
  @Size(max = 500)
  public String changeReason;
}
