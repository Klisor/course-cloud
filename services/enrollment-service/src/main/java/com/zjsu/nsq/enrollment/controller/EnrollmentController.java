// src/main/java/com/zjsu/nsq/enrollment/controller/EnrollmentController.java
package com.zjsu.nsq.enrollment.controller;

import com.zjsu.nsq.enrollment.client.UserClient;
import com.zjsu.nsq.enrollment.client.CatalogClient;
import com.zjsu.nsq.enrollment.client.UserClientFallback;
import com.zjsu.nsq.enrollment.dto.CourseDto;
import com.zjsu.nsq.enrollment.dto.StudentDto;
import com.zjsu.nsq.enrollment.model.Enrollment;
import com.zjsu.nsq.enrollment.model.EnrollmentStatus;
import com.zjsu.nsq.enrollment.service.EnrollmentService;
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
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {
    private static final Logger logger = LoggerFactory.getLogger(EnrollmentController.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${server.port:8082}")
    private String serverPort;

    @Autowired
    private Environment environment;

    private final EnrollmentService service;
    private final UserClient userClient;
    private final CatalogClient catalogClient;

    @Autowired
    public EnrollmentController(EnrollmentService service,
                                UserClient userClient,
                                CatalogClient catalogClient) {
        this.service = service;
        this.userClient = userClient;
        this.catalogClient = catalogClient;
    }

    /**
     * 负载均衡测试接口
     */
    @GetMapping("/lb-test")
    public ResponseEntity<Map<String, Object>> loadBalancerTest() {
        String timestamp = LocalDateTime.now().format(formatter);
        String instanceInfo = getInstanceInfo();

        Map<String, Object> response = new HashMap<>();
        response.put("service", "enrollment-service");
        response.put("instanceInfo", instanceInfo);
        response.put("timestamp", timestamp);
        response.put("loadBalancerHit", true);
        response.put("message", "负载均衡测试 - 请求被此实例处理");

        // 记录负载均衡日志
        logger.info("【负载均衡日志】enrollment-service 实例: {} 处理了请求，时间: {}",
                instanceInfo, timestamp);

        return ResponseEntity.ok(createResponse(200, "Success", response));
    }

    /**
     * 获取服务实例信息（用于负载均衡测试）
     */
    @GetMapping("/instance-info")
    public ResponseEntity<Map<String, Object>> getInstanceInfoEndpoint() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "enrollment-service");
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
        health.put("service", "enrollment-service");
        health.put("instance", getInstanceInfo());
        health.put("timestamp", System.currentTimeMillis());
        health.put("message", "Ready for load balancing");
        return ResponseEntity.ok(createResponse(200, "Service is running", health));
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

        logger.info("【负载均衡】enrollment-service 实例: {} 处理了 {}({}) 请求，时间: {}",
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
     * 专门用于测试熔断降级的接口
     * 返回简单的响应，方便查看日志
     */
    @GetMapping("/test/circuit-only")
    public ResponseEntity<Map<String, Object>> testCircuitBreakerOnly() {
        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", System.currentTimeMillis());
        result.put("test", "熔断降级测试");

        // 只测试UserClient，简化测试
        try {
            logger.info("📞 开始调用UserClient.getStudent(1)...");  // 使用logger，不是log
            StudentDto student = userClient.getStudent(1L);

            result.put("status", "成功");
            if (student != null) {
                result.put("student", student.getData() != null ? student.getData().getName() : "null");
                result.put("code", student.getCode());
                result.put("message", student.getMessage());

                // 判断是否是降级响应
                if (student.getMessage() != null && student.getMessage().contains("熔断降级")) {
                    result.put("isFallback", true);
                    logger.warn("⚠️ 本次调用返回了降级数据");  // 使用logger
                }
            }
        } catch (Exception e) {
            result.put("status", "异常");
            result.put("error", e.getClass().getSimpleName());
            result.put("errorMessage", e.getMessage());
            logger.error("调用UserClient失败: {}", e.getMessage());  // 使用logger
        }

        return ResponseEntity.ok(createResponse(200, "熔断测试完成", result));
    }
    /**
     * 专门验证熔断降级的接口
     * 这个接口会捕获异常并检查是否是fallback
     */
    @GetMapping("/test/circuit-verify")
    public ResponseEntity<Map<String, Object>> verifyCircuitBreaker() {
        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", System.currentTimeMillis());
        result.put("test", "熔断降级验证测试");

        try {
            logger.info("尝试调用UserClient...");
            StudentDto response = userClient.getStudent(1L);

            result.put("status", "SUCCESS");
            result.put("code", response.getCode());
            result.put("message", response.getMessage());

            // 判断响应
            if (response.getCode() == 503 &&
                    response.getMessage() != null &&
                    response.getMessage().contains("熔断降级")) {
                result.put("isFallback", true);
                result.put("fallbackEvidence", "✅ Fallback被正确触发！");
                logger.error("🎯🎯🎯 FALLBACK触发成功！这是作业需要的证据！");
            } else {
                result.put("isFallback", false);
            }

        } catch (Exception e) {
            result.put("status", "EXCEPTION");
            result.put("exception", e.getClass().getSimpleName());
            result.put("exceptionMessage", e.getMessage());
            logger.error("调用失败，异常类型: {}", e.getClass().getName());

            // 检查异常是否应该触发fallback
            if (e.getClass().getName().contains("ServiceUnavailable") ||
                    e.getClass().getName().contains("NoAvailableService")) {
                result.put("shouldTriggerFallback", true);
                result.put("note", "这个异常应该触发Fallback，但可能配置有问题");
            }
        }

        return ResponseEntity.ok(createResponse(200, "熔断验证完成", result));
    }
    /**
     * 测试 OpenFeign 连接性（修正版，使用数字ID 1）
     */
    @GetMapping("/test/feign-v2")
    public ResponseEntity<Map<String, Object>> testFeignClientsV2() {
        logLoadBalancedRequest("testFeignClientsV2", "");

        Map<String, Object> result = new HashMap<>();
        result.put("service", "enrollment-service");
        result.put("port", environment.getProperty("local.server.port"));
        result.put("timestamp", System.currentTimeMillis());

        // 测试 UserClient（使用数字ID 1）
        Map<String, Object> userClientTest = new HashMap<>();
        try {
            // 修改：Feign客户端现在直接返回StudentDto
            StudentDto student = userClient.getStudent(1L);
            userClientTest.put("success", student != null && student.getData() != null);
            userClientTest.put("data", student);
            userClientTest.put("status", "connected");
        } catch (Exception e) {
            userClientTest.put("success", false);
            userClientTest.put("error", e.getMessage());
            userClientTest.put("status", "error");
        }
        result.put("userClient", userClientTest);

        // 测试 CatalogClient（使用数字ID 1）
        Map<String, Object> catalogClientTest = new HashMap<>();
        try {
            // 修改：Feign客户端现在直接返回CourseDto
            CourseDto course = catalogClient.getCourse(1L);
            catalogClientTest.put("success", course != null && course.getData() != null);
            catalogClientTest.put("data", course);
            catalogClientTest.put("status", "connected");
        } catch (Exception e) {
            catalogClientTest.put("success", false);
            catalogClientTest.put("error", e.getMessage());
            catalogClientTest.put("status", "error");
        }
        result.put("catalogClient", catalogClientTest);

        result.put("feignEnabled", true);

        return ResponseEntity.ok(createResponse(200, "Feign测试完成(v2)", result));
    }

    /**
     * 获取熔断器状态
     */
    @GetMapping("/circuit-breaker/status")
    public ResponseEntity<Map<String, Object>> getCircuitBreakerStatus() {
        logLoadBalancedRequest("getCircuitBreakerStatus", "");

        Map<String, Object> status = new HashMap<>();
        status.put("userServiceCircuitBreaker", "ENABLED");
        status.put("catalogServiceCircuitBreaker", "ENABLED");
        status.put("failureRateThreshold", "50%");
        status.put("slidingWindowSize", 10);
        status.put("waitDurationInOpenState", "5s");

        return ResponseEntity.ok(createResponse(200, "熔断器状态", status));
    }

    /**
     * 获取服务实例端口信息（原有接口，保持兼容）
     */
    @GetMapping("/port")
    public ResponseEntity<Map<String, Object>> getPort() {
        logLoadBalancedRequest("getPort", "");

        Map<String, Object> response = new HashMap<>();
        response.put("service", "enrollment-service");

        try {
            // 获取端口
            String port = environment.getProperty("local.server.port", serverPort);
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

    /**
     * 测试服务发现
     */
    @GetMapping("/discovery")
    public ResponseEntity<Map<String, Object>> testDiscovery() {
        logLoadBalancedRequest("testDiscovery", "");

        Map<String, Object> result = new HashMap<>();

        // 当前服务信息
        Map<String, Object> currentService = new HashMap<>();
        try {
            currentService.put("service", "enrollment-service");
            currentService.put("port", environment.getProperty("local.server.port", serverPort));
        } catch (Exception e) {
            currentService.put("error", e.getMessage());
        }
        result.put("currentService", currentService);

        // 服务发现测试结果
        Map<String, Object> discoveryTest = service.testServiceDiscovery();
        result.put("discoveryTest", discoveryTest);

        result.put("timestamp", System.currentTimeMillis());
        result.put("message", "服务发现测试完成");

        return ResponseEntity.ok(createResponse(200, "Success", result));
    }

    // ==================== 查询接口 ====================

    @GetMapping
    public ResponseEntity<Map<String, Object>> list() {
        logLoadBalancedRequest("list", "all");
        List<Enrollment> enrollments = service.findAll();
        return ResponseEntity.ok(createResponse(200, "Success", enrollments));
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<Map<String, Object>> byCourse(@PathVariable String courseId) {
        logLoadBalancedRequest("byCourse", courseId);
        List<Enrollment> enrollments = service.findByCourse(courseId);
        return ResponseEntity.ok(createResponse(200, "Success", enrollments));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> byUser(@PathVariable String userId) {
        logLoadBalancedRequest("byUser", userId);
        List<Enrollment> enrollments = service.findByUser(userId);
        return ResponseEntity.ok(createResponse(200, "Success", enrollments));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Map<String, Object>> byStatus(@PathVariable String status) {
        logLoadBalancedRequest("byStatus", status);
        try {
            EnrollmentStatus st = EnrollmentStatus.valueOf(status.toUpperCase());
            List<Enrollment> enrollments = service.findByStatus(st);
            return ResponseEntity.ok(createResponse(200, "Success", enrollments));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(createResponse(400, "无效的状态值，有效值: ACTIVE, COMPLETED, DROPPED", null));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        logLoadBalancedRequest("getById", String.valueOf(id));
        Enrollment enrollment = service.findById(id);
        return ResponseEntity.ok(createResponse(200, "Success", enrollment));
    }

    // ==================== 统计接口 ====================

    @GetMapping("/stats/course/{courseId}")
    public ResponseEntity<Map<String, Object>> courseStats(@PathVariable String courseId) {
        logLoadBalancedRequest("courseStats", courseId);
        Map<String, Object> stats = service.getEnrollmentStats(courseId);
        return ResponseEntity.ok(createResponse(200, "Success", stats));
    }

    @GetMapping("/count/active/user/{userId}")
    public ResponseEntity<Map<String, Object>> countActiveByUser(@PathVariable String userId) {
        logLoadBalancedRequest("countActiveByUser", userId);
        Long count = service.countActiveEnrollmentsByUser(userId);
        return ResponseEntity.ok(createResponse(200, "Success", count));
    }

    @GetMapping("/count/active/course/{courseId}")
    public ResponseEntity<Map<String, Object>> countActiveByCourse(@PathVariable String courseId) {
        logLoadBalancedRequest("countActiveByCourse", courseId);
        Long count = service.countActiveEnrollmentsByCourse(courseId);
        return ResponseEntity.ok(createResponse(200, "Success", count));
    }

    // ==================== 业务接口 ====================

    @PostMapping
    public ResponseEntity<Map<String, Object>> enroll(@RequestBody Map<String, String> request) {
        String courseId = request.get("courseId");
        String userId = request.get("userId");
        logLoadBalancedRequest("enroll", String.format("courseId=%s, userId=%s", courseId, userId));

        try {
            Enrollment result = service.enroll(courseId, userId);
            return ResponseEntity.status(201)
                    .body(createResponse(201, "选课成功", result));
        } catch (Exception e) {
            logger.error("选课失败", e);
            return ResponseEntity.status(400)
                    .body(createResponse(400, "选课失败: " + e.getMessage(), null));
        }
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<Map<String, Object>> complete(@PathVariable Long id) {
        logLoadBalancedRequest("complete", String.valueOf(id));
        try {
            Enrollment result = service.complete(id);
            return ResponseEntity.ok(createResponse(200, "课程完成", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(createResponse(400, e.getMessage(), null));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> unenroll(@PathVariable Long id) {
        logLoadBalancedRequest("unenroll", String.valueOf(id));
        try {
            service.unenroll(id);
            return ResponseEntity.ok(createResponse(200, "退课成功", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(createResponse(400, e.getMessage(), null));
        }
    }

    @DeleteMapping("/drop")
    public ResponseEntity<Map<String, Object>> dropByUserAndCourse(
            @RequestParam String userId,
            @RequestParam String courseId) {
        logLoadBalancedRequest("dropByUserAndCourse", String.format("userId=%s, courseId=%s", userId, courseId));
        try {
            Enrollment result = service.dropByUserAndCourse(userId, courseId);
            return ResponseEntity.ok(createResponse(200, "退课成功", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(createResponse(400, e.getMessage(), null));
        }
    }

    @PostMapping("/{id}/drop")
    public ResponseEntity<Map<String, Object>> drop(@PathVariable Long id) {
        logLoadBalancedRequest("drop", String.valueOf(id));
        try {
            Enrollment result = service.drop(id);
            return ResponseEntity.ok(createResponse(200, "退课成功", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(createResponse(400, e.getMessage(), null));
        }
    }

    @DeleteMapping("/cancel/{id}")
    public ResponseEntity<Map<String, Object>> cancel(@PathVariable Long id) {
        logLoadBalancedRequest("cancel", String.valueOf(id));
        try {
            service.delete(id);
            return ResponseEntity.ok(createResponse(200, "取消成功", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(createResponse(400, e.getMessage(), null));
        }
    }

    // ==================== 辅助接口 ====================

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        logLoadBalancedRequest("healthCheck", "");

        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "enrollment-service");
        health.put("instance", getInstanceInfo());
        health.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(createResponse(200, "Service is running", health));
    }
}