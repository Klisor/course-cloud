#!/bin/bash
set -e

echo "=== 构建校园选课系统 Docker 镜像 ==="

# 构建应用镜像
echo "1. 构建应用镜像..."
docker build -t course-system:latest .

echo "2. 镜像构建完成，查看镜像信息:"
docker images course-system:latest

echo ""
echo "3. 使用以下命令启动:"
echo "   docker-compose up -d"
echo ""
echo "4. 或者手动运行:"
echo "   docker run -p 8080:8080 course-system:latest"