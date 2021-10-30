package com.yangjie.dynamicquartz.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yangjie.dynamicquartz.entity.JobAndTrigger;
import com.yangjie.dynamicquartz.entity.JobEntity;
import com.yangjie.dynamicquartz.job.DynamicJob;
import com.yangjie.dynamicquartz.mapper.JobAndTriggerMapper;
import com.yangjie.dynamicquartz.mapper.JobEntityMapper;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class DynamicJobServiceImpl implements DynamicJobService {

    @Autowired
    private JobEntityMapper repository;

    @Autowired
    private JobAndTriggerMapper jobAndTriggerMapper;

    @Override
    public PageInfo getJobAndTriggerDetails(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<JobAndTrigger> list = jobAndTriggerMapper.getJobAndTriggerDetails();
        PageInfo<JobAndTrigger> page = new PageInfo<JobAndTrigger>(list);
        return page;
    }

    //通过Id获取Job
    @Override
    public JobEntity getJobEntityById(Integer id) {
        return repository.selectById(id);
    }

    @Override
    public void addJob(Scheduler scheduler, JobEntity jobEntity){
        Date date = null;
        try {
                // 启动调度器
                scheduler.start();
                //构建job信息
                JobDetail jobDetail = JobBuilder.newJob(DynamicJob.class)
                        .withIdentity(jobEntity.getName(), jobEntity.getJobGroup())
                        .build();
                //表达式调度构建器(即任务执行的时间)
                CronScheduleBuilder scheduleBuilder = CronScheduleBuilder
                        .cronSchedule(jobEntity.getCron());
                //按新的cronExpression表达式构建一个新的trigger
                CronTrigger trigger = TriggerBuilder.newTrigger()
                        .withIdentity(jobEntity.getName(), jobEntity.getJobGroup())
                        .withSchedule(scheduleBuilder)
                        .build();
                date = scheduler.scheduleJob(jobDetail, trigger);
                repository.insert(jobEntity);
        } catch (SchedulerException e) {
                System.err.println("创建定时任务失败"+e);
                e.printStackTrace();
        }
    };

    //从数据库中加载获取到所有Job
    @Override
    public List<JobEntity> loadJobs() {
        return repository.selectList(null);
    }

    //获取JobDataMap.(Job参数对象)
    @Override
    public JobDataMap getJobDataMap(JobEntity job) {
        JobDataMap map = new JobDataMap();
        map.put("name", job.getName());
        map.put("jobGroup", job.getJobGroup());
        map.put("cron", job.getCron());
        map.put("parameter", job.getParameter());
        map.put("description", job.getDescription());
        map.put("vmParam", job.getVmParam());
        map.put("jarPath", job.getJarPath());
        map.put("status", job.getStatus());
        return map;
    }

    //获取JobDetail,JobDetail是任务的定义,而Job是任务的执行逻辑,JobDetail里会引用一个Job Class来定义
    @Override
    public JobDetail getJobDetail(JobKey jobKey, String description, JobDataMap map) {
        return JobBuilder.newJob(DynamicJob.class)
                .withIdentity(jobKey)
                .withDescription(description)
                .setJobData(map)
                .storeDurably()
                .build();
    }

    //获取Trigger (Job的触发器,执行规则)
    @Override
    public Trigger getTrigger(JobEntity job) {
        return TriggerBuilder.newTrigger()
                .withIdentity(job.getName(), job.getJobGroup())
                .withSchedule(CronScheduleBuilder.cronSchedule(job.getCron()))
                .build();
    }

    //获取JobKey,包含Name和Group
    @Override
    public JobKey getJobKey(JobEntity job) {
        return JobKey.jobKey(job.getName(), job.getJobGroup());
    }
}
