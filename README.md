# Spring Cloud Demo

Spring Cloud 微服务架构示例项目，演示了服务注册、负载均衡、API 网关等核心功能。

## 项目架构

```
                          ┌─────────────────┐
                          │   API Gateway   │
                          │    (8080)       │
                          └────────┬────────┘
                                   │
              ┌────────────────────┼────────────────────┐
              │                    │                    │
              ▼                    ▼                    ▼
    ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
    │  User Service   │  │  Order Service  │  │ Eureka Server   │
    │   (8081)        │  │   (8082)        │  │    (8761)       │
    └─────────────────┘  └─────────────────┘  └─────────────────┘
```

## 技术栈

- **Java 21**
- **Spring Boot 3.2.0**
- **Spring Cloud 2023.0.0**
- **Maven**

## 服务说明

| 服务 | 端口 | 说明 |
|------|------|------|
| eureka-server | 8761 | 服务注册与发现中心 |
| user-service | 8081 | 用户服务 |
| order-service | 8082 | 订单服务 |
| api-gateway | 8080 | API 网关（统一入口） |

## 快速开始

### 前提条件

- JDK 21+
- Maven 3.8+

### 编译运行

```bash
# 编译所有模块
mvn clean package

# 启动服务（按顺序）
# 1. 启动 Eureka Server
java -jar eureka-server/target/eureka-server-1.0.0.jar

# 2. 启动 User Service
java -jar user-service/target/user-service-1.0.0.jar

# 3. 启动 Order Service
java -jar order-service/target/order-service-1.0.0.jar

# 4. 启动 API Gateway
java -jar api-gateway/target/api-gateway-1.0.0.jar
```

### 访问地址

| 服务 | 地址 |
|------|------|
| Eureka Dashboard | http://localhost:8761 |
| API Gateway | http://localhost:8080 |
| User Service | http://localhost:8081 |
| Order Service | http://localhost:8082 |

## API 接口

### 用户服务

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /users/{id} | 获取用户信息 |
| GET | /users | 获取用户列表 |

### 订单服务

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /orders/{id} | 获取订单信息 |
| GET | /orders | 获取订单列表 |
| POST | /orders | 创建订单 |

## 文档

- [设计文档](docs/design.md)
- [API 文档](docs/api.md)
- [部署说明](docs/deployment.md)

## Git 信息

```
仓库地址: https://github.com/https://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hub.com/ANewName-1024/spring-cloud-demo.git
最后更新: 2026-03-17 08:55:49
```

## License

MIT
