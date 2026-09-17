package com.cunshang.redisadvanced.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

@Service
public class RedisFoundationService {

    private final StringRedisTemplate redisTemplate;

    public RedisFoundationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }


    /**
     * 写入 String 数据。
     * <p>
     * 对应 Redis：
     * <p>
     * SET key value
     */
    public void set(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 获取 String 数据。
     * <p>
     * 对应 Redis：
     * <p>
     * GET key
     */
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }


    /**
     * 写入带 TTL 的数据。
     * <p>
     * 对应：
     * <p>
     * SET key value EX seconds
     */
    public void setWithTtl(String key, String value, long seconds) {
        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(seconds));
    }

    /**
     * 原子自增。
     * <p>
     * 对应：
     * <p>
     * INCR key
     */
    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    /**
     * 查询剩余 TTL。
     * <p>
     * 对应：
     * <p>
     * TTL key
     */
    public Long ttl(String key) {
        return redisTemplate.getExpire(key);
    }

    /**
     * 删除 Key。
     * <p>
     * 对应：
     * <p>
     * DEL key
     */
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    /**
     * Hash：写入一个字段
     * <p>
     * HSET key field value
     */
    public void hashSet(
            String key,
            String field,
            String value
    ) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    /**
     * Hash：获取一个字段
     * <p>
     * HGET key field
     */
    public Object hashGet(
            String key,
            String field
    ) {
        return redisTemplate.opsForHash().get(key, field);
    }

    /**
     * Hash：获取全部字段
     * <p>
     * HGETALL key
     */
    public Map<Object, Object> hashGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * Hash：删除字段
     * <p>
     * HDEL key field
     */
    public Long hashDelete(
            String key,
            String field
    ) {
        return redisTemplate.opsForHash().delete(key, field);
    }
}