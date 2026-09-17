package com.cunshang.redisadvanced.controller;

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
    public String set(@RequestParam String key, @RequestParam String value) {
        redisService.set(key, value);
        return "OK";

    }


    /**
     * GET
     */
    @GetMapping("/string")
    public String get(@RequestParam String key) {
        return redisService.get(key);
    }


    /**
     * SET + TTL
     */
    @PostMapping("/string/ttl")
    public String setWithTtl(@RequestParam String key, @RequestParam String value, @RequestParam long seconds) {
        redisService.setWithTtl(key, value, seconds);
        return "OK";
    }


    /**
     * INCR
     */
    @PostMapping("/counter/increment")
    public Long increment(@RequestParam String key) {
        return redisService.increment(key);
    }


    /**
     * TTL
     */
    @GetMapping("/ttl")
    public Long ttl(@RequestParam String key) {
        return redisService.ttl(key);
    }


    /**
     * DEL
     */
    @DeleteMapping("/key")
    public Boolean delete(@RequestParam String key) {
        return redisService.delete(key);
    }

    /**
     * Hash - HSET
     */
    @PostMapping("/hash")
    public String hashSet(@RequestParam String key, @RequestParam String field, @RequestParam String value) {
        redisService.hashSet(key, field, value);
        return "OK";
    }


    /**
     * Hash - HGET
     */
    @GetMapping("/hash")
    public Object hashGet(@RequestParam String key, @RequestParam String field) {
        return redisService.hashGet(key, field);
    }


    /**
     * Hash - HGETALL
     */
    @GetMapping("/hash/all")
    public Map<Object, Object> hashGetAll(@RequestParam String key) {
        return redisService.hashGetAll(key);
    }


    /**
     * Hash - HDEL
     */
    @DeleteMapping("/hash")
    public Long hashDelete(@RequestParam String key, @RequestParam String field) {
        return redisService.hashDelete(key, field);
    }
}