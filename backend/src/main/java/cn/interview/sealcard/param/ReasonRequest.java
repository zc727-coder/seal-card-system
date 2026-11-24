package cn.interview.sealcard.param;

import io.swagger.annotations.*;
import javax.validation.constraints.*;

@ApiModel(description = "驳回或停用原因")
public class ReasonRequest {
  @ApiModelProperty(value = "操作原因", required = true)
  @NotBlank
  @Size(max = 500)
  public String reason;
}
