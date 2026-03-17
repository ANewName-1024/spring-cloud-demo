package com.example.config.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.info.BuildProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 系统健康检查定时任务
 * 
 * 功能：
 * 1. 检查系统资源使用情况
 * 2. 记录健康日志
 * 3. 告警异常状态
 */
@Component
@ConditionalOnProperty(name = "scheduler.health-check.enabled", havingValue = "true", matchIfMissing = true)
public class HealthCheckTask {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckTask.class);

    private static final double MEMORY_THRESHOLD = 0.85; // 内存阈值 85%
    private static final double CPU_THRESHOLD = 0.80; // CPU 阈值 80%

    @Autowired(required = false)
    private BuildProperties buildProperties;

    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

    /**
     * 健康检查任务
     * 每 5 分钟执行一次
     */
    @Scheduled(fixedRate = 300000)
    public void healthCheck() {
        logger.debug("开始系统健康检查...");
        
        try {
            Map<String, Object> health = collectHealthInfo();
            
            // 检查内存
            double memoryUsage = (double) health.get("memoryUsage");
            if (memoryUsage > MEMORY_THRESHOLD) {
                logger.warn("内存使用率过高: {:.2f}%", memoryUsage * 100);
            }
            
            // 检查线程
            int threadCount = (int) health.get("threadCount");
            long peakThreadCount = threadBean.getPeakThreadCount();
            if (threadCount > peakThreadCount * 0.9) {
                logger.warn("线程数接近峰值: {}/{}", threadCount, peakThreadCount);
            }
            
            logger.debug("健康检查完成: {}", health);
        } catch (Exception e) {
            logger.error("健康检查失败", e);
        }
    }

    /**
     * 应用信息日志
     * 每小时执行一次
     */
    @Scheduled(fixedRate = 3600000)
    public void logApplicationInfo() {
        logger.info("========== 应用信息 ==========");
        logger.info("应用名称: {}", buildProperties != null ? buildProperties.getName() : "unknown");
        logger.info("版本: {}", buildProperties != null ? buildProperties.getVersion() : "unknown");
        logger.info("构建时间: {}", buildProperties != null ? buildProperties.getTime() : "unknown");
        logger.info("启动时间: {}", LocalDateTime.now());
        
        // 记录系统信息
        Runtime runtime = Runtime.getRuntime();
        logger.info("CPU 核心数: {}", runtime.availableProcessors());
        logger.info("最大内存: {} MB", runtime.maxMemory() / 1024 / 1024);
        logger.info("================================");
    }

    /**
     * 收集健康信息
     */
    private Map<String, Object> collectHealthInfo() {
        Map<String, Object> health = new HashMap<>();
        
        // 堆内存使用
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax();
        double memoryUsage = (double) heapUsed / heapMax;
        
        health.put("memoryUsage", memoryUsage);
        health.put("heapUsed", heapUsed);
        health.put("heapMax", heapMax);
        
        // 线程信息
        health.put("threadCount", threadBean.getThreadCount());
        health.put("peakThreadCount", threadBean.getPeakThreadCount());
        
        // 系统负载
        double systemLoad = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
        health.put("systemLoad", systemLoad);
        
        return health;
    }
}
