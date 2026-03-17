#!/bin/bash
# Spring Cloud Demo - 自动化部署脚本
# 用法: ./deploy.sh [start|stop|restart|status|logs|build|perm|help]

set -e

# 配置
APP_NAME="springcloud-demo"
APP_DIR="/opt/${APP_NAME}"
VERSION="${1:-latest}"
CURRENT_USER=$(whoami)

# 端口配置
EUREKA_PORT=8761
CONFIG_PORT=8082
USER_PORT=8081
GATEWAY_PORT=8080
OPS_PORT=8090

# 用户和组
APP_USER="springcloud"
APP_GROUP="springcloud"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_step() { echo -e "${BLUE}[STEP]${NC} $1"; }

# ========== 权限管理 ==========

# 创建应用用户和组
create_app_user() {
    log_info "创建应用用户和组..."
    
    # 创建用户组
    if ! getent group ${APP_GROUP} > /dev/null 2>&1; then
        groupadd ${APP_GROUP}
        log_info "创建用户组: ${APP_GROUP}"
    fi
    
    # 创建用户
    if ! id ${APP_USER} > /dev/null 2>&1; then
        useradd -r -g ${APP_GROUP} -s /sbin/nologin -d /opt/${APP_NAME} ${APP_USER}
        log_info "创建用户: ${APP_USER}"
    fi
    
    # 创建目录
    mkdir -p ${APP_DIR}/{app,config,logs,scripts,backup}
    mkdir -p /var/log/${APP_NAME}
    
    log_info "目录创建完成"
}

