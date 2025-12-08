# 课程选课系统 - 微服务架构

## 一、项目概述

### 1.1 项目简介

本项目是一个基于 Spring Boot 微服务架构的课程选课系统，将功能拆分为三个独立的微服务：**课程目录服务（catalog-service）**、**用户服务（user-service）** 和 **选课服务（enrollment-service）**。系统实现了完整的课程管理、用户管理、选课/退课业务流程，通过服务间调用实现数据一致性。

**版本信息**

- 项目名称：course-cloud
- 版本号：v1.2.0（服务间通信与负载均衡）
- 项目阶段：服务间通信与负载均衡
- 基于版本：v1.1.0

### 1.2 微服务架构说明

| 服务名称           | 端口 | 职责                             | 数据库        |
| ------------------ | ---- | -------------------------------- | ------------- |
| catalog-service    | 8081 | 课程管理（创建、查询、更新容量） | catalog_db    |
| user-service       | 8083 | 用户管理（学生、教师账户）       | user_db       |
| enrollment-service | 8082 | 选课管理（选课、退课、统计）     | enrollment_db |

### 1.3 核心变更（v1.2.0）

1. **服务间通信升级**：使用 OpenFeign 替代 RestTemplate 实现声明式服务调用
2. **容错机制增强**：集成 Resilience4j 实现熔断降级
3. **负载均衡验证**：通过多实例部署验证负载均衡效果
4. **异步消息队列**：使用 RabbitMQ 实现分布式事务最终一致性

## 二、系统架构

### 2.1 架构图

```
客户端
  ↓
  ├─→ catalog-service (8081) ×3实例 → catalog-db (3307) [catalog_db]
  │   └── 课程管理
  │
  ├─→ user-service (8083) ×3实例 → user-db (3308) [user_db]
  │   └── 用户管理
  │
  └─→ enrollment-service (8082) → enrollment-db (3309) [enrollment_db]
      ├── 选课管理
      ├── OpenFeign调用 → user-service（负载均衡）
      ├── OpenFeign调用 → catalog-service（负载均衡）
      └── RabbitMQ消息 → 异步更新课程人数
```

### 2.2 服务间调用关系

```
enrollment-service (8082)
       │
       ├──→ OpenFeign → user-service (8083) ×3实例
       │      ├── @GetMapping("/api/users/students/{id}") 获取学生信息
       │      └── Fallback: UserClientFallback.class
       │
       └──→ OpenFeign → catalog-service (8081) ×3实例
              ├── @GetMapping("/api/courses/{id}") 获取课程信息
              └── Fallback: CatalogClientFallback.class
```

## 三、技术栈

| 技术类别       | 具体技术                | 版本/说明            |
| -------------- | ----------------------- | -------------------- |
| 后端框架       | Spring Boot             | 3.2.3                |
| 开发语言       | Java                    | 17+                  |
| 构建工具       | Maven                   | 3.8+                 |
| 数据库         | MySQL                   | 8.4                  |
| 容器化         | Docker & Docker Compose | 20.10+ & 2.0+        |
| **服务通信**   | **OpenFeign**           | **声明式HTTP客户端** |
| **熔断降级**   | **Resilience4j**        | **断路器模式实现**   |
| 服务注册与发现 | Nacos                   | 2.4.0                |
| 消息队列       | RabbitMQ                | 3.13                 |
| 数据持久化     | Spring Data JPA         | 3.2.3                |

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
course-cloud/
├── services/
│   ├── enrollment-service/
│   │   ├── src/main/java/com/zjgsu/coursecloud/enrollment/
│   │   │   ├── client/
│   │   │   │   ├── UserClient.java
│   │   │   │   ├── UserClientFallback.java
│   │   │   │   ├── CatalogClient.java
│   │   │   │   └── CatalogClientFallback.java
│   │   │   ├── dto/
│   │   │   │   ├── StudentDto.java
│   │   │   │   └── CourseDto.java
│   │   │   └── service/
│   │   │       └── EnrollmentService.java
│   │   └── src/main/resources/
│   │       └── application.yml
│   ├── user-service/
│   └── catalog-service/
├── docker-compose.yml
├── scripts/
│   └── test-load-balance.sh
└── README.md
```

### 5.2 本地构建（开发模式）

```bash
# 1. 克隆项目（如果从Git获取）
git clone <repository-url>
cd course-cloud

