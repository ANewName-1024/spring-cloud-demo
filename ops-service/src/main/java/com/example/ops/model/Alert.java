package com.example.ops.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 告警模型
 */
public class Alert {
    
    private String id;
    private AlertType type;
    private AlertLevel level;
    private String serviceName;
    private String message;
    private Map<String, Object> details;
    private LocalDateTime timestamp;
    private boolean acknowledged;
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public AlertType getType() { return type; }
    public void setType(AlertType type) { this.type = type; }
    
    public AlertLevel getLevel() { return level; }
    public void setLevel(AlertLevel level) { this.level = level; }
    
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public boolean isAcknowledged() { return acknowledged; }
    public void setAcknowledged(boolean acknowledged) { this.acknowledged = acknowledged; }
}
