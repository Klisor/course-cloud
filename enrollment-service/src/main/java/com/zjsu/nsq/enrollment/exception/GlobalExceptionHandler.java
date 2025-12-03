package com.zjsu.nsq.enrollment.exception;

import com.zjsu.nsq.enrollment.service.EnrollmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private Map<String, Object> errorResponse(int code, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", code);
        response.put("message", message);
        response.put("data", null);
        return response;
    }

    @ExceptionHandler(EnrollmentService.EnrollmentNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEnrollmentNotFound(EnrollmentService.EnrollmentNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorResponse(404, e.getMessage()));
    }

    @ExceptionHandler(EnrollmentService.DuplicateEnrollmentException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateEnrollment(EnrollmentService.DuplicateEnrollmentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorResponse(400, e.getMessage()));
    }

    @ExceptionHandler(EnrollmentService.CourseFullException.class)
    public ResponseEntity<Map<String, Object>> handleCourseFull(EnrollmentService.CourseFullException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorResponse(400, e.getMessage()));
    }

    @ExceptionHandler(EnrollmentService.StudentNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleStudentNotFound(EnrollmentService.StudentNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorResponse(404, e.getMessage()));
    }

    @ExceptionHandler(EnrollmentService.CourseNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleCourseNotFound(EnrollmentService.CourseNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorResponse(404, e.getMessage()));
    }

    @ExceptionHandler(EnrollmentService.InvalidEnrollmentOperationException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidOperation(EnrollmentService.InvalidEnrollmentOperationException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorResponse(400, e.getMessage()));
    }

    @ExceptionHandler(EnrollmentService.ServiceCallException.class)
    public ResponseEntity<Map<String, Object>> handleServiceCallException(EnrollmentService.ServiceCallException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse(500, e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse(500, "系统错误: " + e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorResponse(400, e.getMessage()));
    }
}