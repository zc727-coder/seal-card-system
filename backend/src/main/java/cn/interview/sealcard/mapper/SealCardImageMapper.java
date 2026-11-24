package cn.interview.sealcard.mapper;

import cn.interview.sealcard.domain.SealCardImage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 影像索引，不存图片正文 Mapper 接口
 *
 * @author zc
 * @since 2026-11-24
 */
@Mapper
public interface SealCardImageMapper extends BaseMapper<SealCardImage> {}
