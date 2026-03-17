# Spring Cloud Demo 部署说明

## 1. 环境要求

### 1.1 硬件要求

| 资源 | 最低配置 | 推荐配置 |
|------|----------|----------|
| CPU | 2 核 | 4 核 |
| 内存 | 4 GB | 8 GB |
| 磁盘 | 20 GB | 50 GB |

### 1.2 软件要求

| 软件 | 版本要求 |
|------|----------|
| JDK | 21+ |
| Maven | 3.8+ |
| 操作系统 | Linux/macOS/Windows |

---

## 2. 本地部署

### 2.1 编译项目

```bash
# 克隆项目
git clone https://github.com/ANewName-1024/spring-cloud-demo.git
cd spring-cloud-demo

# 编译所有模块
mvn clean package -DskipTests
```

### 2.2 启动服务

**注意**: 必须按顺序启动，先启动注册中心

```bash
# 1. 启动 Eureka Server (注册中心)
java -jar eureka-server/target/eureka-server-1.0.0.jar

# 2. 启动 User Service
java -jar user-service/target/user-service-1.0.0.jar

# 3. 启动 Order Service
java -jar order-service/target/order-service-1.0.0.jar

# 4. 启动 API Gateway
java -jar api-gateway/target/api-gateway-1.0.0.jar
```

### 2.3 验证部署

```bash
# 检查 Eureka Server
curl http://localhost:8761

# 检查 User Service
curl http://localhost:8081/actuator/health

# 检查 Order Service
curl http://localhost:8082/actuator/health

# 检查 API Gateway
curl http://localhost:8080/actuator/health
```

---

## 3. Docker 部署

### 3.1 构建镜像

```bash
# 构建所有服务的镜像
docker build -t spring-cloud-demo/eureka-server:1.0.0 ./eureka-server
docker build -t spring-cloud-demo/user-service:1.0.0 ./user-service
docker build -t spring-cloud-demo/order-service:1.0.0 ./order-service
docker build -t spring-cloud-demo/api-gateway:1.0.0 ./api-gateway
```

### 3.2 Docker Compose 部署

创建 `docker-compose.yml`:

```yaml
version: '3.8'

services:
  eureka-server:
    image: spring-cloud-demo/eureka-server:1.0.0
    ports:
      - "8761:8761"
    environment:
      - SPRING_PROFILES_ACTIVE=prod

  user-service:
    image: spring-cloud-demo/user-service:1.0.0
    ports:
      - "8081:8081"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/
    depends_on:
      - eureka-server

  order-service:
    image: spring-cloud-demo/order-service:1.0.0
    ports:
      - "8082:8082"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/
    depends_on:
      - eureka-server

  api-gateway:
    image: spring-cloud-demo/api-gateway:1.0.0
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/
    depends_on:
      - eureka-server
      - user-service
      - order-service
```

启动:

```bash
docker-compose up -d
```

---

## 4. Kubernetes 部署

### 4.1 部署清单

```yaml
# k8s/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: eureka-server
spec:
  replicas: 1
  selector:
    matchLabels:
      app: eureka-server
  template:
    metadata:
      labels:
        app: eureka-server
    spec:
      containers:
        - name: eureka-server
          image: spring-cloud-demo/eureka-server:1.0.0
          ports:
            - containerPort: 8761
---
apiVersion: v1
kind: Service
metadata:
  name: eureka-server
spec:
  ports:
    - port: 8761
      targetPort: 8761
  selector:
    app: eureka-server
```

### 4.2 部署命令

```bash
kubectl apply -f k8s/
```

---

## 5. 环境配置

### 5.1 配置文件

各服务配置文件位于 `src/main/resources/application.yml`

### 5.2 自定义参数

可通过环境变量覆盖:

```bash
# 修改端口
java -jar user-service.jar --server.port=8081

# 修改 Eureka 地址
java -jar user-service.jar --eureka.client.service-url.defaultZone=http://eureka:8761/eureka/
```

---

## 6. 运维指南

### 6.1 日志查看

```bash
# 查看服务日志
tail -f logs/spring-cloud.log

# Docker 日志
docker logs -f user-service
```

### 6.2 健康检查

```bash
# Eureka 健康检查
curl http://localhost:8761/actuator/health

# 服务健康检查
curl http://localhost:8081/actuator/health
```

### 6.3 滚动更新

```bash
# Docker Compose
docker-compose up -d --build

# Kubernetes
kubectl rollout restart deployment/user-service
```

---

## 7. 故障排查

### 7.1 常见问题

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| 服务无法注册 | Eureka 未启动 | 先启动 Eureka Server |
| 网关路由失败 | 服务未注册 | 检查 Eureka 注册状态 |
| 端口冲突 | 端口被占用 | 修改端口配置 |

### 7.2 端口检查

```bash
# 检查端口占用
netstat -tlnp | grep 8080
```

---

## 8. 安全配置

### 8.1 生产环境建议

1. **启用 HTTPS**
2. **配置防火墙**
3. **使用 Docker 网络隔离**
4. **敏感信息使用环境变量或密钥管理服务**

### 8.2 禁用 Eureka 自我保护

```yaml
eureka:
  server:
    enable-self-preservation: false
```

---

最后更新: 2026-03-17
