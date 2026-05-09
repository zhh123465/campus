#!/usr/bin/env bash
set -euo pipefail

echo "========================================="
echo "  CampusForum 一键部署脚本"
echo "========================================="

# 检查 Docker
if ! command -v docker &>/dev/null; then
    echo "[ERROR] 请先安装 Docker 20.10+"
    exit 1
fi

if ! command -v docker compose &>/dev/null; then
    echo "[ERROR] 请先安装 Docker Compose v2+"
    exit 1
fi

# 检查 .env
if [ ! -f .env ]; then
    echo "[INFO] 未找到 .env 文件，正在从 .env.example 复制..."
    cp .env.example .env
    echo "[WARN] 请编辑 .env 文件，修改默认密码和密钥后重新运行本脚本"
    exit 0
fi

# 加载环境变量
set -a
source .env
set +a

echo "[INFO] 拉取 Docker 镜像..."
docker compose pull

echo "[INFO] 构建应用镜像..."
docker compose build app

echo "[INFO] 启动服务..."
docker compose up -d

echo "[INFO] 等待服务就绪..."
sleep 10

# 健康检查
if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "[OK] 应用启动成功!"
else
    echo "[WARN] 应用可能还在启动中，请稍后检查日志"
fi

echo ""
echo "========================================="
echo "  部署完成！"
echo "  访问地址: http://${APP_DOMAIN:-localhost}"
echo "  MinIO 控制台: http://localhost:9001"
echo "========================================="
