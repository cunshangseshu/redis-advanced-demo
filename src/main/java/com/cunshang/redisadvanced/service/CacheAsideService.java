package com.cunshang.redisadvanced.service;

import com.cunshang.redisadvanced.entity.UserProfile;
import com.cunshang.redisadvanced.mapper.UserProfileMapper;
import com.cunshang.redisadvanced.model.RedisUserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class CacheAsideService {

    private static final Logger log = LoggerFactory.getLogger(CacheAsideService.class);

    private final RedisTemplate<String, Object> objectRedisTemplate;
    private final UserProfileMapper userProfileMapper;

    public CacheAsideService(
            @Qualifier("objectRedisTemplate")
            RedisTemplate<String, Object> objectRedisTemplate,
            UserProfileMapper userProfileMapper
    ) {
        this.objectRedisTemplate = objectRedisTemplate;
        this.userProfileMapper = userProfileMapper;
    }

    public RedisUserProfile getUser(Long id) {
        String key = "user:profile:" + id;
        // 1. 查询 Redis
        Object cachedValue = objectRedisTemplate.opsForValue().get(key);
        if (cachedValue != null) {
            log.info("CACHE HIT, key={}", key);
            if (cachedValue instanceof RedisUserProfile profile) {
                return profile;
            }
            throw new IllegalStateException("Redis 缓存数据类型异常, key=" + key);
        }
        // 2. Cache Miss
        log.info("CACHE MISS, key={}", key);
        // 3. 查询 MySQL
        log.info("QUERY DATABASE, userId={}", id);
        UserProfile entity = userProfileMapper.selectById(id);
        if (entity == null) {
            return null;
        }
        RedisUserProfile profile = new RedisUserProfile(entity.getId(), entity.getUsername(), entity.getAge());
        // 4. 回填 Redis
        objectRedisTemplate.opsForValue().set(key, profile, Duration.ofMinutes(10));
        log.info("CACHE REBUILD, key={}", key);
        return profile;
    }
}