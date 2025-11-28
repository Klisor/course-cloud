package com.zjsu.nsq.catalog.exception;

/**
 * 自定义异常：资源不存在（仅用于 catalog-service，主要捕获「课程找不到」场景）
 */
public class ResourceNotFoundException extends RuntimeException {

    // 核心构造方法：资源类型（如"Course"）+ 资源ID（课程ID）
    public ResourceNotFoundException(String resourceName, String resourceId) {
        super(resourceName + " not found with id: " + resourceId);
    }

    // 重载构造：支持自定义完整错误信息（灵活适配其他场景）
    public ResourceNotFoundException(String message) {
        super(message);
    }
}