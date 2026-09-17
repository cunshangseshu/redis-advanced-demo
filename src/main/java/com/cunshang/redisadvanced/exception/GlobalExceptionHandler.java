package com.cunshang.redisadvanced.exception;

import com.cunshang.redisadvanced.common.ApiResponse;
import com.cunshang.redisadvanced.common.ResultCode;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ResultCode resultCode = e.getResultCode();
        return ResponseEntity
                .status(resultCode.getHttpStatus())
                .body(ApiResponse.fail(resultCode, e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.fail(ResultCode.BAD_REQUEST, e.getMessage()));
    }

    @ExceptionHandler(RedisConnectionFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleRedisConnectionFailure() {
        return ResponseEntity
                .status(ResultCode.REDIS_UNAVAILABLE.getHttpStatus())
                .body(ApiResponse.fail(ResultCode.REDIS_UNAVAILABLE));
    }

    @ExceptionHandler(RedisSystemException.class)
    public ResponseEntity<ApiResponse<Void>> handleRedisSystemException() {
        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.fail(ResultCode.INTERNAL_SERVER_ERROR));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.fail(ResultCode.INTERNAL_SERVER_ERROR));
    }
}