# 设置文件权限
set_permissions() {
    log_info "设置文件权限..."
    
    # 创建目录
    mkdir -p ${APP_DIR}/{app,config,logs,scripts,backup}
    mkdir -p /var/log/${APP_NAME}
    
    # 设置目录权限
    chown -R ${APP_USER}:${APP_GROUP} ${APP_DIR}
    chown -R ${APP_USER}:${APP_GROUP} /var/log/${APP_NAME}
    
    # 设置目录权限 (755)
    chmod 755 ${APP_DIR}
    chmod 755 ${APP_DIR}/app
    chmod 755 ${APP_DIR}/config
    chmod 755 ${APP_DIR}/scripts
    chmod 755 ${APP_DIR}/backup
    
    # 日志目录 (750)
    chmod 750 ${APP_DIR}/logs
    chmod 750 /var/log/${APP_NAME}
    
    # 配置文件 (600 - 只有 owner 可读)
    if [ -d "${APP_DIR}/config" ]; then
        find ${APP_DIR}/config -type f -exec chmod 600 {} \;
    fi
    
    # 脚本文件 (550 - 可执行)
    if [ -d "${APP_DIR}/scripts" ]; then
        chmod 550 ${APP_DIR}/scripts/*.sh
    fi
    
    # 应用 JAR 文件 (500 - 只读)
    if [ -d "${APP_DIR}/app" ]; then
        find ${APP_DIR}/app -name "*.jar" -exec chmod 500 {} \;
    fi
    
    log_info "权限设置完成"
    
    # 显示权限
    show_permissions
}

# 显示权限
show_permissions() {
    echo ""
    echo "========================================"
    echo "  文件权限详情"
    echo "========================================"
    echo ""
    echo -e "${CYAN}目录权限:${NC}"
    ls -ld ${APP_DIR} ${APP_DIR}/app ${APP_DIR}/config ${APP_DIR}/logs ${APP_DIR}/scripts 2>/dev/null || true
    
    echo ""
    echo -e "${CYAN}脚本权限:${NC}"
    ls -l ${APP_DIR}/scripts/*.sh 2>/dev/null || echo "  无脚本文件"
    
    echo ""
    echo -e "${CYAN}应用用户:${NC}"
    id ${APP_USER} 2>/dev/null || echo "  用户不存在"
    
    echo ""
}

# 检查当前用户
check_user() {
    if [ "$CURRENT_USER" = "root" ]; then
        return 0
    else
        log_error "此操作需要 root 权限"
        exit 1
    fi
}

# ========== 服务管理 ==========

# 检查 Java
check_java() {
    if ! command -v java &> /dev/null; then
        log_error "Java 未安装"
        exit 1
    fi
    log_info "Java 版本: $(java -version 2>&1 | head -1)"
}

# 停止服务
stop_services() {
    log_info "停止服务..."
    for service in eureka-server gateway user-service config-service ops-service; do
        if pgrep -f "${service}" > /dev/null; then
            log_warn "停止 ${service}..."
            pkill -f "${service}" || true
        fi
    done
    sleep 3
}

# 启动服务
start_services() {
    log_info "读取配置..."
    if [ ! -f "${APP_DIR}/config/env.conf" ]; then
        log_error "配置文件不存在: ${APP_DIR}/config/env.conf"
        exit 1
    fi
    
    source ${APP_DIR}/config/env.conf
    
    # 设置默认值
    : ${EUREKA_PORT:=8761}
    : ${CONFIG_PORT:=8082}
    : ${USER_PORT:=8081}
    : ${GATEWAY_PORT:=8080}
    : ${OPS_PORT:=8090}
    : ${ENABLE_OPS:=true}
    
    log_info "启动服务 (以 ${APP_USER} 用户运行)..."
    
    # 1. Eureka
    log_step "1/5 启动 Eureka Server (${EUREKA_PORT})..."
    su - ${APP_USER} -s /bin/bash -c "
        nohup java -Xms256m -Xmx512m \
            -Deureka.instance.hostname=${EUREKA_HOST:-localhost} \
            -Deureka.client.service-url.defaultZone=http://${EUREKA_USER:-admin}:${EUREKA_PASSWORD:-admin}@${EUREKA_HOST:-localhost}:${EUREKA_PORT}/eureka/ \
            -Dserver.port=${EUREKA_PORT} \
            ${APP_DIR}/app/eureka-server/*.jar \
            >> /var/log/${APP_NAME}/eureka.log 2>&1 &
    "
    
    sleep 10
    
    # 2. Config Service
    log_step "2/5 启动 Config Service (${CONFIG_PORT})..."
    su - ${APP_USER} -s /bin/bash -c "
        nohup java -Xms256m -Xmx512m \
            -Dspring.datasource.url=jdbc:postgresql://${DB_HOST:-localhost}:${DB_PORT:-5432}/${DB_NAME:-springcloud} \
            -Dspring.datasource.username=${DB_USER:-business} \
            -Dspring.datasource.password=${DB_PASSWORD:-business} \
            -Deureka.client.service-url.defaultZone=http://${EUREKA_USER:-admin}:${EUREKA_PASSWORD:-admin}@${EUREKA_HOST:-localhost}:${EUREKA_PORT}/eureka/ \
            -Dserver.port=${CONFIG_PORT} \
            ${APP_DIR}/app/config-service/*.jar \
            >> /var/log/${APP_NAME}/config.log 2>&1 &
    "
    
    sleep 5
    
    # 3. User Service
    log_step "3/5 启动 User Service (${USER_PORT})..."
    su - ${APP_USER} -s /bin/bash -c "
        nohup java -Xms512m -Xmx1024m \
            -Dspring.datasource.url=jdbc:postgresql://${DB_HOST:-localhost}:${DB_PORT:-5432}/${DB_NAME:-springcloud} \
            -Dspring.datasource.username=${DB_USER:-business} \
            -Dspring.datasource.password=${DB_PASSWORD:-business} \
            -Deureka.client.service-url.defaultZone=http://${EUREKA_USER:-admin}:${EUREKA_PASSWORD:-admin}@${EUREKA_HOST:-localhost}:${EUREKA_PORT}/eureka/ \
            -DJWT_SECRET=${JWT_SECRET:-default-secret} \
            -Dserver.port=${USER_PORT} \
            ${APP_DIR}/app/user-service/*.jar \
            >> /var/log/${APP_NAME}/user.log 2>&1 &
    "
    
    sleep 5
    
    # 4. Gateway
    log_step "4/5 启动 Gateway (${GATEWAY_PORT})..."
    su - ${APP_USER} -s /bin/bash -c "
        nohup java -Xms256m -Xmx512m \
            -Deureka.client.service-url.defaultZone=http://${EUREKA_USER:-admin}:${EUREKA_PASSWORD:-admin}@${EUREKA_HOST:-localhost}:${EUREKA_PORT}/eureka/ \
            -DJWT_SECRET=${JWT_SECRET:-default-secret} \
            -Dserver.port=${GATEWAY_PORT} \
            ${APP_DIR}/app/gateway/*.jar \
            >> /var/log/${APP_NAME}/gateway.log 2>&1 &
    "
    
    sleep 5
    
    # 5. Ops Service
    if [ "$ENABLE_OPS" = "true" ]; then
        log_step "5/5 启动 Ops Service (${OPS_PORT})..."
        su - ${APP_USER} -s /bin/bash -c "
            nohup java -Xms256m -Xmx512m \
                -Deureka.client.service-url.defaultZone=http://${EUREKA_USER:-admin}:${EUREKA_PASSWORD:-admin}@${EUREKA_HOST:-localhost}:${EUREKA_PORT}/eureka/ \
                -Dserver.port=${OPS_PORT} \
                ${APP_DIR}/app/ops-service/*.jar \
                >> /var/log/${APP_NAME}/ops.log 2>&1 &
        "
    fi
    
    sleep 5
}

# 健康检查
health_check() {
    log_info "健康检查..."
    
    local services=(
        "Eureka:${EUREKA_PORT:-8761}"
        "Config:${CONFIG_PORT:-8082}"
        "User:${USER_PORT:-8081}"
        "Gateway:${GATEWAY_PORT:-8080}"
    )
    
    if [ "$ENABLE_OPS" = "true" ]; then
        services+=("Ops:${OPS_PORT:-8090}")
    fi
    
    local failed=0
    
    for service in "${services[@]}"; do
        local name="${service%%:*}"
        local port="${service##*:}"
        
        if nc -z localhost ${port} 2>/dev/null; then
            log_info "✓ ${name} (${port}) - OK"
        else
            log_error "✗ ${name} (${port}) - FAILED"
            failed=1
        fi
    done
    
    return $failed
}

# 查看状态
status() {
    echo ""
    echo "========================================"
    echo "  ${APP_NAME} 服务状态"
    echo "========================================"
    echo ""
    
    local services=(
        "Eureka Server:${EUREKA_PORT:-8761}"
        "Config Service:${CONFIG_PORT:-8082}"
        "User Service:${USER_PORT:-8081}"
        "Gateway:${GATEWAY_PORT:-8080}"
    )
    
    if [ "$ENABLE_OPS" = "true" ]; then
        services+=("Ops Service:${OPS_PORT:-8090}")
    fi
    
    for service in "${services[@]}"; do
        local name="${service%%:*}"
        local port="${service##*:}"
        
        if nc -z localhost ${port} 2>/dev/null; then
            echo -e "  ${GREEN}●${NC} ${name} (${port}) - 运行中"
        else
            echo -e "  ${RED}●${NC} ${name} (${port}) - 已停止"
        fi
    done
    
    echo ""
}

# 查看日志
logs() {
    local service="${1:-all}"
    
    if [ "$service" = "all" ]; then
        tail -f /var/log/${APP_NAME}/*.log
    else
        tail -f /var/log/${APP_NAME}/${service}.log
    fi
}

# 初始化
init() {
    check_user
    create_app_user
    set_permissions
    
    # 复制配置模板
    if [ ! -f "${APP_DIR}/config/env.conf" ]; then
        if [ -f "${APP_DIR}/scripts/env.conf.example" ]; then
            cp ${APP_DIR}/scripts/env.conf.example ${APP_DIR}/config/env.conf
            log_info "配置文件已创建: ${APP_DIR}/config/env.conf"
            log_warn "请编辑配置文件后继续"
        fi
    fi
    
    log_info "初始化完成!"
    show_permissions
}

# 帮助
help() {
    echo "用法: $0 {start|stop|restart|status|logs|build|init|perm|help}"
    echo ""
    echo "命令:"
    echo "  init     初始化环境 (创建用户、设置权限)"
    echo "  start    启动所有服务"
    echo "  stop     停止所有服务"
    echo "  restart  重启所有服务"
    echo "  status   查看服务状态"
    echo "  logs     查看日志 (Usage: $0 logs [service])"
    echo "  build    编译项目"
    echo "  perm     设置文件权限"
    echo "  help     显示帮助"
    echo ""
    echo "示例:"
    echo "  sudo $0 init"
    echo "  sudo $0 start"
    echo "  sudo $0 status"
    echo "  sudo $0 logs gateway"
}

# 主流程
main() {
    case "${1:-help}" in
        init)
            init
            ;;
        perm)
            check_user
            set_permissions
            ;;
        start)
            check_user
            check_java
            start_services
            health_check
            log_info "启动完成!"
            ;;
        stop)
            check_user
            stop_services
            log_info "已停止"
            ;;
        restart)
            check_user
            stop_services
            sleep 3
            start_services
            health_check
            log_info "重启完成!"
            ;;
        status)
            status
            ;;
        logs)
            logs "${2:-all}"
            ;;
        build)
            log_info "编译项目..."
            mvn clean package -DskipTests
            log_info "编译完成!"
            ;;
        help|*)
            help
            ;;
    esac
}

main "$@"
