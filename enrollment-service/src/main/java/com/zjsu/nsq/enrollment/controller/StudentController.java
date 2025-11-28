package com.zjsu.nsq.enrollment.controller;

import com.zjsu.nsq.enrollment.model.Student;
import com.zjsu.nsq.enrollment.service.StudentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService service;

    public StudentController(StudentService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list() {
        try {
            List<Student> students = service.findAll();
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "Success");
            response.put("data", students);
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
            Student student = service.findById(id)
                    .orElseThrow(() -> new RuntimeException("学生不存在"));

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "Success");
            response.put("data", student);
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

    @GetMapping("/student-id/{studentId}")
    public ResponseEntity<Map<String, Object>> getByStudentId(@PathVariable String studentId) {
        try {
            Student student = service.findByStudentId(studentId)
                    .orElseThrow(() -> new RuntimeException("学生不存在"));

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "Success");
            response.put("data", student);
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

    @GetMapping("/email/{email}")
    public ResponseEntity<Map<String, Object>> getByEmail(@PathVariable String email) {
        try {
            Student student = service.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("学生不存在"));

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "Success");
            response.put("data", student);
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
    public ResponseEntity<Map<String, Object>> create(@RequestBody Student student) {
        try {
            // 添加基本参数验证
            if (student.getStudentId() == null || student.getStudentId().trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("code", 400);
                response.put("message", "学号不能为空");
                response.put("data", null);
                return ResponseEntity.status(400).body(response);
            }

            if (student.getName() == null || student.getName().trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("code", 400);
                response.put("message", "学生姓名不能为空");
                response.put("data", null);
                return ResponseEntity.status(400).body(response);
            }

            Student createdStudent = service.create(student);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 201);
            response.put("message", "学生创建成功");
            response.put("data", createdStudent);
            return ResponseEntity.status(201).body(response);
        } catch (StudentService.StudentAlreadyExistsException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 409);
            response.put("message", e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(409).body(response);
        } catch (StudentService.InvalidStudentDataException e) {
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

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id, @RequestBody Student student) {
        try {
            Student updatedStudent = service.update(id, student);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "学生信息更新成功");
            response.put("data", updatedStudent);
            return ResponseEntity.ok(response);
        } catch (StudentService.StudentNotFoundException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 404);
            response.put("message", e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(404).body(response);
        } catch (StudentService.StudentAlreadyExistsException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 409);
            response.put("message", e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(409).body(response);
        } catch (StudentService.InvalidStudentDataException e) {
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

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "删除成功");
            response.put("data", null);
            return ResponseEntity.ok(response);
        } catch (StudentService.StudentNotFoundException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 404);
            response.put("message", e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(404).body(response);
        } catch (StudentService.StudentHasActiveEnrollmentsException e) {
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

    @GetMapping("/search/major/{major}")
    public ResponseEntity<Map<String, Object>> findByMajor(@PathVariable String major) {
        try {
            List<Student> students = service.findByMajor(major);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "Success");
            response.put("data", students);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "Internal server error: " + e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/search/grade/{grade}")
    public ResponseEntity<Map<String, Object>> findByGrade(@PathVariable Integer grade) {
        try {
            List<Student> students = service.findByGrade(grade);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "Success");
            response.put("data", students);
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