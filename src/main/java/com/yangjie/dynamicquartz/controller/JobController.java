package com.yangjie.dynamicquartz.controller;

import com.github.pagehelper.PageInfo;
import com.yangjie.dynamicquartz.dto.ModifyCronDTO;
import com.yangjie.dynamicquartz.entity.JobEntity;
import com.yangjie.dynamicquartz.mapper.JobEntityMapper;
import com.yangjie.dynamicquartz.service.DynamicJobService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@RestController
@Slf4j
@RequestMapping(value="/job")
public class JobController {

    @Autowired
    private SchedulerFactoryBean schedulerFactoryBean;
    @Autowired
    private DynamicJobService jobService;

    @Autowired
    private JobEntityMapper repository;

    //初始化启动所有的Job
    @PostConstruct
    public void initialize() {
        try {
            reStartAllJobs();
            log.info("初始化成功");
        } catch (SchedulerException e) {
            log.error("初始化失败，详细: ", e);
        }
    }

    /**
     * 分页查询
     * @param pageNum
     * @param pageSize
     * @return
     */
    @GetMapping("/queryjob")
    public Map<String, Object> queryjob(@RequestParam(value="pageNum")Integer pageNum, @RequestParam(value="pageSize")Integer pageSize) {
        PageInfo jobAndTrigger = jobService.getJobAndTriggerDetails(pageNum, pageSize);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("JobAndTrigger", jobAndTrigger);
        map.put("number", jobAndTrigger.getTotal());
        return map;
    }

    /**
     * 添加
     * @param
     */
    @PostMapping("/addjob")
    public void addjob(@RequestParam(value="jobEntity")JobEntity jobEntity) {
//        JobEntity jobEntity = new JobEntity(null,"test","test01","0/3 * * * * ? *","127","测试",null,null,"OPEN");
        jobService.addJob(schedulerFactoryBean.getScheduler(),jobEntity);
    }

    /**
     * 暂停
     * @param jobClassName
     * @param jobGroupName
     * @throws Exception
     */
    @PostMapping("/pausejob")
    public void pausejob(@RequestParam(value="jobClassName")String jobClassName, @RequestParam(value="jobGroupName")String jobGroupName) {
        try {
            schedulerFactoryBean.getScheduler().pauseJob(JobKey.jobKey(jobClassName, jobGroupName));
        } catch (SchedulerException e) {
            System.err.println("暂停"+jobClassName+"失败");
            e.printStackTrace();
        }
    }

    /**
     * 恢复
     * @param jobClassName
     * @param jobGroupName
     * @throws Exception
     */
    @PostMapping("/resumejob")
    public void resumejob(@RequestParam(value="jobClassName")String jobClassName, @RequestParam(value="jobGroupName")String jobGroupName) {
        try {
            schedulerFactoryBean.getScheduler().resumeJob(JobKey.jobKey(jobClassName, jobGroupName));
        } catch (SchedulerException e) {
            System.err.println("恢复"+jobClassName+"失败");
            e.printStackTrace();
        }
    }

    /**
     * 删除
     * @param jobClassName
     * @param jobGroupName
     * @throws Exception
     */
    @PostMapping(value="/deletejob")
    public void deletejob(@RequestParam(value="jobClassName")String jobClassName, @RequestParam(value="jobGroupName")String jobGroupName) {
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

    /**
     * 修改定时
     * @param dto
     * @return
     */
    @PostMapping("/modifyJob")
    public String modifyJob(@RequestBody @Validated ModifyCronDTO dto) {
        if (!CronExpression.isValidExpression(dto.getCron()))
            return "cron 错误 !";
        synchronized (log) {
            JobEntity job = jobService.getJobEntityById(dto.getId());
            if (job.getStatus().equals("OPEN")) {
                try {
                    JobKey jobKey = jobService.getJobKey(job);
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
                                .usingJobData(jobService.getJobDataMap(job))
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

    //根据ID重启某个Job
    @GetMapping("/refresh/{id}")
    public String refresh(@PathVariable Integer id) throws SchedulerException {
        String result;
        JobEntity entity = jobService.getJobEntityById(id);
        if (Objects.isNull(entity))
            return "error: id is not exist ";
        synchronized (log) {
            JobKey jobKey = jobService.getJobKey(entity);
            Scheduler scheduler = schedulerFactoryBean.getScheduler();
            /**
             * 1、停止触发器
             * 2、移除触发器
             * 3、删除触发器
             */
            scheduler.pauseJob(jobKey);
            scheduler.unscheduleJob(TriggerKey.triggerKey(jobKey.getName(), jobKey.getGroup()));
            scheduler.deleteJob(jobKey);
            JobDataMap map = jobService.getJobDataMap(entity);
            JobDetail jobDetail = jobService.getJobDetail(jobKey, entity.getDescription(), map);
            if (entity.getStatus().equals("OPEN")) {
                scheduler.scheduleJob(jobDetail, jobService.getTrigger(entity));
                result = "Refresh Job : " + entity.getName() + "\t jarPath: " + entity.getJarPath() + " success !";
            } else {
                result = "Refresh Job : " + entity.getName() + "\t jarPath: " + entity.getJarPath() + " failed ! , " +
                        "Because the Job status is " + entity.getStatus();
            }
        }
        return result;
    }


    //重启数据库中所有的Job
    @RequestMapping("/refresh/all")
    public String refreshAll() {
        String result;
        try {
            reStartAllJobs();
            result = "success";
        } catch (SchedulerException e) {
            result = "exception : " + e.getMessage();
        }
        return "refresh all jobs : " + result;
    }

    /**
     * 重新启动所有的job
     */
    private void reStartAllJobs() throws SchedulerException {
        synchronized (log) {                                                         //只允许一个线程进入操作
            Scheduler scheduler = schedulerFactoryBean.getScheduler();
            Set<JobKey> set = scheduler.getJobKeys(GroupMatcher.anyGroup());
            scheduler.pauseJobs(GroupMatcher.anyGroup());                               //暂停所有JOB
            for (JobKey jobKey : set) {                                                 //删除从数据库中注册的所有JOB
                scheduler.unscheduleJob(TriggerKey.triggerKey(jobKey.getName(), jobKey.getGroup()));
                scheduler.deleteJob(jobKey);
            }
            for (JobEntity job : jobService.loadJobs()) {                               //从数据库中注册的所有JOB
                log.info("Job register name : {} , group : {} , cron : {}", job.getName(), job.getJobGroup(), job.getCron());
                JobDataMap map = jobService.getJobDataMap(job);
                JobKey jobKey = jobService.getJobKey(job);
                JobDetail jobDetail = jobService.getJobDetail(jobKey, job.getDescription(), map);
                if (job.getStatus().equals("OPEN")) scheduler.scheduleJob(jobDetail, jobService.getTrigger(job));
                else
                    log.info("Job jump name : {} , Because {} status is {}", job.getName(), job.getName(), job.getStatus());
            }
        }
    }




}
