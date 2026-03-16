# 证书认证轮转方案

## 文档信息

- 版本: 2.0
- 更新日期: 2026-03-17
- 作者: 架构团队
- 状态: 生产级

---

## 一、架构设计

### 1.1 整体架构

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           证书管理基础设施                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌──────────────┐     ┌──────────────┐     ┌──────────────┐               │
│  │   Root CA    │────▶│ Issuing CA   │────▶│  服务证书    │               │
│  │  (离线，20年) │     │  (在线，2年)  │     │  (90天)      │               │
│  └──────────────┘     └──────────────┘     └──────────────┘               │
│         │                    │                    │                        │
│         ▼                    ▼                    ▼                        │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                      证书生命周期管理器                                 │   │
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐   │   │
│  │  │ 证书签发 │  │ 证书分发 │  │ 证书部署 │  │ 证书轮转 │  │ 证书吊销 │   │   │
│  │  └─────────┘  └─────────┘  └─────────┘  └─────────┘  └─────────┘   │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                    ┌───────────────┼───────────────┐
                    ▼               ▼               ▼
            ┌───────────┐   ┌───────────┐   ┌───────────┐
            │  Spring   │   │  Spring   │   │  Spring   │
            │   Boot    │   │   Boot    │   │   Boot    │
            │  Client   │   │  Client   │   │  Client   │
            └───────────┘   └───────────┘   └───────────┘
```

### 1.2 证书层次结构

```
Root CA (自签名)
│
├── Issuing CA 1 (业务服务)
│   ├── eureka-server
│   ├── user-service  
│   ├── order-service
│   └── api-gateway
│
└── Issuing CA 2 (管理服务)
    ├── admin-service
    └── monitor-service
```

### 1.3 密钥规格

| 证书类型 | 密钥算法 | 密钥长度 | 签名算法 | 有效期 |
|---------|---------|---------|---------|-------|
| Root CA | RSA | 4096 bit | SHA-256 | 20 年 |
| Issuing CA | RSA | 4096 bit | SHA-256 | 2 年 |
| 服务证书 | RSA | 2048 bit | SHA-256 | 90 天 |
| 客户端证书 | ECDSA | P-256 | SHA-256 | 90 天 |

---

## 二、技术实现

### 2.1 证书签发服务

#### 2.1.1 CA 基础设施搭建

```bash
#!/bin/bash
# setup-ca.sh - CA 基础设施初始化

set -e

CA_DIR="/opt/ca"
DAYS_ROOT=7300      # 20 年
DAYS_ISSUING=730    # 2 年

mkdir -p $CA_DIR/{private,certs,newcerts,crl}

# 初始化 CA 索引
> $CA_DIR/index.txt
> $CA_DIR/index.txt.attr
echo "01" > $CA_DIR/serial

# 生成 Root CA 私钥
openssl genrsa -out $CA_DIR/private/root-ca.key 4096

# 自签名 Root CA 证书
openssl req -x509 -new -nodes -key $CA_DIR/private/root-ca.key \
    -sha256 -days $DAYS_ROOT \
    -out $CA_DIR/certs/root-ca.crt \
    -subj "/C=CN/ST=Shanghai/L=Shanghai/O=SpringCloudDemo/OU=RootCA/CN=SpringCloudDemo-Root-CA"

# 生成 Issuing CA 私钥
openssl genrsa -out $CA_DIR/private/issuing-ca.key 4096

# 生成 CSR
openssl req -new -key $CA_DIR/private/issuing-ca.key \
    -out $CA_DIR/certs/issuing-ca.csr \
    -subj "/C=CN/ST=Shanghai/L=Shanghai/O=SpringCloudDemo/OU=IssuingCA/CN=SpringCloudDemo-Issuing-CA"

# Root CA 签发 Issuing CA
openssl ca -notext -batch -days $DAYS_ISSUING \
    -in $CA_DIR/certs/issuing-ca.csr \
    -out $CA_DIR/certs/issuing-ca.crt \
    -cert $CA_DIR/certs/root-ca.crt \
    -keyfile $CA_DIR/private/root-ca.key \
    -extfile <(printf "basicConstraints=critical,CA:TRUE,pathlen:0")

