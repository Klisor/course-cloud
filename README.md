# 课程选课系统 - 微服务架构 

## 一、项目概述

### 1.1 项目简介

本项目是一个基于 Spring Boot 微服务架构的课程选课系统，将功能拆分为三个独立的微服务：**课程目录服务（catalog-service）**、**用户服务（user-service）** 和 **选课服务（enrollment-service）**。系统实现了完整的课程管理、用户管理、选课/退课业务流程，通过服务间调用实现数据一致性。

**版本信息**

- 项目名称：course-microservices
- 版本号：v1.0.0
- 项目阶段：微服务架构（三服务拆分）
- 基于版本：course:v1.1.0（hw04b）进行微服务改造

### 1.2 微服务架构说明

| 服务名称           | 端口 | 职责                             | 数据库        |
| ------------------ | ---- | -------------------------------- | ------------- |
| catalog-service    | 8081 | 课程管理（创建、查询、更新容量） | catalog_db    |
| user-service       | 8083 | 用户管理（学生、教师账户）       | user_db       |
| enrollment-service | 8082 | 选课管理（选课、退课、统计）     | enrollment_db |

### 1.3 核心业务流程

1. **课程创建流程**：管理员通过 catalog-service 创建课程
2. **用户注册流程**：用户通过 user-service 注册账户
3. **选课流程**：学生通过 enrollment-service 选课，该服务会：
   - 调用 user-service 验证用户存在
   - 调用 catalog-service 验证课程存在和容量
   - 创建选课记录
   - 异步更新课程已选人数

## 二、系统架构

### 2.1 架构图

```
客户端
  ↓
  ├─→ catalog-service (8081) → catalog-db (3307) [catalog_db]
  │   └── 课程管理
  │
  ├─→ user-service (8083) → user-db (3308) [user_db]
  │   └── 用户管理
  │
  └─→ enrollment-service (8082) → enrollment-db (3309) [enrollment_db]
      ├── 选课管理
      ├── HTTP调用 → user-service（验证用户）
      └── HTTP调用 → catalog-service（验证课程）
```

### 2.2 服务间调用关系

```
enrollment-service (8082)
       │
       ├──→ user-service (8083)
       │      ├── GET /api/users/{userId}     验证用户存在
       │      └── GET /api/users/by-userid/{userId} 查询用户信息
       │
       └──→ catalog-service (8081)
              ├── GET /api/courses/{courseId}   获取课程信息和容量
              └── PUT /api/courses/{courseId}/enrolled 更新已选人数
```

## 三、技术栈

| 技术类别   | 具体技术                | 版本/说明        |
| ---------- | ----------------------- | ---------------- |
| 后端框架   | Spring Boot             | 3.2.3            |
| 开发语言   | Java                    | 17+              |
| 构建工具   | Maven                   | 3.8+             |
| 数据库     | MySQL                   | 8.4              |
| 容器化     | Docker & Docker Compose | 20.10+ & 2.0+    |
| 服务通信   | RestTemplate            | Spring Boot 内置 |
| 数据持久化 | Spring Data JPA         | 3.2.3            |

## 四、环境要求

### 4.1 开发环境

- **JDK**: 17+
- **Maven**: 3.8+
- **Docker**: 20.10+
- **Docker Compose**: 2.0+
- **Git**: 任意版本（用于版本控制）

### 4.2 运行环境

- **内存**: 至少 4GB RAM
- **磁盘空间**: 至少 2GB 可用空间
- **操作系统**: Windows 10+/Linux/macOS
- **网络**: 需要访问 Docker Hub 下载镜像

## 五、构建和运行步骤

### 5.1 项目结构

```
course-microservices/
├── docker-compose.yml          # Docker编排文件
├── catalog-service/            # 课程服务
│   ├── src/main/java/com/zjsu/nsq/catalog/
│   ├── src/main/resources/application.yml
│   └── Dockerfile
├── user-service/               # 用户服务
│   ├── src/main/java/com/zjsu/nsq/user/
│   ├── src/main/resources/application.yml
│   └── Dockerfile
├── enrollment-service/         # 选课服务
│   ├── src/main/java/com/zjsu/nsq/enrollment/
│   ├── src/main/resources/application.yml
│   └── Dockerfile
└── README.md
```

