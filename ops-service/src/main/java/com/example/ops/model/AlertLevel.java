package com.example.ops.model;

/**
 * 告警级别枚举
 */
public enum AlertLevel {
    INFO("信息", 1),
    WARNING("警告", 2),
    CRITICAL("严重", 3);
    
    private final String description;
    private final int priority;
    
    AlertLevel(String description, int priority) {
        this.description = description;
        this.priority = priority;
    }
    
    public String getDescription() { return description; }
    public int getPriority() { return priority; }
}
