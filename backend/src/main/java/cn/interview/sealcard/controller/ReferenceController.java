package cn.interview.sealcard.controller;

import cn.interview.sealcard.adapter.ImageStorage;
import cn.interview.sealcard.response.ApiResponse;
import cn.interview.sealcard.response.ApiModels;
import cn.interview.sealcard.service.*;
import io.swagger.annotations.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@Api(tags = "工作台与模拟客户中心")
@RestController
@RequestMapping("/api/v1")
public class ReferenceController {
  private final SealBusinessService seals;
  private final JdbcTemplate jdbc;
  private final ImageStorage images;
  private final String instance = UUID.randomUUID().toString();

  public ReferenceController(SealBusinessService seals, JdbcTemplate jdbc, ImageStorage images) {
    this.seals = seals;
    this.jdbc = jdbc;
    this.images = images;
  }

  @ApiOperation(value = "按CIF核验模拟客户及结算账户", response = ApiModels.CustomerResponse.class)
  @io.swagger.annotations.ApiResponses({@io.swagger.annotations.ApiResponse(code = 200, message = "成功", response = ApiModels.CustomerResponse.class)})
  @GetMapping("/customers")
  @AuditAction("QUERY_CUSTOMER")
  public ApiResponse<ApiModels.Customer> customer(@RequestParam String customerNo) {
    return ApiModels.response(seals.customer(customerNo), ApiModels.Customer.class);
  }

  @ApiOperation(value = "当前身份可见的实时业务统计", response = ApiModels.DashboardResponse.class)
  @io.swagger.annotations.ApiResponses({@io.swagger.annotations.ApiResponse(code = 200, message = "成功", response = ApiModels.DashboardResponse.class)})
  @GetMapping("/dashboard")
  public ApiResponse<ApiModels.Dashboard> dashboard() {
    return ApiModels.response(seals.dashboard(), ApiModels.Dashboard.class);
  }

  @ApiOperation(value = "运行连通信息；不暴露数据库密码或存储路径", response = ApiModels.SystemStatusResponse.class)
  @io.swagger.annotations.ApiResponses({@io.swagger.annotations.ApiResponse(code = 200, message = "成功", response = ApiModels.SystemStatusResponse.class)})
  @GetMapping("/system/status")
  public ApiResponse<ApiModels.SystemStatus> status() {
    Integer value = jdbc.queryForObject("SELECT 1", Integer.class);
    return ApiModels.response(
        BusinessStore.m(
            "service",
            "seal-card-service",
            "database",
            Integer.valueOf(1).equals(value) ? "UP" : "DOWN",
            "instanceId",
            instance,
            "storage",
            images.ready() ? "UP" : "DOWN",
            "mode",
            "DEMO"), ApiModels.SystemStatus.class);
  }
}
