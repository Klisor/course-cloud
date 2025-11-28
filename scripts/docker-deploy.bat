@echo off
chcp 65001 >nul
echo ========================================
echo   校园选课系统 Docker 部署脚本
echo ========================================
echo.

echo [INFO] 1. 构建Docker镜像...
docker build -t course-system:latest .

if %errorlevel% neq 0 (
    echo [ERROR] 镜像构建失败!
    pause
    exit /b 1
)

echo [SUCCESS] 镜像构建完成!

echo.
echo [INFO] 2. 查看镜像信息:
docker images course-system:latest

echo.
echo [INFO] 3. 启动所有服务...
docker-compose up -d

echo.
echo [INFO] 4. 等待服务启动...
timeout /t 30 /nobreak > nul

echo [INFO] 5. 检查服务状态:
docker-compose ps

echo.
echo [INFO] 6. 健康检查...
curl -s http://localhost:8080/health/db > nul
if %errorlevel% equ 0 (
    echo [SUCCESS] 应用健康检查通过!
    echo.
    echo [INFO] 应用信息:
    curl -s http://localhost:8080/health/db
) else (
    echo [WARNING] 应用尚未就绪，请稍后重试
)

echo.
echo ========================================
echo   部署完成
echo ========================================
echo.
echo 常用命令:
echo   docker-compose logs -f course-app    # 查看应用日志
echo   docker-compose logs -f mysql         # 查看数据库日志
echo   docker-compose restart course-app    # 重启应用
echo   docker-compose down                  # 停止所有服务
echo.
pause