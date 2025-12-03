package com.zjsu.nsq.enrollment.exception;

/**
 * 自定义异常：资源不存在（学生、课程、选课记录等找不到时抛出）
 */
public class ResourceNotFoundException extends RuntimeException {

    // 构造方法：接收资源类型（如"Student"）和资源ID，生成友好错误信息
    public ResourceNotFoundException(String resourceName, String resourceId) {
        super(resourceName + " not found with id: " + resourceId);
    }

    // 重载构造方法：支持自定义完整错误信息
    public ResourceNotFoundException(String message) {
        super(message);
    }
}