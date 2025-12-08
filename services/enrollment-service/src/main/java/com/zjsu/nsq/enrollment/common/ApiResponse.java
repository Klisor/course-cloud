package com.zjsu.nsq.enrollment.common;

/**
 * 统一接口响应格式：所有接口返回该对象，结构一致（code+message+data）
 */
public class ApiResponse<T> {
    // 响应状态码（200=成功，400=参数错误，404=资源不存在，500=系统错误）
    private int code;
    // 响应提示信息
    private String message;
    // 响应数据（成功时返回业务数据，失败时为null）
    private T data;

    // 私有构造方法：通过静态工厂方法创建对象
    private ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // 静态工厂方法：成功响应（带数据）
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "操作成功", data);
    }

    // 静态工厂方法：成功响应（无数据）
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(200, "操作成功", null);
    }

    // 静态工厂方法：错误响应（自定义状态码和信息）
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    // 静态工厂方法：错误响应（默认500系统错误）
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(500, message, null);
    }

    // Getter方法（序列化需要，无需Setter）
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