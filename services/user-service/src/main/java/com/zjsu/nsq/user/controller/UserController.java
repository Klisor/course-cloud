package com.zjsu.nsq.user.controller;

import com.zjsu.nsq.user.model.User;
import com.zjsu.nsq.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${server.port}")
    private String serverPort;

    private final UserService service;
    private final Environment environment;

    @Autowired
    public UserController(UserService service, Environment environment) {
        this.service = service;
        this.environment = environment;
    }

    /**
     * 获取当前用户信息（从网关请求头中获取）
     */
    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Username") String username,
            @RequestHeader("X-User-Role") String role) {

        logger.info("网关传递的用户信息 - ID: {}, Username: {}, Role: {}",
                userId, username, role);

        try {
            Optional<User> userOptional = service.findByUserId(userId);
            if (!userOptional.isPresent()) {
                return ResponseEntity.status(404)
                        .body(createResponse(404, "用户不存在", null));
            }

            User user = userOptional.get();
            user.setPassword(null); // 不返回密码

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("user", user);
            responseData.put("gatewayHeaders", Map.of(
                    "X-User-Id", userId,
                    "X-Username", username,
                    "X-User-Role", role
            ));

            return ResponseEntity.ok(createResponse(200, "获取当前用户信息成功", responseData));

        } catch (Exception e) {
            logger.error("获取当前用户信息失败: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(createResponse(500, "获取用户信息失败: " + e.getMessage(), null));
        }
    }

    /**
     * 获取学生信息（供 enrollment-service 的 Feign Client 调用）
     * 作业要求接口：GET /api/users/students/{id}
     */
    @GetMapping("/students/{id}")
    public ResponseEntity<Map<String, Object>> getStudentById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) String authUserId,
            @RequestHeader(value = "X-Username", required = false) String authUsername) {

        logger.info("用户 {} 查询学生信息 - ID: {}", authUsername, id);

        try {
            Optional<User> userOptional = service.findById(id);
            if (!userOptional.isPresent()) {
                return ResponseEntity.status(404)
                        .body(createResponse(404, "用户不存在", null));
            }

            User user = userOptional.get();

            // 构建返回给 enrollment-service 的 StudentDto 格式
            Map<String, Object> studentData = new HashMap<>();
            studentData.put("id", user.getId());
            studentData.put("userId", user.getUserId());       // 学号/工号
            studentData.put("name", user.getName());           // 姓名
            studentData.put("role", user.getRole().name());    // 角色
            studentData.put("major", user.getMajor());         // 专业
            studentData.put("grade", user.getGrade());         // 年级
            studentData.put("email", user.getEmail());         // 邮箱
            studentData.put("createdAt", user.getCreatedAt()); // 创建时间

            logger.info("✅ 返回学生信息 - id: {}, name: {}, role: {}",
                    id, user.getName(), user.getRole());

            return ResponseEntity.ok(createResponse(200, "成功", studentData));

        } catch (Exception e) {
            logger.error("❌ 获取学生信息失败 - id: {}", id, e);
            return ResponseEntity.status(500)
                    .body(createResponse(500, "获取学生信息失败: " + e.getMessage(), null));
        }
    }

    /**
     * 兼容接口：同时支持字符串ID的查询（为现有接口提供兼容）
     */
    @GetMapping("/students/by-userid/{userId}")
    public ResponseEntity<Map<String, Object>> getStudentByUserId(
            @PathVariable String userId,
            @RequestHeader(value = "X-User-Id", required = false) String authUserId,
            @RequestHeader(value = "X-Username", required = false) String authUsername) {

        logger.info("用户 {} 查询学生信息 - UserID: {}", authUsername, userId);

        try {
            Optional<User> userOptional = service.findByUserId(userId);
            if (!userOptional.isPresent()) {
                return ResponseEntity.status(404)
                        .body(createResponse(404, "用户不存在: " + userId, null));
            }

            User user = userOptional.get();

            Map<String, Object> studentData = new HashMap<>();
            studentData.put("id", user.getId());
            studentData.put("userId", user.getUserId());
            studentData.put("name", user.getName());
            studentData.put("role", user.getRole().name());
            studentData.put("major", user.getMajor());
            studentData.put("grade", user.getGrade());
            studentData.put("email", user.getEmail());
            studentData.put("createdAt", user.getCreatedAt());

            logger.info("✅ 返回学生信息 - userId: {}, name: {}", userId, user.getName());

            return ResponseEntity.ok(createResponse(200, "成功", studentData));

        } catch (Exception e) {
            logger.error("❌ 获取学生信息失败 - userId: {}", userId, e);
            return ResponseEntity.status(500)
                    .body(createResponse(500, "获取学生信息失败: " + e.getMessage(), null));
        }
    }

    /**
     * 负载均衡测试接口
     */
    @GetMapping("/lb-test")
    public ResponseEntity<Map<String, Object>> loadBalancerTest(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Username", required = false) String username) {

        logger.info("用户 {} 访问负载均衡测试", username);

        String timestamp = LocalDateTime.now().format(formatter);
        String instanceInfo = getInstanceInfo();

        Map<String, Object> response = new HashMap<>();
        response.put("service", "user-service");
        response.put("instanceInfo", instanceInfo);
        response.put("timestamp", timestamp);
        response.put("loadBalancerHit", true);
        response.put("message", "负载均衡测试 - 请求被此实例处理");
        response.put("currentUser", Map.of(
                "userId", userId,
                "username", username
        ));

        logger.info("【负载均衡日志】user-service 实例: {} 处理了用户 {} 的请求，时间: {}",
                instanceInfo, username, timestamp);

        return ResponseEntity.ok(createResponse(200, "Success", response));
    }

    /**
     * 获取实例信息字符串
     */
    private String getInstanceInfo() {
        try {
            return String.format("IP: %s, Port: %s",
                    InetAddress.getLocalHost().getHostAddress(),
                    serverPort);
        } catch (Exception e) {
            return "Port: " + serverPort;
        }
    }

    /**
     * 记录负载均衡请求日志
     */
    private void logLoadBalancedRequest(String methodName, String param) {
        String timestamp = LocalDateTime.now().format(formatter);
        String instanceInfo = getInstanceInfo();

        logger.info("【负载均衡】user-service 实例: {} 处理了 {}({}) 请求，时间: {}",
                instanceInfo, methodName, param, timestamp);
    }

    /**
     * 创建统一响应格式
     */
    private Map<String, Object> createResponse(int code, String msg, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", code);
        response.put("message", msg);
        response.put("data", data);
        return response;
    }

    /**
     * 获取服务实例信息（用于负载均衡测试）
     */
    @GetMapping("/instance-info")
    public ResponseEntity<Map<String, Object>> getInstanceInfoEndpoint(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Username", required = false) String username) {

        logger.info("用户 {} 查询实例信息", username);

        Map<String, Object> response = new HashMap<>();
        response.put("service", "user-service");
        response.put("instance", getInstanceInfo());
        response.put("timestamp", System.currentTimeMillis());
        response.put("requestedBy", username);
        return ResponseEntity.ok(createResponse(200, "Success", response));
    }

    /**
     * 健康检查接口（负载均衡专用）
     */
    @GetMapping("/health/lb")
    public ResponseEntity<Map<String, Object>> loadBalancerHealth(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Username", required = false) String username) {

        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "user-service");
        health.put("instance", getInstanceInfo());
        health.put("timestamp", System.currentTimeMillis());
        health.put("message", "Ready for load balancing");
        health.put("currentUser", username);
        return ResponseEntity.ok(createResponse(200, "Service is running", health));
    }

    /**
     * 获取服务实例信息（用于负载均衡测试）
     */
    @GetMapping("/port")
    public ResponseEntity<Map<String, Object>> getPort(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Username", required = false) String username) {

        logLoadBalancedRequest("getPort", "");

        Map<String, Object> response = new HashMap<>();
        response.put("service", "user-service");
        response.put("requestedBy", username);

        try {
            // 获取端口
            String port = environment.getProperty("local.server.port");
            response.put("port", port);

            // 获取IP地址
            String ip = InetAddress.getLocalHost().getHostAddress();
            response.put("ip", ip);

            response.put("timestamp", System.currentTimeMillis());
        } catch (Exception e) {
            response.put("error", e.getMessage());
            response.put("port", "unknown");
            response.put("ip", "unknown");
            response.put("timestamp", System.currentTimeMillis());
        }

        return ResponseEntity.ok(createResponse(200, "Success", response));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Username", required = false) String username) {

        logger.info("用户 {} 查询所有用户", username);
        logLoadBalancedRequest("list", "all");

        return ResponseEntity.ok(createResponse(200, "Success", service.findAll()));
    }

    @GetMapping("/{identifier}")
    public ResponseEntity<Map<String, Object>> getUserById(
            @PathVariable String identifier,
            @RequestHeader(value = "X-User-Id", required = false) String authUserId,
            @RequestHeader(value = "X-Username", required = false) String authUsername) {

        logger.info("用户 {} 查询用户信息: {}", authUsername, identifier);
        logLoadBalancedRequest("getUserById", identifier);

        Optional<User> user = Optional.empty();

        // 尝试作为数字ID查询
        if (identifier != null && identifier.matches("\\d+")) {
            Long id = Long.parseLong(identifier);
            user = service.findById(id);
        }

        // 如果不是数字或者按数字没找到，尝试按用户ID查询
        if (!user.isPresent()) {
            user = service.findByUserId(identifier);
        }

        if (user.isPresent()) {
            return ResponseEntity.ok(createResponse(200, "成功", user.get()));
        } else {
            throw new com.zjsu.nsq.user.service.UserService.UserNotFoundException(
                    "用户不存在: " + identifier);
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @RequestBody User u,
            @RequestHeader("X-User-Id") String operatorId,
            @RequestHeader("X-Username") String operatorName,
            @RequestHeader("X-User-Role") String operatorRole) {

        logger.info("用户 {} ({}, 角色: {}) 创建新用户: {}",
                operatorName, operatorId, operatorRole, u.getUserId());

        logLoadBalancedRequest("create", u.getUserId());

        // 检查操作者权限
        if (!"ADMIN".equals(operatorRole) && !"TEACHER".equals(operatorRole)) {
            logger.warn("用户 {} (角色: {}) 尝试创建用户，但权限不足", operatorName, operatorRole);
            return ResponseEntity.status(403)
                    .body(createResponse(403, "只有管理员或教师可以创建用户", null));
        }

        User createdUser = service.create(u);
        return ResponseEntity.ok(createResponse(200, "用户创建成功", createdUser));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable Long id,
            @RequestBody User u,
            @RequestHeader("X-User-Id") String operatorId,
            @RequestHeader("X-Username") String operatorName,
            @RequestHeader("X-User-Role") String operatorRole) {

        logger.info("用户 {} (角色: {}) 更新用户ID: {}", operatorName, operatorRole, id);
        logLoadBalancedRequest("update", String.valueOf(id));

        // 检查操作者权限
        if (!"ADMIN".equals(operatorRole)) {
            // 非管理员只能更新自己的信息
            Optional<User> targetUser = service.findById(id);
            if (targetUser.isPresent() && !targetUser.get().getUserId().equals(operatorId)) {
                logger.warn("用户 {} 尝试更新其他用户 {} 的信息", operatorName, id);
                return ResponseEntity.status(403)
                        .body(createResponse(403, "只能更新自己的用户信息", null));
            }
        }

        User updatedUser = service.update(id, u);
        return ResponseEntity.ok(createResponse(200, "用户更新成功", updatedUser));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String operatorId,
            @RequestHeader("X-Username") String operatorName,
            @RequestHeader("X-User-Role") String operatorRole) {

        logger.info("用户 {} (角色: {}) 删除用户ID: {}", operatorName, operatorRole, id);
        logLoadBalancedRequest("delete", String.valueOf(id));

        // 检查操作者权限
        if (!"ADMIN".equals(operatorRole)) {
            logger.warn("用户 {} (角色: {}) 尝试删除用户，但权限不足", operatorName, operatorRole);
            return ResponseEntity.status(403)
                    .body(createResponse(403, "只有管理员可以删除用户", null));
        }

        service.delete(id);
        return ResponseEntity.ok(createResponse(200, "用户删除成功", null));
    }

    @GetMapping("/by-userid/{userId}")
    public ResponseEntity<Map<String, Object>> getByUserId(
            @PathVariable String userId,
            @RequestHeader(value = "X-User-Id", required = false) String authUserId,
            @RequestHeader(value = "X-Username", required = false) String authUsername) {

        logger.info("用户 {} 查询用户: {}", authUsername, userId);
        logLoadBalancedRequest("getByUserId", userId);

        Optional<User> user = service.findByUserId(userId);
        if (user.isPresent()) {
            return ResponseEntity.ok(createResponse(200, "Success", user.get()));
        } else {
            throw new com.zjsu.nsq.user.service.UserService.UserNotFoundException(
                    "用户不存在: " + userId);
        }
    }

    // 健康检查接口
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Username", required = false) String username) {

        logger.info("用户 {} 访问健康检查", username);
        logLoadBalancedRequest("healthCheck", "");

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "User Service is running");
        response.put("timestamp", System.currentTimeMillis());
        response.put("service", "user-service");
        response.put("instance", getInstanceInfo());
        response.put("currentUser", username);
        return ResponseEntity.ok(response);
    }
}