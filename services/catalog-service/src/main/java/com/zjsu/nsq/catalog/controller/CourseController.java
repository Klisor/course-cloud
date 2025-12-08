package com.zjsu.nsq.catalog.controller;

import com.zjsu.nsq.catalog.model.Course;
import com.zjsu.nsq.catalog.service.CourseService;
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

@RestController
@RequestMapping("/api/courses")
public class CourseController {
    private static final Logger logger = LoggerFactory.getLogger(CourseController.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${server.port}")
    private String serverPort;

    private final CourseService service;

    @Autowired
    private Environment environment;

    @Autowired
    public CourseController(CourseService service) {
        this.service = service;
    }

    // ==================== 辅助方法 ====================

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

        logger.info("【负载均衡】catalog-service 实例: {} 处理了 {}({}) 请求，时间: {}",
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



    // ==================== 核心业务接口 ====================

    /**
     * 获取课程信息（供 enrollment-service 的 Feign Client 调用）
     * 作业要求接口：GET /api/courses/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getCourseById(@PathVariable Long id) {
        logLoadBalancedRequest("getCourseById", String.valueOf(id));

        try {
            Course course = service.findById(id)
                    .orElseThrow(() -> new RuntimeException("课程不存在"));

            // 构建返回给 enrollment-service 的 CourseDto 格式
            Map<String, Object> courseData = new HashMap<>();
            courseData.put("id", course.getId());
            courseData.put("code", course.getCode());
            courseData.put("title", course.getTitle());
            courseData.put("capacity", course.getCapacity());
            courseData.put("enrolled", course.getEnrolled());
            courseData.put("createdAt", course.getCreatedAt());

            // 如果有 instructor 信息
            if (course.getInstructor() != null) {
                courseData.put("instructorName", course.getInstructor().getName());
                courseData.put("instructorEmail", course.getInstructor().getEmail());
            }

            // 如果有 schedule 信息
            if (course.getSchedule() != null) {
                courseData.put("scheduleDay", course.getSchedule().getDayOfWeek());
                courseData.put("scheduleStartTime", course.getSchedule().getStartTime());
                courseData.put("scheduleEndTime", course.getSchedule().getEndTime());
            }

            logger.info("✅ 返回课程信息 - id: {}, code: {}, title: {}",
                    id, course.getCode(), course.getTitle());

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "Success");
            response.put("data", courseData);
            response.put("instance", getInstanceInfo());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("❌ 课程不存在 - id: {}", id);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 404);
            response.put("message", e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(404).body(response);
        } catch (Exception e) {
            logger.error("❌ 获取课程信息失败 - id: {}", id, e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "Internal server error: " + e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 根据课程代码获取课程信息
     */
    @GetMapping("/by-code/{code}")
    public ResponseEntity<Map<String, Object>> getCourseByCode(@PathVariable String code) {
        logLoadBalancedRequest("getCourseByCode", code);

        try {
            Course course = service.findByCode(code)
                    .orElseThrow(() -> new RuntimeException("课程不存在: " + code));

            Map<String, Object> courseData = new HashMap<>();
            courseData.put("id", course.getId());
            courseData.put("code", course.getCode());
            courseData.put("title", course.getTitle());
            courseData.put("capacity", course.getCapacity());
            courseData.put("enrolled", course.getEnrolled());
            courseData.put("createdAt", course.getCreatedAt());

            if (course.getInstructor() != null) {
                courseData.put("instructorName", course.getInstructor().getName());
                courseData.put("instructorEmail", course.getInstructor().getEmail());
            }

            if (course.getSchedule() != null) {
                courseData.put("scheduleDay", course.getSchedule().getDayOfWeek());
                courseData.put("scheduleStartTime", course.getSchedule().getStartTime());
                courseData.put("scheduleEndTime", course.getSchedule().getEndTime());
            }

            logger.info("✅ 返回课程信息 - code: {}, title: {}", code, course.getTitle());

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "Success");
            response.put("data", courseData);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 404);
            response.put("message", e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(404).body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "Internal server error: " + e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(500).body(response);
        }
    }

    // ==================== CRUD 操作 ====================

    @GetMapping
    public ResponseEntity<Map<String, Object>> list() {
        logLoadBalancedRequest("list", "all");

        try {
            List<Course> courses = service.findAll();
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "Success");
            response.put("data", courses);
            response.put("instance", getInstanceInfo());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "Internal server error: " + e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Course course) {
        logLoadBalancedRequest("create", course.getCode() != null ? course.getCode() : "new");

        try {
            if (course.getCode() == null || course.getCode().trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("code", 400);
                response.put("message", "课程代码不能为空");
                response.put("data", null);
                return ResponseEntity.status(400).body(response);
            }

            if (course.getTitle() == null || course.getTitle().trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("code", 400);
                response.put("message", "课程名称不能为空");
                response.put("data", null);
                return ResponseEntity.status(400).body(response);
            }

            Course createdCourse = service.create(course);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 201);
            response.put("message", "课程创建成功");
            response.put("data", createdCourse);
            return ResponseEntity.status(201).body(response);
        } catch (CourseService.CourseAlreadyExistsException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 409);
            response.put("message", e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(409).body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "Internal server error: " + e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(500).body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id, @RequestBody Course course) {
        logLoadBalancedRequest("update", String.valueOf(id));

        try {
            Course updatedCourse = service.update(id, course);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "课程更新成功");
            response.put("data", updatedCourse);
            return ResponseEntity.ok(response);
        } catch (CourseService.CourseNotFoundException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 404);
            response.put("message", e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(404).body(response);
        } catch (CourseService.CourseAlreadyExistsException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 409);
            response.put("message", e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(409).body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "Internal server error: " + e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(500).body(response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        logLoadBalancedRequest("delete", String.valueOf(id));

        try {
            service.delete(id);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "删除成功");
            response.put("data", null);
            return ResponseEntity.ok(response);
        } catch (CourseService.CourseNotFoundException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 404);
            response.put("message", e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(404).body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "Internal server error: " + e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(500).body(response);
        }
    }

    // ==================== 业务操作接口 ====================

    /**
     * 更新课程选课人数接口（供 enrollment-service 调用）
     */
    @PutMapping("/{id}/enrolled")
    public ResponseEntity<Map<String, Object>> updateEnrolledCount(
            @PathVariable("id") Long courseId,
            @RequestParam("count") Integer newEnrolledCount) {

        logLoadBalancedRequest("updateEnrolledCount", String.format("courseId=%d, count=%d", courseId, newEnrolledCount));

        try {
            Course updatedCourse = service.updateEnrolledCount(courseId, newEnrolledCount);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "选课人数更新成功");
            response.put("data", updatedCourse);
            return ResponseEntity.ok(response);

        } catch (CourseService.CourseNotFoundException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 404);
            response.put("message", e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(404).body(response);
        } catch (CourseService.InvalidCourseDataException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 400);
            response.put("message", e.getMessage());
            response.put("data", null);
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "更新选课人数失败：" + e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 减少选课人数接口（原子操作）
     */
    @PostMapping("/{id}/drop")
    public ResponseEntity<Map<String, Object>> dropCourse(@PathVariable("id") Long courseId) {
        logLoadBalancedRequest("dropCourse", String.valueOf(courseId));

        try {
            Course updatedCourse = service.decrementEnrolledCount(courseId);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "退课成功，课程人数已减少");
            response.put("data", updatedCourse);
            return ResponseEntity.ok(response);

        } catch (CourseService.CourseNotFoundException e) {
            return ResponseEntity.status(404)
                    .body(Map.of("code", 404, "message", e.getMessage(), "data", null));
        } catch (CourseService.InvalidCourseDataException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("code", 400, "message", e.getMessage(), "data", null));
        }
    }

    /**
     * 检查课程容量接口
     */
    @GetMapping("/{id}/capacity")
    public ResponseEntity<Map<String, Object>> getCourseCapacity(@PathVariable("id") Long courseId) {
        logLoadBalancedRequest("getCourseCapacity", String.valueOf(courseId));

        try {
            int availableCapacity = service.getAvailableCapacity(courseId);
            boolean hasCapacity = service.hasAvailableCapacity(courseId);

            Map<String, Object> data = new HashMap<>();
            data.put("courseId", courseId);
            data.put("availableCapacity", availableCapacity);
            data.put("hasCapacity", hasCapacity);

            return ResponseEntity.ok(Map.of(
                    "code", 200,
                    "message", "查询成功",
                    "data", data
            ));

        } catch (CourseService.CourseNotFoundException e) {
            return ResponseEntity.status(404)
                    .body(Map.of("code", 404, "message", e.getMessage(), "data", null));
        }
    }

    // ==================== 查询接口 ====================

    @GetMapping("/search/title/{title}")
    public ResponseEntity<Map<String, Object>> findByTitle(@PathVariable String title) {
        logLoadBalancedRequest("findByTitle", title);

        try {
            List<Course> courses = service.findByTitleContaining(title);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "Success");
            response.put("data", courses);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "Internal server error: " + e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/search/instructor/{instructorName}")
    public ResponseEntity<Map<String, Object>> findByInstructor(@PathVariable String instructorName) {
        logLoadBalancedRequest("findByInstructor", instructorName);

        try {
            List<Course> courses = service.findByInstructorName(instructorName);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "Success");
            response.put("data", courses);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "Internal server error: " + e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/available")
    public ResponseEntity<Map<String, Object>> findAvailableCourses() {
        logLoadBalancedRequest("findAvailableCourses", "");

        try {
            List<Course> courses = service.findAvailableCourses();
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "Success");
            response.put("data", courses);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "Internal server error: " + e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(500).body(response);
        }
    }

    // ==================== 负载均衡测试接口 ====================

    @GetMapping("/lb-test")
    public ResponseEntity<Map<String, Object>> loadBalancerTest() {
        String timestamp = LocalDateTime.now().format(formatter);
        String instanceInfo = getInstanceInfo();

        Map<String, Object> response = new HashMap<>();
        response.put("service", "catalog-service");
        response.put("instanceInfo", instanceInfo);
        response.put("timestamp", timestamp);
        response.put("loadBalancerHit", true);
        response.put("message", "负载均衡测试 - 请求被此实例处理");

        logger.info("【负载均衡日志】catalog-service 实例: {} 处理了请求，时间: {}",
                instanceInfo, timestamp);

        return ResponseEntity.ok(createResponse(200, "Success", response));
    }

    @GetMapping("/instance-info")
    public ResponseEntity<Map<String, Object>> getInstanceInfoEndpoint() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "catalog-service");
        response.put("instance", getInstanceInfo());
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(createResponse(200, "Success", response));
    }

    @GetMapping("/health/lb")
    public ResponseEntity<Map<String, Object>> loadBalancerHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "catalog-service");
        health.put("instance", getInstanceInfo());
        health.put("timestamp", System.currentTimeMillis());
        health.put("message", "Ready for load balancing");
        return ResponseEntity.ok(createResponse(200, "Service is running", health));
    }

    @GetMapping("/port")
    public ResponseEntity<Map<String, Object>> getPort() {
        logLoadBalancedRequest("getPort", "");

        Map<String, Object> response = new HashMap<>();
        response.put("service", "catalog-service");

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

    // ==================== 健康检查 ====================

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "catalog-service");
        health.put("instance", getInstanceInfo());
        health.put("timestamp", System.currentTimeMillis());
        health.put("message", "Catalog Service is healthy");
        return ResponseEntity.ok(createResponse(200, "Service is running", health));
    }
}