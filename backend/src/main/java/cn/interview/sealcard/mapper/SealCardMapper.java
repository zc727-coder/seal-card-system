package cn.interview.sealcard.mapper;

import cn.interview.sealcard.domain.SealCard;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 印鉴卡主表 Mapper 接口
 *
 * @author zc
 * @since 2026-11-24
 */
@Mapper
public interface SealCardMapper extends BaseMapper<SealCard> {}