# 2. 分别构建每个服务
cd services/enrollment-service && mvn clean package
cd ../user-service && mvn clean package
cd ../catalog-service && mvn clean package

# 3. 启动所有服务（包含Nacos和RabbitMQ）
docker-compose up -d

# 4. 验证服务注册
# 访问Nacos控制台：http://localhost:8848/nacos
# 查看服务列表，确认三个服务均已注册
```

### 5.3 Docker 容器化运行（多实例）

```bash
# 1. 确保在项目根目录
cd course-cloud

# 2. 使用 Docker Compose 一键启动所有服务（含多实例）
docker-compose up -d --scale user-service=3 --scale catalog-service=3

# 3. 查看服务状态
docker-compose ps

# 应看到类似以下输出：
# Name                    Command               State           Ports
# ---------------------------------------------------------------------------
# catalog-service-1       java -jar app.jar     Up              8081/tcp
# catalog-service-2       java -jar app.jar     Up              8081/tcp
# catalog-service-3       java -jar app.jar     Up              8081/tcp
# enrollment-service-1    java -jar app.jar     Up              0.0.0.0:8082->8082/tcp
# nacos                   bash -c cd /home/na   Up              0.0.0.0:8848->8848/tcp
# rabbitmq                docker-entrypoint.sh  Up              15672/tcp, 5672/tcp
# user-service-1          java -jar app.jar     Up              8083/tcp
# user-service-2          java -jar app.jar     Up              8083/tcp
# user-service-3          java -jar app.jar     Up              8083/tcp

# 4. 查看日志（可选）
docker-compose logs -f user-service
docker-compose logs -f catalog-service

# 5. 停止服务
docker-compose down
```

### 5.4 负载均衡测试

```bash
# 1. 进入scripts目录
cd scripts

# 2. 运行负载均衡测试脚本
./test-load-balance.sh

# 或手动测试：
# 连续发送10次选课请求，观察负载均衡效果
for i in {1..10}; do
  echo "请求 $i:"
  curl -X POST http://localhost:8082/api/enrollments \
    -H "Content-Type: application/json" \
    -d '{"courseId":"1","userId":"stu001"}' \
    -s | jq '.message'
  sleep 1
done
```

## 六、OpenFeign 配置说明

### 6.1 依赖配置

在 `enrollment-service` 的 `pom.xml` 中添加：

```xml
<!-- OpenFeign -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>

<!-- Resilience4j 熔断器 -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>
```

### 6.2 启用 Feign 客户端

在 `EnrollmentServiceApplication.java` 中添加注解：

```java
@SpringBootApplication
@EnableFeignClients
public class EnrollmentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EnrollmentServiceApplication.class, args);
    }
}
```

### 6.3 Feign Client 接口定义

**UserClient.java:**

```java
@FeignClient(
    name = "user-service",
    fallback = UserClientFallback.class
)
public interface UserClient {
    @GetMapping("/api/users/students/{id}")
    StudentDto getStudent(@PathVariable Long id);
    
    @GetMapping("/api/users/by-userid/{userId}")
    UserDto getUserByUserId(@PathVariable String userId);
}
```

**CatalogClient.java:**

```java
@FeignClient(
    name = "catalog-service",
    fallback = CatalogClientFallback.class
)
public interface CatalogClient {
    @GetMapping("/api/courses/{id}")
    CourseDto getCourse(@PathVariable Long id);
    
    @PutMapping("/api/courses/{id}/enrolled")
    ResponseResult<Void> updateEnrolledCount(@PathVariable Long id, 
                                             @RequestParam("count") int count);
}
```

### 6.4 Fallback 降级实现

**UserClientFallback.java:**

```java
@Component
@Slf4j
public class UserClientFallback implements UserClient {
    @Override
    public StudentDto getStudent(Long id) {
        log.warn("UserClient fallback triggered for student id: {}", id);
        throw new ServiceUnavailableException("用户服务暂时不可用，请稍后再试");
    }
    
    @Override
    public UserDto getUserByUserId(String userId) {
        log.warn("UserClient fallback triggered for userId: {}", userId);
        throw new ServiceUnavailableException("用户服务暂时不可用，请稍后再试");
    }
}
```

**CatalogClientFallback.java:**

```java
@Component
@Slf4j
public class CatalogClientFallback implements CatalogClient {
    @Override
    public CourseDto getCourse(Long id) {
        log.warn("CatalogClient fallback triggered for course id: {}", id);
        throw new ServiceUnavailableException("课程服务暂时不可用，请稍后再试");
    }
    
