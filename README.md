# 课程选课系统 - 微服务架构 v2.0.0

## 一、项目概述

### 1.1 项目简介

本项目是一个基于 Spring Boot 微服务架构的课程选课系统，将功能拆分为三个独立的微服务：**课程目录服务（catalog-service）**、**用户服务（user-service）** 和 **选课服务（enrollment-service）**。系统实现了完整的课程管理、用户管理、选课/退课业务流程，通过服务间调用实现数据一致性。

**版本信息**

- 项目名称：course-cloud
- **版本号：v2.0.0（引入 API Gateway，重大架构变更）**
- **基于版本：v1.2.0**

### 1.2 微服务架构说明

| 服务名称            | 端口     | 职责                             | 数据库        |
| ------------------- | -------- | -------------------------------- | ------------- |
| **gateway-service** | **8090** | **API网关（统一入口和认证）**    | -             |
| catalog-service     | 8081     | 课程管理（创建、查询、更新容量） | catalog_db    |
| user-service        | 8083     | 用户管理（学生、教师账户）       | user_db       |
| enrollment-service  | 8082     | 选课管理（选课、退课、统计）     | enrollment_db |

### 1.3 核心变更（v2.0.0）

1. **统一API入口**：引入Spring Cloud Gateway作为系统统一入口（端口8090）
2. **统一认证机制**：基于JWT实现统一认证，替代各服务独立认证
3. **架构演进**：客户端 → Gateway → 后端服务的三层架构
4. **责任分离**：网关负责认证和路由，后端服务专注业务逻辑

## 二、系统架构

### 2.1 架构图

```
客户端（浏览器/移动端）
       ↓
API Gateway (8090)
       ├─→ JWT认证过滤器
       │   ├── 白名单检查
       │   ├── Token验证
       │   └── 添加用户信息到请求头
       │
       ├─→ user-service (8083) ×3实例 → user-db (3308) [user_db]
       │   └── 用户管理、认证
       │
       ├─→ catalog-service (8081) ×3实例 → catalog-db (3307) [catalog_db]
       │   └── 课程管理
       │
       └─→ enrollment-service (8082) → enrollment-db (3309) [enrollment_db]
           ├── 选课管理
           ├── OpenFeign调用 → user-service（负载均衡）
           ├── OpenFeign调用 → catalog-service（负载均衡）
           └── RabbitMQ消息 → 异步更新课程人数
```

### 2.2 服务间调用关系

```
客户端 → Gateway (8090)
       ↓
Gateway → 各后端服务（添加X-User-Id, X-Username, X-User-Role请求头）

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

| 技术类别       | 具体技术                 | 版本/说明        |
| -------------- | ------------------------ | ---------------- |
| 后端框架       | Spring Boot              | 3.2.3            |
| 开发语言       | Java                     | 17+              |
| 构建工具       | Maven                    | 3.8+             |
| 数据库         | MySQL                    | 8.4              |
| 容器化         | Docker & Docker Compose  | 20.10+ & 2.0+    |
| **API网关**    | **Spring Cloud Gateway** | **统一API入口**  |
| **认证机制**   | **JWT (JSON Web Token)** | **统一认证**     |
| 服务通信       | OpenFeign                | 声明式HTTP客户端 |
| 熔断降级       | Resilience4j             | 断路器模式实现   |
| 服务注册与发现 | Nacos                    | 2.4.0            |
| 消息队列       | RabbitMQ                 | 3.13             |
| 数据持久化     | Spring Data JPA          | 3.2.3            |

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
│   ├── gateway-service/                   # 新增：网关服务
│   │   ├── src/main/java/com/zjsu/nsq/gateway/
│   │   │   ├── GatewayApplication.java
│   │   │   ├── filter/
│   │   │   │   └── JwtAuthenticationFilter.java
│   │   │   └── util/
│   │   │       └── JwtUtil.java
│   │   └── src/main/resources/
│   │       └── application.yml
│   ├── enrollment-service/
│   │   ├── src/main/java/com/zjsu/nsq/enrollment/
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
│   │   ├── src/main/java/com/zjsu/nsq/user/
│   │   │   ├── api/
│   │   │   │   ├── UserController.java
│   │   │   │   └── AuthController.java      # 新增：认证控制器
│   │   │   ├── util/
│   │   │   │   └── JwtUtil.java            # 新增：JWT工具类
│   │   │   └── service/
│   │   │       └── UserService.java
│   │   └── src/main/resources/
│   │       └── application.yml
│   └── catalog-service/
├── docker-compose.yml
├── scripts/
│   ├── test-load-balance.sh
│   └── test-auth.sh                      # 新增：认证测试脚本
└── README.md
```

### 5.2 本地构建（开发模式）