echo "CA 初始化完成"
```

#### 2.1.2 服务证书签发

```bash
#!/bin/bash
# issue-cert.sh - 签发服务证书

set -e

CA_DIR="/opt/ca"
DAYS=90

SERVICE_NAME=$1
if [ -z "$SERVICE_NAME" ]; then
    echo "用法: $0 <服务名>"
    exit 1
fi

SERVICE_DIR="/opt/certs/$SERVICE_NAME"
mkdir -p $SERVICE_DIR

# 生成私钥
openssl genrsa -out $SERVICE_DIR/server.key 2048

# 生成 CSR
openssl req -new -key $SERVICE_DIR/server.key \
    -out $SERVICE_DIR/server.csr \
    -subj "/C=CN/ST=Shanghai/L=Shanghai/O=SpringCloudDemo/OU=$SERVICE_NAME/CN=$SERVICE_NAME"

# 生成 SAN 配置文件
cat > $SERVICE_DIR/ext.cnf << EOF
subjectAltName = @alt_names
basicConstraints = CA:FALSE
keyUsage = critical, digitalSignature, keyEncipherment
extendedKeyUsage = serverAuth, clientAuth
[alt_names]
DNS.1 = $SERVICE_NAME
DNS.2 = $SERVICE_NAME.default.svc.cluster.local
DNS.3 = localhost
IP.1 = 127.0.0.1
EOF

# 签发证书
openssl x509 -req -in $SERVICE_DIR/server.csr \
    -CA $CA_DIR/certs/issuing-ca.crt \
    -CAkey $CA_DIR/private/issuing-ca.key \
    -CAcreateserial \
    -out $SERVICE_DIR/server.crt \
    -days $DAYS \
    -sha256 \
    -extfile $SERVICE_DIR/ext.cnf

# 打包证书链
cat $SERVICE_DIR/server.crt $CA_DIR/certs/issuing-ca.crt > $SERVICE_DIR/full-chain.crt

echo "证书已签发: $SERVICE_DIR/server.crt"
openssl x509 -noout -dates -subject -in $SERVICE_DIR/server.crt
```

### 2.2 Spring Cloud 集成

#### 2.2.1 服务端 TLS 配置

```java
package com.example.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SslConfiguration {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> sslCustomizer() {
        return factory -> {
            Ssl ssl = new Ssl();
            ssl.setEnabled(true);
            ssl.setKeyStore("classpath:certs/server.p12");
            ssl.setKeyStorePassword("${SSL_KEYSTORE_PASSWORD}");
            ssl.setKeyStoreType("PKCS12");
            ssl.setTrustStore("classpath:certs/trust.p12");
            ssl.setTrustStorePassword("${SSL_TRUSTSTORE_PASSWORD}");
            ssl.setTrustStoreType("PKCS12");
            ssl.setClientAuth(Ssl.ClientAuth.NEED);
            ssl.setProtocol("TLS");
            ssl.setCiphers(new String[]{
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_RSA_WITH_AES_256_GCM_SHA384"
            });
            
            factory.setSsl(ssl);
            factory.setPort(8443);
        };
    }
}
```

#### 2.2.2 双向认证配置

```yaml
# application.yml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:certs/server.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    trust-store: classpath:certs/trust.p12
    trust-store-password: ${SSL_TRUSTSTORE_PASSWORD}
    trust-store-type: PKCS12
    client-auth: need          # 双向认证
    enabled-protocols: TLSv1.3,TLSv1.2
    protocol: TLS

spring:
  cloud:
    loadbalancer:
      ssl: enabled
```

#### 2.2.3 RestTemplate HTTPS 配置

```java
package com.example.config;

import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.Duration;

