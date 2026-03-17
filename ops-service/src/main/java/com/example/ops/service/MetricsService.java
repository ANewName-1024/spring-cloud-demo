package com.example.ops.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 指标服务
 */
@Service
public class MetricsService {

    private static final Logger logger = LoggerFactory.getLogger(MetricsService.class);

    @Autowired(required = false)
    private MetricsEndpoint metricsEndpoint;

    private final AtomicLong requestCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);

    /**
     * 记录请求
     */
    public void recordRequest() {
        requestCount.incrementAndGet();
    }

    /**
     * 记录错误
     */
    public void recordError() {
        errorCount.incrementAndGet();
    }

    /**
     * 获取系统指标
     */
    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // 请求统计
        metrics.put("requests", Map.of(
                "total", requestCount.get(),
                "errors", errorCount.get(),
                "errorRate", errorCount.get() * 100.0 / Math.max(requestCount.get(), 1)
        ));

        // JVM 内存
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        Map<String, Object> memory = new HashMap<>();
        memory.put("heap", Map.of(
                "used", memoryMXBean.getHeapMemoryUsage().getUsed(),
                "max", memoryMXBean.getHeapMemoryUsage().getMax(),
                "committed", memoryMXBean.getHeapMemoryUsage().getCommitted()
        ));
        memory.put("nonHeap", Map.of(
                "used", memoryMXBean.getNonHeapMemoryUsage().getUsed(),
                "max", memoryMXBean.getNonHeapMemoryUsage().getMax()
        ));
        metrics.put("memory", memory);

        // 系统负载
        OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();
        Map<String, Object> system = new HashMap<>();
        system.put("availableProcessors", osMXBean.getAvailableProcessors());
        system.put("systemLoadAverage", osMXBean.getSystemLoadAverage());
        metrics.put("system", system);

        // 线程信息
        Map<String, Object> threads = new HashMap<>();
        threads.put("count", ManagementFactory.getThreadMXBean().getThreadCount());
        threads.put("peak", ManagementFactory.getThreadMXBean().getPeakThreadCount());
        metrics.put("threads", threads);

        return metrics;
    }

    /**
     * 获取日志级别
     */
    public Map<String, String> getLoggerLevels() {
        Map<String, String> loggers = new HashMap<>();
        loggers.put("root", "INFO");
        loggers.put("com.example", "DEBUG");
        loggers.put("org.springframework", "INFO");
        loggers.put("org.springframework.cloud.sleuth", "DEBUG");
        return loggers;
    }
}
