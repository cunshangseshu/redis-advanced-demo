package com.cunshang.redisadvanced.controller;

import com.cunshang.redisadvanced.common.ApiResponse;
import com.cunshang.redisadvanced.service.RedisFoundationService;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/redis")
public class RedisFoundationController {
    private final RedisFoundationService redisService;

    public RedisFoundationController(RedisFoundationService redisService) {
        this.redisService = redisService;
    }

    /**
     * SET
     * <p>
     * POST
     * /api/redis/string
     * <p>
     * example:
     * <p>
     * key=user:1001:name
     * value=cunshang
     */
    @PostMapping("/string")
    public ApiResponse<Void> set(
            @RequestParam String key,
            @RequestParam String value
    ) {
        redisService.set(key, value);
        //return "OK";
        return ApiResponse.success();
    }

    /**
     * GET
     */
    @GetMapping("/string")
    public ApiResponse<String> get(@RequestParam String key) {
        //return redisService.get(key);
        return ApiResponse.success(redisService.get(key));
    }

    /**
     * SET + TTL
     */
    @PostMapping("/string/ttl")
    public ApiResponse<Void> setWithTtl(
            @RequestParam String key,
            @RequestParam String value,
            @RequestParam long seconds
    ) {
        /*redisService.setWithTtl(key, value, seconds);
        return "OK";*/
        if (seconds <= 0) throw new IllegalArgumentException("你个人才，设置 0 秒？！意义何在啊；");
        redisService.setWithTtl(key, value, seconds);
        return ApiResponse.success();
    }

    /**
     * INCR
     */
    @PostMapping("/counter/increment")
    public ApiResponse<Long> increment(@RequestParam String key) {
        // return redisService.increment(key);
        return ApiResponse.success(redisService.increment(key));
    }

    /**
     * TTL
     */
    @GetMapping("/ttl")
    public ApiResponse<Long> ttl(@RequestParam String key) {
        // return redisService.ttl(key);
        return ApiResponse.success(redisService.ttl(key));
    }

    /**
     * DEL
     */
    @DeleteMapping("/key")
    public ApiResponse<Boolean> delete(@RequestParam String key) {
        // return redisService.delete(key);
        return ApiResponse.success(redisService.delete(key));
    }

    /**
     * Hash - HSET
     */
    @PostMapping("/hash")
    public ApiResponse<Void> hashSet(
            @RequestParam String key,
            @RequestParam String field,
            @RequestParam String value
    ) {
        redisService.hashSet(key, field, value);
        // return "OK";
        return ApiResponse.success();
    }

    /**
     * Hash - HGET
     */
    @GetMapping("/hash")
    public ApiResponse<Object> hashGet(
            @RequestParam String key,
            @RequestParam String field
    ) {
        //return redisService.hashGet(key, field);
        return ApiResponse.success(redisService.hashGet(key, field));
    }

    /**
     * Hash - HGETALL
     */
    @GetMapping("/hash/all")
    public ApiResponse<Map<Object, Object>> hashGetAll(@RequestParam String key) {
        //return redisService.hashGetAll(key);
        return ApiResponse.success(redisService.hashGetAll(key));
    }

    /**
     * Hash - HDEL
     */
    @DeleteMapping("/hash")
    public Long hashDelete(@RequestParam String key, @RequestParam String field) {
        return redisService.hashDelete(key, field);
    }
}