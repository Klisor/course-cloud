package com.zjsu.nsq.catalog.exception;

import com.zjsu.nsq.catalog.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * catalog-service 全局异常处理器：捕获所有接口异常，统一返回 ApiResponse 格式
 */
@RestControllerAdvice // 标识为全局异常处理，作用于所有 @RestController
public class GlobalExceptionHandler {

    // 🌟 捕获：课程不存在异常（ResourceNotFoundException）
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException e) {
        ApiResponse<Void> response = ApiResponse.error(HttpStatus.NOT_FOUND.value(), e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    // 捕获：参数错误异常（如传入null、格式错误，比如课程容量为负数）
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
        ApiResponse<Void> response = ApiResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // 捕获：业务冲突异常（如创建已存在的课程、更新不存在的课程）
    @ExceptionHandler({RuntimeException.class})
    public ResponseEntity<ApiResponse<Void>> handleBusinessConflict(RuntimeException e) {
        ApiResponse<Void> response = ApiResponse.error(HttpStatus.CONFLICT.value(), e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    // 🌟 兜底：捕获所有未定义的异常（避免返回默认500错误页面）
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAllUncaughtException(Exception e) {
        e.printStackTrace(); // 打印异常栈，便于调试
        ApiResponse<Void> response = ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "系统错误：" + e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}