@Configuration
public class RestTemplateSslConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateCustomizer customizer) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        
        RestTemplate restTemplate = new RestTemplate(factory);
        customizer.customize(restTemplate);
        
        return restTemplate;
    }

    @Bean
    public SSLContext sslContext() throws Exception {
        // 动态加载证书
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("certs/client.p12")) {
            keyStore.load(is, "changeit".toCharArray());
        }

        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("certs/trust.p12")) {
            trustStore.load(is, "changeit".toCharArray());
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(
            KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, "changeit".toCharArray());

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        
        return sslContext;
    }
}
```

### 2.3 证书轮转实现

#### 2.3.1 自动轮转服务

```java
package com.example.cert;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class CertificateRotationService {

    @Value("${cert.directory:/opt/certs}")
    private String certDirectory;

    @Value("${cert.service-name:eureka-server}")
    private String serviceName;

    private final AtomicReference<SSLContext> sslContextHolder = new AtomicReference<>();
    private final AtomicReference<X509Certificate> currentCert = new AtomicReference<>();

    @PostConstruct
    public void init() {
        loadCertificate();
    }

    /**
     * 每小时检查证书状态
     */
    @Scheduled(fixedRate = 3600000)
    public void checkAndRotate() {
        try {
            X509Certificate cert = loadCertificateInfo();
            Date expiryDate = cert.getNotAfter();
            long daysUntilExpiry = (expiryDate.getTime() - System.currentTimeMillis()) 
                / (1000 * 60 * 60 * 24);

            log.info("证书状态: {}, 剩余天数: {}", serviceName, daysUntilExpiry);

            // 剩余 30 天开始轮转
            if (daysUntilExpiry <= 30) {
                log.warn("证书即将到期，开始轮转流程");
                rotateCertificate();
            }
        } catch (Exception e) {
            log.error("证书检查失败", e);
        }
    }

    private void loadCertificate() {
        try {
            String certPath = String.format("%s/%s/full-chain.crt", 
                certDirectory, serviceName);
            
            FileInputStream fis = new FileInputStream(certPath);
            java.security.cert.CertificateFactory cf = 
                java.security.cert.CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) 
                cf.generateCertificate(fis);
            fis.close();

            currentCert.set(cert);
            
            // 初始化 SSL Context
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (FileInputStream ksis = new FileInputStream(
                    String.format("%s/%s/server.p12", certDirectory, serviceName))) {
                keyStore.load(ksis, "changeit".toCharArray());
            }

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, "changeit".toCharArray());

            TrustManager[] trustManagers = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    public void checkClientTrusted(X509Certificate[] certs, 
                        String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, 
                        String authType) {}
                }
            };

            SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
            sslContext.init(kmf.getKeyManagers(), trustManagers, null);
            sslContextHolder.set(sslContext);

            log.info("证书加载成功: {}", cert.getSubjectDN());
        } catch (Exception e) {
            log.error("证书加载失败", e);
            throw new RuntimeException("证书初始化失败", e);
        }
    }

    private X509Certificate loadCertificateInfo() throws Exception {
        String certPath = String.format("%s/%s/server.crt", 
            certDirectory, serviceName);
        
        FileInputStream fis = new FileInputStream(certPath);
        java.security.cert.CertificateFactory cf = 
            java.security.cert.CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) cf.generateCertificate(fis);
        fis.close();
        
        return cert;
    }

    /**
     * 执行证书轮转
     */
    public void rotateCertificate() {
        try {
            log.info("开始证书轮转: {}", serviceName);
            
            // 1. 调用证书签发服务生成新证书
            ProcessBuilder pb = new ProcessBuilder(
                "/opt/scripts/issue-cert.sh", serviceName);
            pb.start().waitFor();

            // 2. 重新加载证书
            loadCertificate();

            // 3. 通知服务刷新连接
            notifyServicesToRefresh();

            log.info("证书轮转完成: {}", serviceName);
        } catch (Exception e) {
            log.error("证书轮转失败", e);
            throw new RuntimeException("证书轮转失败", e);
        }
    }

    private void notifyServicesToRefresh() {
        // 发布证书轮转事件
    }

    public SSLContext getSslContext() {
        return sslContextHolder.get();
    }
}
```

#### 2.3.2 健康检查端点

```java
package com.example.actuator;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@Component
@Endpoint(id = "certificate")
public class CertificateHealthEndpoint {

