// src/main/java/com/zjsu/nsq/enrollment/client/UserClient.java
package com.zjsu.nsq.enrollment.client;

import com.zjsu.nsq.enrollment.dto.StudentDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", fallback = UserClientFallback.class)
public interface UserClient {
    @GetMapping("/api/users/students/{id}")
    StudentDto getStudent(@PathVariable Long id);
}