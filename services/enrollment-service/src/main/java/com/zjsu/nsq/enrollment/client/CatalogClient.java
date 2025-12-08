package com.zjsu.nsq.enrollment.client;

import com.zjsu.nsq.enrollment.dto.CourseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "catalog-service", fallback = CatalogClientFallback.class)
public interface CatalogClient {
    @GetMapping("/api/courses/{id}")
    CourseDto getCourse(@PathVariable Long id);

    @PutMapping("/api/courses/{id}/enrolled")
    void updateCourseEnrollment(@PathVariable Long id, @RequestParam int count);
}