    @Override
    public ResponseResult<Void> updateEnrolledCount(Long id, int count) {
        log.warn("CatalogClient fallback triggered for updating course id: {}", id);
        throw new ServiceUnavailableException("课程服务暂时不可用，请稍后再试");
    }
}
```

### 6.5 配置文件

**application.yml 配置示例：**

```yaml
spring:
  application:
    name: enrollment-service
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER_ADDR:nacos:8848}
        namespace: dev
        group: DEFAULT_GROUP

feign:
  circuitbreaker:
    enabled: true
  client:
    config:
      default:
        connectTimeout: 3000  # 3秒连接超时
        readTimeout: 5000     # 5秒读取超时
        loggerLevel: basic

resilience4j:
  circuitbreaker:
    instances:
      user-service:
        slidingWindowSize: 10         # 滑动窗口大小
        failureRateThreshold: 50      # 失败率阈值50%
        waitDurationInOpenState: 5s   # 断路器开启持续时间
        permittedNumberOfCallsInHalfOpenState: 3  # 半开状态允许的调用次数
        slidingWindowType: COUNT_BASED # 基于计数的滑动窗口
      catalog-service:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 5s
        permittedNumberOfCallsInHalfOpenState: 3
        slidingWindowType: COUNT_BASED
```

### 6.6 使用 Feign Client

在 `EnrollmentService.java` 中：

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class EnrollmentService {
    private final UserClient userClient;
    private final CatalogClient catalogClient;
    
    public Enrollment enrollCourse(EnrollmentRequest request) {
        // 1. 验证用户存在（通过Feign调用）
        try {
            UserDto user = userClient.getUserByUserId(request.getUserId());
            log.info("验证用户成功: {}", user.getName());
        } catch (Exception e) {
            throw new ServiceUnavailableException("无法验证用户信息: " + e.getMessage());
        }
        
        // 2. 验证课程存在和容量（通过Feign调用）
        try {
            CourseDto course = catalogClient.getCourse(request.getCourseId());
            if (course.getEnrolled() >= course.getCapacity()) {
                throw new CourseFullException("课程已满");
            }
            log.info("验证课程成功: {}", course.getTitle());
        } catch (Exception e) {
            throw new ServiceUnavailableException("无法获取课程信息: " + e.getMessage());
        }
        
        // 3. 创建选课记录
        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(request.getUserId());
        enrollment.setCourseId(request.getCourseId());
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment.setEnrolledAt(LocalDateTime.now());
        
        return enrollmentRepository.save(enrollment);
    }
}
```

## 七、API 文档

### 7.1 课程服务 (catalog-service:8081)

（与v1.1.0保持一致）

### 7.2 用户服务 (user-service:8083)

新增端点：

| 方法    | 端点                       | 描述                               |
| ------- | -------------------------- | ---------------------------------- |
| **GET** | `/api/users/students/{id}` | 获取学生信息                       |
| **GET** | `/api/users/port`          | 获取服务端口号（用于负载均衡测试） |

### 7.3 选课服务 (enrollment-service:8082)

新增端点：

| 方法    | 端点                          | 描述                   |
| ------- | ----------------------------- | ---------------------- |
| **GET** | `/api/enrollments/discovery`  | 测试服务发现和负载均衡 |
| **GET** | `/api/enrollments/feign-test` | 测试OpenFeign调用      |

### 7.4 服务间调用示例

**OpenFeign调用验证：**

```bash
# 测试OpenFeign调用
curl http://localhost:8082/api/enrollments/feign-test

# 测试负载均衡
curl http://localhost:8082/api/enrollments/discovery
```

## 八、多实例部署配置

### 8.1 docker-compose.yml 配置