### 5.2 本地构建（开发模式）

```bash
# 1. 克隆项目（如果从Git获取）
git clone <repository-url>
cd course-microservices

# 2. 分别构建每个服务
cd catalog-service && mvn clean package
cd ../user-service && mvn clean package
cd ../enrollment-service && mvn clean package

# 3. 启动数据库
docker run -d --name mysql \
  -e MYSQL_ROOT_PASSWORD=123456 \
  -e MYSQL_DATABASE=course_system \
  -p 3306:3306 \
  mysql:8

# 4. 分别启动服务（需要按顺序）
# 启动用户服务
java -jar user-service/target/*.jar

# 启动课程服务（新终端）
java -jar catalog-service/target/*.jar

# 启动选课服务（新终端）
java -jar enrollment-service/target/*.jar
```

### 5.3 Docker 容器化运行

```bash
# 1. 确保在项目根目录
cd course-microservices

# 2. 使用 Docker Compose 一键启动所有服务
docker-compose up -d

# 3. 查看服务状态
docker-compose ps

# 4. 查看日志（可选）
docker-compose logs -f

# 5. 停止服务
docker-compose down
```

### 5.4 数据库初始化

服务启动后会自动创建表和初始数据。如果需要手动重置数据库：

```bash
# 清理现有数据
docker exec mysql mysql -uroot -p123456 -e "
USE course_system;
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE enrollments;
TRUNCATE TABLE courses;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;"
```

## 六、API 文档

### 6.1 课程服务 (catalog-service:8081)

| 方法       | 端点                                              | 描述                         | 请求体示例                                                |      |
| ---------- | ------------------------------------------------- | ---------------------------- | --------------------------------------------------------- | ---- |
| **GET**    | `/api/courses`                                    | 获取所有课程                 | -                                                         |      |
| **GET**    | `/api/courses/{id}`                               | 获取单个课程（按ID）         | -                                                         |      |
| **GET**    | `/api/courses/code/{code}`                        | 获取单个课程（按课程代码）   | -                                                         |      |
| **GET**    | `/api/courses/search/title/{title}`               | 按标题搜索课程（模糊匹配）   | -                                                         |      |
| **GET**    | `/api/courses/search/instructor/{instructorName}` | 按教师姓名搜索课程           | -                                                         |      |
| **GET**    | `/api/courses/available`                          | 获取可用课程（有容量的课程） | -                                                         |      |
| **GET**    | `/api/courses/{id}/capacity`                      | 检查课程容量                 | -                                                         |      |
| **POST**   | `/api/courses`                                    | 创建课程                     | `{"code":"CS101","title":"计算机科学导论","capacity":60}` |      |
| **PUT**    | `/api/courses/{id}`                               | 更新课程信息                 | `{"title":"新课程名称","capacity":80}`                    |      |
| **PUT**    | `/api/courses/{id}/enrolled?count={n}`            | 更新课程已选人数             | -                                                         |      |
| **POST**   | `/api/courses/{id}/drop`                          | 减少课程已选人数（退课）     | -                                                         |      |
| **DELETE** | `/api/courses/{id}`                               | 删除课程                     | -                                                         |      |

**完整创建课程请求示例：**
```json
{
  "code": "CS101",
  "title": "计算机科学导论",
  "instructor": {
    "id": "T001",
    "name": "张教授",
    "email": "zhang@example.edu.cn"
  },
  "schedule": {
    "dayOfWeek": "MONDAY",
    "startTime": "08:00",
    "endTime": "10:00",
    "expectedAttendance": 50
  },
  "capacity": 60,
  "enrolled": 0
}
```

### 6.2 用户服务 (user-service:8083)

