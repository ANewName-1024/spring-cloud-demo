# 变更日志

所有值得记录的变更都会记录在此文件中。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)。

## [未发布]

### 添加
- 用户认证模块 (user-service)
- API 网关模块 (gateway)
- 配置服务模块 (config-service)
- Spring Cloud Config 集成
- OIDC/OAuth 2.0 授权服务
- RBAC 权限管理系统
- 机机账户 (AKSK) 认证
- 国密算法支持 (SM2/SM3/SM4)
- JWT Token 认证
- Refresh Token 支持
- OpenClaw 配置管理
- 弱密码检测

### 更改
- Gateway 统一认证
- 配置服务移除独立登录

### 修复
- 敏感信息泄露问题
- 密码强度验证

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
