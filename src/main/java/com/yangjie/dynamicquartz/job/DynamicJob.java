package com.yangjie.dynamicquartz.job;

import com.alibaba.druid.util.StringUtils;
import com.yangjie.dynamicquartz.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@Slf4j
@DisallowConcurrentExecution
public class DynamicJob  extends QuartzJobBean {

    /**
     * 核心方法,Quartz Job真正的执行逻辑.
     *
     * @param executorContext executorContext JobExecutionContext中封装有Quartz运行所需要的所有信息
     * @throws JobExecutionException execute()方法只允许抛出JobExecutionException异常
     */
    @Override
    public void executeInternal(JobExecutionContext executorContext) throws JobExecutionException {
        //JobDetail中的JobDataMap是共用的,从getMergedJobDataMap获取的JobDataMap是全新的对象
        JobDataMap map = executorContext.getMergedJobDataMap();
        String jarPath = map.getString("jarPath");
        String parameter = map.getString("parameter");
        String vmParam = map.getString("vmParam");

        //Job相关参数的打印
        log.info("执行中的 Job 名 : {} ", map.getString("name"));
        log.info("执行中的 Job 组别: {} ", map.getString("jobGroup"));
        log.info("执行中的 Job 描述 : {}", map.getString("description"));
        log.info(String.format("执行中的 Job cron : %s", map.getString("cron")));
        log.info("执行中的 Job parameter : {} ", parameter);
        log.info("执行中的 Job vmParam : {} ", vmParam);
        log.info("执行中的 Job jar path : {} ", jarPath);

        long startTime = System.currentTimeMillis();

        if (!StringUtils.isEmpty(jarPath)) {
            File jar = new File(jarPath);
            if (jar.exists()) {
                //使用ProcessBuilder来执行外部程序
                ProcessBuilder processBuilder = new ProcessBuilder();
                processBuilder.directory(jar.getParentFile());
                List<String> commands = new ArrayList<>();
                commands.add("java");
                if (!StringUtils.isEmpty(vmParam))
                    commands.add(vmParam);
                commands.add("-jar");
                commands.add(jarPath);
                if (!com.alibaba.druid.util.StringUtils.isEmpty(parameter))
                    commands.add(parameter);
                processBuilder.command(commands);
                log.info("执行中的 Job 初始化 : >>>>>>>>>>>>>>>>>>>>: ");
                log.info("执行中的 Job 工作项 : {}  ", StringUtil.getListString(commands));
                try {
                        Process process = processBuilder.start();
                        logProcess(process.getInputStream(), process.getErrorStream());
                } catch (IOException e) {
                    throw new JobExecutionException(e);
                }
            } else
                throw new JobExecutionException("jar 包未找到 >>  " + jarPath);
        }
        long endTime = System.currentTimeMillis();
        log.info(">>>>>>>>>>>>> Running Job has been completed , cost time : {}ms\n ", (endTime - startTime));
    }

    //记录Job执行内容
    private void logProcess(InputStream inputStream, InputStream errorStream) throws IOException {
        String inputLine;
        String errorLine;
        BufferedReader inputReader = new BufferedReader(new InputStreamReader(inputStream));
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream));
        while (Objects.nonNull(inputLine = inputReader.readLine())) log.info(inputLine);
        while (Objects.nonNull(errorLine = errorReader.readLine())) log.error(errorLine);
    }
}