```bash
# 1. 克隆项目（如果从Git获取）
git clone <repository-url>
cd course-cloud

# 2. 分别构建每个服务
cd services/gateway-service && mvn clean package -DskipTests
cd ../user-service && mvn clean package -DskipTests
cd ../catalog-service && mvn clean package -DskipTests
cd ../enrollment-service && mvn clean package -DskipTests

# 3. 启动所有服务（包含Nacos、RabbitMQ和Gateway）
docker-compose up -d

# 4. 验证服务注册
# 访问Nacos控制台：http://localhost:8848/nacos
# 查看服务列表，确认所有服务均已注册，包括gateway-service
```

### 5.3 Docker 容器化运行

```bash
# 1. 确保在项目根目录
cd course-cloud

# 2. 使用 Docker Compose 一键启动所有服务
docker-compose up -d --scale user-service=3 --scale catalog-service=3

# 3. 查看服务状态
docker-compose ps

# 应看到类似以下输出：
# Name                    Command               State           Ports
# ---------------------------------------------------------------------------
# gateway-service         java -jar app.jar     Up              0.0.0.0:8090->8090/tcp
# catalog-service-1       java -jar app.jar     Up              8081/tcp
# catalog-service-2       java -jar app.jar     Up              8081/tcp
# catalog-service-3       java -jar app.jar     Up              8081/tcp
# enrollment-service-1    java -jar app.jar     Up              0.0.0.0:8082->8082/tcp
# nacos                   bash -c cd /home/na   Up              0.0.0.0:8848->8848/tcp
# rabbitmq                docker-entrypoint.sh  Up              15672/tcp, 5672/tcp
# user-service-1          java -jar app.jar     Up              8083/tcp
# user-service-2          java -jar app.jar     Up              8083/tcp
# user-service-3          java -jar app.jar     Up              8083/tcp

# 4. 停止服务
docker-compose down
```

## 六、API 网关配置说明

### 6.1 网关服务概览

**位置**：`gateway-service/`
**端口**：8090
**功能**：统一API入口、JWT认证、请求路由、CORS跨域支持

### 6.2 依赖配置

在 `gateway-service/pom.xml` 中添加：

```xml
<!-- Spring Cloud Gateway -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>

<!-- JWT 依赖 -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>

<!-- Nacos 服务发现 -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
```

### 6.3 网关配置文件

**application.yml 配置：**

```yaml
server:
  port: 8090

spring:
  application:
    name: gateway-service
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER_ADDR:nacos:8848}
        namespace: dev
        group: DEFAULT_GROUP
        ephemeral: true
        heart-beat-interval: 5000
        heart-beat-timeout: 15000
    
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true
      
      # 路由配置
      routes:
        - id: user-service-auth
          uri: lb://user-service
          predicates:
            - Path=/api/auth/**
          filters:
            - StripPrefix=1
        
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/users/**
          filters:
            - StripPrefix=1
        
        - id: catalog-service
          uri: lb://catalog-service
          predicates:
            - Path=/api/courses/**
          filters:
            - StripPrefix=1
        
        - id: enrollment-service
          uri: lb://enrollment-service
          predicates:
            - Path=/api/enrollments/**
          filters:
            - StripPrefix=1
      
      # CORS跨域配置
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins: "*"
            allowedMethods: "*"
            allowedHeaders: "*"
            allowCredentials: true

# JWT配置（与user-service一致）
jwt:
  secret: ${JWT_SECRET:course-gateway-secret-key-256-bit-course-gateway-secret-key-256-bit}
  expiration: ${JWT_EXPIRATION:86400000}  # 24小时

# 日志配置
logging:
  level:
    com.zjsu.nsq.gateway.filter.JwtAuthenticationFilter: DEBUG
    org.springframework.cloud.gateway: DEBUG
```

### 6.4 JWT认证过滤器

**位置**：`gateway-service/src/main/java/com/zjsu/nsq/gateway/filter/JwtAuthenticationFilter.java`

**核心功能**：
1. **白名单检查**：`/api/auth/login`、`/api/auth/register` 等路径直接放行
2. **Token验证**：验证Authorization头中的Bearer Token
3. **用户信息提取**：解析Token获取userId、username、role
4. **请求头添加**：将用户信息添加到请求头（X-User-Id、X-Username、X-User-Role）
5. **请求转发**：将修改后的请求转发给后端服务

**关键代码片段：**
```java
@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {
    
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/api/auth/login",
            "/api/auth/register",
            "/actuator/health"
    );
    
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getPath().value();
            
            // 1. 白名单放行
            if (isWhiteList(path)) {
                return chain.filter(exchange);
            }
            
            // 2. 获取并验证Token
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
            
            String token = authHeader.substring(7);
            if (!jwtUtil.validateToken(token)) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
            
            // 3. 解析Token获取用户信息
            Claims claims = jwtUtil.parseToken(token);
            String userId = claims.getSubject();  // 从subject获取userId
            String username = claims.get("username", String.class);
            String role = claims.get("role", String.class);
            
            // 4. 添加用户信息到请求头
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", userId)
                    .header("X-Username", username)
                    .header("X-User-Role", role)
                    .build();
            
            // 5. 转发请求
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }
}
```

