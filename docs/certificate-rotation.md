# 证书认证轮转方案

## 概述

本文档设计一套完整的证书自动轮转方案，确保服务间通信安全。

## 1. 证书架构

```
┌─────────────────────────────────────────────────────────────┐
│                      根 CA (Root CA)                        │
│                   (长期有效，离线存储)                        │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   签发 CA (Issuing CA)                      │
│                  (有效期 1-2 年，定期轮转)                   │
└─────────────────────────────────────────────────────────────┘
                              │
          ┌───────────────────┼───────────────────┐
          ▼                   ▼                   ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│  Eureka Server │  │  User Service  │  │  Order Service │
│    证书         │  │    证书         │  │    证书         │
│  (有效期 90天)  │  │  (有效期 90天)  │  │  (有效期 90天)  │
└─────────────────┘  └─────────────────┘  └─────────────────┘
```

## 2. 证书类型与有效期

| 证书类型 | 有效期 | 轮转频率 | 用途 |
|---------|-------|---------|------|
| Root CA | 10-20 年 | 几乎不轮转 | 信任锚点 |
| Issuing CA | 1-2 年 | 每年 | 签发服务证书 |
| 服务证书 | 90 天 | 每 30 天 | 服务间 TLS 通信 |
| 客户端证书 | 90 天 | 每 30 天 | 服务身份认证 |

## 3. 轮转策略

### 3.1 自动轮转流程

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  证书到期前   │────▶│  生成新证书  │────▶│  部署新证书  │
│   30 天      │     │   (自动)     │     │   (滚动)    │
└──────────────┘     └──────────────┘     └──────────────┘
        │                                        │
        │           ┌──────────────┐             │
        └──────────▶│  重启服务    │◀────────────┘
                    │  (零停机)   │
                    └──────────────┘
```

### 3.2 轮转时间线（示例：90天证书）

| 天数 | 操作 |
|------|------|
| Day 0 | 颁发证书 |
| Day 60 | 生成新证书（提前 30 天） |
| Day 65 | 部署新证书到测试环境 |
| Day 70 | 部署到生产环境 |
| Day 90 | 旧证书过期 |

## 4. 实现方案

### 4.1 使用 Cert-Manager（推荐 Kubernetes）

```yaml
# cert-manager Issuer
apiVersion: cert-manager.io/v1
kind: Issuer
metadata:
  name: selfsigned-issuer
spec:
  selfSigned: {}
---
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: eureka-server-tls
spec:
  secretName: eureka-server-tls
  issuerRef:
    name: selfsigned-issuer
  commonName: eureka-server
  dnsNames:
    - eureka-server
    - eureka-server.default.svc.cluster.local
  duration: 2160h           # 90 天
  renewBefore: 720h         # 提前 30 天
```

### 4.2 使用 Spring Cloud Certificate Rotation

```java
// 配置证书自动刷新
@Configuration
public class SslConfig {
    
    @Bean
    public SSLContext sslContext() throws Exception {
        // 定期刷新证书
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // 重新加载证书
                refreshSslContext();
            } catch (Exception e) {
                log.error("证书刷新失败", e);
            }
        }, 30, 30, TimeUnit.DAYS);
        
        return sslContext;
    }
}
```

### 4.3 使用脚本轮转（非 Kubernetes）

```bash
#!/bin/bash
# rotate-certs.sh

CERT_DIR="/opt/certs"
DAYS_BEFORE=30

# 1. 检查证书到期时间
check_expiry() {
    local cert=$1
    local expiry=$(openssl x509 -enddate -noout -in $cert | cut -d= -f2)
    local expiry_ts=$(date -d "$expiry" +%s)
    local now_ts=$(date +%s)
    local days_left=$(( (expiry_ts - now_ts) / 86400 ))
    
    echo $days_left
}

# 2. 生成新证书
generate_cert() {
    local service=$1
    local days=90
    
    openssl req -new -newkey rsa:4096 \
        -keyout $CERT_DIR/${service}.key \
        -out $CERT_DIR/${service}.csr \
        -nodes \
        -subj "/CN=${service}"
    
    openssl x509 -req -in $CERT_DIR/${service}.csr \
        -CA $CERT_DIR/ca.crt \
        -CAkey $CERT_DIR/ca.key \
        -CAcreateserial \
        -out $CERT_DIR/${service}.crt \
        -days $days \
        -extfile <(printf "subjectAltName=DNS:%s" $service)
}

# 3. 轮转证书
rotate_certs() {
    for service in eureka-server user-service order-service api-gateway; do
        days_left=$(check_expiry $CERT_DIR/${service}.crt)
        
        if [ $days_left -lt $DAYS_BEFORE ]; then
            echo "轮转 $service 证书 (剩余 $days_left 天)"
            generate_cert $service
            
            # 通知服务重载证书
            systemctl reload $service
        fi
    done
}

# 4. 加入 crontab
# 0 2 * * * /opt/scripts/rotate-certs.sh
```

## 5. 证书存储

### 5.1 开发/测试环境

```
src/
└── main/
    └── resources/
        ├── certs/
        │   ├── server.crt
        │   ├── server.key
        │   └── ca.crt
        └── application.yml
```

### 5.2 生产环境

- **Kubernetes**: 使用 Sealed Secret 或 Vault
- **传统部署**: 使用 HashiCorp Vault 或 AWS Secrets Manager

```yaml
# application.yml (生产)
spring:
  ssl:
    key-store: vault://secret/myapp/tls
    key-store-password: ${VAULT_TOKEN}
    trust-store: vault://secret/myapp/trust
```

## 6. 监控与告警

### 6.1 证书到期监控

```yaml
# Prometheus 告警规则
groups:
- name: certificate
  rules:
  - alert: CertificateExpiringSoon
    expr: (cert_manager_certificate_expiration_timestamp_seconds - time()) < 2592000
    for: 1h
    labels:
      severity: warning
    annotations:
      summary: "证书即将到期"
      description: "{{ $labels.name }} 将在 {{ $value | humanizeDuration }} 后过期"
```

### 6.2 指标采集

| 指标名 | 描述 |
|--------|------|
| `certificate_expiry_days` | 证书剩余天数 |
| `certificate_rotation_total` | 轮转次数 |
| `certificate_rotation_errors` | 轮转失败次数 |

## 7. 应急回滚

### 7.1 快速回滚脚本

```bash
#!/bin/bash
# rollback-certs.sh

BACKUP_DIR="/opt/certs/backup/$(date +%Y%m%d)"

# 1. 停止服务
systemctl stop eureka-server user-service order-service api-gateway

# 2. 恢复证书
cp -r $BACKUP_DIR/* /opt/certs/

# 3. 启动服务
systemctl start eureka-server user-service order-service api-gateway

# 4. 验证
curl -k https://localhost:8761/health
```

### 7.2 保持双证书

```yaml
# 配置双证书支持（无缝切换）
spring:
  ssl:
    key-store: /opt/certs/server.crt
    key-store-pass: ${CERT_PASSWORD}
    # 保持旧证书直到新证书完全生效
    # 部署时同时保留新旧证书
```

## 8. 最佳实践

1. **分层 CA**: 使用 Root CA → Intermediate CA → Service Certificate
2. **最小权限**: 每个服务使用独立证书
3. **自动轮转**: 至少提前 30 天开始轮转
4. **监控告警**: 证书到期前 30/14/7 天告警
5. **定期测试**: 每月测试一次轮转流程
6. **离线 Root CA**: Root CA 保持离线存储
7. **证书透明**: 使用 Certificate Transparency 日志
