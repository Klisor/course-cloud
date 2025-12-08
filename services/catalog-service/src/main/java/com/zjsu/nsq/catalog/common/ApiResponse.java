package com.zjsu.nsq.catalog.common;

/**
 * 统一接口响应格式：所有 catalog-service 接口返回该对象
 * 结构：code（状态码）+ message（提示）+ data（业务数据）
 */
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;

    // 私有构造：通过静态工厂方法创建，避免随意修改字段
    private ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // 静态工厂：成功响应（带业务数据，如课程列表、单个课程）
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "操作成功", data);
    }

    // 静态工厂：成功响应（无业务数据，如删除、更新成功）
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(200, "操作成功", null);
    }

    // 静态工厂：错误响应（自定义状态码和提示，如404、409）
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    // 静态工厂：默认错误响应（500系统错误）
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(500, message, null);
    }

    // Getter方法（JSON序列化必须，无需Setter）
    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}