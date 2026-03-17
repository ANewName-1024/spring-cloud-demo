package com.example.ops.service;

import com.example.ops.model.Alert;
import com.example.ops.model.AlertLevel;
import com.example.ops.model.AlertType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AlertServiceTest {

    @Autowired
    private AlertService alertService;

    @Test
    void testSendAlert() {
        Map<String, Object> details = new HashMap<>();
        details.put("errorRate", 0.15);
        
        Alert alert = alertService.sendAlert(
                AlertType.ERROR_RATE,
                AlertLevel.CRITICAL,
                "user-service",
                "错误率超过阈值",
                details
        );
        
        assertNotNull(alert.getId());
        assertEquals(AlertType.ERROR_RATE, alert.getType());
        assertEquals("user-service", alert.getServiceName());
    }

    @Test
    void testQueryAlerts() {
        // 发送测试告警
        alertService.sendAlert(AlertType.SERVICE_DOWN, AlertLevel.CRITICAL, "test-service", "服务宕机", null);
        
        List<Alert> alerts = alertService.queryAlerts("test-service", null, false);
        assertTrue(alerts.size() > 0);
    }

    @Test
    void testAcknowledgeAlert() {
        Alert alert = alertService.sendAlert(
                AlertType.MEMORY_USAGE,
                AlertLevel.WARNING,
                "test-service",
                "内存使用率高",
                null
        );
        
        boolean result = alertService.acknowledgeAlert(alert.getId());
        assertTrue(result);
        
        List<Alert> unacknowledged = alertService.getUnacknowledgedAlerts();
        assertFalse(unacknowledged.stream().anyMatch(a -> a.getId().equals(alert.getId())));
    }

    @Test
    void testDeleteAlert() {
        Alert alert = alertService.sendAlert(
                AlertType.CPU_USAGE,
                AlertLevel.INFO,
                "test-service",
                "CPU使用率正常",
                null
        );
        
        String alertId = alert.getId();
        boolean result = alertService.deleteAlert(alertId);
        assertTrue(result);
        
        List<Alert> alerts = alertService.queryAlerts("test-service", null, false);
        assertFalse(alerts.stream().anyMatch(a -> a.getId().equals(alertId)));
    }

    @Test
    void testGetStatistics() {
        Map<String, Object> stats = alertService.getAlertStatistics();
        
        assertNotNull(stats);
        assertTrue(stats.containsKey("total"));
        assertTrue(stats.containsKey("critical"));
        assertTrue(stats.containsKey("warning"));
    }
}
