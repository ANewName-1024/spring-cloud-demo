package com.example.ops.controller;

import com.example.ops.model.Alert;
import com.example.ops.model.AlertLevel;
import com.example.ops.model.AlertType;
import com.example.ops.service.AlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 告警管理控制器
 */
@RestController
@RequestMapping("/ops/alerts")
public class AlertController {

    @Autowired
    private AlertService alertService;

    /**
     * 获取所有告警
     */
    @GetMapping
    public Map<String, Object> getAlerts(
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) AlertLevel level,
            @RequestParam(defaultValue = "false") boolean unacknowledged) {
        
        List<Alert> alerts = unacknowledged 
                ? alertService.getUnacknowledgedAlerts()
                : alertService.queryAlerts(serviceName, level, false);
        
        Map<String, Object> result = new HashMap<>();
        result.put("alerts", alerts);
        result.put("total", alerts.size());
        return result;
    }

    /**
     * 获取告警统计
     */
    @GetMapping("/statistics")
    public Map<String, Object> getStatistics() {
        return alertService.getAlertStatistics();
    }

    /**
     * 创建告警 (手动触发)
     */
    @PostMapping
    public Map<String, Object> createAlert(@RequestBody Map<String, Object> request) {
        AlertType type = AlertType.valueOf((String) request.get("type"));
        AlertLevel level = AlertLevel.valueOf((String) request.get("level"));
        String serviceName = (String) request.get("serviceName");
        String message = (String) request.get("message");
        
        Alert alert = alertService.sendAlert(type, level, serviceName, message, request);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("alertId", alert.getId());
        return result;
    }

    /**
     * 确认告警
     */
    @PostMapping("/{alertId}/acknowledge")
    public Map<String, Object> acknowledge(@PathVariable String alertId) {
        boolean success = alertService.acknowledgeAlert(alertId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "告警已确认" : "告警不存在");
        return result;
    }

    /**
     * 删除告警
     */
    @DeleteMapping("/{alertId}")
    public Map<String, Object> delete(@PathVariable String alertId) {
        boolean success = alertService.deleteAlert(alertId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "告警已删除" : "告警不存在");
        return result;
    }
}
