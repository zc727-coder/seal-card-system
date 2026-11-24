package cn.interview.sealcard.mapper;

import cn.interview.sealcard.domain.SealCardAuditLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 追加审计；无外键以允许失败/未找到目标留痕 Mapper 接口
 *
 * @author zc
 * @since 2026-11-24
 */
@Mapper
public interface SealCardAuditLogMapper extends BaseMapper<SealCardAuditLog> {}
