package com.example.ops.service;

import com.example.ops.model.Alert;
import com.example.ops.model.AlertLevel;
import com.example.ops.model.AlertType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 告警服务
 */
@Service
public class AlertService {

    private static final Logger logger = LoggerFactory.getLogger(AlertService.class);

    private final Map<String, Alert> alerts = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    /**
     * 发送告警
     */
    public Alert sendAlert(AlertType type, AlertLevel level, String serviceName, String message, Map<String, Object> details) {
        Alert alert = new Alert();
        alert.setId(generateId());
        alert.setType(type);
        alert.setLevel(level);
        alert.setServiceName(serviceName);
        alert.setMessage(message);
        alert.setDetails(details);
        alert.setTimestamp(LocalDateTime.now());
        alert.setAcknowledged(false);

        alerts.put(alert.getId(), alert);

        // 发送通知
        sendNotification(alert);

        logger.warn("告警触发: {} - {} - {}", serviceName, type, message);
        return alert;
    }

    /**
     * 查询告警列表
     */
    public List<Alert> queryAlerts(String serviceName, AlertLevel level, boolean acknowledged) {
        return alerts.values().stream()
                .filter(alert -> serviceName == null || alert.getServiceName().equals(serviceName))
                .filter(alert -> level == null || alert.getLevel() == level)
                .filter(alert -> !acknowledged || alert.isAcknowledged())
                .sorted(Comparator.comparing(Alert::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 获取所有未确认告警
     */
    public List<Alert> getUnacknowledgedAlerts() {
        return alerts.values().stream()
                .filter(alert -> !alert.isAcknowledged())
                .sorted(Comparator.comparing(Alert::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 确认告警
     */
    public boolean acknowledgeAlert(String alertId) {
        Alert alert = alerts.get(alertId);
        if (alert != null) {
            alert.setAcknowledged(true);
            logger.info("告警已确认: {}", alertId);
            return true;
        }
        return false;
    }

    /**
     * 删除告警
     */
    public boolean deleteAlert(String alertId) {
        return alerts.remove(alertId) != null;
    }

    /**
     * 获取告警统计
     */
    public Map<String, Object> getAlertStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("total", alerts.size());
        stats.put("unacknowledged", alerts.values().stream().filter(a -> !a.isAcknowledged()).count());
        stats.put("critical", alerts.values().stream().filter(a -> a.getLevel() == AlertLevel.CRITICAL).count());
        stats.put("warning", alerts.values().stream().filter(a -> a.getLevel() == AlertLevel.WARNING).count());
        
        // 按服务统计
        Map<String, Long> byService = alerts.values().stream()
                .collect(Collectors.groupingBy(Alert::getServiceName, Collectors.counting()));
        stats.put("byService", byService);
        
        return stats;
    }

    /**
     * 发送通知 (可扩展为钉钉、邮件等)
     */
    private void sendNotification(Alert alert) {
        // 这里可以集成钉钉、邮件、短信等通知渠道
        logger.info("发送告警通知: [{}] {} - {}", alert.getLevel(), alert.getServiceName(), alert.getMessage());
        
        if (alert.getLevel() == AlertLevel.CRITICAL) {
            // 严重告警，触发紧急通知
            sendCriticalNotification(alert);
        }
    }

    private void sendCriticalNotification(Alert alert) {
        logger.error("紧急告警: {} - {}", alert.getServiceName(), alert.getMessage());
    }

    private String generateId() {
        return "alert-" + idGenerator.getAndIncrement() + "-" + System.currentTimeMillis();
    }
}
