package cn.interview.sealcard.service.impl;

import cn.interview.sealcard.domain.SealUploadTask;
import cn.interview.sealcard.mapper.SealUploadTaskMapper;
import cn.interview.sealcard.service.SealUploadTaskService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 上传幂等与可恢复补偿任务 服务实现类
 *
 * @author zc
 * @since 2026-11-24
 */
@Service
public class SealUploadTaskServiceImpl extends ServiceImpl<SealUploadTaskMapper, SealUploadTask>
    implements SealUploadTaskService {}
