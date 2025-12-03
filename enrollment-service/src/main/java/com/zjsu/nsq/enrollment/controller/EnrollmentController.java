package com.zjsu.nsq.enrollment.controller;

import com.zjsu.nsq.enrollment.model.Enrollment;
import com.zjsu.nsq.enrollment.model.EnrollmentStatus;
import com.zjsu.nsq.enrollment.service.EnrollmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {
    @Autowired
    private Environment environment;
    private final EnrollmentService service;

    @Autowired
    public EnrollmentController(EnrollmentService service) {
        this.service = service;
    }

    private Map<String, Object> createResponse(int code, String msg, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", code);
        response.put("message", msg);
        response.put("data", data);
        return response;
    }
    /**
     * 获取Enrollment服务实例信息
     */
    @GetMapping("/port")
    public ResponseEntity<Map<String, Object>> getPort() {
        Map<String, Object> response = new HashMap<>();

        try {
            response.put("service", "enrollment-service");
            response.put("port", environment.getProperty("local.server.port"));
            response.put("ip", InetAddress.getLocalHost().getHostAddress());
            response.put("timestamp", System.currentTimeMillis());
        } catch (Exception e) {
            response.put("service", "enrollment-service");
            response.put("port", environment.getProperty("server.port", "8082"));
            response.put("ip", "unknown");
            response.put("error", e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 测试服务发现
     */
    @GetMapping("/discovery")
    public ResponseEntity<Map<String, Object>> testDiscovery() {
        Map<String, Object> result = new HashMap<>();

        // 当前服务信息
        Map<String, Object> currentService = new HashMap<>();
        try {
            currentService.put("service", "enrollment-service");
            currentService.put("port", environment.getProperty("local.server.port"));
        } catch (Exception e) {
            currentService.put("error", e.getMessage());
        }
        result.put("currentService", currentService);

        // 服务发现测试结果
        Map<String, Object> discoveryTest = service.testServiceDiscovery();
        result.put("discoveryTest", discoveryTest);

        result.put("timestamp", System.currentTimeMillis());
        result.put("message", "服务发现测试完成");

        return ResponseEntity.ok(result);
    }

    // ==================== 查询接口 ====================

    @GetMapping
    public ResponseEntity<Map<String, Object>> list() {
        List<Enrollment> enrollments = service.findAll();
        return ResponseEntity.ok(createResponse(200, "Success", enrollments));
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<Map<String, Object>> byCourse(@PathVariable String courseId) {
        List<Enrollment> enrollments = service.findByCourse(courseId);
        return ResponseEntity.ok(createResponse(200, "Success", enrollments));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> byUser(@PathVariable String userId) {
        List<Enrollment> enrollments = service.findByUser(userId);
        return ResponseEntity.ok(createResponse(200, "Success", enrollments));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Map<String, Object>> byStatus(@PathVariable String status) {
        EnrollmentStatus st = EnrollmentStatus.valueOf(status.toUpperCase());
        List<Enrollment> enrollments = service.findByStatus(st);
        return ResponseEntity.ok(createResponse(200, "Success", enrollments));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        Enrollment enrollment = service.findById(id);
        return ResponseEntity.ok(createResponse(200, "Success", enrollment));
    }

    // ==================== 统计接口 ====================

    @GetMapping("/stats/course/{courseId}")
    public ResponseEntity<Map<String, Object>> courseStats(@PathVariable String courseId) {
        Map<String, Object> stats = service.getEnrollmentStats(courseId);
        return ResponseEntity.ok(createResponse(200, "Success", stats));
    }

    @GetMapping("/count/active/user/{userId}")
    public ResponseEntity<Map<String, Object>> countActiveByUser(@PathVariable String userId) {
        Long count = service.countActiveEnrollmentsByUser(userId);
        return ResponseEntity.ok(createResponse(200, "Success", count));
    }

    @GetMapping("/count/active/course/{courseId}")
    public ResponseEntity<Map<String, Object>> countActiveByCourse(@PathVariable String courseId) {
        Long count = service.countActiveEnrollmentsByCourse(courseId);
        return ResponseEntity.ok(createResponse(200, "Success", count));
    }

    // ==================== 业务接口 ====================

    @PostMapping
    public ResponseEntity<Map<String, Object>> enroll(@RequestBody Map<String, String> request) {
        String courseId = request.get("courseId");
        String userId = request.get("userId");

        Enrollment result = service.enroll(courseId, userId);
        return ResponseEntity.status(201)
                .body(createResponse(201, "选课成功", result));
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<Map<String, Object>> complete(@PathVariable Long id) {
        Enrollment result = service.complete(id);
        return ResponseEntity.ok(createResponse(200, "课程完成", result));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> unenroll(@PathVariable Long id) {
        service.unenroll(id);
        return ResponseEntity.ok(createResponse(200, "退课成功", null));
    }

    @DeleteMapping("/drop")
    public ResponseEntity<Map<String, Object>> dropByUserAndCourse(
            @RequestParam String userId,
            @RequestParam String courseId) {
        Enrollment result = service.dropByUserAndCourse(userId, courseId);
        return ResponseEntity.ok(createResponse(200, "退课成功", result));
    }

    @PostMapping("/{id}/drop")
    public ResponseEntity<Map<String, Object>> drop(@PathVariable Long id) {
        Enrollment result = service.drop(id);
        return ResponseEntity.ok(createResponse(200, "退课成功", result));
    }

    @DeleteMapping("/cancel/{id}")
    public ResponseEntity<Map<String, Object>> cancel(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(createResponse(200, "取消成功", null));
    }

    // ==================== 辅助接口 ====================

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "enrollment-service");
        health.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(createResponse(200, "Service is running", health));
    }
}