### 6.5 JWT工具类

**位置**：
- Gateway服务：`gateway-service/src/main/java/com/zjsu/nsq/gateway/util/JwtUtil.java`
- User服务：`user-service/src/main/java/com/zjsu/nsq/user/util/JwtUtil.java`

**核心方法**：
```java
@Component
public class JwtUtil {
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration}")
    private Long expiration;
    
    // 生成Token
    public String generateToken(String userId, String username, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
                .setSubject(userId)  // userId存储在subject中
                .claim("username", username)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }
    
    // 解析Token
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    // 验证Token有效性
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

## 七、用户认证模块

### 7.1 认证控制器

**位置**：`user-service/src/main/java/com/zjsu/nsq/user/api/AuthController.java`

**核心接口**：
```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        // 1. 验证用户名和密码
        User user = userService.findByUsername(request.getUserId());
        if (user == null || !user.getPassword().equals(request.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponse("用户ID或密码错误"));
        }
        
        // 2. 生成JWT Token
        String token = jwtUtil.generateToken(
                user.getUserId(),
                user.getUserId(),  // 使用userId作为username
                user.getRole().name()
        );
        
        // 3. 返回Token和用户信息
        user.setPassword(null); // 不返回密码
        return ResponseEntity.ok(new LoginResponse(token, user));
    }
    
    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@RequestBody RegisterRequest request) {
        // 注册逻辑，生成Token并返回
    }
}
```

### 7.2 认证请求/响应DTO

**LoginRequest内部类：**
```java
public static class LoginRequest {
    private String userId;  // 使用userId作为用户名
    private String password;
    // getter/setter
}
```

**LoginResponse内部类：**
```java
public static class LoginResponse {
    private String token;
    private User user;
    private String message;
    // getter/setter
}
```

## 八、后端服务获取用户信息

### 8.1 User Service 修改

**UserController.java：**
```java
@GetMapping("/current")
public ResponseEntity<Map<String, Object>> getCurrentUser(
        @RequestHeader("X-User-Id") String userId,
        @RequestHeader("X-Username") String username,
        @RequestHeader("X-User-Role") String role) {
    
    logger.info("网关传递的用户信息 - ID: {}, Username: {}, Role: {}", userId, username, role);
    // 业务逻辑
}

@PostMapping
public ResponseEntity<Map<String, Object>> create(
        @RequestBody User u,
        @RequestHeader("X-User-Id") String operatorId,
        @RequestHeader("X-Username") String operatorName,
        @RequestHeader("X-User-Role") String operatorRole) {
    
    // 权限检查：只有管理员或教师可以创建用户
    if (!"ADMIN".equals(operatorRole) && !"TEACHER".equals(operatorRole)) {
        return ResponseEntity.status(403)
                .body(createResponse(403, "只有管理员或教师可以创建用户", null));
    }
    // 业务逻辑
}
```

### 8.2 Enrollment Service 修改

**EnrollmentController.java：**
```java
@PostMapping
public ResponseEntity<?> createEnrollment(
        @RequestHeader("X-User-Id") String userId,
        @RequestHeader("X-Username") String username,
        @RequestBody EnrollmentRequest request) {
    
    logger.info("用户 {} (ID: {}) 发起选课请求", username, userId);
    // 使用从网关传递的用户ID
    request.setUserId(userId);
    // 业务逻辑
}
```

## 九、认证流程说明

### 9.1 完整认证流程

```
1. 客户端 → POST /api/auth/login (Gateway) → user-service
   ↓
2. user-service 验证凭证，生成JWT Token，返回给客户端
   ↓
3. 客户端携带Token访问其他API：Authorization: Bearer <token>
   ↓
4. Gateway验证Token，提取用户信息，添加到请求头
   ↓
5. Gateway转发请求到对应服务（携带X-User-Id, X-Username, X-User-Role）
   ↓
6. 后端服务从请求头获取用户信息，执行业务逻辑
```

### 9.2 Token结构

```json
{
  "sub": "stu2024004",           // subject存储userId
  "username": "stu2024004",      // 用户名（使用userId）
  "role": "STUDENT",             // 用户角色
  "iat": 1765625986,             // 签发时间
  "exp": 1765712386              // 过期时间（24小时后）
}
```

### 9.3 请求头传递

Gateway向后端服务传递的用户信息头：
- `X-User-Id`：用户唯一标识（从Token的subject中提取）
- `X-Username`：用户名（从Token的username claim中提取）
- `X-User-Role`：用户角色（从Token的role claim中提取）

## 十、测试方法

### 10.1 认证测试脚本

```bash
#!/bin/bash
# scripts/test-auth.sh

