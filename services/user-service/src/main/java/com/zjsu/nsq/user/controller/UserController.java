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
     * 获取学生信息（供 enrollment-service 的 Feign Client 调用）
     * 作业要求接口：GET /api/users/students/{id}
     */
    @GetMapping("/students/{id}")
    public ResponseEntity<Map<String, Object>> getStudentById(@PathVariable Long id) {
        logLoadBalancedRequest("getStudentById", String.valueOf(id));

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
    public ResponseEntity<Map<String, Object>> getStudentByUserId(@PathVariable String userId) {
        logLoadBalancedRequest("getStudentByUserId", userId);

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
    public ResponseEntity<Map<String, Object>> loadBalancerTest() {
        String timestamp = LocalDateTime.now().format(formatter);
        String instanceInfo = getInstanceInfo();

        Map<String, Object> response = new HashMap<>();
        response.put("service", "user-service");
        response.put("instanceInfo", instanceInfo);
        response.put("timestamp", timestamp);
        response.put("loadBalancerHit", true);
        response.put("message", "负载均衡测试 - 请求被此实例处理");

        logger.info("【负载均衡日志】user-service 实例: {} 处理了请求，时间: {}",
                instanceInfo, timestamp);

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
    public ResponseEntity<Map<String, Object>> getInstanceInfoEndpoint() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "user-service");
        response.put("instance", getInstanceInfo());
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(createResponse(200, "Success", response));
    }

    /**
     * 健康检查接口（负载均衡专用）
     */
    @GetMapping("/health/lb")
    public ResponseEntity<Map<String, Object>> loadBalancerHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "user-service");
        health.put("instance", getInstanceInfo());
        health.put("timestamp", System.currentTimeMillis());
        health.put("message", "Ready for load balancing");
        return ResponseEntity.ok(createResponse(200, "Service is running", health));
    }

    /**
     * 获取服务实例信息（用于负载均衡测试）
     */
    @GetMapping("/port")
    public ResponseEntity<Map<String, Object>> getPort() {
        logLoadBalancedRequest("getPort", "");

        Map<String, Object> response = new HashMap<>();
        response.put("service", "user-service");

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
    public ResponseEntity<Map<String, Object>> list() {
        logLoadBalancedRequest("list", "all");

        return ResponseEntity.ok(createResponse(200, "Success", service.findAll()));
    }

    @GetMapping("/{identifier}")
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable String identifier) {
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
    public ResponseEntity<Map<String, Object>> create(@RequestBody User u) {
        String userId = u.getUserId() != null ? u.getUserId() : "new";
        logLoadBalancedRequest("create", userId);

        User createdUser = service.create(u);
        return ResponseEntity.ok(createResponse(200, "用户创建成功", createdUser));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id, @RequestBody User u) {
        logLoadBalancedRequest("update", String.valueOf(id));

        User updatedUser = service.update(id, u);
        return ResponseEntity.ok(createResponse(200, "用户更新成功", updatedUser));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        logLoadBalancedRequest("delete", String.valueOf(id));

        service.delete(id);
        return ResponseEntity.ok(createResponse(200, "用户删除成功", null));
    }

    @GetMapping("/by-userid/{userId}")
    public ResponseEntity<Map<String, Object>> getByUserId(@PathVariable String userId) {
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
    public ResponseEntity<Map<String, Object>> healthCheck() {
        logLoadBalancedRequest("healthCheck", "");

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "User Service is running");
        response.put("timestamp", System.currentTimeMillis());
        response.put("service", "user-service");
        response.put("instance", getInstanceInfo());
        return ResponseEntity.ok(response);
    }
}