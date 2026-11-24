package cn.interview.sealcard.service.impl;

import cn.interview.sealcard.domain.SealCardAccount;
import cn.interview.sealcard.mapper.SealCardAccountMapper;
import cn.interview.sealcard.service.SealCardAccountService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 按审批版本保存的账户适用范围 服务实现类
 *
 * @author zc
 * @since 2026-11-24
 */
@Service
public class SealCardAccountServiceImpl extends ServiceImpl<SealCardAccountMapper, SealCardAccount>
    implements SealCardAccountService {}
