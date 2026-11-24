package cn.interview.sealcard.service;

import cn.interview.sealcard.domain.SealCardAuditLog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 追加审计；无外键以允许失败/未找到目标留痕 服务类
 *
 * @author zc
 * @since 2026-11-24
 */
public interface SealCardAuditLogService extends IService<SealCardAuditLog> {}
