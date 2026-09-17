package com.cunshang.redisadvanced.common;

/**
 * 统一 API 响应格式
 */
public record ApiResponse<T>(String code, String message, T data) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
                ResultCode.SUCCESS.getCode(),
                ResultCode.SUCCESS.getMessage(),
                data
        );
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(
                ResultCode.SUCCESS.getCode(),
                ResultCode.SUCCESS.getMessage(),
                null
        );
    }

    public static ApiResponse<Void> fail(ResultCode resultCode) {
        return new ApiResponse<>(
                resultCode.getCode(),
                resultCode.getMessage(),
                null
        );
    }

    public static ApiResponse<Void> fail(ResultCode resultCode, String message) {
        return new ApiResponse<>(
                resultCode.getCode(),
                message,
                null
        );
    }
}