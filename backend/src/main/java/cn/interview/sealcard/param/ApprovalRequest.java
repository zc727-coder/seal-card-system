package cn.interview.sealcard.param;

import io.swagger.annotations.*;
import java.time.LocalDateTime;

@ApiModel(description = "复核通过参数")
public class ApprovalRequest {
  @ApiModelProperty("为空立即生效；未来时刻预约生效，Asia/Shanghai")
  public LocalDateTime effectiveFrom;
}
