package cn.interview.sealcard.response;

import cn.interview.sealcard.security.RequestContext;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "统一接口响应")
public class ApiResponse<T> {
  @ApiModelProperty("成功为0，否则为稳定业务错误码")
  public String code;

  @ApiModelProperty("操作说明")
  public String message;

  @ApiModelProperty("请求追踪编号")
  public String requestId;

  @ApiModelProperty("响应数据")
  public T data;

  public ApiResponse(String code, String message, T data) {
    this.code = code;
    this.message = message;
    this.data = data;
    this.requestId = RequestContext.requestId();
  }

  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>("0", "成功", data);
  }
}
