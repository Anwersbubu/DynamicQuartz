package com.yangjie.dynamicquartz.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yangjie.dynamicquartz.entity.JobAndTrigger;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Created by EalenXie on 2018/6/4 14:27
 */
@Mapper
public interface JobAndTriggerMapper extends BaseMapper<JobAndTrigger> {

    public List<com.yangjie.dynamicquartz.entity.JobAndTrigger> getJobAndTriggerDetails();

}
