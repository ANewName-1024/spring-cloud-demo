# Spring Cloud Demo

基于 Spring Cloud 的微服务 Demo 项目，包含用户认证、权限管理等核心功能。

## 项目文档

| 文档 | 说明 |
|------|------|
| [DESIGN.md](./DESIGN.md) | 系统设计文档 |
| [DEPLOY.md](./DEPLOY.md) | 部署说明 |
| [API.md](./API.md) | API 接口文档 |

## 技术栈

| 组件 | 技术 |
|------|------|
| 框架 | Spring Boot 3.2.0 |
| 网关 | Spring Cloud Gateway 4.1.0 |
| 注册中心 | Spring Cloud Eureka |
| 安全 | Spring Security + JWT |
| 数据库 | PostgreSQL |
| 内网穿透 | frp |

## 快速开始

### 本地开发

```bash
# 1. 克隆项目
git clone https://github.com/ANewName-1024/spring-cloud-demo.git
cd spring-cloud-demo

# 2. 配置环境变量
cp user-service/.env.example user-service/.env
# 编辑 .env 填入实际配置

# 3. 启动 frp 客户端
cd frp/frp_0.52.3_windows_amd64
./frpc.exe -c frpc.toml

# 4. 启动服务
cd user-service && mvn spring-boot:run
# 另开终端
cd gateway && mvn spring-boot:run
```

### 访问地址

| 服务 | 地址 |
|------|------|
| Gateway | http://localhost:8080 |
| User Service | http://localhost:8081 |
| Eureka | http://localhost:9000 |

## 核心功能

### 用户认证
- ✅ 用户名密码登录
- ✅ JWT Token 认证
- ✅ 机机账户 AKSK 认证
- ✅ AKSK 轮转

### 权限管理
- ✅ RBAC 权限模型
- ✅ 角色管理
- ✅ 权限管理
- ✅ 方法级权限控制

### 安全特性
- ✅ 密码加密存储
- ✅ 密码强度验证
- ✅ 敏感信息环境变量配置
- ✅ 越权漏洞修复

## 项目结构

```
springcloud-demo/
├── user-service/        # 用户服务 (8081)
├── gateway/            # API 网关 (8080)
├── eureka-server/      # 服务注册中心 (9000)
├── config-service/     # 配置中心 (8082)
├── frp/               # 内网穿透客户端
├── DESIGN.md          # 设计文档
├── DEPLOY.md          # 部署说明
├── API.md             # 接口文档
└── docker-compose.yml # Docker 部署
```

## 环境要求

- JDK 21+
- Maven 3.9+
- PostgreSQL (可选本地，使用云端)
- frp (本地开发需要)

## 相关文档

详细文档请查看：
- [系统设计](./DESIGN.md)
- [部署说明](./DEPLOY.md)  
- [接口文档](./API.md)

## License

MIT
