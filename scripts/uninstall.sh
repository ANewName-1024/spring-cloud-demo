#!/bin/bash
# uninstall.sh - 完整卸载脚本

set -e

APP_NAME="springcloud-demo"
APP_DIR="/opt/${APP_NAME}"
APP_USER="springcloud"
APP_GROUP="springcloud"

# 颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

echo "========================================"
echo "  警告: 此操作将完全卸载 ${APP_NAME}"
echo "========================================"
echo ""
echo "将执行以下操作:"
echo "  1. 停止所有服务"
echo "  2. 删除应用用户和组"
echo "  3. 删除应用目录"
echo "  4. 删除日志文件"
echo "  5. 删除备份"
echo "  6. 清理定时任务"
echo ""
echo "以下内容需要手动清理:"
echo "  1. 数据库"
echo "  2. 防火墙规则"
echo "  3. Nginx 配置"
echo ""
read -p "确认继续? (yes/no): " confirm

if [ "$confirm" != "yes" ]; then
    echo "取消卸载"
    exit 0
fi

# 1. 停止所有服务
log_info "停止所有服务..."
for service in eureka-server gateway user-service config-service ops-service; do
    pkill -9 -f "${service}" 2>/dev/null || true
done

# 2. 删除定时任务
log_info "清理定时任务..."
crontab -r 2>/dev/null || true

# 3. 删除应用用户和组
log_info "删除应用用户和组..."
# 删除用户的所有进程
pkill -u ${APP_USER} 2>/dev/null || true
# 删除用户
userdel -r ${APP_USER} 2>/dev/null || true
# 删除用户组
groupdel ${APP_GROUP} 2>/dev/null || true

# 4. 删除应用目录
log_info "删除应用目录..."
rm -rf ${APP_DIR}

# 5. 删除日志
log_info "删除日志..."
rm -rf /var/log/${APP_NAME}

# 6. 删除 systemd 服务文件 (如果存在)
log_info "清理 systemd 服务..."
rm -f /etc/systemd/system/${APP_NAME}.service
systemctl daemon-reload 2>/dev/null || true

echo ""
echo "========================================"
echo "  卸载完成!"
echo "========================================"
echo ""
echo "手动清理项:"
echo "  1. 数据库:"
echo "     DROP DATABASE springcloud;"
echo "     DROP USER business;"
echo ""
echo "  2. 防火墙规则:"
echo "     ufw delete allow 8761"
echo "     ufw delete allow 8080"
echo "     ufw delete allow 8081"
echo "     ufw delete allow 8082"
echo "     ufw delete allow 8090"
echo ""
echo "  3. Nginx 配置:"
echo "     rm /etc/nginx/sites-enabled/${APP_NAME}"
echo "     nginx -t && nginx -s reload"
echo ""
