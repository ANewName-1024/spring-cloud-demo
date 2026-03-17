# Ops 运维服务设计

本文档描述 Ops 运维服务的架构设计，集成 Spring Cloud Sleuth 分布式追踪和 ELK 日志管理。

## 1. 系统架构

### 1.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              用户请求                                         │
└─────────────────────────────────┬───────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           API Gateway (8080)                                 │
│                        Spring Cloud Sleuth                                   │
└─────────────────────────────────┬───────────────────────────────────────────┘
                                  │
        ┌─────────────────────────┼─────────────────────────┐
        │                         │                         │
        ▼                         ▼                         ▼
┌───────────────┐       ┌───────────────┐       ┌───────────────┐
│ User Service  │       │ Order Service │       │ Config Svc   │
│   (8081)      │       │   (8082)      │       │   (8888)     │
└───────┬───────┘       └───────┬───────┘       └───────┬───────┘
        │                         │                         │
        └─────────────────────────┼─────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      Ops Service (8090) - 运维服务                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │  日志收集   │  │  指标监控   │  │  告警管理   │  │  链路追踪   │        │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘        │
└─────────────────────────────────┬───────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         ELK Stack                                            │
│  ┌────────────┐   ┌────────────┐   ┌────────────┐   ┌────────────┐         │
│  │  Beats     │──▶│ Logstash   │──▶│Elasticsearch│──▶│   Kibana   │         │
│  │ (Filebeat) │   │ (处理/过滤)│   │  (存储)     │   │  (可视化)  │         │
│  └────────────┘   └────────────┘   └────────────┘   └────────────┘         │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 组件说明

| 组件 | 职责 | 技术栈 |
|------|------|--------|
| API Gateway | 请求入口、路由、Sleuth 集成 | Spring Cloud Gateway |
| Microservices | 业务服务、输出日志和追踪数据 | Spring Boot + Sleuth |
| Ops Service | 运维监控、告警、日志聚合 | Spring Boot Admin |
| Filebeat | 日志文件采集 | Beats |
| Logstash | 日志解析、过滤、转换 | Logstash |
| Elasticsearch | 日志存储和检索 | Elasticsearch |
| Kibana | 日志可视化分析 | Kibana |

## 2. Spring Cloud Sleuth 集成

### 2.1 追踪原理

Spring Cloud Sleuth 为每个请求生成唯一的 Trace ID 和 Span ID：

```
Trace ID: 1a2b3c4d5e6f7g8h (全局请求ID)
Span ID:  abc123def456    (当前服务调用ID)
|
├── Span: gateway         (API网关)
│   └── Span: user-service (用户服务)
│       └── Span: database  (数据库查询)
└── Span: order-service   (订单服务)
```

### 2.2 Maven 依赖

```xml
<!-- Spring Cloud Sleuth -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-sleuth</artifactId>
</dependency>

<!-- Zipkin 客户端 (可选，用于链路可视化) -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-zipkin</artifactId>
</dependency>

<!-- Logstash 日志 appender -->
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

### 2.3 配置示例

```yaml
spring:
  application:
    name: user-service
  
  sleuth:
    sampler:
      probability: 1.0  # 100% 采样率 (生产环境可调整)
    
  zipkin:
    base-url: http://localhost:9411  # Zipkin 地址
    sender:
      type: web  # 使用 HTTP 发送追踪数据

  # Logstash 配置
  logstash:
    endpoint: localhost:5044
```

### 2.4 日志格式

使用 JSON 格式输出日志，便于 ELK 解析：

```json
{
  "timestamp": "2026-03-17T12:00:00.000+0800",
  "level": "INFO",
  "thread": "http-nio-8081-exec-1",
  "logger": "com.example.user.UserController",
  "message": "User login successful",
  "traceId": "1a2b3c4d5e6f7g8h",
  "spanId": "abc123def456",
  "service": {
    "name": "user-service",
    "version": "1.0.0",
    "port": 8081
  },
  "request": {
    "method": "POST",
    "uri": "/auth/login",
    "ip": "192.168.1.100"
  }
}
```

## 3. ELK 集成设计

### 3.1 Logstash 配置文件

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
  
  # 添加时间戳
  date {
    match => ["timestamp", "yyyy-MM-dd'T'HH:mm:ss.SSSZ"]
    target => "@timestamp"
  }
  
  # 提取 Sleuth 追踪信息
  mutate {
    add_field => {
      "trace_id" => "%{[json][traceId]}"
      "span_id" => "%{[json][spanId]}"
      "service_name" => "%{[json][service][name]}"
    }
  }
  
  # 过滤敏感信息
  mutate {
    gsub => [
      "message", "password\=[^\s]+", "password=***",
      "message", "token\=[^\s]+", "token=***"
    ]
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "spring-cloud-%{+YYYY.MM.dd}"
  }
}
```