```yaml
version: '3.8'

services:
  # Nacos 服务注册中心
  nacos:
    image: nacos/nacos-server:v2.4.0
    container_name: nacos
    environment:
      MODE: standalone
      SPRING_DATASOURCE_PLATFORM: mysql
      MYSQL_SERVICE_HOST: mysql
      MYSQL_SERVICE_DB_NAME: nacos
      MYSQL_SERVICE_USER: root
      MYSQL_SERVICE_PASSWORD: 123456
    ports:
      - "8848:8848"
      - "9848:9848"
      - "9849:9849"
    networks:
      - coursehub-network

  # 用户服务（3个实例）
  user-service:
    build: ./services/user-service
    image: user-service:v1.2.0
    environment:
      SERVER_PORT: 8083
      SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR: nacos:8848
      SPRING_CLOUD_NACOS_DISCOVERY_NAMESPACE: dev
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/user_db?useSSL=false&characterEncoding=utf8
    networks:
      - coursehub-network
    deploy:
      replicas: 3

  # 课程服务（3个实例）
  catalog-service:
    build: ./services/catalog-service
    image: catalog-service:v1.2.0
    environment:
      SERVER_PORT: 8081
      SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR: nacos:8848
      SPRING_CLOUD_NACOS_DISCOVERY_NAMESPACE: dev
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/catalog_db?useSSL=false&characterEncoding=utf8
    networks:
      - coursehub-network
    deploy:
      replicas: 3

  # 选课服务（1个实例）
  enrollment-service:
    build: ./services/enrollment-service
    image: enrollment-service:v1.2.0
    ports:
      - "8082:8082"
    environment:
      SERVER_PORT: 8082
      SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR: nacos:8848
      SPRING_CLOUD_NACOS_DISCOVERY_NAMESPACE: dev
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/enrollment_db?useSSL=false&characterEncoding=utf8
      USER_SERVICE_URL: http://user-service:8083
      CATALOG_SERVICE_URL: http://catalog-service:8081
    networks:
      - coursehub-network
    depends_on:
      - nacos
      - user-service
      - catalog-service

  # RabbitMQ 消息队列
  rabbitmq:
    image: rabbitmq:3.13-management
    container_name: rabbitmq
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest
    networks:
      - coursehub-network

  # MySQL 数据库
  mysql:
    image: mysql:8.4
    container_name: mysql
    environment:
      MYSQL_ROOT_PASSWORD: 123456
      MYSQL_DATABASE: course_system
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
    networks:
      - coursehub-network

networks:
  coursehub-network:
    driver: bridge

volumes:
  mysql-data:
```

### 8.2 多实例验证

```bash
# 启动所有服务（包含多实例）
docker-compose up -d

# 查看实例数量
docker-compose ps | grep user-service
docker-compose ps | grep catalog-service

# 访问Nacos控制台查看服务实例
# http://localhost:8848/nacos → 服务管理 → 服务列表
# 应看到user-service和catalog-service各有3个实例
```

## 九、功能测试

### 9.1 负载均衡测试

**测试步骤：**

1. **启动所有服务**
   ```bash
   docker-compose up -d
   ```

2. **查看Nacos服务注册**
   - 访问 http://localhost:8848/nacos
   - 登录（nacos/nacos）
   - 进入"服务管理" → "服务列表"
   - 确认user-service和catalog-service各有3个实例

3. **执行负载均衡测试**
   
   ```bash
   # 连续发送10次请求
   for i in {1..10}; do
     echo "请求 $i:"
     curl -X POST http://localhost:8082/api/enrollments \
       -H "Content-Type: application/json" \
       -d '{"courseId":"1","userId":"stu001"}' \
       -s | jq '.message'
     sleep 0.5
   done
   ```
   
4. **查看日志验证负载均衡**
   ```bash
   # 查看不同实例的日志
   docker-compose logs user-service-1 | grep "处理请求"
   docker-compose logs user-service-2 | grep "处理请求"
   docker-compose logs user-service-3 | grep "处理请求"
   ```

**预期结果：**
- 请求被均匀分配到3个user-service实例
- 每个实例都处理了一定数量的请求

### 9.2 熔断降级测试

**测试步骤：**

1. **停止所有user-service实例**
   ```bash
   docker-compose stop user-service
   ```

2. **发送选课请求**
   ```bash
   curl -X POST http://localhost:8082/api/enrollments \
     -H "Content-Type: application/json" \
     -d '{"courseId":"1","userId":"stu001"}'
   ```

3. **验证fallback触发**
   ```bash
   # 查看enrollment-service日志
   docker-compose logs enrollment-service | grep "fallback"
   
   # 预期看到：
   # UserClient fallback triggered for userId: stu001
   ```

4. **重启服务验证恢复**
   ```bash
   docker-compose start user-service
   
   # 等待服务恢复注册（约30秒）
   sleep 30
   
   # 再次发送请求
   curl -X POST http://localhost:8082/api/enrollments \
     -H "Content-Type: application/json" \
     -d '{"courseId":"1","userId":"stu001"}'
   
   # 应正常处理，不再触发fallback
   ```

### 9.3 OpenFeign vs RestTemplate 对比

