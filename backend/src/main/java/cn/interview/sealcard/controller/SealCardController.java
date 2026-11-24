package cn.interview.sealcard.controller;

import cn.interview.sealcard.param.*;
import cn.interview.sealcard.response.ApiResponse;
import cn.interview.sealcard.response.ApiModels;
import cn.interview.sealcard.service.*;
import io.swagger.annotations.*;
import java.util.*;
import javax.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Api(tags = "印鉴档案与复核")
@RestController
@RequestMapping("/api/v1/seal-cards")
public class SealCardController {
  private final SealBusinessService seals;

  public SealCardController(SealBusinessService seals) {
    this.seals = seals;
  }

  @ApiOperation(value = "按客户、结算账户、状态检索候选印鉴，支持管理工作台分页", response = ApiModels.CardPageResponse.class)
  @io.swagger.annotations.ApiResponses({@io.swagger.annotations.ApiResponse(code = 200, message = "成功", response = ApiModels.CardPageResponse.class)})
  @GetMapping
  @AuditAction("QUERY")
  public ApiResponse<ApiModels.CardPage> list(
      @RequestParam(required = false) String customerNo,
      @RequestParam(required = false) String accountNo,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize) {
    return ApiModels.response(seals.list(customerNo, accountNo, status, page, pageSize), ApiModels.CardPage.class);
  }

  @ApiOperation(value = "档案详情和完整历史版本；不返回存储对象ID", response = ApiModels.CardDetailResponse.class)
  @io.swagger.annotations.ApiResponses({@io.swagger.annotations.ApiResponse(code = 200, message = "成功", response = ApiModels.CardDetailResponse.class)})
  @GetMapping("/{cardId}")
  @AuditAction("QUERY")
  public ApiResponse<ApiModels.CardDetail> detail(@PathVariable long cardId) {
    return ApiModels.response(seals.detail(cardId), ApiModels.CardDetail.class);
  }

  @ApiOperation(value = "上传多页PNG/JPEG并创建待复核印鉴；metadata为JSON文件part，files重复上传", response = ApiModels.UploadResponse.class)
  @io.swagger.annotations.ApiResponses({@io.swagger.annotations.ApiResponse(code = 201, message = "成功", response = ApiModels.UploadResponse.class), @io.swagger.annotations.ApiResponse(code = 202, message = "成功", response = ApiModels.UploadResponse.class)})
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @AuditAction(value = "UPLOAD", success = false)
  public ResponseEntity<ApiResponse<ApiModels.UploadResult>> create(
      @RequestPart("metadata") @Valid UploadMetadata metadata,
      @RequestPart("files") List<MultipartFile> files,
      @RequestHeader("Idempotency-Key") String key) {
    Map<String, Object> result = seals.upload(null, metadata, files, key);
    return ResponseEntity.status("PENDING_APPROVAL".equals(result.get("status")) ? 201 : 202)
        .body(ApiModels.response(result, ApiModels.UploadResult.class));
  }

  @ApiOperation(value = "提交变更版本；不会覆盖当前生效资料", response = ApiModels.UploadResponse.class)
  @io.swagger.annotations.ApiResponses({@io.swagger.annotations.ApiResponse(code = 201, message = "成功", response = ApiModels.UploadResponse.class), @io.swagger.annotations.ApiResponse(code = 202, message = "成功", response = ApiModels.UploadResponse.class)})
  @PostMapping(value = "/{cardId}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @AuditAction(value = "UPLOAD", success = false)
  public ResponseEntity<ApiResponse<ApiModels.UploadResult>> change(
      @PathVariable long cardId,
      @RequestPart("metadata") @Valid UploadMetadata metadata,
      @RequestPart("files") List<MultipartFile> files,
      @RequestHeader("Idempotency-Key") String key) {
    Map<String, Object> result = seals.upload(cardId, metadata, files, key);
    return ResponseEntity.status("PENDING_APPROVAL".equals(result.get("status")) ? 201 : 202)
        .body(ApiModels.response(result, ApiModels.UploadResult.class));
  }

  @ApiOperation(value = "提交账户适用范围变更，复用当前影像且必须再次复核", response = ApiModels.VersionResponse.class)
  @io.swagger.annotations.ApiResponses({@io.swagger.annotations.ApiResponse(code = 200, message = "成功", response = ApiModels.VersionResponse.class)})
  @PostMapping("/{cardId}/accounts")
  @AuditAction(value = "BIND", success = false)
  public ApiResponse<ApiModels.Version> accounts(
      @PathVariable long cardId, @RequestBody @Valid AccountChangeRequest body) {
    return ApiModels.response(seals.accounts(cardId, body), ApiModels.Version.class);
  }

  @ApiOperation(value = "双人复核通过；可指定未来生效时间", response = ApiModels.VersionResponse.class)
  @io.swagger.annotations.ApiResponses({@io.swagger.annotations.ApiResponse(code = 200, message = "成功", response = ApiModels.VersionResponse.class)})
  @PostMapping("/{cardId}/versions/{versionId}/approve")
  @AuditAction(value = "APPROVE", success = false)
  public ApiResponse<ApiModels.Version> approve(
      @PathVariable long cardId, @PathVariable long versionId, @RequestBody ApprovalRequest body) {
    return ApiModels.response(seals.approve(cardId, versionId, body), ApiModels.Version.class);
  }

  @ApiOperation(value = "复核驳回，必须说明原因", response = ApiModels.VersionResponse.class)
  @io.swagger.annotations.ApiResponses({@io.swagger.annotations.ApiResponse(code = 200, message = "成功", response = ApiModels.VersionResponse.class)})
  @PostMapping("/{cardId}/versions/{versionId}/reject")
  @AuditAction(value = "REJECT", success = false)
  public ApiResponse<ApiModels.Version> reject(
      @PathVariable long cardId,
      @PathVariable long versionId,
      @RequestBody @Valid ReasonRequest body) {
    return ApiModels.response(seals.reject(cardId, versionId, body.reason), ApiModels.Version.class);
  }

  @ApiOperation(value = "停用或挂失印鉴；保留历史版本和适用区间", response = ApiModels.CardDetailResponse.class)
  @io.swagger.annotations.ApiResponses({@io.swagger.annotations.ApiResponse(code = 200, message = "成功", response = ApiModels.CardDetailResponse.class)})
  @PostMapping("/{cardId}/disable")
  @AuditAction(value = "DISABLE", success = false)
  public ApiResponse<ApiModels.CardDetail> disable(@PathVariable long cardId, @RequestBody @Valid ReasonRequest body) {
    return ApiModels.response(seals.disable(cardId, body.reason), ApiModels.CardDetail.class);
  }

  @ApiOperation(value = "授权审计员查看单卡操作记录", response = ApiModels.AuditPageResponse.class)
  @io.swagger.annotations.ApiResponses({@io.swagger.annotations.ApiResponse(code = 200, message = "成功", response = ApiModels.AuditPageResponse.class)})
  @GetMapping("/{cardId}/audits")
  public ApiResponse<ApiModels.AuditPage> audits(
      @PathVariable long cardId,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize) {
    return ApiModels.response(seals.audits(cardId, null, null, null, page, pageSize), ApiModels.AuditPage.class);
  }
}
