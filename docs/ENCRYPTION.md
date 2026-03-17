# 数据库敏感字段加密方案

## 概述

本文档描述数据库敏感字段的国密加密存储方案，使用 AES-256-GCM 算法（等效国密 SM4）。

## 敏感字段清单

以下字段在数据库中以密文形式存储：

| 实体 | 字段 | 说明 |
|------|------|------|
| User | password | 用户密码 |
| RefreshToken | token | 刷新令牌 |
| ServiceAccount | secretKeyHash | 密钥哈希 |
| OidcClient | clientSecret | OIDC 客户端密钥 |

## 加密算法

### AES-256-GCM

- **算法**: AES-256-GCM
- **密钥长度**: 256 位 (32 字节)
- **IV 长度**: 12 字节
- **Tag 长度**: 128 位
- **模式**: GCM (Galois/Counter Mode)

### 密钥派生

使用 SM3 模拟 KDF (Key Derivation Function) 进行密码派生：

```
SM4-KDF(password, salt, keyLength) → 派生密钥
```

## 配置说明

### 1. 生成加密密钥

在 application.yml 中配置加密密钥：

```yaml
encryption:
  key: your-base64-encoded-32-byte-key
```

### 2. 生成密钥的方式

通过 GmCryptUtil 生成随机密钥：

```java
@Autowired
private GmCryptUtil cryptUtil;

// 生成随机密钥
String key = cryptUtil.generateKey(); // 返回 Base64 编码的 32 字节密钥
```

### 3. 密钥管理

**重要**: 
- 密钥必须妥善保管，丢失后将无法解密已有数据
- 生产环境建议使用配置中心或密钥管理服务 (KMS)
- 密钥定期轮换需要配合数据迁移

## 自动加密机制

### 实体监听器

使用 JPA `@EntityListeners` 实现自动加密解密：

```java
@Entity
@EntityListeners(EntityEncryptListener.class)
public class User {
    private String password;
    // ...
}
```

### 工作流程

```
保存实体 (PrePersist/PreUpdate):
  1. 获取明文字段值
  2. 使用加密密钥加密
  3. 存储密文到数据库

加载实体 (PostLoad):
  1. 获取密文字段值
  2. 使用加密密钥解密
  3. 返回明文给应用
```

## 使用示例

### 1. 配置加密密钥

```yaml
# application.yml
encryption:
  key: aGVsbG93b3JsZGhlbGxvd29ybGRoZWxsb3dvcmxkaGVsbG8=
```

### 2. 实体自动加密

```java
// 保存用户时自动加密密码
User user = new User();
user.setUsername("test");
user.setPassword("myPassword123"); // 自动加密存储

userRepository.save(user); // 数据库中存储的是密文

// 查询时自动解密
User loaded = userRepository.findByUsername("test");
loaded.getPassword(); // 返回解密后的明文
```

### 3. 手动加密/解密

```java
@Autowired
private GmCryptUtil cryptUtil;

// 手动加密
String encrypted = cryptUtil.encrypt("原始数据", encryptionKey);

// 手动解密
String decrypted = cryptUtil.decrypt(encryptedData, encryptionKey);
```

## 安全注意事项

1. **密钥安全**: 密钥不能硬编码在代码中，应使用环境变量或配置中心
2. **密钥轮换**: 定期更换密钥需要重新加密数据
3. **传输安全**: 密钥在网络传输时必须使用 HTTPS
4. **日志脱敏**: 避免在日志中打印明文密钥或敏感数据

## 代码结构

```
common/
├── pom.xml
└── src/main/java/com/example/common/
    ├── encryption/
    │   └── GmCryptUtil.java      # 国密加密工具类
    └── listener/
        └── EntityEncryptListener.java  # 实体加密监听器
```

---

最后更新: 2026-03-17
