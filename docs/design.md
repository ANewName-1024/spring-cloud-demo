# Spring Cloud Demo 设计文档

## 1. 系统架构

### 1.1 整体架构

本项目采用标准的 Spring Cloud 微服务架构，包含以下核心组件：

- **服务注册中心 (Eureka Server)**: 负责服务注册与发现
- **服务提供者 (Provider)**: User Service、Order Service
- **服务网关 (API Gateway)**: 统一入口，路由转发

### 1.2 架构图

```
                                    ┌─────────────────┐
                                    │   API Gateway   │
                                    │    (8080)       │
                                    └────────┬────────┘
                                             │
                   ┌─────────────────────────┼─────────────────────────┐
                   │                         │                         │
                   ▼                         ▼                         ▼
         ┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
         │  User Service   │       │  Order Service  │       │ Eureka Server   │
         │   (8081)        │       │   (8082)        │       │    (8761)       │
         └────────┬────────┘       └────────┬────────┘       └─────────────────┘
                  │                        │
                  └────────────────────────┘
                           │
                   ┌───────┴───────┐
                   │  Eureka Server │
                   │  (服务注册发现)  │
                   └─────────────────┘
```

## 2. 服务设计

### 2.1 Eureka Server (服务注册中心)

**职责**:
- 服务注册：接收各服务的注册信息
- 服务发现：提供给其他服务查询可用服务实例
- 心跳检测：监控服务健康状态

**配置**:
- 端口: 8761
- 关闭自我保护模式 (enable-self-preservation: false)

### 2.2 User Service (用户服务)

**功能**:
- 用户信息管理
- 提供用户查询接口

**技术栈**:
- Spring Boot Web
- Spring Cloud Netflix Eureka Client

**端口**: 8081

### 2.3 Order Service (订单服务)

**功能**:
- 订单管理
- 提供订单 CRUD 接口

**技术栈**:
- Spring Boot Web
- Spring Cloud Netflix Eureka Client

**端口**: 8082

### 2.4 API Gateway (API 网关)

**职责**:
- 路由转发：将请求路由到对应的微服务
- 负载均衡（通过 Eureka）
- 统一入口

**路由规则**:
| 路径 | 目标服务 |
|------|----------|
| /users/** | user-service:8081 |
| /orders/** | order-service:8082 |

**端口**: 8080

## 3. 关键技术点

### 3.1 服务注册与发现

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true
```

### 3.2 API 网关路由

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: http://localhost:8081
          predicates:
            - Path=/users/**
        - id: order-service
          uri: http://localhost:8082
          predicates:
            - Path=/orders/**
```

## 4. 扩展设计

### 4.1 未来可扩展组件

| 组件 | 用途 |
|------|------|
| Spring Cloud Config | 配置中心 |
| Spring Cloud Sleuth | 分布式追踪 |
| Spring Cloud Gateway | 增强版网关 |
| Spring Cloud Circuit Breaker | 熔断器 |

### 4.2 容器化部署

```dockerfile
# Dockerfile 示例
FROM openjdk:21-jdk-slim
WORKDIR /app
COPY target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## 5. 安全设计

### 5.1 当前状态

- 基础版本，未配置安全认证
- 仅用于演示目的

### 5.2 生产环境建议

- 添加 Spring Security 认证
- 使用 HTTPS 加密通信
- 配置防火墙规则
- 实现服务间认证 (mTLS)

## 6. 监控与运维

### 6.1 健康检查

Eureka 会定期检查服务实例的健康状态。

### 6.2 日志管理

建议使用:
- ELK Stack (Elasticsearch + Logstash + Kibana)
- Loki + Promtail + Grafana

### 6.3 指标监控

推荐使用:
- Prometheus + Grafana
- Spring Boot Actuator

---

最后更新: 2026-03-17
