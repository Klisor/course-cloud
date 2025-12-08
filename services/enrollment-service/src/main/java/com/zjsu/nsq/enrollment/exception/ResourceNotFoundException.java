package com.zjsu.nsq.enrollment.exception;


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