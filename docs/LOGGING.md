# 日志设计规范

## 概述

本文档定义 Spring Cloud Demo 项目的日志设计规范，确保日志的一致性、可读性和可分析性。

## 日志级别使用规范

| 级别 | 使用场景 | 示例 |
|------|----------|------|
| **DEBUG** | 调试信息，详细流程 | 方法入参、SQL 语句、循环变量 |
| **INFO** | 正常业务流程 | 用户登录、API 调用、任务完成 |
| **WARN** | 潜在问题、异常但可处理 | 权限不足、重试成功、超时 |
| **ERROR** | 错误、需要关注 | 异常、失败、致命错误 |

## 日志格式

### 标准格式

```
yyyy-MM-dd HH:mm:ss.SSS [线程名] LEVEL 完整类名 - 消息内容
```

### JSON 格式 (ELK 采集)

```json
{
  "timestamp": "2026-03-17T16:00:00.000+0800",
  "level": "INFO",
  "thread": "http-nio-8081-exec-1",
  "logger": "com.example.user.service.AuthService",
  "message": "User login successful",
  "traceId": "1a2b3c4d5e6f7g8h",
  "service": {
    "name": "user-service",
    "version": "1.0.0"
  }
}
```

## 日志分类

### 1. 应用日志

记录应用程序运行状态：

```java
logger.info("Application started successfully");
logger.debug("Loading configuration from: {}", configPath);
```

### 2. 业务日志

记录业务操作：

```java
// 用户登录
logger.info("User login: username={}, ip={}", username, ip);

// 创建订单
logger.info("Order created: orderId={}, userId={}", orderId, userId);
```

### 3. 审计日志

记录敏感操作（单独文件）：

```java
// 使用专用审计 Logger
logger.info("AUDIT: {} | {} | {} | {}", 
    timestamp, 
    username, 
    operation, 
    result);
```

### 4. 访问日志

HTTP 请求日志：

```
192.168.1.100 - - [17/Mar/2026:16:00:00 +0800] "GET /api/users HTTP/1.1" 200 1234
```

## 日志输出

### 开发环境

```
控制台 (彩色) + 文件日志
```

### 生产环境

```
JSON 文件 (Filebeat 采集) + 错误日志 + 审计日志
```

## 各模块日志要点

### user-service

| 操作 | 日志级别 | 说明 |
|------|----------|------|
| 用户登录 | INFO | 记录用户名、IP |
| 用户登出 | INFO | 记录用户名 |
| 注册/注销 | INFO | 记录操作类型 |
| 权限变更 | WARN | 需要关注 |
| 认证失败 | WARN | 记录原因 |
| 密码重置 | ERROR | 需审计 |

### gateway

| 操作 | 日志级别 | 说明 |
|------|----------|------|
| 请求路由 | DEBUG | 路径、目标服务 |
| 认证成功 | DEBUG | 用户信息 |
| 认证失败 | WARN | 原因 |
| 限流触发 | WARN | 限流原因 |
| 路由错误 | ERROR | 目标服务不可用 |

### config-service

| 操作 | 日志级别 | 说明 |
|------|----------|------|
| 配置获取 | DEBUG | 配置名称、环境 |
| 配置加密 | DEBUG | 加密操作 |
| Git 操作 | INFO | 拉取、提交 |
| 密钥操作 | WARN | 轮换、过期 |

### eureka-server

| 操作 | 日志级别 | 说明 |
|------|----------|------|
| 服务注册 | INFO | 服务名、IP |
| 服务注销 | INFO | 服务名 |
| 心跳超时 | WARN | 警告 |
| 自我保护 | WARN | 触发原因 |

### ops-service

| 操作 | 日志级别 | 说明 |
|------|----------|------|
| 告警触发 | WARN | 告警详情 |
| 告警恢复 | INFO | 恢复详情 |
| 监控指标 | DEBUG | 指标数据 |

## 日志文件

| 文件 | 说明 | 保留时间 |
|------|------|----------|
| `{app}.log` | 应用日志 | 30 天 |
| `{app}.json` | JSON 日志 (ELK) | 7 天 |
| `{app}-error.log` | 错误日志 | 30 天 |
| `{app}-audit.log` | 审计日志 | 90 天 |
| `{app}-http.log` | HTTP 访问日志 | 7 天 |

## 敏感信息处理

### 脱敏规则

```java
// 自动脱敏
logger.info("User info: {}", maskSensitive(userInfo));

// 手动脱敏
private String maskSensitive(String value) {
    if (value == null) return null;
    if (value.length() <= 4) return "****";
    return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
}
```

### 需要脱敏的字段

- password
- token
- secret
- key
- creditCard
- phone
- email
- idCard

## 分布式追踪

### Trace ID

在微服务调用链中传递 Trace ID：

```java
// 获取 Trace ID
String traceId = MDC.get("traceId");

// 设置 Trace ID
MDC.put("traceId", UUID.randomUUID().toString());
```

### MDC 属性

| 属性 | 说明 |
|------|------|
| traceId | 链路追踪 ID |
| spanId | 当前 Span ID |
| userId | 当前用户 ID |
| ip | 客户端 IP |

## 配置示例

### application.yml

```yaml
logging:
  level:
    root: INFO
    com.example: DEBUG
  file:
    name: logs/application.log
  logback:
    rolling-policy:
      max-history: 30
      max-file-size: 100MB
```

### Logstash 配置

```conf
input {
  beats {
    port => 5044
  }
}

filter {
  # 解析 JSON 日志
  json {
    source => "message"
    target => "json"
  }
  
  # 提取追踪信息
  mutate {
    add_field => {
      "trace_id" => "%{[json][traceId]}"
      "span_id" => "%{[json][spanId]}"
    }
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "spring-cloud-%{+YYYY.MM.dd}"
  }
}
```

## 快速查询

### Kibana 查询示例

```kql
# 查询错误日志
level: ERROR

# 查询特定用户操作
userId: 12345 AND traceId: *

# 查询慢请求
responseTime: > 3000

# 查询特定服务
service.name: user-service
```

---

最后更新: 2026-03-17
