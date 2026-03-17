# 文件权限管理设计

## 概述

本文档定义 Spring Cloud Demo 项目的文件权限管理方案，确保系统安全性和合规性。

---

## 1. 用户和组设计

### 1.1 创建专用用户

```bash
# 创建用户组
groupadd springcloud

# 创建应用用户
useradd -r -g springcloud -s /sbin/nologin -d /opt/springcloud-demo springcloud
```

### 1.2 用户说明

| 用户 | 组 | 说明 |
|------|-----|------|
| springcloud | springcloud | 应用运行用户，禁用登录 |

### 1.3 权限矩阵

| 用户 | 读 | 写 | 执行 | 说明 |
|------|-----|-----|------|------|
| root | ✓ | ✓ | ✓ | 完全控制 |
| springcloud | ✓ | ✓ | ✓ | 应用目录 |
| 其他用户 | ✗ | ✗ | ✗ | 无权限 |

---

## 2. 目录权限

### 2.1 目录结构

```
/opt/springcloud-demo/
├── app/              # 应用 JAR 包
├── config/          # 配置文件
├── logs/            # 日志目录
├── scripts/         # 脚本文件
└── backup/         # 备份目录
```

### 2.2 权限设置

| 目录 | 所有者 | 权限 | 说明 |
|------|--------|------|------|
| /opt/springcloud-demo | root:springcloud | 755 | 主目录 |
| app/ | root:springcloud | 755 | 应用文件 |
| config/ | root:springcloud | 750 | 敏感配置 |
| logs/ | root:springcloud | 750 | 日志目录 |
| scripts/ | root:springcloud | 755 | 脚本文件 |
| backup/ | root:springcloud | 750 | 备份目录 |

---

## 3. 文件权限

### 3.1 配置文件

```bash
# 配置文件权限 (600)
# 只有 springcloud 用户可读
-rw------- 1 springcloud springcloud env.conf
-rw------- 1 springcloud springcloud application.yml
```

**敏感配置**：
- 数据库密码
- JWT 密钥
- API 密钥
- 加密密钥

### 3.2 应用文件

```bash
# JAR 文件权限 (500)
# 只读，防止意外修改
-r-x------ 1 springcloud springcloud eureka-server-1.0.0.jar
-r-x------ 1 springcloud springcloud gateway-1.0.0.jar
-r-x------ 1 springcloud springcloud user-service-1.0.0.jar
```

### 3.3 脚本文件

```bash
# 脚本权限 (550)
# 所有者可读写和执行，组可执行
-r-xr-x--- 1 springcloud springcloud deploy.sh
-r-xr-x--- 1 springcloud springcloud upgrade.sh
-r-xr-x--- 1 springcloud springcloud uninstall.sh
```

### 3.4 日志文件

```bash
# 日志权限 (640)
# 所有者可读写，组可读
-rw-r----- 1 springcloud springcloud eureka.log
-rw-r----- 1 springcloud springcloud gateway.log
```

---

## 4. 系统权限

### 4.1 端口限制

| 端口 | 服务 | 外部访问 |
|------|------|----------|
| 8761 | Eureka | 仅内网 |
| 8080 | Gateway | 允许外部 |
| 8081 | User Service | 仅内网 |
| 8082 | Config | 仅内网 |
| 8090 | Ops | 管理员 |

### 4.2 防火墙规则

```bash
# 仅允许特定 IP 访问内部服务
ufw allow from 10.0.0.0/8 to any port 8761
ufw allow from 10.0.0.0/8 to any port 8081
ufw allow from 10.0.0.0/8 to any port 8082

# 允许外部访问 Gateway
ufw allow 8080/tcp
```

### 4.3 sudo 权限

```bash
# /etc/sudoers.d/springcloud
# 允许特定脚本以 root 权限执行
springcloud ALL=(root) NOPASSWD: /opt/springcloud-demo/scripts/deploy.sh
springcloud ALL=(root) NOPASSWD: /opt/springcloud-demo/scripts/upgrade.sh
```

---

## 5. 数据库权限

### 5.1 数据库用户

```sql
-- 创建应用数据库用户
CREATE USER business WITH PASSWORD 'SecurePassword123';
GRANT CONNECT ON DATABASE springcloud TO business;

-- 授予 schema 权限
GRANT USAGE, CREATE ON SCHEMA public TO business;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO business;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO business;
```

### 5.2 权限矩阵

| 对象 | business | 其他用户 |
|------|----------|----------|
| public schema | ALL | USAGE |
| users table | ALL | SELECT |
| roles table | ALL | SELECT |
| permissions table | ALL | SELECT |
| sensitive tables | ALL | - |

---

## 6. 安全基线

### 6.1 文件完整性检查

```bash
#!/bin/bash
# check-integrity.sh - 文件完整性检查

APP_DIR="/opt/springcloud-demo"
REPORT="/tmp/integrity-report.txt"

echo "文件完整性检查" > $REPORT
echo "时间: $(date)" >> $REPORT
echo "" >> $REPORT

# 检查权限
echo "=== 权限检查 ===" >> $REPORT
find $APP_DIR -type f -perm /4777 >> $REPORT  # 检查 SUID/SGID/Sticky bit

# 检查可写文件
echo "" >> $REPORT
echo "=== 可写文件检查 ===" >> $REPORT
find $APP_DIR -type f -perm -002 >> $REPORT

# 检查敏感文件权限
echo "" >> $REPORT
echo "=== 敏感文件权限 ===" >> $REPORT
find $APP_DIR/config -type f -perm /006 >> $REPORT

# 发送报告
mail -s "文件完整性报告" admin@example.com < $REPORT
```

### 6.2 审计日志

```bash
# 审计配置 /etc/audit/rules.d/springcloud.rules
-w /opt/springcloud-demo -p wa -k springcloud
-w /var/log/springcloud-demo -p wa -k springcloud-logs
```

---

## 7. 快速命令

### 7.1 初始化权限

```bash
# 首次部署
sudo ./scripts/deploy.sh init

# 或手动执行
groupadd springcloud
useradd -r -g springcloud -s /sbin/nologin -d /opt/springcloud-demo springcloud
mkdir -p /opt/springcloud-demo/{app,config,logs,scripts,backup}
chown -R springcloud:springcloud /opt/springcloud-demo
chmod 750 /opt/springcloud-demo/config
chmod 600 /opt/springcloud-demo/config/*
```

### 7.2 验证权限

```bash
# 查看目录权限
ls -la /opt/springcloud-demo/

# 查看用户
id springcloud

# 测试文件访问
su - springcloud -s /bin/bash -c "cat /opt/springcloud-demo/config/env.conf"
```

---

## 8. 故障排查

### 8.1 常见问题

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| 服务无法启动 | 权限不足 | 检查文件所有者 |
| 无法读取配置 | 文件权限 600 | 以正确用户运行 |
| 日志无法写入 | 目录权限不足 | 检查目录权限 |

### 8.2 修复命令

```bash
# 修复所有权
chown -R springcloud:springcloud /opt/springcloud-demo

# 修复目录权限
chmod 755 /opt/springcloud-demo
chmod 750 /opt/springcloud-demo/config

# 修复配置文件权限
chmod 600 /opt/springcloud-demo/config/*

# 修复脚本权限
chmod 550 /opt/springcloud-demo/scripts/*.sh
```

---

最后更新: 2026-03-17
