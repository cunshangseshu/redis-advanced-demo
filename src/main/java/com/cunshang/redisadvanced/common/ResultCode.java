package com.cunshang.redisadvanced.common;

import org.springframework.http.HttpStatus;

/**
 * 统一业务状态码
 */
public enum ResultCode {
    SUCCESS("SUCCESS", "success", HttpStatus.OK),
    BAD_REQUEST("BAD_REQUEST", "请求参数错误", HttpStatus.BAD_REQUEST),
    NOT_FOUND("NOT_FOUND", "资源不存在", HttpStatus.NOT_FOUND),
    CONFLICT("CONFLICT", "资源状态冲突", HttpStatus.CONFLICT),
    REDIS_KEY_TYPE_CONFLICT("REDIS_KEY_TYPE_CONFLICT", "Redis Key 数据类型冲突", HttpStatus.CONFLICT),
    REDIS_UNAVAILABLE("REDIS_UNAVAILABLE", "Redis 服务暂时不可用", HttpStatus.SERVICE_UNAVAILABLE),
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "服务器内部错误", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ResultCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}