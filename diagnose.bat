@echo off
chcp 65001 >nul
echo ================================================
echo  问题诊断
echo ================================================

echo.
echo 1. 检查所有服务状态：
docker-compose ps

echo.
echo 2. 查看Enrollment Service日志（最后20行）：
docker-compose logs --tail=20 enrollment-service

echo.
echo 3. 查看User Service实例日志：
echo.
echo "User Service实例1 (8083):"
docker-compose logs --tail=10 user-service

echo.
echo "User Service实例2 (8084):"
docker-compose logs --tail=10 user-service-2

echo.
echo "User Service实例3 (8085):"
docker-compose logs --tail=10 user-service-3

echo.
echo 4. 直接测试各服务端点：
echo.
echo "测试Enrollment Service (8082):"
curl -v http://localhost:8082/api/enrollments/discovery

echo.
echo "测试User Service实例1 (8083):"
curl -v http://localhost:8083/api/users/port

echo.
echo "测试Catalog Service (8081):"
curl -v http://localhost:8081/api/catalogs/port

echo.
echo 5. 检查Nacos服务注册：
echo.
echo "查看所有服务列表："
curl -s "http://localhost:8848/nacos/v1/ns/service/list?pageNo=1&pageSize=10"

echo.
echo "查看User Service实例："
curl -s "http://localhost:8848/nacos/v1/ns/instance/list?serviceName=user-service"

echo.
echo "查看Enrollment Service实例："
curl -s "http://localhost:8848/nacos/v1/ns/instance/list?serviceName=enrollment-service"

echo.
echo 6. 检查网络连通性：
echo.
echo "从Enrollment容器内部测试连接："
docker exec enrollment-service curl -s http://user-service:8083/api/users/health || echo "无法连接user-service"

echo.
pause