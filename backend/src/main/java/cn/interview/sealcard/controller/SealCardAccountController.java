package cn.interview.sealcard.controller;

import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 账户适用关系 前端控制器
 *
 * @author zc
 * @since 2026-11-24
 */
@Api(tags = "账户适用关系", description = "按审批版本保存的单位结算账户范围")
@RestController
@RequestMapping("/sealCardAccount")
public class SealCardAccountController {}
