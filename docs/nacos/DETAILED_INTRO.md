# Nacos 详细介绍

## 1. 什么是 Nacos？

**Nacos** (Dynamic Naming and Configuration Service) 是阿里巴巴开源的**配置中心**和**服务注册发现中心**。

```
Nacos = 配置管理 + 服务注册发现
```

| 功能 | 说明 |
|------|------|
| 配置管理 | 集中管理应用配置，支持动态刷新 |
| 服务注册发现 | 服务注册、健康检查、负载均衡 |

---

## 2. 核心概念

### 2.1 命名空间 (Namespace)

```
┌─────────────────────────────────────────┐
│              Nacos Server               │
│  ┌───────────────────────────────────┐ │
│  │ Namespace: public (默认)          │ │
│  │  ├── Group: DEFAULT_GROUP         │ │
│  │  │   ├── user-service-dev.yml   │ │
│  │  │   ├── gateway-prod.yml       │ │
│  │  └── Group: SHARED_GROUP         │ │
│  │      └── shared-config.yml       │ │
│  └───────────────────────────────────┘ │
│  ┌───────────────────────────────────┐ │
│  │ Namespace: dev                     │ │
│  │  └── ...                          │ │
│  └───────────────────────────────────┘ │
│  ┌───────────────────────────────────┐ │
│  │ Namespace: prod                    │ │
│  │  └── ...                          │ │
│  └───────────────────────────────────┘ │
└─────────────────────────────────────────┘
```

### 2.2 配置模型

```
Data ID: {application}-{profile}.yml
  └── 例如: user-service-dev.yml

Group: DEFAULT_GROUP / SHARED_GROUP / 自定义

Namespace: public / dev / prod
```

### 2.3 配置格式

支持多种格式：
- **YAML** (最常用)
- **Properties**
- **JSON**
- **XML** (不推荐)

---

## 3. 核心功能

### 3.1 配置管理

| 功能 | 说明 |
|------|------|
| **版本管理** | 配置变更历史，回滚 |
| **变更推送** | 配置变更自动推送到客户端 |
| **敏感加密** | 支持加密配置 (AES) |
| **权限控制** | 配置级别的读写权限 |
| **导入导出** | 批量导入导出配置 |
| **配置监听** | 监听配置变更事件 |

### 3.2 服务注册发现

| 功能 | 说明 |
|------|------|
| **服务注册** | 微服务注册到 Nacos |
| **健康检查** | 主动健康检查/心跳机制 |
| **负载均衡** | 多种负载均衡策略 |
| **DNS 集成** | 支持 DNS-SD |
| **流量管理** | 支持流量权重、熔断 |

---

## 4. 架构原理

### 4.1 整体架构

```
┌─────────────────────────────────────────────────────────┐
│                      Nacos Client                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │
│  │ Service     │  │ Config      │  │ Naming         │  │
│  │ Manager    │  │ Manager    │  │ Manager        │  │
│  └─────────────┘  └─────────────┘  └─────────────────┘  │
└────────────────────────┬────────────────────────────────┘
                         │ HTTP / gRPC
                         ▼
┌─────────────────────────────────────────────────────────┐
│                      Nacos Server                        │
│  ┌─────────────────────────────────────────────────────┐│
│  │                   Open API                          ││
│  └─────────────────────────────────────────────────────┘│
│           │                    │                    │      │
│           ▼                    ▼                    ▼      │
│  ┌─────────────┐  ┌─────────────────┐  ┌──────────┐  │
│  │ Service     │  │ Config          │  │ Naming   │  │
│  │ Controller │  │ Controller      │  │ Controller│  │
│  └─────────────┘  └─────────────────┘  └──────────┘  │
│           │                    │                    │      │
│           └────────────────────┼────────────────────┘      │
│                                ▼                          │
│  ┌─────────────────────────────────────────────────────┐  │
│  │              Distro / Raft Protocol                │  │
│  │         (一致性协议 - 分布式存储)                  │  │
│  └─────────────────────────────────────────────────────┘  │
│                                │                          │
│                                ▼                          │
│  ┌─────────────────────────────────────────────────────┐  │
│  │              Database (MySQL/PostgreSQL)           │  │
│  └─────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

### 4.2 配置推送原理

```
1. 服务启动
   │
   ▼
2. 拉取配置 (GET /nacos/v1/cs/configs?dataId=xxx&group=xxx)
   │
   ▼
3. Nacos 返回配置内容
   │
   ▼
4. 客户端监听配置 (Long Polling / HTTP Long Polling)
   │
   ▼
5. 配置变更时
   │
   ├── 6a. Nacos 推送变更通知
   │
   └── 6b. 客户端拉取新配置
   │
   ▼
7. @RefreshScope Bean 自动刷新
```

### 4.3 服务注册原理

```
微服务                    Nacos Server
   │                          │
   │──注册 (POST /ns/instance)──▶
   │                          │
   │──心跳 (HEARTBEAT)───────▶│
   │                          │
   │                      ┌────┴────┐
   │                      │ 服务列表 │
   │                      │ 更新    │
   │                      └─────────┘
   │                          │
   │◀──服务发现 (GET /ns/instance)──│
   │                          │
