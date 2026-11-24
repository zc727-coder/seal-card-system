package cn.interview.sealcard.service.impl;

import cn.interview.sealcard.domain.SealCardImage;
import cn.interview.sealcard.mapper.SealCardImageMapper;
import cn.interview.sealcard.service.SealCardImageService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 影像索引，不存图片正文 服务实现类
 *
 * @author zc
 * @since 2026-11-24
 */
@Service
public class SealCardImageServiceImpl extends ServiceImpl<SealCardImageMapper, SealCardImage>
    implements SealCardImageService {}