### 3.2 Kibana 可视化

创建以下 Dashboard：

| Dashboard | 内容 |
|-----------|------|
| 服务概览 | 各服务请求量、响应时间、错误率 |
| 链路追踪 | 请求完整调用链、耗时分析 |
| 日志搜索 | 关键词搜索、日志过滤 |
| 告警统计 | 告警数量、响应时间趋势 |

### 3.3 日志索引模式

```json
{
  "index_patterns": "spring-cloud-*",
  "template": {
    "mappings": {
      "properties": {
        "traceId": { "type": "keyword" },
        "spanId": { "type": "keyword" },
        "service": {
          "properties": {
            "name": { "type": "keyword" },
            "port": { "type": "integer" }
          }
        },
        "request": {
          "properties": {
            "method": { "type": "keyword" },
            "uri": { "type": "keyword" },
            "duration": { "type": "long" }
          }
        }
      }
    }
  }
}
```

## 4. Ops Service 功能设计

### 4.1 核心功能

| 模块 | 功能 | 实现 |
|------|------|------|
| 服务监控 | 健康检查、状态监控 | Spring Boot Actuator |
| 日志聚合 | 统一日志收集、查询 | ELK 集成 |
| 链路追踪 | 分布式追踪分析 | Zipkin + Sleuth |
| 告警管理 | 异常告警、通知 | 自定义 + 钉钉/邮件 |
| 配置管理 | 动态配置、配置刷新 | Spring Cloud Config |

### 4.2 API 接口

```yaml
# 运维服务 API
/ops/
├── /health              # 健康检查
├── /metrics            # 指标数据
├── /loggers            # 日志级别管理
├── /env                # 环境变量
├── /beans              # Bean 信息
├── /caches             # 缓存管理
├── /scheduled-tasks    # 定时任务
└── /traces             # 链路追踪数据 (对接 Zipkin)
```

### 4.3 告警规则

```yaml
alerts:
  - name: high_error_rate
    condition: error_rate > 5%
    duration: 5m
    severity: critical
    notify: [dingtalk, email]
    
  - name: high_response_time
    condition: p95_duration > 2000ms
    duration: 10m
    severity: warning
    notify: [dingtalk]
    
  - name: service_down
    condition: health == DOWN
    duration: 1m
    severity: critical
    notify: [dingtalk, phone]
```

## 5. Docker Compose 部署

### 5.1 ELK 集群

```yaml
version: '3.8'

services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.12.0
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
    ports:
      - "9200:9200"
    volumes:
      - es-data:/usr/share/elasticsearch/data

  kibana:
    image: docker.elastic.co/kibana/kibana:8.12.0
    ports:
      - "5601:5601"
    environment:
      - ELASTICSEARCH_HOSTS=http://elasticsearch:9200

  logstash:
    image: docker.elastic.co/logstash/logstash:8.12.0
    ports:
      - "5044:5044"
      - "9600:9600"
    volumes:
      - ./logstash/pipeline:/usr/share/logstash/pipeline
    depends_on:
      - elasticsearch

  zipkin:
    image: openzipkin/zipkin
    ports:
      - "9411:9411"

  filebeat:
    image: docker.elastic.co/beats/filebeat:8.12.0
    volumes:
      - ./logs:/var/log/spring-cloud
      - ./filebeat.yml:/usr/share/filebeat/filebeat.yml
    depends_on:
      - logstash

volumes:
  es-data:
```

### 5.2 服务日志配置

```xml
<!-- logback-spring.xml -->
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    
    <!-- Logstash 输出 -->
    <appender name="LOGSTASH" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
        <destination>logstash:5044</destination>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"service":"${APP_NAME}"}</customFields>
        </encoder>
    </appender>
    
    <!-- 控制台输出 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="LOGSTASH"/>
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

## 6. 运维监控指标

### 6.1 核心指标

| 指标 | 说明 | 告警阈值 |
|------|------|----------|
| QPS | 每秒请求数 | > 10000 |
| 响应时间 P99 | 99% 请求响应时间 | > 3000ms |
| 错误率 | 请求错误比例 | > 5% |
| CPU 使用率 | 容器 CPU 使用 | > 80% |
| 内存使用率 | 堆内存使用 | > 85% |
| 连接池 | 数据库连接池使用 | > 90% |

### 6.2 链路追踪指标

| 指标 | 说明 |
|------|------|
| Trace 数量 | 每分钟产生的 Trace 数 |
| Span 数量 | 每个 Trace 的Span 数 |
| 服务依赖 | 服务调用依赖图 |
| 慢请求 | 响应时间超过阈值的请求 |

## 7. 快速开始

### 7.1 本地开发

```bash
# 1. 启动 ELK (可选，使用 Docker)
docker-compose -f elk/docker-compose.yml up -d