echo "=== API网关与统一认证测试 ==="

# 等待服务启动
sleep 10

# 1. 注册用户
echo -e "\n1. 测试用户注册..."
curl -X POST http://localhost:8090/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "testuser",
    "password": "test123",
    "name": "测试用户",
    "role": "STUDENT",
    "email": "test@example.com"
  }'

# 2. 登录获取Token
echo -e "\n2. 测试用户登录..."
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8090/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"userId":"testuser","password":"test123"}')

echo "登录响应: $LOGIN_RESPONSE"
TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
echo "Token: ${TOKEN:0:30}..."

# 3. 测试未认证访问
echo -e "\n3. 测试未认证访问（应返回401）..."
curl -X GET http://localhost:8090/api/users \
  -H "Content-Type: application/json" \
  -w "\nHTTP状态码: %{http_code}\n"

# 4. 测试认证访问
echo -e "\n4. 测试认证访问（应返回200）..."
curl -X GET http://localhost:8090/api/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -w "\nHTTP状态码: %{http_code}\n"

echo -e "\n=== 测试完成 ==="
```

### 10.2 手动测试命令

```bash
# 1. 登录获取Token
curl -X POST http://localhost:8090/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"userId":"admin001","password":"admin123"}'

# 2. 使用Token访问
TOKEN="eyJhbGciOiJIUzUxMiJ9..."
curl -X GET http://localhost:8090/api/users/current \
  -H "Authorization: Bearer $TOKEN"

# 3. 测试路由转发
curl -X GET http://localhost:8090/api/courses \
  -H "Authorization: Bearer $TOKEN"

# 4. 测试选课
curl -X POST http://localhost:8090/api/enrollments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"courseId": 1}'
```

## 十一、常见问题与解决方案

### 问题1：网关返回503 Service Unavailable

**症状**：通过网关访问返回503错误

**解决方案**：
1. 检查Nacos服务注册状态
2. 验证网关路由配置正确性
3. 确保后端服务健康检查通过

```bash
# 检查服务状态
docker-compose ps

# 查看网关日志
docker-compose logs gateway-service

# 检查Nacos服务列表
# 访问 http://localhost:8848/nacos
```

### 问题2：Token验证失败

**症状**：网关返回401 Unauthorized

**解决方案**：
1. 确保Gateway和User服务的JWT密钥一致
2. 检查Token格式是否正确（Bearer <token>）
3. 验证Token是否过期

```yaml
# 确保两个服务的jwt.secret配置相同
jwt:
  secret: ${JWT_SECRET:course-gateway-secret-key-256-bit-course-gateway-secret-key-256-bit}
  expiration: ${JWT_EXPIRATION:86400000}
```

### 问题3：后端服务获取不到用户信息

**症状**：后端服务日志显示用户信息为null

**解决方案**：
1. 检查网关过滤器是否正确添加请求头
2. 验证请求头名称匹配（大小写敏感）
3. 检查后端服务的@RequestHeader注解

```java
// 后端服务正确获取请求头
@RequestHeader("X-User-Id") String userId
@RequestHeader("X-Username") String username
@RequestHeader("X-User-Role") String role
```

### 问题4：白名单路径被拦截

**症状**：登录/注册接口需要认证

**解决方案**：
1. 检查网关过滤器的白名单配置
2. 确保路径匹配正确

```java
private static final List<String> WHITE_LIST = Arrays.asList(
        "/api/auth/login",
        "/api/auth/register",
        "/actuator/health"
);
```

## 十二、架构优势总结

### 12.1 引入网关的优势

1. **统一入口**：所有请求通过统一网关入口，便于管理和监控
2. **统一认证**：集中处理认证逻辑，避免各服务重复实现
3. **安全提升**：Token验证在网关层完成，后端服务更安全
4. **职责清晰**：网关负责流量控制和认证，业务服务专注业务逻辑
5. **扩展性强**：新增服务只需在网关配置路由，不影响现有架构

### 12.2 性能考虑

1. **网关性能**：Spring Cloud Gateway基于WebFlux，支持高并发
2. **Token验证**：JWT无需查询数据库，验证速度快
3. **负载均衡**：网关自动集成负载均衡，分发请求到多个实例

### 12.3 安全性考虑

1. **HTTPS支持**：生产环境应启用HTTPS
2. **Token安全**：使用强密钥，设置合理过期时间
3. **防止重放攻击**：可考虑添加Token唯一性验证
4. **权限控制**：网关可集成更细粒度的权限控制

---

**版本状态**：✅ v2.0.0 已完成  
**部署方式**：Docker Compose一键部署  
**认证方式**：JWT Token统一认证  
**API入口**：http://localhost:8090
