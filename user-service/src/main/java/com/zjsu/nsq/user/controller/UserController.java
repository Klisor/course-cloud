package com.zjsu.nsq.user.controller;

import com.zjsu.nsq.user.model.User;
import com.zjsu.nsq.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    private Map<String, Object> createResponse(int code, String msg, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", code);
        response.put("message", msg);
        response.put("data", data);
        return response;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list() {
        return ResponseEntity.ok(createResponse(200, "Success", service.findAll()));
    }

    @GetMapping("/{identifier}")
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable String identifier) {
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
            // 这里会抛出异常，由全局异常处理器处理
            throw new com.zjsu.nsq.user.service.UserService.UserNotFoundException(
                    "用户不存在: " + identifier);
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody User u) {
        User createdUser = service.create(u);
        return ResponseEntity.ok(createResponse(200, "用户创建成功", createdUser));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id, @RequestBody User u) {
        User updatedUser = service.update(id, u);
        return ResponseEntity.ok(createResponse(200, "用户更新成功", updatedUser));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(createResponse(200, "用户删除成功", null));
    }

    @GetMapping("/by-userid/{userId}")
    public ResponseEntity<Map<String, Object>> getByUserId(@PathVariable String userId) {
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
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "User Service is running");
        response.put("timestamp", System.currentTimeMillis());
        response.put("service", "user-service");
        return ResponseEntity.ok(response);
    }
}