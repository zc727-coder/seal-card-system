package cn.interview.sealcard.controller;

import cn.interview.sealcard.response.ApiResponse;
import cn.interview.sealcard.response.ApiModels;
import cn.interview.sealcard.service.*;
import io.swagger.annotations.*;
import org.springframework.web.bind.annotation.*;

@Api(tags = "授权操作审计")
@RestController
@RequestMapping("/api/v1/audits")
public class SealCardAuditLogController {
  private final SealBusinessService seals;

  public SealCardAuditLogController(SealBusinessService seals) {
    this.seals = seals;
  }

  @ApiOperation(value = "审计人员按卡片、动作、结果和请求号分页查询", response = ApiModels.AuditPageResponse.class)
  @io.swagger.annotations.ApiResponses({@io.swagger.annotations.ApiResponse(code = 200, message = "成功", response = ApiModels.AuditPageResponse.class)})
  @GetMapping
  public ApiResponse<ApiModels.AuditPage> query(
      @RequestParam(required = false) Long cardId,
      @RequestParam(required = false) String action,
      @RequestParam(required = false) String result,
      @RequestParam(required = false) String requestId,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize) {
    return ApiModels.response(seals.audits(cardId, action, result, requestId, page, pageSize), ApiModels.AuditPage.class);
  }
}
