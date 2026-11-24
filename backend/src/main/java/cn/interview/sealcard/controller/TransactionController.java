package cn.interview.sealcard.controller;

import cn.interview.sealcard.response.ApiResponse;
import cn.interview.sealcard.response.ApiModels;
import cn.interview.sealcard.service.*;
import io.swagger.annotations.*;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@Api(tags = "交易印鉴调阅")
@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {
  private final SealBusinessService seals;

  public TransactionController(SealBusinessService seals) {
    this.seals = seals;
  }

  @ApiOperation(value = "按实际付款账号定位候选印鉴；历史签发日期只允许CHECK流程", response = ApiModels.TransactionResponse.class)
  @io.swagger.annotations.ApiResponses({@io.swagger.annotations.ApiResponse(code = 200, message = "成功", response = ApiModels.TransactionResponse.class)})
  @GetMapping("/seals")
  @AuditAction("QUERY_TRANSACTION")
  public ApiResponse<ApiModels.TransactionCandidates> query(
      @RequestParam String customerNo,
      @RequestParam String accountNo,
      @RequestParam String transactionType,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate documentIssueDate,
      @RequestParam String purpose) {
    return ApiModels.response(
        seals.transactions(customerNo, accountNo, transactionType, documentIssueDate, purpose), ApiModels.TransactionCandidates.class);
  }
}
