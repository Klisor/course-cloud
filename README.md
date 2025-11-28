# 课程选课系统 - 微服务项目 README

## 一、项目概述
### 1.1 项目简介
本项目是一个基于 Spring Boot 微服务架构的课程选课系统，将功能拆分为「课程目录服务（catalog-service）」和「选课服务（enrollment-service）」，实现课程管理、学生管理、选课/退课、数据同步等核心功能。系统支持 Docker 容器化部署，具备高可扩展性、环境一致性和便捷的运维特性。

**版本信息**

• 项目名称：course-cloud

• 版本号：v1.0.0

• Git 分支：main

• 项目阶段：微服务架构（初次拆分）

• 基于版本：course:v1.1.0（hw04b）

### 1.2 核心功能
| 服务名称                | 核心功能                                                                 |
|-------------------------|--------------------------------------------------------------------------|
| 课程目录服务（catalog-service） | 课程创建、课程查询（按ID/代码）、选课人数同步、课程状态管理               |
| 选课服务（enrollment-service）  | 学生创建、选课/退课操作、选课记录查询、选课统计（按课程/学生）、异常场景处理 |

### 1.3 项目价值
- 微服务拆分：服务职责单一，降低耦合，便于独立开发和维护；
- 容器化部署：环境一致，一键启动，适配开发/测试/部署全流程；
- 数据一致性：选课/退课操作同步更新课程人数，避免数据不一致；
- 异常兼容：支持重复选课、课程不存在、课程满员等异常场景友好提示。

## 二、技术架构
### 2.1 技术栈
| 类别         | 技术选型                                                                 |
|--------------|--------------------------------------------------------------------------|
| 开发框架     | Spring Boot 3.x、Spring Data JPA（数据访问）、Spring Web（RESTful API）   |
| 数据库       | MySQL 8.0（关系型数据库，存储课程、学生、选课记录）                       |
| 容器化       | Docker（容器化）、Docker Compose（服务编排）                              |
| 开发环境     | JDK 17、Maven 3.8+、IntelliJ IDEA（开发工具）                            |
| 服务通信     | RESTful API（同步 HTTP 通信）                                             |
| 其他依赖     | RestTemplate（服务间调用）、Lombok（简化实体类）                          |

### 2.2 架构图
```
┌─────────────────┐      ┌─────────────────┐
│  客户端（HTTP）  │      │  外部工具       │
└────────┬────────┘      └────────┬────────┘
         │                        │
         ▼                        ▼
┌─────────────────┐      ┌─────────────────┐
│ enrollment-service │      │  Docker Compose │
│ （选课/学生管理） │◄────►│ （服务编排）    │
└────────┬────────┘      └────────┬────────┘
         │                        │
         ▼                        ▼
┌─────────────────┐      ┌─────────────────┐
│ catalog-service  │      │  MySQL 8.0      │
│ （课程管理）     │◄────►│ （数据存储）    │
└────────┬────────┘      └─────────────────┘
         │
         ▼
┌─────────────────┐
│  服务间通信     │
│ （RESTful API） │
└─────────────────┘
```

### 2.3 数据模型
#### 核心实体关系
- 课程（Course）：存储课程基本信息（代码、名称、容量、已选人数等）；
- 学生（Student）：存储学生基本信息（学号、姓名、专业、年级等）；
- 选课记录（Enrollment）：关联课程ID和学生ID，存储选课状态（ACTIVE/DROPPED/COMPLETED）。

## 三、环境准备
### 3.1 前置依赖安装
| 依赖工具       | 版本要求 | 安装说明                                                                 |
|----------------|----------|--------------------------------------------------------------------------|
| JDK            | 17+      | 配置环境变量 `JAVA_HOME`，验证：`java -version` 显示 17+                  |
| Maven          | 3.8+     | 配置环境变量 `MAVEN_HOME`，验证：`mvn -version` 显示 3.8+                |
| Docker         | 20.10+   | 参考官方文档：https://docs.docker.com/get-docker/                        |
| Docker Compose | 2.10+    | 参考官方文档：https://docs.docker.com/compose/install/                   |
| Git            | 任意版本 | 可选（用于拉取代码），Windows 可安装 Git Bash（辅助脚本运行）             |

### 3.2 环境验证
安装完成后，执行以下命令验证环境可用性：
```bash
# 验证 JDK
java -version

# 验证 Maven
mvn -version

# 验证 Docker
docker --version

# 验证 Docker Compose
docker-compose --version
```

