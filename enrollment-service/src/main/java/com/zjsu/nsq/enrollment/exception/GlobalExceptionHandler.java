package com.zjsu.nsq.enrollment.exception;

import com.zjsu.nsq.enrollment.common.ApiResponse;
import com.zjsu.nsq.enrollment.service.EnrollmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器：捕获所有接口抛出的异常，统一返回ApiResponse格式
 */
@RestControllerAdvice // 标识为全局异常处理
public class GlobalExceptionHandler {

    // 捕获：资源不存在异常（ResourceNotFoundException）
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException e) {
        ApiResponse<Void> response = ApiResponse.error(HttpStatus.NOT_FOUND.value(), e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    // 捕获：EnrollmentService中的内部异常（重复选课、课程已满等）
    @ExceptionHandler({
            EnrollmentService.DuplicateEnrollmentException.class,
            EnrollmentService.CourseFullException.class,
            EnrollmentService.InvalidEnrollmentOperationException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleEnrollmentBusinessException(RuntimeException e) {
        ApiResponse<Void> response = ApiResponse.error(HttpStatus.CONFLICT.value(), e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    // 捕获：参数错误异常（比如传入null、格式错误）
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
        ApiResponse<Void> response = ApiResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // 捕获：所有其他未定义的异常（兜底）
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAllUncaughtException(Exception e) {
        // 打印异常栈（便于调试）
        e.printStackTrace();
        ApiResponse<Void> response = ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "系统错误：" + e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}