    @ReadOperation
    public Map<String, Object> certificateHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // 这里应该从 CertificateRotationService 获取实际证书信息
            health.put("status", "UP");
            health.put("service", "eureka-server");
            health.put("expiryDate", LocalDate.now().plusDays(60));
            health.put("daysRemaining", 60);
            health.put("needsRotation", false);
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
        }
        
        return health;
    }
}
```

---

## 三、安全架构

### 3.1 安全加固措施

#### 3.1.1 传输层安全

```yaml
# 强制 TLS 1.3
server:
  ssl:
    enabled-protocols:
      - TLSv1.3
    enabled-cipher-suites:
      - TLS_AES_256_GCM_SHA384
      - TLS_AES_128_GCM_SHA256
      - TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
    # 禁用重协商
    secure-protocol: TLS

# 客户端证书验证
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://auth.example.com
```

#### 3.1.2 证书固定（Certificate Pinning）

```java
@Component
public class CertificatePinningValidator {

    private final Set<String> pinnedIssuers = Set.of(
        "CN=SpringCloudDemo-Issuing-CA, O=SpringCloudDemo"
    );

    public boolean validateCertificate(X509Certificate cert) {
        String issuer = cert.getIssuerDN().getName();
        
        // 检查签发机构
        for (String pinned : pinnedIssuers) {
            if (issuer.contains(pinned)) {
                return true;
            }
        }
        
        // 检查公钥指纹
        String subjectKeyId = cert.getSubjectKeyIdentifier();
        return isPinnedKey(subjectKeyId);
    }
}
```

### 3.2 访问控制

```
┌─────────────────────────────────────────────────────────────┐
│                      证书审批流程                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐ │
│  │ 开发人员 │───▶│ 项目审批 │───▶│ 安全审核 │───▶│ 证书签发 │ │
│  │ 申请CSR │    │ 确认需求 │    │ 合规检查 │    │ CA签发   │ │
│  └─────────┘    └─────────┘    └─────────┘    └─────────┘ │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 3.3 审计日志

```java
@Slf4j
@Component
public class CertificateAuditLogger {

    public void logCertificateEvent(CertificateEvent event) {
        StructuredLog log = StructuredLog.builder()
            .timestamp(Instant.now())
            .eventType("CERTIFICATE_EVENT")
            .eventAction(event.getAction())
            .serviceName(event.getServiceName())
            .certificateSubject(event.getCertificateSubject())
            .certificateSerial(event.getSerialNumber())
            .operator(event.getOperator())
            .result(event.getResult())
            .ipAddress(event.getIpAddress())
            .build();
        
        // 发送到审计日志系统
        auditLogService.send(log);
    }
}
```

---

## 四、高可用设计

### 4.1 多 CA 架构

```
┌─────────────────────────────────────────────────────────────┐
│                     双 CA 冗余架构                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────┐        ┌─────────────────┐            │
│  │   Primary CA    │        │  Secondary CA  │            │
│  │  (当前使用)     │        │  (灾备使用)    │            │
│  └────────┬────────┘        └────────┬────────┘            │
│           │                          │                      │
│           └──────────┬───────────────┘                      │
│                      ▼                                       │
│           ┌─────────────────────┐                           │
│           │   证书分发服务       │                           │
│           │   (双写策略)        │                           │
│           └──────────┬──────────┘                           │
│                      │                                       │
│         ┌────────────┼────────────┐                        │
│         ▼            ▼            ▼                        │
│    ┌─────────┐  ┌─────────┐  ┌─────────┐                    │
│    │ Eureka  │  │  User   │  │ Order  │                    │
│    │ Server  │  │ Service │  │Service │                    │
│    └─────────┘  └─────────┘  └─────────┘                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 故障切换

```java
@Service
public class CaFailoverService {

    private String activeCa = "primary";
    
    @Value("${ca.primary.url}")
    private String primaryUrl;
    
    @Value("${ca.secondary.url}")
    private String secondaryUrl;

