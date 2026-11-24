package cn.interview.sealcard.controller;

import cn.interview.sealcard.response.ApiResponse;
import cn.interview.sealcard.response.ApiModels;
import cn.interview.sealcard.service.*;
import io.swagger.annotations.*;
import org.springframework.web.bind.annotation.*;

@Api(tags = "上传任务与失败恢复")
@RestController
@RequestMapping("/api/v1/upload-tasks")
public class SealUploadTaskController {
  private final UploadTaskWorker worker;

  public SealUploadTaskController(UploadTaskWorker worker) {
    this.worker = worker;
  }

  @ApiOperation(value = "本人上传任务或管理员任务列表", response = ApiModels.TaskPageResponse.class)
  @io.swagger.annotations.ApiResponses({@io.swagger.annotations.ApiResponse(code = 200, message = "成功", response = ApiModels.TaskPageResponse.class)})
  @GetMapping
  public ApiResponse<ApiModels.TaskPage> list(
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize) {
    return ApiModels.response(worker.list(status, page, pageSize), ApiModels.TaskPage.class);
  }

  @ApiOperation(value = "查询单个上传任务进度", response = ApiModels.TaskResponse.class)
  @io.swagger.annotations.ApiResponses({@io.swagger.annotations.ApiResponse(code = 200, message = "成功", response = ApiModels.TaskResponse.class)})
  @GetMapping("/{taskId}")
  public ApiResponse<ApiModels.Task> get(@PathVariable long taskId) {
    return ApiModels.response(worker.get(taskId), ApiModels.Task.class);
  }

  @ApiOperation(value = "显式人工重试失败任务；自动重试次数仍有上限", response = ApiModels.TaskResponse.class)
  @io.swagger.annotations.ApiResponses({@io.swagger.annotations.ApiResponse(code = 200, message = "成功", response = ApiModels.TaskResponse.class)})
  @PostMapping("/{taskId}/retry")
  @AuditAction(value = "RETRY_UPLOAD", success = false)
  public ApiResponse<ApiModels.Task> retry(@PathVariable long taskId) {
    return ApiModels.response(worker.retry(taskId), ApiModels.Task.class);
  }
}
