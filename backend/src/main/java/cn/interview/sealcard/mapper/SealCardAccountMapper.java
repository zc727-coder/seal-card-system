package cn.interview.sealcard.mapper;

import cn.interview.sealcard.domain.SealCardAccount;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 按审批版本保存的账户适用范围 Mapper 接口
 *
 * @author zc
 * @since 2026-11-24
 */
@Mapper
public interface SealCardAccountMapper extends BaseMapper<SealCardAccount> {}