| 方法       | 端点                            | 描述                       | 请求体示例                                           |      |
| ---------- | ------------------------------- | -------------------------- | ---------------------------------------------------- | ---- |
| **GET**    | `/api/users`                    | 获取所有用户               | -                                                    |      |
| **GET**    | `/api/users/{identifier}`       | 获取用户（支持ID或userId） | -                                                    |      |
| **GET**    | `/api/users/by-userid/{userId}` | 按用户ID获取用户           | -                                                    |      |
| **POST**   | `/api/users`                    | 创建用户                   | `{"userId":"stu001","name":"张三","role":"STUDENT"}` |      |
| **PUT**    | `/api/users/{id}`               | 更新用户信息               | `{"name":"李四","email":"new@example.com"}`          |      |
| **DELETE** | `/api/users/{id}`               | 删除用户                   | -                                                    |      |
| **GET**    | `/api/users/health`             | 健康检查                   | -                                                    |      |

**完整创建用户请求示例：**
```json
{
  "userId": "stu001",
  "name": "张三",
  "role": "STUDENT",
  "major": "计算机科学与技术",
  "grade": 2024,
  "email": "zhangsan@example.edu.cn"
}
```

**创建教师用户示例：**
```json
{
  "userId": "T001",
  "name": "张教授",
  "role": "TEACHER",
  "email": "zhang@example.edu.cn"
}
```

### 6.3 选课服务 (enrollment-service:8082)

| 方法       | 端点                                              | 描述                 | 请求体/参数                          |      |
| ---------- | ------------------------------------------------- | -------------------- | ------------------------------------ | ---- |
| **GET**    | `/api/enrollments`                                | 获取所有选课记录     | -                                    |      |
| **GET**    | `/api/enrollments/{id}`                           | 获取单个选课记录     | -                                    |      |
| **GET**    | `/api/enrollments/course/{courseId}`              | 按课程查询选课记录   | -                                    |      |
| **GET**    | `/api/enrollments/user/{userId}`                  | 按用户查询选课记录   | -                                    |      |
| **GET**    | `/api/enrollments/status/{status}`                | 按状态查询选课记录   | -                                    |      |
| **GET**    | `/api/enrollments/stats/course/{courseId}`        | 获取课程统计信息     | -                                    |      |
| **GET**    | `/api/enrollments/count/active/user/{userId}`     | 获取用户活跃选课数量 | -                                    |      |
| **GET**    | `/api/enrollments/count/active/course/{courseId}` | 获取课程活跃选课数量 | -                                    |      |
| **POST**   | `/api/enrollments`                                | 学生选课             | `{"courseId":"1","userId":"stu001"}` |      |
| **PUT**    | `/api/enrollments/{id}/complete`                  | 标记课程完成         | -                                    |      |
| **POST**   | `/api/enrollments/{id}/drop`                      | 按ID退课             | -                                    |      |
| **DELETE** | `/api/enrollments/drop?userId=&courseId=`         | 按用户和课程退课     | query参数: userId, courseId          |      |
| **DELETE** | `/api/enrollments/{id}`                           | 删除选课记录（退课） | -                                    |      |
| **DELETE** | `/api/enrollments/cancel/{id}`                    | 取消选课（删除记录） | -                                    |      |
| **GET**    | `/api/enrollments/health`                         | 健康检查             | -                                    |      |

**状态参数说明：**
- `status` 参数可选值：`ACTIVE`, `DROPPED`, `COMPLETED`, `CANCELLED`

**服务间调用依赖：**

- 选课时验证用户：调用 `user-service:8083/api/users/by-userid/{userId}`
- 选课时验证课程：调用 `catalog-service:8081/api/courses/{courseId}`
- 更新课程人数：调用 `catalog-service:8081/api/courses/{courseId}/enrolled?count={n}`

### 6.4 API 响应格式

所有接口返回统一的JSON响应格式：

```json
{
  "code": 200,            // 状态码
  "message": "Success",   // 消息
  "data": {              // 数据（成功时）
    // 具体数据结构
  }
}
```

**错误响应示例：**

```json
{
  "code": 404,
  "message": "用户不存在: stu999",
  "data": null
}
```

### 6.5 状态码说明