| 特性           | OpenFeign        | RestTemplate       |
| -------------- | ---------------- | ------------------ |
| **声明式编程** | ✅ 通过接口定义   | ❌ 需要编写具体实现 |
| **负载均衡**   | ✅ 自动集成Ribbon | ❌ 需要手动实现     |
| **熔断降级**   | ✅ 原生支持       | ❌ 需要额外集成     |
| **配置简化**   | ✅ 注解配置       | ❌ XML或Java配置    |
| **代码可读性** | ✅ 高             | ❌ 低               |
| **维护成本**   | ✅ 低             | ❌ 高               |

**优势总结：**
1. **开发效率**：OpenFeign通过注解自动生成客户端，减少样板代码
2. **维护性**：服务接口变更时只需修改接口定义，无需修改调用代码
3. **集成性**：与Spring Cloud生态无缝集成（负载均衡、熔断器等）
4. **可测试性**：可以轻松创建Mock客户端进行单元测试

## 十、常见问题与解决方案

### 问题1：OpenFeign调用失败

**症状**：`FeignException$NotFound` 或 `FeignException$InternalServerError`

**解决方案**：
1. 检查服务名是否正确：`@FeignClient(name = "user-service")`
2. 确认路径匹配：Feign接口的路径需要与服务提供者的路径完全一致
3. 检查参数注解：`@PathVariable`、`@RequestParam` 等注解使用正确

### 问题2：熔断器不生效

**症状**：服务不可用时没有触发fallback

**解决方案**：
1. 确认配置正确：`feign.circuitbreaker.enabled: true`
2. 检查Resilience4j配置：滑动窗口大小和失败率阈值
3. 确认Fallback类被Spring管理：添加`@Component`注解

### 问题3：多实例负载不均衡

**症状**：请求总是路由到同一个实例

**解决方案**：
1. 检查Nacos服务发现：确认所有实例都已注册
2. 验证负载均衡策略：默认使用轮询策略
3. 检查网络配置：确保所有服务在同一Docker网络中

### 问题4：服务启动顺序问题

**症状**：enrollment-service启动时无法发现其他服务

**解决方案**：
1. 添加依赖关系：在docker-compose中配置`depends_on`
2. 增加健康检查：等待依赖服务完全启动
3. 添加重试机制：在应用启动时重试服务发现

```yaml
spring:
  cloud:
    nacos:
      discovery:
        # 服务发现失败时重试
        retry:
          max-attempts: 10
          initial-interval: 2000ms
          multiplier: 1.5
```

## 十一、监控与管理

### 11.1 健康检查端点

| 服务               | 端点                   | 描述                         |
| ------------------ | ---------------------- | ---------------------------- |
| enrollment-service | `GET /actuator/health` | 健康检查（包含依赖服务状态） |
| user-service       | `GET /actuator/health` | 健康检查                     |
| catalog-service    | `GET /actuator/health` | 健康检查                     |

### 11.2 Resilience4j监控

添加Actuator端点监控熔断器状态：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,circuitbreakers,metrics
  metrics:
    export:
      prometheus:
        enabled: true
```

访问端点：`GET /actuator/circuitbreakers`

### 11.3 日志配置

为每个实例添加唯一标识：

```java
// 在Controller中添加
@Value("${server.port}")
private String port;

@GetMapping("/api/users/port")
public Map<String, Object> getPort() {
    Map<String, Object> result = new HashMap<>();
    result.put("service", "user-service");
    result.put("port", port);
    result.put("timestamp", LocalDateTime.now());
    return result;
}
```

## 十二、性能优化建议

### 12.1 OpenFeign优化

1. **连接池配置**：
   
   ```yaml
   feign:
     httpclient:
       enabled: true
       max-connections: 200
       max-connections-per-route: 50
   ```
   
2. **超时配置优化**：
   ```yaml
   feign:
     client:
       config:
         default:
           connectTimeout: 2000
           readTimeout: 5000
   ```

### 12.2 Resilience4j优化

1. **动态配置**：根据业务需求调整熔断器参数
2. **监控告警**：集成Prometheus监控熔断器状态
3. **分层熔断**：为不同重要性的服务设置不同的熔断策略

### 12.3 缓存策略

1. **本地缓存**：对不经常变化的用户/课程信息使用Caffeine缓存
2. **分布式缓存**：考虑引入Redis作为分布式缓存
3. **缓存失效策略**：合理设置缓存过期时间

