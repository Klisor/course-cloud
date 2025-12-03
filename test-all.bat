@echo off
chcp 65001 >nul
echo ================================================
echo  CourseHub微服务系统 - 完整测试脚本
echo  (包含Nacos服务注册验证)
echo ================================================

echo.
echo [步骤1/8] 启动所有服务
echo -------------------------------------------------
echo 启动Nacos和所有微服务...
docker-compose up -d
echo 等待60秒让服务启动注册...
timeout /t 60 /nobreak > nul

echo.
echo [步骤2/8] 验证Nacos服务注册
echo -------------------------------------------------
echo Nacos控制台：http://localhost:8848/nacos (nacos/nacos)
echo.
echo 使用API检查服务注册：
echo 1. 获取服务列表：
curl -s "http://localhost:8848/nacos/v1/ns/service/list?pageNo=1&pageSize=10"
echo.
echo 2. 检查各服务实例：
for %%s in (catalog-service user-service enrollment-service) do (
    echo 服务 %%s 实例：
    curl -s "http://localhost:8848/nacos/v1/ns/instance/list?serviceName=%%s"
    echo.
)

echo 请现在打开Nacos控制台截图服务列表！
echo 按任意键继续（截图完成后）...
pause > nul

echo.
echo [步骤3/8] 验证各服务API端点
echo -------------------------------------------------
echo 1. Catalog Service (8081):
curl -s "http://localhost:8081/api/courses/port"
echo.
echo 2. User Service实例 (8083):
curl -s "http://localhost:8083/api/users/port"
echo.
echo 3. User Service实例 (8084):
curl -s "http://localhost:8084/api/users/port"
echo.
echo 4. User Service实例 (8085):
curl -s "http://localhost:8085/api/users/port"
echo.
echo 5. Enrollment Service (8082):
curl -s "http://localhost:8082/api/enrollments/port"

echo.
echo [步骤4/8] 负载均衡效果测试
echo -------------------------------------------------
echo 通过Enrollment服务调用测试负载均衡（10次）：
echo 注意观察响应中的不同端口号（8083、8084、8085）
echo.
for /l %%i in (1,1,10) do (
    echo 第 %%i 次调用：
    curl -s "http://localhost:8082/api/enrollments/discovery"
    echo.
    timeout /t 1 /nobreak > nul
)

echo 请截图显示负载均衡效果！
echo 按任意键继续...
pause > nul

echo.
echo [步骤5/8] 故障转移测试
echo -------------------------------------------------
echo 1. 停止一个User Service实例 (8084)：
docker-compose stop user-service-2
echo.
echo 2. 等待20秒让Nacos检测：
timeout /t 20 /nobreak > nul
echo.
echo 3. 验证8084已停止：
curl -s "http://localhost:8084/api/users/port" && echo "8084仍在运行" || echo "8084已停止"
echo.
echo 4. 测试故障转移（5次调用）：
for /l %%i in (1,1,5) do (
    echo 故障后第 %%i 次调用：
    curl -s "http://localhost:8082/api/enrollments/discovery"
    echo.
    timeout /t 1 /nobreak > nul
)

echo 请截图显示故障转移效果！
echo 按任意键继续...
pause > nul

echo.
echo [步骤6/8] 恢复测试
echo -------------------------------------------------
echo 1. 恢复停止的实例：
docker-compose start user-service-2
echo.
echo 2. 等待30秒重新注册：
timeout /t 30 /nobreak > nul
echo.
echo 3. 验证恢复：
curl -s "http://localhost:8084/api/users/port" && echo "8084已恢复" || echo "8084恢复失败"

echo.
echo [步骤7/8] 最终状态检查
echo -------------------------------------------------
echo 1. 容器状态：
docker-compose ps
echo.
echo 2. Nacos最终服务列表：
curl -s "http://localhost:8848/nacos/v1/ns/service/list?pageNo=1&pageSize=10"
echo.
echo 3. 各服务最终健康状态：
for %%p in (8081 8082 8083 8084 8085) do (
    echo 端口%%p:
    curl -s "http://localhost:%%p/actuator/health" 2>nul | findstr "UP" >nul && echo   健康 || echo   异常
)

echo.
echo [步骤8/8] 测试完成总结
echo -------------------------------------------------
echo ✓ Nacos控制台：http://localhost:8848/nacos
echo   用户名：nacos，密码：nacos
echo.
echo ✓ 需要截图的内容：
echo   1. Nacos服务列表
echo   2. 负载均衡效果（不同端口响应）
echo   3. 故障转移效果（停止实例后仍能工作）
echo.
echo ✓ 微服务访问地址：
echo   Catalog:    http://localhost:8081/api/courses/port
echo   Enrollment: http://localhost:8082/api/enrollments/port
echo   User1:      http://localhost:8083/api/users/port
echo   User2:      http://localhost:8084/api/users/port
echo   User3:      http://localhost:8085/api/users/port
echo.
echo 按任意键退出...
pause > nul