| 状态码 | 含义           | 常见场景                                 |
| ------ | -------------- | ---------------------------------------- |
| 200    | 请求成功       | 查询、更新操作成功                       |
| 201    | 创建成功       | 课程、用户、选课记录创建成功             |
| 400    | 请求参数错误   | 参数缺失、格式错误、重复操作、课程已满等 |
| 404    | 资源未找到     | 用户不存在、课程不存在、选课记录不存在   |
| 409    | 资源冲突       | 课程代码已存在                           |
| 500    | 服务器内部错误 | 数据库错误、服务间调用失败等             |



## 七、功能测试

### 7.1 测试流程

按照以下顺序测试确保系统正常工作：

1. **启动所有服务**：`docker-compose up -d`
2. **创建课程**：通过 catalog-service (8081) 创建课程
3. **创建用户**：通过 user-service (8083) 创建学生账户
4. **学生选课**：通过 enrollment-service (8082) 进行选课
5. **验证数据同步**：检查课程已选人数是否更新

### 7.2 完整测试用例

```bash
# 1. 创建课程
curl -X POST http://localhost:8081/api/courses \
  -H "Content-Type: application/json" \
  -d '{"code":"CS101","title":"计算机科学导论","capacity":60}'

# 2. 创建学生
curl -X POST http://localhost:8083/api/users \
  -H "Content-Type: application/json" \
  -d '{"userId":"stu001","name":"张三","role":"STUDENT"}'

# 3. 学生选课（假设课程ID为1）
curl -X POST http://localhost:8082/api/enrollments \
  -H "Content-Type: application/json" \
  -d '{"courseId":"1","userId":"stu001"}'

# 4. 查看选课记录
curl http://localhost:8082/api/enrollments

# 5. 查看课程已选人数
curl http://localhost:8081/api/courses/1

# 6. 测试异常：重复选课
curl -X POST http://localhost:8082/api/enrollments \
  -H "Content-Type: application/json" \
  -d '{"courseId":"1","userId":"stu001"}'

# 7. 测试异常：课程不存在
curl -X POST http://localhost:8082/api/enrollments \
  -H "Content-Type: application/json" \
  -d '{"courseId":"999","userId":"stu001"}'

# 8. 测试退课
curl -X POST http://localhost:8082/api/enrollments/1/drop

# 9. 查看课程统计
curl http://localhost:8082/api/enrollments/stats/course/1
```

## 八、常见问题与解决方案

### 问题1：服务启动失败，端口被占用

**解决方案**：
```bash
# 查找占用端口的进程
netstat -ano | findstr :8081

# 或使用 PowerShell
Get-Process -Id (Get-NetTCPConnection -LocalPort 8081).OwningProcess

# 停止占用进程或修改服务端口
```

### 问题2：数据库连接失败

**解决方案**：
1. 检查 MySQL 容器是否运行：`docker ps | grep mysql`
2. 检查数据库连接配置是否正确
3. 重启数据库：`docker restart mysql`

### 问题3：服务间调用失败（Connection refused）

**解决方案**：
```yaml
# 在 enrollment-service 的配置中确保使用正确的服务名
USER_SERVICE_URL: http://user-service:8083
CATALOG_SERVICE_URL: http://catalog-service:8081
```

### 问题4：中文乱码

**解决方案**：
1. 数据库字符集设置为 UTF-8
2. 在 PowerShell 中设置编码：
```powershell
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$ProgressPreference = 'SilentlyContinue'
```

## 九、部署说明

### 9.1 生产环境部署建议

1. **数据库**：使用独立的 MySQL 实例，配置主从复制
2. **服务发现**：考虑集成 Consul 或 Eureka
3. **配置管理**：使用 Spring Cloud Config
4. **监控**：集成 Prometheus + Grafana
5. **日志**：使用 ELK 堆栈收集日志

### 9.2 性能优化

1. **数据库连接池**：配置合适的连接池大小
2. **缓存**：对频繁查询的课程和用户信息添加缓存
3. **异步处理**：将非关键操作（如发送通知）异步化
4. **服务降级**：在用户服务不可用时提供降级方案

