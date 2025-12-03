package com.zjsu.nsq.catalog.controller;

import com.zjsu.nsq.catalog.model.Course;
import com.zjsu.nsq.catalog.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/courses")
public class CourseController {
    private final CourseService service;
    @Autowired  // 使用@Autowired注入
    private Environment environment;

    @Autowired
    public CourseController(CourseService service) {
        this.service = service;
    }
    /**
     * 获取服务实例信息
     */
    @GetMapping("/port")
    public ResponseEntity<Map<String, Object>> getPort() {
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

        return ResponseEntity.ok(response);
    }
    @GetMapping
    public ResponseEntity<Map<String, Object>> list() {
        try {
            List<Course> courses = service.findAll();
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

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable Long id) {
        try {
            Course course = service.findById(id)
                    .orElseThrow(() -> new RuntimeException("课程不存在"));

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "Success");
            response.put("data", course);
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

    @GetMapping("/code/{code}")
    public ResponseEntity<Map<String, Object>> getByCode(@PathVariable String code) {
        try {
            Course course = service.findByCode(code)
                    .orElseThrow(() -> new RuntimeException("课程不存在"));

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "Success");
            response.put("data", course);
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

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Course course) {
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

    // 🔥 新增：更新课程选课人数接口（供 enrollment-service 调用）
    // 路径：/api/courses/{id}/enrolled，支持 PUT 请求，参数通过 URL 传递
    @PutMapping("/{id}/enrolled")
    public ResponseEntity<Map<String, Object>> updateEnrolledCount(
            @PathVariable("id") Long courseId,
            @RequestParam("count") Integer newEnrolledCount) {

        try {
            // 使用新的 updateEnrolledCount 方法
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
    // 🔥 新增：减少选课人数接口（原子操作）
    @PostMapping("/{id}/drop")
    public ResponseEntity<Map<String, Object>> dropCourse(@PathVariable("id") Long courseId) {
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

    // 🔥 新增：检查课程容量接口
    @GetMapping("/{id}/capacity")
    public ResponseEntity<Map<String, Object>> getCourseCapacity(@PathVariable("id") Long courseId) {
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
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
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

    @GetMapping("/search/title/{title}")
    public ResponseEntity<Map<String, Object>> findByTitle(@PathVariable String title) {
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

}