# 2. 启动 Zipkin
docker run -d -p 9411:9411 openzipkin/zipkin

# 3. 启动服务
mvn spring-boot:run
```

### 7.2 查看日志

```bash
# Kibana 访问
http://localhost:5601

# Zipkin 访问
http://localhost:9411

# Elasticsearch API
curl http://localhost:9200/_cluster/health
```

## 8. 告警服务设计

### 8.1 告警模块

```java
// 告警服务接口
public interface AlertService {
    void sendAlert(Alert alert);
    List<Alert> queryAlerts(AlertQuery query);
}

// 告警类型
public enum AlertType {
    ERROR_RATE,        // 错误率告警
    RESPONSE_TIME,    // 响应时间告警
    SERVICE_DOWN,      // 服务宕机
    MEMORY_USAGE,     // 内存使用率
    CPU_USAGE         // CPU 使用率
}

// 告警级别
public enum AlertLevel {
    INFO,     // 信息
    WARNING,  // 警告
    CRITICAL  // 严重
}
```

### 8.2 通知渠道

| 渠道 | 配置 | 用途 |
|------|------|------|
| 钉钉 | webhook URL | 实时告警 |
| 邮件 | SMTP 配置 | 告警汇总 |
| 短信 | API 配置 | 紧急告警 |

### 8.3 告警规则引擎

```java
// 规则匹配
public class AlertRuleEngine {
    
    public boolean match(AlertRule rule, Metrics metrics) {
        switch (rule.getType()) {
            case ERROR_RATE:
                return metrics.getErrorRate() > rule.getThreshold();
            case RESPONSE_TIME:
                return metrics.getP99ResponseTime() > rule.getThreshold();
            case MEMORY_USAGE:
                return metrics.getMemoryUsagePercent() > rule.getThreshold();
            default:
                return false;
        }
    }
}
```

## 9. 服务注册与发现集成

### 9.1 Eureka 集成

所有服务启动时自动注册到 Eureka，Ops Service 可以从 Eureka 获取服务列表：

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true
```

### 9.2 服务健康检查

```java
// 健康检查服务
@Service
public class HealthCheckService {
    
    public Map<String, HealthStatus> checkAllServices() {
        // 获取所有注册服务
        List<InstanceInfo> instances = eurekaClient.getInstancesByAppId("USER-SERVICE");
        
        // 检查每个实例
        for (InstanceInfo instance : instances) {
            HealthStatus status = checkHealth(instance);
            // ...
        }
    }
}
```

## 10. 统一日志规范

### 10.1 日志级别使用规范

| 级别 | 使用场景 |
|------|----------|
| DEBUG | 调试信息，详细流程 |
| INFO | 正常业务流程 |
| WARN | 潜在问题、异常但可处理 |
| ERROR | 错误、需要关注 |

### 10.2 日志字段标准

```json
{
  "timestamp": "ISO8601 时间",
  "level": "日志级别",
  "traceId": "链路追踪ID",
  "spanId": "Span ID",
  "service": {
    "name": "服务名",
    "version": "版本",
    "ip": "服务IP"
  },
  "request": {
    "method": "请求方法",
    "uri": "请求路径",
    "duration": "耗时(ms)",
    "status": "HTTP状态码"
  },
  "message": "日志消息",
  "exception": "异常信息(可选)"
}
```

## 11. 性能优化建议

### 11.1 日志优化

- 使用异步日志 appender
- 合理设置日志级别
- 限制日志文件大小和数量
- 使用日志压缩

### 11.2 Sleuth 优化

- 生产环境采样率调整为 10-50%
- 使用消息队列传输追踪数据
- 定期清理 Zipkin 存储

## 12. 测试用例

### 12.1 单元测试

```java
@SpringBootTest
class MetricsServiceTest {
    
    @Test
    void testRecordRequest() {
        metricsService.recordRequest();
        assertEquals(1, metricsService.getRequestCount());
    }
    
    @Test
    void testRecordError() {
        metricsService.recordError();
        assertEquals(1, metricsService.getErrorCount());
    }
}
```

### 12.2 集成测试

```java
@SpringBootTest
class OpsIntegrationTest {
    
    @Test
    void testHealthEndpoint() {
        restTemplate.getForObject("/ops/health", Map.class);
        // 验证健康检查
    }
}
```

---

最后更新: 2026-03-17
