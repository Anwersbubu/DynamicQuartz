package com.yangjie.dynamicquartz.controller;

import com.github.pagehelper.PageInfo;
import com.yangjie.dynamicquartz.dto.ModifyCronDTO;
import com.yangjie.dynamicquartz.entity.JobEntity;
import com.yangjie.dynamicquartz.mapper.JobEntityMapper;
import com.yangjie.dynamicquartz.service.DynamicJobService;
import com.yangjie.dynamicquartz.service.JobAndTriggerService;
import com.yangjie.dynamicquartz.util.JobTriggUtil;
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
    private JobAndTriggerService jobAndTriggerService;

    @Autowired
    private JobEntityMapper repository;

    //初始化启动所有的Job
    @PostConstruct
    public void initialize() {
        String reslut = jobService.reStartAllJobs();
        log.info("初始化"+reslut);
    }

    /**
     * 分页查询
     * @param pageNum
     * @param pageSize
     * @return
     */
    @GetMapping("/queryjob")
    public Map<String, Object> queryjob(@RequestParam(value="pageNum")Integer pageNum, @RequestParam(value="pageSize")Integer pageSize) {
        PageInfo jobAndTrigger = jobAndTriggerService.getJobAndTriggerDetails(pageNum, pageSize);
        Map<String, Object> map = new HashMap<>();
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
        jobService.pausejob(jobClassName,jobGroupName);
    }

    /**
     * 恢复
     * @param jobClassName
     * @param jobGroupName
     * @throws Exception
     */
    @PostMapping("/resumejob")
    public void resumejob(@RequestParam(value="jobClassName")String jobClassName, @RequestParam(value="jobGroupName")String jobGroupName) {
        jobService.resumejob(jobClassName,jobGroupName);
    }

    /**
     * 删除
     * @param jobClassName
     * @param jobGroupName
     * @throws Exception
     */
    @PostMapping(value="/deletejob")
    public void deletejob(@RequestParam(value="jobClassName")String jobClassName, @RequestParam(value="jobGroupName")String jobGroupName) {
        jobService.deleteJob(jobClassName, jobGroupName);
    }

    /**
     * 修改定时
     * @param dto
     * @return
     */
    @PostMapping("/modifyJob")
    public String modifyJob(@RequestBody @Validated ModifyCronDTO dto) {
        return jobService.modifyJob(dto);
    }

    //根据ID重启某个Job
    @GetMapping("/refresh/{id}")
    public String refresh(@PathVariable Integer id) throws SchedulerException {
        return jobService.refresh(id);
    }


    //重启数据库中所有的Job
    @RequestMapping("/refresh/all")
    public String refreshAll() {
        return "重启所有定时任务 : " + jobService.reStartAllJobs();
    }

}
