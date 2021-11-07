package com.yangjie.dynamicquartz.util;

import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;

@Slf4j
public class TimerJobListener implements JobListener {

    /**
     * 用于获取该JobListener的名称。
     * @return
     */
    @Override
    public String getName() {
        return null;
    }

    /**
     * Scheduler在JobDetail将要被执行时调用这个方法。
     * @param context
     */
    @Override
    public void jobToBeExecuted(JobExecutionContext context) {

    }

    /**
     * Scheduler在JobDetail即将被执行，但又被TriggerListerner否决时会调用该方法
     * @param context
     */
    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {

    }

    /**
     * Scheduler在JobDetail被执行之后调用这个方法
     * @param context
     * @param jobException
     */
    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {

    }
}
