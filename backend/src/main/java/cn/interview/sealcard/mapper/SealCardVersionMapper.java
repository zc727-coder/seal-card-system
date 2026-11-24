package cn.interview.sealcard.mapper;

import cn.interview.sealcard.domain.SealCardVersion;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 印鉴卡版本与复核记录 Mapper 接口
 *
 * @author zc
 * @since 2026-11-24
 */
@Mapper
public interface SealCardVersionMapper extends BaseMapper<SealCardVersion> {}
