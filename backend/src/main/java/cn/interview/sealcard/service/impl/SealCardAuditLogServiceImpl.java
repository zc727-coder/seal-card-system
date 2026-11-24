package cn.interview.sealcard.service.impl;

import cn.interview.sealcard.domain.SealCardAuditLog;
import cn.interview.sealcard.mapper.SealCardAuditLogMapper;
import cn.interview.sealcard.service.SealCardAuditLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 追加审计；无外键以允许失败/未找到目标留痕 服务实现类
 *
 * @author zc
 * @since 2026-11-24
 */
@Service
public class SealCardAuditLogServiceImpl
    extends ServiceImpl<SealCardAuditLogMapper, SealCardAuditLog>
    implements SealCardAuditLogService {}
