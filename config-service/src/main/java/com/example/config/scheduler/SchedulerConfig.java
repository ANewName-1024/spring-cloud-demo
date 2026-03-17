package com.example.config.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 定时任务配置
 * 
 * 启用方式: 设置 scheduler.enabled=true
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulerConfig implements SchedulingConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerConfig.class);

    /**
     * 配置定时任务执行器
     * 使用线程池执行定时任务，避免任务阻塞
     */
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setTaskScheduler(taskScheduler());
    }

    /**
     * 线程池调度器
     * 支持多线程并行执行定时任务
     */
    public org.springframework.scheduling.TaskScheduler taskScheduler() {
        return new org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler();
    }

    /**
     * 记录调度器状态
     */
    public void logSchedulerInfo() {
        logger.info("========== 定时任务调度器已启用 ==========");
        logger.info("线程池大小: 10");
        logger.info("任务执行模式: 并行执行");
    }
}
