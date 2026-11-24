package cn.interview.sealcard.service.impl;

import cn.interview.sealcard.domain.SealCard;
import cn.interview.sealcard.mapper.SealCardMapper;
import cn.interview.sealcard.service.SealCardService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 印鉴卡主表 服务实现类
 *
 * @author zc
 * @since 2026-11-24
 */
@Service
public class SealCardServiceImpl extends ServiceImpl<SealCardMapper, SealCard>
    implements SealCardService {}