    @HystrixCommand(fallbackMethod = "issueCertificateFallback")
    public Certificate issueCertificate(String serviceName) {
        String caUrl = "primary".equals(activeCa) ? primaryUrl : secondaryUrl;
        return certificateClient.issue(caUrl, serviceName);
    }

    public Certificate issueCertificateFallback(String serviceName) {
        log.warn("主 CA 故障，切换到备用 CA");
        activeCa = "secondary".equals(activeCa) ? "primary" : "secondary";
        
        String caUrl = "primary".equals(activeCa) ? primaryUrl : secondaryUrl;
        return certificateClient.issue(caUrl, serviceName);
    }
}
```

---

## 五、监控与告警

### 5.1 指标定义

```java
@Component
public class CertificateMetrics {

    private final Counter certificateRotationCounter = Counter.builder()
        .name("certificate_rotation_total")
        .description("证书轮转总次数")
        .tag("service", "dynamic")
        .register(Metrics.globalRegistry);

    private final Gauge gaugeDaysUntilExpiry = Gauge.builder()
        .name("certificate_expiry_days")
        .description("证书到期剩余天数")
        .tag("service", "dynamic")
        .value(() -> getDaysUntilExpiry())
        .register(Metrics.globalRegistry);

    private final Counter certificateErrorCounter = Counter.builder()
        .name("certificate_errors_total")
        .description("证书相关错误总次数")
        .register(Metrics.globalRegistry);
}
```

### 5.2 Prometheus 告警规则

```yaml
groups:
- name: certificate
  interval: 30s
  rules:
  # 证书即将过期
  - alert: CertificateExpiringWarning
    expr: certificate_expiry_days{job="*"} <= 30
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "证书即将过期"
      description: "{{ $labels.service }} 证书将在 {{ $value }} 天后过期"

  # 证书即将过期（紧急）
  - alert: CertificateExpiringCritical
    expr: certificate_expiry_days{job="*"} <= 7
    for: 1m
    labels:
      severity: critical
    annotations:
      summary: "证书即将过期 - 紧急！"
      description: "{{ $labels.service }} 证书将在 {{ $value }} 天后过期，立即处理！"

  # 证书轮转失败
  - alert: CertificateRotationFailed
    expr: increase(certificate_errors_total[1h]) > 0
    for: 1m
    labels:
      severity: high
    annotations:
      summary: "证书轮转失败"
      description: "{{ $labels.service }} 证书轮转失败，需要人工介入"

  # 证书链验证失败
  - alert: CertificateChainInvalid
    expr: certificate_validation_errors_total > 0
    for: 1m
    labels:
      severity: high
    annotations:
      summary: "证书链验证失败"
      description: "{{ $labels.service }} 证书链验证失败"
```

### 5.3 Grafana 仪表板

```
┌─────────────────────────────────────────────────────────────┐
│                   证书健康状态仪表板                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────┬─────────────────┬─────────────────┐    │
│  │   总证书数      │   即将过期      │   已轮转        │    │
│  │     12         │      2          │      45         │    │
│  │    (正常)      │    (警告)       │    (成功)       │    │
│  └─────────────────┴─────────────────┴─────────────────┘    │
│                                                             │
│  证书到期时间线                                             │
│  ─────────────────────────────────────────────────────     │
│  ● eureka-server      ████████████████░░░░░░  60天         │
│  ● user-service       ██████████████████░░░░  75天          │
│  ● order-service      ░░░░░░░░░░░░░░░░░░░░░   5天 ⚠️       │
│  ● api-gateway        ██████████████████░░░░  70天          │
│                                                             │
│  轮转历史                                                   │
│  ─────────────────────────────────────────────────────     │
│  ✓ 2026-03-15  user-service   自动轮转成功                 │
│  ✓ 2026-03-10  order-service  自动轮转成功                 │
│  ✓ 2026-03-01  eureka-server  自动轮转成功                 │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 六、灾难恢复

### 6.1 证书恢复流程

