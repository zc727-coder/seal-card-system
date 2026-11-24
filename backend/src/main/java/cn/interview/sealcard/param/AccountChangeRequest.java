package cn.interview.sealcard.param;

import io.swagger.annotations.*;
import java.util.*;
import javax.validation.constraints.*;

@ApiModel(description = "账户范围变更申请，必须再次复核")
public class AccountChangeRequest {
  @NotEmpty
  @Size(max = 20)
  @ApiModelProperty("新的完整结算账户范围")
  public List<@NotBlank @Size(max = 32) String> accountNos;

  @Size(max = 128)
  @ApiModelProperty("共用授权引用")
  public String authorizationRef;

  @NotBlank
  @Size(max = 500)
  @ApiModelProperty("变更原因")
  public String changeReason;
}
