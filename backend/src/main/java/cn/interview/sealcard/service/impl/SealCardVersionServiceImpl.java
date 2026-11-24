package cn.interview.sealcard.service.impl;

import cn.interview.sealcard.domain.SealCardVersion;
import cn.interview.sealcard.mapper.SealCardVersionMapper;
import cn.interview.sealcard.service.SealCardVersionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 印鉴卡版本与复核记录 服务实现类
 *
 * @author zc
 * @since 2026-11-24
 */
@Service
public class SealCardVersionServiceImpl extends ServiceImpl<SealCardVersionMapper, SealCardVersion>
    implements SealCardVersionService {}
