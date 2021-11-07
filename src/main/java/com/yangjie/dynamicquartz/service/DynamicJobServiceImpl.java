package com.yangjie.dynamicquartz.service;

import com.yangjie.dynamicquartz.dto.ModifyCronDTO;
import com.yangjie.dynamicquartz.entity.JobEntity;
import com.yangjie.dynamicquartz.job.DynamicJob;
import com.yangjie.dynamicquartz.listener.TimerTriggerListener;
import com.yangjie.dynamicquartz.mapper.JobEntityMapper;
import com.yangjie.dynamicquartz.util.JobTriggUtil;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
public class DynamicJobServiceImpl implements DynamicJobService {

    @Autowired
    private SchedulerFactoryBean schedulerFactoryBean;

    @Autowired
    private JobEntityMapper repository;

    //通过Id获取Job
    @Override
    public JobEntity getJobEntityById(Integer id) {
        return repository.selectById(id);
    }

    //添加任务
    @Override
    public void addJob(Scheduler scheduler, JobEntity jobEntity){
        Date date;
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
                scheduler.getListenerManager().addTriggerListener(new TimerTriggerListener(jobEntity.getName()));
                repository.insert(jobEntity);
        } catch (SchedulerException e) {
                System.err.println("创建定时任务失败"+e);
                e.printStackTrace();
        }
    }

    //暂停任务
    @Override
    public void pausejob(String jobClassName, String jobGroupName) {
        try {
            schedulerFactoryBean.getScheduler().pauseJob(JobKey.jobKey(jobClassName, jobGroupName));
            log.info("暂停"+jobClassName+"成功");
        } catch (SchedulerException e) {
            log.info("暂停"+jobClassName+"失败");
            e.printStackTrace();
        }
    }

    //恢复任务
    @Override
    public void resumejob(String jobClassName, String jobGroupName) {
        try {
            schedulerFactoryBean.getScheduler().resumeJob(JobKey.jobKey(jobClassName, jobGroupName));
            log.info("恢复"+jobClassName+"成功");
        } catch (SchedulerException e) {
            log.info("恢复"+jobClassName+"失败");
            e.printStackTrace();
        }
    }

    @Override
    public void deleteJob(String jobClassName, String jobGroupName) {
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        try {
            scheduler.pauseTrigger(TriggerKey.triggerKey(jobClassName, jobGroupName));
            scheduler.unscheduleJob(TriggerKey.triggerKey(jobClassName, jobGroupName));
            scheduler.deleteJob(JobKey.jobKey(jobClassName, jobGroupName));
        } catch (SchedulerException e) {
            System.err.println("删除"+jobClassName+"失败");
            e.printStackTrace();
        }
    }

    @Override
    public String modifyJob(ModifyCronDTO dto) {
        if (!CronExpression.isValidExpression(dto.getCron()))
            return "cron 错误 !";
        synchronized (log) {
            JobEntity job = getJobEntityById(dto.getId());
            if (job.getStatus().equals("OPEN")) {
                try {
                    JobKey jobKey = JobTriggUtil.getJobKey(job);
                    TriggerKey triggerKey = TriggerKey.triggerKey(jobKey.getName(), jobKey.getGroup());
                    Scheduler scheduler = schedulerFactoryBean.getScheduler();
                    CronTrigger cronTrigger = (CronTrigger) scheduler.getTrigger(triggerKey);
                    String oldCron = cronTrigger.getCronExpression();
                    if (!oldCron.equalsIgnoreCase(dto.getCron())) {
                        job.setCron(dto.getCron());
                        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(dto.getCron());
                        CronTrigger trigger = TriggerBuilder.newTrigger()
                                .withIdentity(jobKey.getName(), jobKey.getGroup())
                                .withSchedule(cronScheduleBuilder)
                                .usingJobData(JobTriggUtil.getJobDataMap(job))
                                .build();
                        scheduler.rescheduleJob(triggerKey, trigger);
                        repository.update(job,null);
                    }
                } catch (Exception e) {
                    log.error("printStackTrace", e);
                }
            } else {
                log.info("Job jump name : {} , Because {} status is {}", job.getName(), job.getName(), job.getStatus());
                return "modify failure , because the job is closed";
            }
        }
        return "modify success";
    }

    @Override
    public String refresh(Integer id) {
        String result = null;
        JobEntity entity = getJobEntityById(id);
        if (Objects.isNull(entity))
            return "error: id is not exist ";
        synchronized (log) {
            JobKey jobKey = JobTriggUtil.getJobKey(entity);
            Scheduler scheduler = schedulerFactoryBean.getScheduler();
            /*
              1、停止触发器
              2、移除触发器
              3、删除触发器
             */
            try {
                scheduler.pauseJob(jobKey);
                scheduler.unscheduleJob(TriggerKey.triggerKey(jobKey.getName(), jobKey.getGroup()));
                scheduler.deleteJob(jobKey);
                JobDataMap map = JobTriggUtil.getJobDataMap(entity);
                JobDetail jobDetail = JobTriggUtil.getJobDetail(jobKey, entity.getDescription(), map);
                if (entity.getStatus().equals("OPEN")) {
                    scheduler.scheduleJob(jobDetail, JobTriggUtil.getTrigger(entity));
                    result = "Refresh Job : " + entity.getName() + "\t jarPath: " + entity.getJarPath() + " success !";
                } else {
                    result = "Refresh Job : " + entity.getName() + "\t jarPath: " + entity.getJarPath() + " failed ! , " +
                            "Because the Job status is " + entity.getStatus();
                }
            } catch (SchedulerException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    @Override
    public String reStartAllJobs() {
        String result;
        synchronized (log) {                                                         //只允许一个线程进入操作
            Scheduler scheduler = schedulerFactoryBean.getScheduler();
            Set<JobKey> set;
            try {
                set = scheduler.getJobKeys(GroupMatcher.anyGroup());
                scheduler.pauseJobs(GroupMatcher.anyGroup());                               //暂停所有JOB
                for (JobKey jobKey : set) {                                                 //删除从数据库中注册的所有JOB
                    scheduler.unscheduleJob(TriggerKey.triggerKey(jobKey.getName(), jobKey.getGroup()));
                    scheduler.deleteJob(jobKey);
                }
                for (JobEntity job : loadJobs()) {                               //从数据库中注册的所有JOB
                    log.info("Job register name : {} , group : {} , cron : {}", job.getName(), job.getJobGroup(), job.getCron());
                    JobDataMap map = JobTriggUtil.getJobDataMap(job);
                    JobKey jobKey = JobTriggUtil.getJobKey(job);
                    JobDetail jobDetail = JobTriggUtil.getJobDetail(jobKey, job.getDescription(), map);
                    if (job.getStatus().equals("OPEN")) scheduler.scheduleJob(jobDetail, JobTriggUtil.getTrigger(job));
                    else
                        log.info("Job jump name : {} , Because {} status is {}", job.getName(), job.getName(), job.getStatus());
                }
                result = "成功";
            } catch (SchedulerException e) {
                e.printStackTrace();
                result = "失败,详细信息:"+e;
            }
            return result;
        }
    }

    //从数据库中加载获取到所有Job
    @Override
    public List<JobEntity> loadJobs() {
        return repository.selectList(null);
    }

}
