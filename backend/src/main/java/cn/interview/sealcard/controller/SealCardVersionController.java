package cn.interview.sealcard.controller;

import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 印鉴卡版本与复核记录 前端控制器
 *
 * @author zc
 * @since 2026-11-24
 */
@Api(tags = "印鉴卡版本", description = "影像版本、复核记录与生效区间")
@RestController
@RequestMapping("/sealCardVersion")
public class SealCardVersionController {}
