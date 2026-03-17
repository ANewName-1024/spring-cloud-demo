# 任务调度设计

本文档描述 Spring Cloud Demo 项目的任务调度方案，使用 XXL-JOB 分布式任务调度框架。

## 概述

XXL-JOB 是一个轻量级分布式任务调度平台，具有以下特点：
- 分布式任务调度
- 可视化管理界面
- 任务失败重试
- 任务超时设置
- 任务分片
- 动态任务管理

## 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                    XXL-JOB Admin (调度中心)                    │
│                    http://localhost:8080                    │
├─────────────────────────────────────────────────────────────┤
│  - 任务管理                                                  │
│  - 任务执行记录                                             │
│  - 调度日志                                                 │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    执行器 (Config Service)                    │
├─────────────────────────────────────────────────────────────┤
│  - ConfigRefreshJob                                        │
│  - KeyRotationJob                                         │
│  - CleanupJob                                             │
│  - HealthCheckJob                                         │
└─────────────────────────────────────────────────────────────┘
```

## 任务列表

### 配置刷新任务

| 任务名 | Handler | Cron 表达式 | 说明 |
|--------|---------|-------------|------|
| 配置刷新 | configRefreshJobHandler | 0 0/5 * * ? | 每5分钟刷新配置缓存 |
| 配置变更检测 | configChangeDetectJobHandler | 0 0/1 * * ? | 每1分钟检测Git配置变更 |
| 配置同步 | configSyncJobHandler | 0 0/10 * * ? | 每10分钟同步配置到各服务 |

### 密钥管理任务

| 任务名 | Handler | Cron 表达式 | 说明 |
|--------|---------|-------------|------|
| 密钥轮换 | keyRotationJobHandler | 0 0 2 * * ? | 每天凌晨2点轮换密钥 |
| 密钥健康检查 | keyHealthCheckJobHandler | 0 0 0/1 * * ? | 每小时检查密钥健康状态 |
| 密钥备份 | keyBackupJobHandler | 0 0 3 ? * SUN | 每周日凌晨3点备份密钥 |
| 密钥版本清理 | keyCleanupJobHandler | 0 0 4 1 * ? | 每月1号凌晨4点清理旧版本 |

### 清理任务

| 任务名 | Handler | Cron 表达式 | 说明 |
|--------|---------|-------------|------|
| 配置历史清理 | configHistoryCleanupJobHandler | 0 0 4 * * ? | 每天凌晨4点清理90天前历史 |
| 无效配置清理 | invalidConfigCleanupJobHandler | 0 0 12 * * ? | 每天中午12点清理无效配置 |
| 临时文件清理 | tempFileCleanupJobHandler | 0 0 0/1 * * ? | 每小时清理临时文件 |

### 健康检查任务

| 任务名 | Handler | Cron 表达式 | 说明 |
|--------|---------|-------------|------|
| 健康检查 | healthCheckJobHandler | 0 0/5 * * ? | 每5分钟检查系统健康状态 |
| 应用信息记录 | appInfoLogJobHandler | 0 0 0/1 * * ? | 每小时记录应用信息 |

## 配置说明

### XXL-JOB 配置

```yaml
xxl:
  job:
    enabled: true
    admin:
      addresses: http://127.0.0.1:8080/xxl-job-admin
    access-token: default_token
    executor:
      appname: config-service
      port: 9999
      logpath: ./logs/xxl-job
      logretentiondays: 30
```

### 参数说明

| 参数 | 说明 | 示例值 |
|------|------|--------|
| xxl.job.admin.addresses | 调度中心地址 | http://127.0.0.1:8080/xxl-job-admin |
| xxl.job.access-token | 访问令牌 | default_token |
| xxl.job.executor.appname | 执行器名称 | config-service |
| xxl.job.executor.port | 执行器端口 | 9999 |
| xxl.job.executor.logpath | 日志路径 | ./logs/xxl-job |
| xxl.job.executor.logretentiondays | 日志保留天数 | 30 |

## 使用示例

### 1. 创建任务

在 XXL-JOB 管理界面创建任务：

```json
{
  "jobGroup": 1,
  "jobDesc": "配置刷新任务",
  "author": "admin",
  "scheduleType": "CRON",
  "scheduleConf": "0 0/5 * * ?",
  "glueType": "BEAN",
  "executorHandler": "configRefreshJobHandler",
  "executorParam": "",
  "executorBlockStrategy": "SERIAL_EXECUTION",
  "executorTimeout": 300,
  "executorFailRetryCount": 3,
  "author": "admin"
}
```

### 2. 任务参数

任务支持传递参数：

```java
@XxlJob("configSyncJobHandler")
public void configSync(String params) {
    // 解析参数
    JSONObject paramObj = JSONObject.parseObject(params);
    String targetService = paramObj.getString("service");
    // ...
}
```

### 3. 任务结果

```java
@XxlJob("healthCheckJobHandler")
public void healthCheck() {
    try {
        // 执行任务逻辑
        // ...
        
        // 返回成功
        XxlJobHelper.handleSuccess("任务执行成功");
    } catch (Exception e) {
        // 返回失败
        XxlJobHelper.handleFail("任务执行失败: " + e.getMessage());
    }
}
```

## 任务执行结果

| 方法 | 说明 |
|------|------|
| XxlJobHelper.handleSuccess() | 标记任务成功 |
| XxlJobHelper.handleFail() | 标记任务失败 |
| XxlJobHelper.handleSuccess(String msg) | 成功并返回消息 |
| XxlJobHelper.handleFail(String msg) | 失败并返回消息 |

## 部署 XXL-JOB

### 1. 部署调度中心

```bash
# 下载源码
git clone https://github.com/xuxueli/xxl-job.git
cd xxl-job

# 修改数据库配置
vim xxl-job-admin/src/main/resources/application.properties

# 编译
mvn clean package -DskipTests

# 启动
java -jar xxl-job-admin/target/xxl-job-admin-*.jar
```

### 2. 配置执行器

在 config-service 中配置 XXL-JOB 执行器：

```yaml
xxl:
  job:
    enabled: true
    admin:
      addresses: http://your-xxl-job-host:8080/xxl-job-admin
    executor:
      appname: config-service
      port: 9999
```

## 监控与告警

### 调度日志

XXL-JOB 提供完整的调度日志，包括：
- 调度时间
- 执行时间
- 执行结果
- 失败原因

### 告警配置

在 XXL-JOB 管理界面配置告警：
- 任务失败告警
- 任务超时告警
- 调度阻塞告警

---

最后更新: 2026-03-17
