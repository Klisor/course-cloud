package com.zjsu.nsq.enrollment.controller;

import com.zjsu.nsq.enrollment.exception.ResourceNotFoundException;
import com.zjsu.nsq.enrollment.model.Enrollment;
import com.zjsu.nsq.enrollment.model.EnrollmentStatus;
import com.zjsu.nsq.enrollment.service.EnrollmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {
    // 你的变量名是 service，不是 enrollmentService（修复变量名不一致错误）
    private final EnrollmentService service;

    public EnrollmentController(EnrollmentService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list() {
        try {
            List<Enrollment> enrollments = service.findAll();
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "Success");
            response.put("data", enrollments);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "Internal server error: " + e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<Map<String, Object>> byCourse(@PathVariable String courseId) {
        try {
            List<Enrollment> enrollments = service.findByCourse(courseId);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "Success");
            response.put("data", enrollments);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "Internal server error: " + e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<Map<String, Object>> byStudent(@PathVariable Long studentId) {
        try {
            List<Enrollment> enrollments = service.findByStudent(studentId);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "Success");
            response.put("data", enrollments);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "Internal server error: " + e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Map<String, Object>> byStatus(@PathVariable String status) {
        try {
            List<Enrollment> enrollments = service.findByStatus(
                    EnrollmentStatus.valueOf(status.toUpperCase())
            );
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "Success");
            response.put("data", enrollments);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 400);
            response.put("message", "Invalid status: " + e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(400).body(response);
        }
    }

    // 🌟 修复1：变量名 enrollmentService → service（和注入的变量名一致）
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> unenroll(@PathVariable Long id) {
        try {
            service.unenroll(id); // 这里改了！
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "退课成功");
            response.put("data", null);
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 404);
            response.put("message", e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(404).body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "退课失败：" + e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> enroll(@RequestBody Map<String, Object> request) {
        try {
            String courseId = (String) request.get("courseId");
            Long studentId = Long.valueOf(request.get("studentId").toString());

            // 验证请求参数
            if (courseId == null || courseId.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("code", 400);
                response.put("message", "courseId 不能为空");
                response.put("data", null);
                return ResponseEntity.status(400).body(response);
            }

            if (studentId == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("code", 400);
                response.put("message", "studentId 不能为空");
                response.put("data", null);
                return ResponseEntity.status(400).body(response);
            }

            Enrollment result = service.enroll(courseId, studentId);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 201);
            response.put("message", "选课成功");
            response.put("data", result);
            return ResponseEntity.status(201).body(response);
        } catch (EnrollmentService.DuplicateEnrollmentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 409);
            response.put("message", e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(409).body(response);
        } catch (EnrollmentService.CourseFullException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 409);
            response.put("message", e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(409).body(response);
        } catch (EnrollmentService.CourseNotFoundException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 404);
            response.put("message", e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(404).body(response);
        } catch (EnrollmentService.StudentNotFoundException e) {
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

    // 基于学生和课程ID的退课端点
    @PostMapping("/drop")
    public ResponseEntity<Map<String, Object>> dropByStudentAndCourse(@RequestBody Map<String, Object> request) {
        try {
            String courseId = (String) request.get("courseId");
            Long studentId = Long.valueOf(request.get("studentId").toString());

            // 验证请求参数
            if (courseId == null || courseId.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("code", 400);
                response.put("message", "courseId 不能为空");
                response.put("data", null);
                return ResponseEntity.status(400).body(response);
            }

            if (studentId == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("code", 400);
                response.put("message", "studentId 不能为空");
                response.put("data", null);
                return ResponseEntity.status(400).body(response);
            }

            Enrollment result = service.dropByStudentAndCourse(studentId, courseId);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "退课成功");
            response.put("data", result);
            return ResponseEntity.ok(response);
        } catch (EnrollmentService.EnrollmentNotFoundException e) {
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

    @PostMapping("/{enrollmentId}/drop")
    public ResponseEntity<Map<String, Object>> drop(@PathVariable Long enrollmentId) {
        try {
            Enrollment result = service.drop(enrollmentId);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "退课成功");
            response.put("data", result);
            return ResponseEntity.ok(response);
        } catch (EnrollmentService.EnrollmentNotFoundException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 404);
            response.put("message", e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(404).body(response);
        } catch (EnrollmentService.InvalidEnrollmentOperationException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 400);
            response.put("message", e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(400).body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "Internal server error: " + e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/{enrollmentId}/complete")
    public ResponseEntity<Map<String, Object>> complete(@PathVariable Long enrollmentId) {
        try {
            Enrollment result = service.complete(enrollmentId);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "课程完成");
            response.put("data", result);
            return ResponseEntity.ok(response);
        } catch (EnrollmentService.EnrollmentNotFoundException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 404);
            response.put("message", e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(404).body(response);
        } catch (EnrollmentService.InvalidEnrollmentOperationException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 400);
            response.put("message", e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(400).body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "Internal server error: " + e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(500).body(response);
        }
    }

    // 🌟 修复2：删除重复的 @DeleteMapping("/{id}")，修改路径为 /cancel/{id}（避免映射冲突）
    @DeleteMapping("/cancel/{id}")
    public ResponseEntity<Map<String, Object>> cancel(@PathVariable Long id) {
        try {
            service.delete(id);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "取消选课成功");
            response.put("data", null);
            return ResponseEntity.ok(response);
        } catch (EnrollmentService.EnrollmentNotFoundException e) {
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

    @GetMapping("/stats/course/{courseId}")
    public ResponseEntity<Map<String, Object>> courseStats(@PathVariable String courseId) {
        try {
            Long activeCount = service.countActiveEnrollmentsByCourse(courseId);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "Success");
            response.put("data", Map.of("activeEnrollments", activeCount));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "Internal server error: " + e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/stats/student/{studentId}")
    public ResponseEntity<Map<String, Object>> studentStats(@PathVariable Long studentId) {
        try {
            Long activeCount = service.countActiveEnrollmentsByStudent(studentId);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "Success");
            response.put("data", Map.of("activeEnrollments", activeCount));
            return ResponseEntity.ok(response);
        } catch (EnrollmentService.StudentNotFoundException e) {
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
}