package com.example.ops.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.*;
import com.example.ops.service.MetricsService;

import java.util.HashMap;
import java.util.Map;

/**
 * 运维服务健康检查和指标接口
 */
@RestController
@RequestMapping("/ops")
@RefreshScope
public class OpsController {

    @Autowired
    private MetricsService metricsService;

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("service", "ops-service");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    /**
     * 获取服务指标
     */
    @GetMapping("/metrics")
    public Map<String, Object> metrics() {
        return metricsService.getMetrics();
    }

    /**
     * 获取日志级别
     */
    @GetMapping("/loggers")
    public Map<String, Object> loggers() {
        Map<String, Object> result = new HashMap<>();
        result.put("loggers", metricsService.getLoggerLevels());
        return result;
    }

    /**
     * 修改日志级别
     */
    @PostMapping("/loggers/{name}/{level}")
    public Map<String, String> setLogLevel(
            @PathVariable String name,
            @PathVariable String level) {
        Map<String, String> result = new HashMap<>();
        result.put("logger", name);
        result.put("level", level);
        result.put("status", "ok");
        return result;
    }

    /**
     * 获取环境信息
     */
    @GetMapping("/env")
    public Map<String, Object> env() {
        Map<String, Object> result = new HashMap<>();
        result.put("java.version", System.getProperty("java.version"));
        result.put("os.name", System.getProperty("os.name"));
        result.put("os.arch", System.getProperty("os.arch"));
        result.put("os.version", System.getProperty("os.version"));
        result.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        result.put("freeMemory", Runtime.getRuntime().freeMemory());
        result.put("maxMemory", Runtime.getRuntime().maxMemory());
        result.put("totalMemory", Runtime.getRuntime().totalMemory());
        return result;
    }
}
