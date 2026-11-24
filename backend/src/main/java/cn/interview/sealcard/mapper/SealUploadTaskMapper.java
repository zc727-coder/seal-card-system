package cn.interview.sealcard.mapper;

import cn.interview.sealcard.domain.SealUploadTask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 上传幂等与可恢复补偿任务 Mapper 接口
 *
 * @author zc
 * @since 2026-11-24
 */
@Mapper
public interface SealUploadTaskMapper extends BaseMapper<SealUploadTask> {}
