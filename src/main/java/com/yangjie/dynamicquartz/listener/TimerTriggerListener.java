package com.yangjie.dynamicquartz.listener;

import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.TriggerListener;

/**
 * 跟触发器有关的事件包括：触发器被触发，触发器触发失败，以及触发器触发完成（触发器完成后作业任务开始运行）。
 */

@Slf4j
public class TimerTriggerListener implements TriggerListener {

    private String timerTriggerName;

    public TimerTriggerListener(String timerTriggerName) {
        this.timerTriggerName = timerTriggerName;
    }

    /**
     * 用于获取触发器的名称
     * @return
     */
    @Override
    public String getName() {
        return this.timerTriggerName;
    }

    /**
     * 当与监听器相关联的Trigger被触发，Job上的execute()方法将被执行时，Scheduler就调用该方法。
     * @param trigger
     * @param context
     */
    @Override
    public void triggerFired(Trigger trigger, JobExecutionContext context) {

    }

    /**
     * 在 Trigger 触发后，Job 将要被执行时由 Scheduler 调用这个方法。TriggerListener 给了一个选择去否决 Job 的执行。假如这个方法返回 true，这个 Job 将不会为此次 Trigger 触发而得到执行。
     * @param trigger
     * @param context
     * @return
     */
    @Override
    public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context) {
        return false;
    }

    /**
     * Scheduler 调用这个方法是在 Trigger 错过触发时。
     * @param trigger
     */
    @Override
    public void triggerMisfired(Trigger trigger) {

    }

    /**
     * Trigger 被触发并且完成了 Job 的执行时，Scheduler 调用这个方法。
     * @param trigger
     * @param context
     * @param triggerInstructionCode
     */
    @Override
    public void triggerComplete(Trigger trigger, JobExecutionContext context, Trigger.CompletedExecutionInstruction triggerInstructionCode) {

    }
}
