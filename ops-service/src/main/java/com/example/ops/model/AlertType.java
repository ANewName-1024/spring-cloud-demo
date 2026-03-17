package com.example.ops.model;

/**
 * 告警类型枚举
 */
public enum AlertType {
    ERROR_RATE,        // 错误率告警
    RESPONSE_TIME,    // 响应时间告警
    SERVICE_DOWN,      // 服务宕机
    MEMORY_USAGE,     // 内存使用率
    CPU_USAGE,        // CPU 使用率
    THREAD_POOL,      // 线程池告警
    DATABASE_POOL     // 数据库连接池告警
}