```

---

## 5. 与其他配置中心对比

| 特性 | Nacos | Spring Cloud Config | Apollo | Consul |
|------|-------|---------------------|-------|--------|
| 配置格式 | YAML/Properties/JSON | YAML/Properties | YAML/Properties/JSON | JSON/KV |
| 动态刷新 | ✅ 自动推送 | ✅ 手动/ Bus | ✅ 自动推送 | ✅ |
| GUI 管理 | ✅ 完善 | ❌ 无 | ✅ 完善 | ⚠️ 基础 |
| 权限控制 | ✅ | ❌ | ✅ | ✅ |
| 版本管理 | ✅ | ✅ (Git) | ✅ | ❌ |
| 加密存储 | ✅ | ❌ | ✅ | ❌ |
| 多环境 | ✅ Namespace | ✅ Profile | ✅ 环境 | ✅ |
| 社区活跃 | ✅ 阿里维护 | ✅ Spring | ✅ 携程 | ✅ |
| 学习成本 | 低 | 中 | 高 | 中 |

---

## 6. 数据模型

### 6.1 配置数据结构

```json
{
  "dataId": "user-service-dev.yml",
  "group": "DEFAULT_GROUP",
  "content": "spring:\n  datasource:\n    url: ...",
  "type": "yaml",
  "encryptedDataKey": "aes.key",
  "md5": "xxx",
  "version": 1,
  "timestamp": 1699999999999,
  "desc": "用户服务配置"
}
```

### 6.2 服务元数据

```json
{
  "serviceName": "user-service",
  "ip": "192.168.1.100",
  "port": 8081,
  "namespaceId": "public",
  "ephemeral": true,
  "metadata": {
    "version": "1.0.0",
    "region": "cn-shanghai"
  },
  "clusterName": "DEFAULT",
  "weight": 1.0,
  "enabled": true,
  "healthy": true
}
```

---

## 7. 客户端使用

### 7.1 Spring Boot 集成

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
    <version>2023.0.1.2</version>
</dependency>
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
    <version>2023.0.1.2</version>
</dependency>
```

### 7.2 配置示例

```yaml
spring:
  cloud:
    nacos:
      server-addr: localhost:8848
      username: nacos
      password: nacos
      config:
        namespace: public
        group: DEFAULT_GROUP
        file-extension: yml
        refresh-enabled: true
```

### 7.3 动态刷新

```java
@RestController
@RefreshScope  // 启用动态刷新
public class UserController {
    
    @Value("${user.service.timeout:5000}")
    private int timeout;
    
    // 当 Nacos 中配置变更时，timeout 会自动更新
}
```

---

## 8. 高级特性

### 8.1 配置加密

```bash
# Nacos 控制台支持 AES 加密
# 1. 在 Nacos 控制台创建配置
# 2. 选择"加密存储"
# 3. 输入明文配置，Nacos 自动加密存储

# 客户端使用
spring.datasource.password: '{cipher}xxx'
```

### 8.2 共享配置

```yaml
spring:
  cloud:
    nacos:
      config:
        shared-configs:
          - data-id: shared-datasource.yml
            group: SHARED_GROUP
            refresh: true
          - data-id: shared-jwt.yml
            group: SHARED_GROUP
            refresh: true
```

### 8.3 配置监听

```java
@NacosConfigurationProperties(dataId = "xxx", groupId = "DEFAULT")
@Data
public class Config {
    private String value;
}

// 或使用 Listener
@NacosInjected
private NacosConfigManager nacosConfigManager;

public void listener() throws NacosException {
    nacosConfigManager.getConfigService().addListener(
        "dataId", "group", new Listener() {
            @Override
            public Executor getExecutor() {
                return Executors.newSingleThreadExecutor();
            }
            
            @Override
            public void receiveConfigInfo(String configInfo) {
                // 配置变更回调
            }
        }
    );
}
```

---

## 9. 集群部署

```
                    ┌─────────────┐
                    │   Nginx     │
                    │  (LB:8848) │
                    └──────┬──────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
        ▼                  ▼                  ▼
  ┌─────────┐       ┌─────────┐       ┌─────────┐
  │ Nacos-1 │       │ Nacos-2 │       │ Nacos-3 │
  │ (Leader)│◄─────►│(Follower)│◄─────►│(Follower)│
  └────┬────┘       └────┬────┘       └────┬────┘
       │                 │                 │
       └─────────────────┼─────────────────┘
                         │
                         ▼
                  ┌─────────────┐
                  │  Database   │
                  │ (MySQL/PG)  │
                  └─────────────┘
```

**建议生产环境至少 3 个节点 + 1 个数据库**

---

## 10. 版本选择

| 版本 | Spring Boot | Spring Cloud | 状态 |
|------|-------------|--------------|------|
| 2023.0.1.2 | 3.x | 2023.x | ✅ 推荐 |
| 2.2.x | 2.6.x / 2.7.x | 2021.x / 2022.x | ⚠️ 维护 |

---

## 总结

Nacos 是一个功能强大的配置中心 + 服务发现平台，特别适合：
- 微服务架构
- 需要配置动态刷新
- 需要 GUI 管理配置
- 需要配置版本管理和回滚
- 需要敏感配置加密

相比 Spring Cloud Config，Nacos 开箱即用，无需额外搭建 Git 仓库。