## 四、项目部署（Docker 容器化）
### 4.1 目录结构
项目部署需遵循以下目录结构（确保 Docker 能正确识别文件）：
```
course-cloud/
├── README.md
├── docker-compose.yml
├── test-services.sh
├── VERSION
│
├── catalog-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       └── main/
│           ├── java/com/zjgsu/initial/catalog/
│           │   ├── CatalogServiceApplication.java
│           │   ├── common/
│           │   │   └── ApiResponse.java
│           │   ├── controller/
│           │   │   └── CourseController.java
│           │   ├── exception/
│           │   │   ├── GlobalExceptionHandler.java
│           │   │   └── ResourceNotFoundException.java
│           │   ├── model/
│           │   │   ├── Course.java
│           │   │   ├── Instructor.java
│           │   │   └── ScheduleSlot.java
│           │   ├── repository/
│           │   │   └── CourseRepository.java
│           │   └── service/
│           │       └── CourseService.java
│           └── resources/
│               ├── application.yml
│               └── application-prod.yml
│
└── enrollment-service/
    ├── Dockerfile
    ├── pom.xml
    └── src/
        └── main/
            ├── java/com/zjgsu/initial/enrollment/
            │   ├── EnrollmentServiceApplication.java
            │   ├── common/
            │   ├── controller/
            │   │   ├── EnrollmentController.java
            │   │   └── StudentController.java
            │   ├── exception/
            │   ├── model/
            │   │   ├── Enrollment.java
            │   │   ├── EnrollmentStatus.java
            │   │   └── Student.java
            │   ├── repository/
            │   │   ├── EnrollmentRepository.java
            │   │   └── StudentRepository.java
            │   └── service/
            │       ├── EnrollmentService.java
            │       └── StudentService.java
            └── resources/
                ├── application.yml
                └── application-prod.yml
```

### 4.2 部署步骤
#### 步骤 1：打包服务 Jar 包
进入项目根目录，分别对两个服务进行 Maven 打包（跳过测试以加快速度）：
```bash
# 打包 catalog-service
cd catalog-service
mvn clean package -Dmaven.test.skip=true

# 打包 enrollment-service
cd ../enrollment-service
mvn clean package -Dmaven.test.skip=true
```
- 打包成功后，Jar 包会生成在 `各自服务的 target/` 目录下（如 `catalog-service/target/nsq-course-0.0.1-SNAPSHOT.jar`）。

#### 步骤 2：启动 Docker 容器
进入 `docker-deploy/` 目录（含 `docker-compose.yml` 文件），执行以下命令一键启动所有服务：
```bash
cd docker-deploy
docker-compose up -d
```
- 命令说明：`-d` 表示后台运行，避免终端被占用；
- 启动成功后，执行 `docker-compose ps` 查看服务状态，所有服务 `State` 为 `Up` 即正常。

#### 步骤 3：验证部署成功
通过 HTTP 接口验证服务是否可用：
```bash
# 验证 catalog-service（查询所有课程）
curl http://localhost:8081/api/courses

# 验证 enrollment-service（查询所有学生）
curl http://localhost:8082/api/students
```
- 预期响应：返回 `{"code":200,"message":"Success","data":[]}`，说明服务启动正常。

### 4.3 常用 Docker 命令（运维用）
| 命令                          | 功能描述                                                                 |
|-------------------------------|--------------------------------------------------------------------------|
| `docker-compose up -d`        | 启动所有服务（后台运行）                                                 |
| `docker-compose down`         | 停止所有服务（保留数据卷，数据不丢失）                                   |
| `docker-compose down -v`       | 停止所有服务并删除数据卷（数据丢失，谨慎使用）                           |
| `docker-compose restart`       | 重启所有服务                                                             |
| `docker-compose logs -f 服务名` | 查看指定服务日志（如 `docker-compose logs -f catalog-service`）           |
| `docker-compose ps`            | 查看所有服务运行状态                                                     |
| `docker exec -it 容器名 bash`  | 进入容器内部（如 `docker exec -it course-mysql bash` 进入 MySQL 容器）    |

## 五、功能测试（自动化脚本 + 手动验证）
### 5.1 测试概述
测试目标：验证核心流程（课程创建→学生创建→选课→人数同步）和异常场景处理的正确性，确保服务间通信正常。  
测试环境：Docker 容器部署完成后（所有服务 `Up` 状态）；  
测试工具：PowerShell（Windows）/ Bash（Linux/Mac）、curl/Postman（手动验证）。

### 5.2 自动化测试脚本（推荐）
在项目根目录创建 `test-all.ps1`（Windows）或 `test-all.sh`（Linux/Mac），复制以下脚本执行，一键完成全流程测试：

