package cn.interview.sealcard.controller;

import cn.interview.sealcard.adapter.*;
import cn.interview.sealcard.exception.BusinessException;
import cn.interview.sealcard.service.*;
import io.swagger.annotations.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@Api(tags = "受控影像读取")
@RestController
@RequestMapping("/api/v1/seal-cards/{cardId}/versions/{versionId}/images")
public class SealCardImageController {
  private final SealBusinessService seals;
  private final ImageStorage images;
  private final AuditService audit;

  public SealCardImageController(
      SealBusinessService seals, ImageStorage images, AuditService audit) {
    this.seals = seals;
    this.images = images;
    this.audit = audit;
  }

  @ApiOperation("逐次鉴权、完整性校验并流式读取影像；交易人员必须提供付款账号、类型及用途")
  @GetMapping("/{pageNo}")
  @AuditAction(value = "VIEW_IMAGE", success = false)
  public ResponseEntity<org.springframework.core.io.InputStreamResource> image(
      @PathVariable long cardId,
      @PathVariable long versionId,
      @PathVariable int pageNo,
      @RequestParam(required = false) String accountNo,
      @RequestParam(required = false) String transactionType,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate documentIssueDate,
      @RequestParam(required = false) String purpose) {
    Map<String, Object> row =
        seals.image(
            cardId, versionId, pageNo, accountNo, transactionType, documentIssueDate, purpose);
    String object = row.get("image_id").toString();
    java.io.InputStream stream;
    try {
      java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
      long size = 0;
      try (java.io.InputStream check = images.open(object)) {
        byte[] buffer = new byte[32768];
        for (int n; (n = check.read(buffer)) != -1; ) {
          digest.update(buffer, 0, n);
          size += n;
        }
      }
      StringBuilder sha = new StringBuilder();
      for (byte b : digest.digest()) sha.append(String.format("%02x", b & 255));
      if (!row.get("sha256").equals(sha.toString())
          || size != ((Number) row.get("file_size")).longValue())
        throw new java.io.IOException("Integrity mismatch");
      stream = images.open(object);
    } catch (Exception e) {
      throw new BusinessException(503, "IMAGE_STORAGE_UNAVAILABLE", "影像平台不可用或文件完整性校验失败");
    }
    try {
      audit.write("VIEW_IMAGE", cardId, versionId, purpose, "SUCCESS", null);
    } catch (RuntimeException e) {
      try {
        stream.close();
      } catch (java.io.IOException ignored) {
      }
      throw e;
    }
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(row.get("mime_type").toString()))
        .contentLength(((Number) row.get("file_size")).longValue())
        .cacheControl(CacheControl.noStore())
        .header("Content-Disposition", "inline; filename=seal-card-page-" + pageNo)
        .body(new org.springframework.core.io.InputStreamResource(stream));
  }
}
