# 部署说明

## 1. 环境要求

### 1.1 开发环境

| 组件 | 要求 |
|------|------|
| JDK | 21+ |
| Maven | 3.9+ |
| PostgreSQL | 8432 端口 |
| Eureka Server | 9000 端口 |
| frp (本地) | 用于内网穿透 |

### 1.2 生产环境（阿里云）

| 组件 | 配置 |
|------|------|
| 服务器 | 2核 4GB+ |
| 系统 | Ubuntu 20.04+ |
| PostgreSQL | 8432 端口 |
| Eureka | 9000 端口 |
| frps | 9999 端口 |

---

## 2. 快速开始

### 2.1 本地开发模式

```bash
# 1. 克隆代码
git clone https://github.com/ANewName-1024/spring-cloud-demo.git
cd spring-cloud-demo

# 2. 设置环境变量
cp user-service/.env.example user-service/.env
# 编辑 .env 文件，填入实际配置

# 3. 启动 frp 客户端（需要先启动 frps）
cd frp/frp_0.52.3_windows_amd64
./frpc.exe -c frpc.toml

# 4. 启动服务（另开终端）
cd user-service
mvn spring-boot:run

# 5. 启动 Gateway（另开终端）
cd gateway
mvn spring-boot:run
```

### 2.2 访问地址

| 服务 | 本地地址 | 公网地址 |
|------|---------|---------|
| Gateway | http://localhost:8080 | http://your-server:8080 |
| User Service | http://localhost:8081 | http://your-server:8080/user |
| Eureka | http://localhost:9000 | http://your-server:9000 |

---

## 3. 阿里云服务器部署

### 3.1 frp 服务器部署

```bash
# 1. SSH 连接到服务器
ssh -i your-key.pem -p 2222 root@your-server-ip

# 2. 下载 frp
cd /opt
wget https://github.com/fatedier/frp/releases/download/v0.52.3/frp_0.52.3_linux_amd64.tar.gz
tar -xzf frp_0.52.3_linux_amd64.tar.gz
mv frp_0.52.3_linux_amd64 frp

# 3. 配置 frps
cat > /opt/frp/frps.toml << 'EOF'
[common]
bind_addr = 0.0.0.0
bind_port = 9999
token = your-secure-token
allow_ports = 8080,8081,8082

[gateway]
type = tcp
local_ip = 127.0.0.1
local_port = 8080
remote_port = 8080
EOF

# 4. 启动 frps
nohup /opt/frp/frps -c /opt/frp/frps.toml > /var/log/frps.log 2>&1 &

# 5. 验证
ps aux | grep frps
ss -tlnp | grep 9999
```

### 3.2 阿里云安全组配置

在阿里云控制台添加安全组规则：

| 端口 | 协议 | 用途 |
|------|------|------|
| 9999 | TCP | frp 服务端口 |
| 8080 | TCP | Gateway 入口 |
| 9000 | TCP | Eureka 注册中心 |
| 8432 | TCP | PostgreSQL 数据库 |

### 3.3 本地 frpc 配置

```toml
# frpc.toml
[common]
server_addr = your-server-ip
server_port = 9999
token = your-secure-token

[gateway]
type = tcp
local_ip = 127.0.0.1
local_port = 8080
remote_port = 8080
```

---

## 4. 服务配置

### 4.1 环境变量

在 `user-service/.env` 中配置：

```bash
# 数据库配置
DB_HOST=your-server-ip
DB_PORT=8432
DB_NAME=business_db
DB_USERNAME=business
DB_PASSWORD=your-db-password

# SSL 证书路径
SSL_CERT_PATH=path/to/server.crt

# Eureka 配置
EUREKA_HOST=your-server-ip
EUREKA_PORT=9000
EUREKA_USER=admin
EUREKA_PASSWORD=your-eureka-password

# JWT 配置
JWT_SECRET=your-jwt-secret-key
JWT_EXPIRATION=86400000

# 注册配置
INVITE_CODE_REQUIRED=false
INVITE_CODES=
```

### 4.2 启动服务

```bash
# 方式1: Maven 启动
export $(cat user-service/.env | xargs) && mvn spring-boot:run

# 方式2: JAR 启动
mvn clean package -DskipTests
java -jar -DDB_HOST=your-server-ip -DDB_PASSWORD=your-password user-service/target/user-service-1.0.0.jar
```

---

## 5. Docker 部署

### 5.1 构建镜像

```bash
docker-compose build
```

### 5.2 启动服务

```bash
docker-compose up -d
```

### 5.3 查看日志

```bash
docker-compose logs -f
```

---

## 6. 验证部署

### 6.1 检查服务状态

```bash
# 检查本地端口
netstat -ano | findstr "8080 8081 9000"

# 检查 Eureka 注册
curl http://localhost:9000/eureka/apps

# 测试登录
curl -X POST http://localhost:8081/user/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test123"}'
```

### 6.2 健康检查

| 服务 | 健康检查地址 |
|------|-------------|
| Gateway | http://localhost:8080/actuator/health |
| User Service | http://localhost:8081/actuator/health |

---

## 7. 常见问题

### 7.1 无法连接到 Eureka

- 检查 Eureka 服务是否启动
- 检查防火墙端口 9000 是否开放
- 检查环境变量 EUREKA_HOST 配置

### 7.2 数据库连接失败

- 检查 PostgreSQL 服务状态
- 检查数据库用户名密码
- 检查 SSL 证书路径

### 7.3 frp 连接失败

- 检查 frps 是否启动
- 检查安全组端口 9999 是否开放
- 检查 token 是否匹配