#### Windows（PowerShell）脚本：`test-all.ps1`
```powershell
# 解决中文乱码 + 初始化配置
$OutputEncoding = [console]::InputEncoding = [console]::OutputEncoding = New-Object System.Text.UTF8Encoding
Add-Type -AssemblyName System.Net.Http
$CATALOG_URL = "http://localhost:8081/api"
$ENROLLMENT_URL = "http://localhost:8082/api"
$httpClient = New-Object System.Net.Http.HttpClient

Write-Host "`n==================================================" -ForegroundColor Cyan
Write-Host "📝 课程选课系统全流程自动化测试" -ForegroundColor Cyan
Write-Host "==================================================`n" -ForegroundColor Cyan

# 1. 创建课程
Write-Host "🔧 1. 创建课程（catalog-service）" -ForegroundColor Green
$createCourseJson = @'
{
  "code": "CS101",
  "title": "Introduction to Computer Science",
  "instructor": {
    "name": "Prof. Zhang",
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
'@
$content = New-Object System.Net.Http.StringContent($createCourseJson, [System.Text.Encoding]::UTF8, "application/json")
$courseResponse = $httpClient.PostAsync("$CATALOG_URL/courses", $content).Result
$courseData = $courseResponse.Content.ReadAsStringAsync().Result | ConvertFrom-Json
if ($courseResponse.IsSuccessStatusCode) {
  $courseId = $courseData.data.id
  Write-Host "✅ 课程创建成功！CourseId: $courseId`n" -ForegroundColor Green
} else {
  Write-Host "❌ 课程创建失败：$($courseData.message)" -ForegroundColor Red
  exit 1
}

# 2. 创建学生
Write-Host "🔧 2. 创建学生（enrollment-service）" -ForegroundColor Green
$createStudentJson = @'
{
  "studentId": "2024001",
  "name": "Zhang San",
  "major": "Computer Science and Technology",
  "grade": 2024,
  "email": "zhangsan@example.edu.cn"
}
'@
$content = New-Object System.Net.Http.StringContent($createStudentJson, [System.Text.Encoding]::UTF8, "application/json")
$studentResponse = $httpClient.PostAsync("$ENROLLMENT_URL/students", $content).Result
$studentData = $studentResponse.Content.ReadAsStringAsync().Result | ConvertFrom-Json
if ($studentResponse.IsSuccessStatusCode) {
  $studentId = $studentData.data.studentId
  Write-Host "✅ 学生创建成功！StudentId: $studentId`n" -ForegroundColor Green
} else {
  Write-Host "❌ 学生创建失败：$($studentData.message)" -ForegroundColor Red
  exit 1
}

# 3. 核心测试：学生选课（服务间通信）
Write-Host "🔧 3. 学生选课（服务间通信核心测试）" -ForegroundColor Green
$enrollJson = @"
{
  "courseId": "$courseId",
  "studentId": "$studentId"
}
"@
$content = New-Object System.Net.Http.StringContent($enrollJson, [System.Text.Encoding]::UTF8, "application/json")
$enrollResponse = $httpClient.PostAsync("$ENROLLMENT_URL/enrollments", $content).Result
$enrollData = $enrollResponse.Content.ReadAsStringAsync().Result | ConvertFrom-Json
if ($enrollResponse.IsSuccessStatusCode) {
  Write-Host "✅ 选课成功！服务间通信正常`n" -ForegroundColor Green
} else {
  Write-Host "❌ 选课失败：$($enrollData.message)" -ForegroundColor Red
}

# 4. 验证课程已选人数更新
Write-Host "🔧 4. 验证课程已选人数（catalog-service）" -ForegroundColor Green
$courseDetailResponse = $httpClient.GetAsync("$CATALOG_URL/courses/$courseId").Result
$courseDetailData = $courseDetailResponse.Content.ReadAsStringAsync().Result | ConvertFrom-Json
$enrolledCount = $courseDetailData.data.enrolled
if ($enrolledCount -eq 1) {
  Write-Host "✅ 课程已选人数更新成功（enrolled: $enrolledCount）`n" -ForegroundColor Green
} else {
  Write-Host "❌ 课程已选人数未更新（当前: $enrolledCount）`n" -ForegroundColor Red
}

# 5. 测试异常：重复选课
Write-Host "🔧 5. 测试异常：重复选课" -ForegroundColor Green
$content = New-Object System.Net.Http.StringContent($enrollJson, [System.Text.Encoding]::UTF8, "application/json")
$repeatEnrollResponse = $httpClient.PostAsync("$ENROLLMENT_URL/enrollments", $content).Result
$repeatEnrollData = $repeatEnrollResponse.Content.ReadAsStringAsync().Result | ConvertFrom-Json
if (-not $repeatEnrollResponse.IsSuccessStatusCode) {
  Write-Host "✅ 重复选课异常处理成功！提示：$($repeatEnrollData.message)`n" -ForegroundColor Green
} else {
  Write-Host "❌ 重复选课异常处理失败" -ForegroundColor Red
}

# 6. 测试异常：选不存在的课程
Write-Host "🔧 6. 测试异常：选不存在的课程" -ForegroundColor Green
$invalidEnrollJson = @'
{
  "courseId": "9999",
  "studentId": "2024001"
}
'@
$content = New-Object System.Net.Http.StringContent($invalidEnrollJson, [System.Text.Encoding]::UTF8, "application/json")
$invalidEnrollResponse = $httpClient.PostAsync("$ENROLLMENT_URL/enrollments", $content).Result
$invalidEnrollData = $invalidEnrollResponse.Content.ReadAsStringAsync().Result | ConvertFrom-Json
if (-not $invalidEnrollResponse.IsSuccessStatusCode) {
  Write-Host "✅ 课程不存在异常处理成功！提示：$($invalidEnrollData.message)`n" -ForegroundColor Green
} else {
  Write-Host "❌ 课程不存在异常处理失败" -ForegroundColor Red
}

# 清理资源
$httpClient.Dispose()
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "🎉 全流程测试执行完毕！" -ForegroundColor Cyan
Write-Host "==================================================`n" -ForegroundColor Cyan
```

#### 执行脚本步骤
1. 保存脚本到项目根目录（`Course/test-all.ps1`）；
2. 打开 PowerShell，进入项目根目录：`cd D:\微程序\项目\Course`；
3. 执行脚本：`.\test-all.ps1`；
4. 预期结果：所有测试步骤显示 `✅`，无报错。

### 5.3 手动测试（可选，Postman/curl）
若需手动验证核心接口，可执行以下命令：
#### 1. 创建课程
```bash
curl -X POST http://localhost:8081/api/courses \
-H "Content-Type: application/json" \
-d '{
  "code": "CS101",
  "title": "Introduction to Computer Science",
  "instructor": {"name": "Prof. Zhang", "email": "zhang@example.edu.cn"},
  "schedule": {"dayOfWeek": "MONDAY", "startTime": "08:00", "endTime": "10:00", "expectedAttendance": 50},
  "capacity": 60,
  "enrolled": 0
}'
```
- 预期响应：`code=201`，返回课程ID。

#### 2. 创建学生
```bash
curl -X POST http://localhost:8082/api/students \
-H "Content-Type: application/json" \
-d '{
  "studentId": "2024001",
  "name": "Zhang San",
  "major": "Computer Science and Technology",
  "grade": 2024,
  "email": "zhangsan@example.edu.cn"
}'
```
- 预期响应：`code=201`，返回学生信息。

#### 3. 学生选课
```bash
curl -X POST http://localhost:8082/api/enrollments \
-H "Content-Type: application/json" \
-d '{
  "courseId": "1",  # 替换为实际课程ID
  "studentId": "2024001"
}'
```
- 预期响应：`code=201`，选课成功。

#### 4. 验证人数更新
```bash
curl http://localhost:8081/api/courses/1  # 替换为实际课程ID
```
- 预期响应：`enrolled` 字段为 `1`。

### 5.4 测试结果验证标准
| 测试场景                | 成功标准                                                                 |
|-------------------------|--------------------------------------------------------------------------|
| 服务启动验证            | `docker-compose ps` 所有服务状态为 `Up`，接口返回 `code=200`             |
| 课程/学生创建           | 返回 `code=201`，数据能通过查询接口获取                                  |
| 选课功能                | 选课接口返回成功，课程已选人数从 `0` 变为 `1`                            |
| 服务间通信              | 选课服务能调用课程服务的人数更新接口，数据同步正常                        |
| 重复选课异常            | 返回 `400` 错误或「已选该课程」提示                                      |
| 课程不存在异常          | 返回 `404` 错误或「课程不存在」提示                                      |

## 六、核心功能说明
### 6.1 课程目录服务（catalog-service）
#### 核心能力
1. 课程创建：支持录入课程代码、名称、讲师信息、排期、容量等；
2. 课程查询：支持按 ID/代码查询单个课程，按标题/讲师模糊查询；
3. 人数同步：接收选课服务的更新请求，同步课程已选人数（避免超容）；
4. 可用课程筛选：查询有剩余容量的课程（`GET /api/courses/available`）。

#### 关键约束
- 课程代码唯一：避免重复创建相同代码的课程；
- 人数合法性：已选人数不能为负数，不能超过课程容量；
- 必要字段非空：课程代码、名称为必填项。

### 6.2 选课服务（enrollment-service）
#### 核心能力
1. 学生管理：支持创建学生（学号、姓名、专业等信息）；
2. 选课操作：学生选择课程（验证课程存在、未超容、未重复选课）；
3. 退课操作：支持按选课ID退课、按学生+课程退课（仅活跃状态可退）；
4. 记录查询：查询所有选课记录、按课程/学生/状态筛选；
5. 统计功能：统计课程活跃选课人数、学生活跃选课数量。

#### 异常处理
- 重复选课：同一学生不能重复选择同一门活跃课程；
- 课程不存在：选课/退课时验证课程是否存在；
- 课程满员：选课人数达到容量上限时禁止选课；
- 非法状态操作：已退课/已完成的课程不能再次退课或选课。

## 七、核心接口文档（摘要）
### 7.1 课程目录服务（catalog-service）
| 接口功能         | 请求方式 | 请求地址                  | 请求体示例                                                                 | 响应说明                     |
|------------------|----------|---------------------------|--------------------------------------------------------------------------|------------------------------|
| 创建课程         | POST     | `/api/courses`            | `{"code":"CS101","title":"计算机科学导论","instructor":{"name":"张教授","email":"zhang@example.edu.cn"},"capacity":60}` | `code=201` 成功，返回课程信息 |
| 查询所有课程     | GET      | `/api/courses`            | -                                                                        | `code=200`，返回课程列表     |
| 查询单个课程（ID）| GET      | `/api/courses/{id}`       | -                                                                        | `code=200` 成功，`code=404` 不存在 |
| 更新选课人数     | PUT      | `/api/courses/{id}/enrolled?count=xxx` | -                                                                        | `code=200` 成功，`code=400` 人数非法 |
| 查询可用课程     | GET      | `/api/courses/available`  | -                                                                        | `code=200`，返回有剩余容量的课程 |

### 7.2 选课服务（enrollment-service）
| 接口功能         | 请求方式 | 请求地址                  | 请求体示例                                                                 | 响应说明                     |
|------------------|----------|---------------------------|--------------------------------------------------------------------------|------------------------------|
| 创建学生         | POST     | `/api/students`           | `{"studentId":"2024001","name":"张三","major":"计算机科学与技术","grade":"2024","email":"zhangsan@example.edu.cn"}` | `code=201` 成功，返回学生信息 |
| 学生选课         | POST     | `/api/enrollments`        | `{"courseId":"1","studentId":"2024001"}`                                  | `code=201` 成功，返回选课记录 |
| 按学生+课程退课  | POST     | `/api/enrollments/drop`   | `{"courseId":"1","studentId":"2024001"}`                                  | `code=200` 成功，返回更新后记录 |
| 查询选课记录     | GET      | `/api/enrollments`        | -                                                                        | `code=200`，返回选课列表     |
| 统计课程活跃人数 | GET      | `/api/enrollments/stats/course/{courseId}` | -                                                                        | `code=200`，返回统计结果     |

## 八、注意事项
### 8.1 服务间通信
- 选课服务调用课程服务时，依赖 `catalog-service.url` 配置（默认 `http://catalog-service:8081`），Docker 容器内通过服务名自动解析；
- 若本地开发调试，需将配置改为 `http://localhost:8081`（避免容器网络隔离）。

### 8.2 数据持久化
- MySQL 数据挂载到本地 `docker-deploy/mysql/data` 目录，删除容器后数据不丢失；
- 开发环境建议开启 `spring.jpa.hibernate.ddl-auto=update`（自动建表/更新表结构），生产环境改为 `none`。

### 8.3 编码问题
- 脚本运行时中文乱码：参考测试脚本优化方案，或在终端执行 `chcp 65001` 切换 UTF-8 编码；
- 服务响应中文乱码：确保服务端配置 `spring.http.encoding.charset=UTF-8`。

### 8.4 常见问题排查
1. 服务启动失败：查看日志 `docker-compose logs -f 服务名`，排查数据库连接失败、端口占用；
2. 接口调用失败：检查服务是否启动、端口是否正确（8081 课程服务，8082 选课服务）；
3. 数据同步失败：确保选课服务调用课程人数更新接口时，课程 ID 格式正确（数字字符串）；
4. 脚本执行报错“连接被意外关闭”：延长服务初始化时间（脚本中 `Start-Sleep -Seconds 15`），确保服务完全就绪。