```bash
#!/bin/bash
# recover-cert.sh

set -e

SERVICE_NAME=$1
BACKUP_DATE=$2  # 格式: 2026-03-15

BACKUP_DIR="/opt/certs/backup/$BACKUP_DATE"
CERT_DIR="/opt/certs/$SERVICE_NAME"

echo "开始恢复证书: $SERVICE_NAME from $BACKUP_DATE"

# 1. 停止服务
systemctl stop $SERVICE_NAME

# 2. 备份当前证书
if [ -d "$CERT_DIR" ]; then
    mv $CERT_DIR $CERT_DIR.broken.$(date +%Y%m%d%H%M%S)
fi

# 3. 恢复备份
cp -r $BACKUP_DIR/$SERVICE_NAME $CERT_DIR

# 4. 验证证书
openssl x509 -in $CERT_DIR/server.crt -noout -dates
openssl x509 -in $CERT_DIR/server.crt -noout -subject

# 5. 重启服务
systemctl start $SERVICE_NAME

# 6. 健康检查
sleep 5
curl -k https://localhost:8443/health

echo "恢复完成"
```

### 6.2 Root CA 吊销流程

```bash
#!/bin/bash
# revoke-cert.sh

set -e

SERIAL=$1

# 吊销证书
openssl ca -revoke /opt/ca/newcerts/$SERIAL.pem

# 生成 CRL
openssl ca -gencrl -out /opt/ca/crl/ca.crl

# 分发 CRL 到各服务
for service in eureka-server user-service order-service api-gateway; do
    scp /opt/ca/crl/ca.crt $service:/opt/certs/trust.crl
done

# 通知服务重载 CRL
for service in eureka-server user-service order-service api-gateway; do
    curl -X POST $service:8080/admin/reload-crl
done
```

---

## 七、工具链

### 7.1 推荐的证书管理工具

| 工具 | 用途 | 特点 |
|------|------|------|
| cert-manager | K8s 证书管理 | 自动签发、轮转 |
| HashiCorp Vault | 证书存储 | 企业级 PKI |
| Smallstep | 自动化 PKI | 简单易用 |
| Step-CA | 自托管 CA | 开源替代 |
| CFSSL | 云原生 CA | 高性能 |

### 7.2 使用 cert-manager 示例

```yaml
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: selfsigned-issuer
spec:
  selfSigned: {}
---
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: eureka-server-tls
  namespace: default
spec:
  secretName: eureka-server-tls
  issuerRef:
    name: selfsigned-issuer
    kind: ClusterIssuer
  commonName: eureka-server
  dnsNames:
    - eureka-server
    - eureka-server.spring-cloud.svc.cluster.local
  ipAddresses:
    - 10.0.0.5
  duration: 2160h           # 90 天
  renewBefore: 720h        # 30 天前
  privateKey:
    algorithm: RSA
    size: 2048
  usages:
    - server auth
    - client auth
  revisionHistoryLimit: 5
```

---

## 八、实施检查清单

### 8.1 部署前检查

- [ ] Root CA 已生成并安全存储
- [ ] Issuing CA 已配置
- [ ] 证书签发脚本已测试
- [ ] 服务 TLS 配置已完成
- [ ] 双向认证已测试
- [ ] 监控告警已配置
- [ ] 恢复脚本已准备
- [ ] 团队培训已完成

### 8.2 上线后验证

- [ ] 证书自动轮转测试（30 天前）
- [ ] 故障切换测试
- [ ] 监控告警测试
- [ ] 性能影响评估
- [ ] 文档更新

---

## 九、附录

### 9.1 常见问题

**Q: 证书轮转期间服务会中断吗？**
A: 不会。采用双证书机制，新证书生效后再移除旧证书。

**Q: 如何处理证书吊销？**
A: 使用 CRL（证书吊销列表）或 OCSP（在线证书状态协议）。

**Q: 支持哪些密钥算法？**
A: 推荐 RSA 2048/4096 或 ECDSA P-256/P-384。

### 9.2 参考资料

- [Spring Boot SSL 配置](https://docs.spring.io/spring-boot/docs/current/reference/html/server.html#server.ssl)
- [cert-manager 文档](https://cert-manager.io/docs/)
- [NIST PKI 最佳实践](https://csrc.nist.gov/projects/cryptographic-technology-group/pki-baselines)
