# 敏感信息保护方案

## 问题分析

### 当前方案
```yaml
# application.yml
spring:
  datasource:
    password: ${DB_PASSWORD}  # 环境变量
```

**风险点**：
1. 进程启动命令可能被 history 记录
2. `ps aux` 可查看进程环境变量
3. 容器编排文件 (docker-compose.yml) 明文存储
4. CI/CD 流水线配置明文存储
5. Git 仓库可能误提交敏感信息

## 优化方案

### 方案一：配置中心 + 加密存储 (推荐)

```
┌─────────────────────────────────────────────────────────┐
│                    Spring Cloud Config                   │
│                    (配置中心服务)                         │
├─────────────────────────────────────────────────────────┤
│  1. 配置加密存储 (AES-256-GCM)                          │
│  2. 启动时从配置中心拉取                                 │
│  3. 使用 master-key 解密                                │
└─────────────────────────────────────────────────────────┘
```

**实现**：
1. 配置使用 `{cipher}encrypted-value` 格式
2. 启动时自动解密
3. master-key 通过环境变量注入

---

### 方案二：Docker Secrets (容器环境)

```yaml
# docker-compose.yml
services:
  user-service:
    image: user-service:1.0.0
    secrets:
      - db_password
      - jwt_secret
```

---

### 方案三：外部密钥服务 (KMS)

```
┌──────────────┐      ┌──────────────┐      ┌──────────────┐
│   应用服务   │ ───▶ │   KMS API    │ ───▶ │  密钥管理服务 │
│              │      │  (获取密钥)  │      │  (AWS/阿里)  │
└──────────────┘      └──────────────┘      └──────────────┘
```

---

### 方案四：Vault 方案 (企业级)

```
┌──────────────┐      ┌──────────────┐
│   应用服务   │ ───▶ │    Vault     │
│              │      │  (密钥保险库) │
└──────────────┘      └──────────────┘
```

---

## 行业推荐算法

### 对称加密: AES-256-GCM

| 项目 | 值 |
|------|-----|
| 算法 | AES |
| 密钥长度 | 256 位 |
| 模式 | GCM (Galois/Counter Mode) |
| IV 长度 | 12 字节 |
| Tag 长度 | 128 位 |

```java
// 加密
String ciphertext = cryptoUtil.encrypt("敏感数据", encryptionKey);
// 解密
String plaintext = cryptoUtil.decrypt(ciphertext, encryptionKey);
```

### 密码哈希: bcrypt

| 项目 | 值 |
|------|-----|
| 算法 | bcrypt |
| 强度 | 12 (2^12 迭代) |
| Salt | 自动生成 |

```java
// 哈希密码
String hashed = cryptoUtil.hashPassword("userPassword");
// 验证密码
boolean match = cryptoUtil.verifyPassword("userPassword", hashed);
```

### 密钥派生: PBKDF2

| 项目 | 值 |
|------|-----|
| 算法 | PBKDF2WithHmacSHA256 |
| 迭代次数 | 100,000 |
| 密钥长度 | 256 位 |

```java
// 派生密钥
String salt = cryptoUtil.generateSalt();
String derivedKey = cryptoUtil.deriveKey(password, salt);
```

---

## 推荐方案：配置中心加密 + Vault

### 配置示例

```yaml
# config-server/application.yml
spring:
  cloud:
    config:
      server:
        encrypt:
          key: ${CONFIG_MASTER_KEY}
```

```yaml
# 客户端配置
spring:
  datasource:
    password: {cipher}AQBf8mK3...  # 加密存储
```

---

## 密钥管理矩阵

| 环境 | 存储方式 | 密钥来源 | 轮换周期 |
|------|----------|----------|----------|
| 开发 | 配置中心(加密) | 共享 master-key | 按需 |
| 测试 | 配置中心(加密) | 独立 master-key | 每月 |
| 生产 | Vault + HSM | 自动轮换 | 每周/每日 |

---

## 禁止使用的算法

| 算法 | 原因 |
|------|------|
| MD5 | 可碰撞，不安全 |
| SHA-1 | 可碰撞，不安全 |
| DES | 密钥太短，易破解 |
| 3DES | 效率低，安全性不足 |
| RC4 | 已废弃 |
| ECB 模式 | 无随机化，泄露模式 |

---

## 实施步骤

### 1. 立即可做
- [x] 删除 Git 中的敏感配置
- [x] 使用 .gitignore 排除敏感文件
- [x] 启用配置加密存储 (AES-256-GCM)
- [x] 使用 bcrypt 进行密码哈希

### 2. 短期目标
- [ ] 部署 Vault (开发/测试环境)
- [ ] 集成 KMS API
- [ ] 配置 Docker Secrets

### 3. 长期目标
- [ ] 生产环境使用 HSM
- [ ] 自动化密钥轮换
- [ ] 密钥审计日志

---

## 安全检查清单

```bash
# 检查敏感信息泄露
grep -r "password\s*=" --include="*.yml" .
grep -r "secret\s*=" --include="*.yml" .

# 检查禁止算法
grep -r "MD5\|SHA-1\|DES\|RC4" --include="*.java" .
```

---

最后更新: 2026-03-17
