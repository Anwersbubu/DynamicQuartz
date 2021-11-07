package com.yangjie.dynamicquartz.service;

import com.yangjie.dynamicquartz.dto.ModifyCronDTO;
import com.yangjie.dynamicquartz.entity.JobEntity;
import org.quartz.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface DynamicJobService {

    //通过Id获取Job
    JobEntity getJobEntityById(Integer id);

    //从数据库中加载获取到所有Job
    List<JobEntity> loadJobs();

    //添加job
    void addJob(Scheduler scheduler,JobEntity jobEntity);

    //暂停
    void pausejob(String jobClassName, String jobGroupName);

    //恢复
    void resumejob(String jobClassName, String jobGroupName);

    //删除
    void deleteJob(String jobClassName, String jobGroupName);

    //修改
    String modifyJob(ModifyCronDTO dto);

    //根据ID重启
    String refresh(Integer id);

    //重启所有
    String reStartAllJobs();

}
