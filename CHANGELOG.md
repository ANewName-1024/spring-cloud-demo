# 变更日志

所有值得记录的变更都会记录在此文件中。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)。

## [未发布]

### 添加
- 添加 common 公共模块
- 添加 ops-service 运维服务模块
- 添加 testing 测试基类模块
- 全局统一响应 ApiResponse
- 全局异常处理 GlobalExceptionHandler
- 业务异常 BusinessException + ErrorCode
- 日志设计规范 (LOGGING.md)
- 异常处理设计 (EXCEPTION.md)
- 敏感字段国密加密方案 (ENCRYPTION.md)
- 敏感信息保护方案 (SECURITY.md)
- 测试规范 (TESTING.md)
- 开发规范 (DEVELOPMENT.md)
- 分支策略 (BRANCH_STRATEGY.md)
- 请求日志过滤器 (RequestLogFilter)
- 限流过滤器 (RateLimitFilter)
- 响应包装过滤器 (ResponseWrapperFilter)
- 配置服务认证服务 (ConfigAuthService)
- 自动化部署脚本 (deploy.sh/upgrade.sh/uninstall.sh)
- 权限管理方案 (PERMISSION.md)
- 自动化部署方案 (AUTO_DEPLOY.md)
- XXL-JOB 分布式任务调度
- 定时任务配置能力

### 更改
- Gateway 升级为 Spring Cloud Gateway
- 完善 Gateway 过滤器链
- 完善 application.yml 配置
- 优化日志配置文件结构
- 定时任务从 Spring @Scheduled 迁移到 XXL-JOB

### 修复
- 修复编译错误 (Pattern import, Vault import)
- 修复 ConfigUser/ConfigAuthService 缺失
- 修复日志配置文件引用
- 修复敏感信息泄露问题
- 修复密码强度验证

---

## [v1.0.0] - 2024-03-17

### 添加
- 初始版本发布
- 用户管理 CRUD
- 用户注册/登录
- JWT Token 签发
- RBAC 权限模型
- Gateway 路由
- Spring Cloud Eureka 集成
- PostgreSQL 数据库集成
- 用户认证模块 (user-service)
- API 网关模块 (gateway)
- 配置服务模块 (config-service)
- Spring Cloud Config 集成
- OIDC/OAuth 2.0 授权服务
- 机机账户 (AKSK) 认证
- 国密算法支持 (SM2/SM3/SM4)
- Refresh Token 支持
- OpenClaw 配置管理
- 弱密码检测

### 更改
- Gateway 统一认证
- 配置服务移除独立登录

### 修复
- 敏感信息泄露问题
- 密码强度验证
