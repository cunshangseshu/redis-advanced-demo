package com.cunshang.redisadvanced.service;

import com.cunshang.redisadvanced.config.RabbitMqConfig;
import com.cunshang.redisadvanced.entity.UserProfile;
import com.cunshang.redisadvanced.mapper.UserProfileMapper;
import com.cunshang.redisadvanced.model.RedisUserProfile;
import com.cunshang.redisadvanced.model.message.CacheInvalidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class CacheAsideService {

    private static final Logger log = LoggerFactory.getLogger(CacheAsideService.class);

    private final RedisTemplate<String, Object> objectRedisTemplate;
    private final UserProfileMapper userProfileMapper;
    private static final String NULL_CACHE_VALUE = "NULL";
    private static final String LOCK_KEY_PREFIX = "lock:user:profile:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);
    private static final Duration CACHE_BASE_TTL = Duration.ofMinutes(10);
    private static final long CACHE_TTL_JITTER_SECONDS = 120;
    private static final int CACHE_DELETE_MAX_ATTEMPTS = 3;
    private static final long CACHE_DELETE_RETRY_DELAY_MS = 200;
    private final RabbitTemplate rabbitTemplate;

    public CacheAsideService(@Qualifier("objectRedisTemplate") RedisTemplate<String, Object> objectRedisTemplate, UserProfileMapper userProfileMapper, RabbitTemplate rabbitTemplate) {
        this.objectRedisTemplate = objectRedisTemplate;
        this.userProfileMapper = userProfileMapper;
        this.rabbitTemplate = rabbitTemplate;
    }

    public RedisUserProfile getUser(Long id) {
        String key = "user:profile:" + id;
        String lockKey = LOCK_KEY_PREFIX + id;
        // 1. 第一次查询缓存
        Object cachedValue = objectRedisTemplate.opsForValue().get(key);
        if (cachedValue != null) {
            log.info("CACHE HIT, key={}", key);
            if (NULL_CACHE_VALUE.equals(cachedValue)) {
                log.info("CACHE NULL HIT, key={}", key);
                return null;
            }
            if (cachedValue instanceof RedisUserProfile profile) {
                return profile;
            }
            throw new IllegalStateException("Redis 缓存数据类型异常, key=" + key);
        }
        log.info("CACHE MISS, key={}", key);
        // 2. 没抢到锁的线程等待
        while (!tryLock(lockKey)) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("等待缓存重建锁时线程被中断", e);
            }
            // 等待期间重新检查缓存
            cachedValue = objectRedisTemplate.opsForValue().get(key);
            if (cachedValue != null) {
                log.info("CACHE HIT AFTER WAIT, key={}", key);
                if (NULL_CACHE_VALUE.equals(cachedValue)) {
                    return null;
                }
                if (cachedValue instanceof RedisUserProfile profile) {
                    return profile;
                }
                throw new IllegalStateException("Redis 缓存数据类型异常, key=" + key);
            }
        }
        log.info("LOCK ACQUIRED, lockKey={}", lockKey);
        try {
            // 3. Double Check
            cachedValue = objectRedisTemplate.opsForValue().get(key);
            if (cachedValue != null) {
                log.info("CACHE HIT AFTER LOCK, key={}", key);
                if (NULL_CACHE_VALUE.equals(cachedValue)) {
                    return null;
                }
                if (cachedValue instanceof RedisUserProfile profile) {
                    return profile;
                }
                throw new IllegalStateException("Redis 缓存数据类型异常, key=" + key);
            }
            // 4. 真正需要回源数据库
            log.info("QUERY DATABASE, userId={}", id);
            // 临时用于放大缓存击穿实验
            /*try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }*/
            UserProfile entity = userProfileMapper.selectById(id);
            // 5. 防缓存穿透：数据库也不存在
            if (entity == null) {
                objectRedisTemplate.opsForValue().set(key, NULL_CACHE_VALUE, Duration.ofMinutes(2));
                log.info("CACHE NULL REBUILD, key={}", key);
                return null;
            }
            RedisUserProfile profile = new RedisUserProfile(entity.getId(), entity.getUsername(), entity.getAge());
            // 6. 重建正常缓存
            Duration ttl = buildCacheTtl();
            objectRedisTemplate.opsForValue().set(key, profile, ttl);
            log.info("CACHE REBUILD, key={}, ttl={}s", key, ttl.toSeconds());
            return profile;
        } finally {
            // 7. 无论成功还是异常都释放锁
            unlock(lockKey);
            log.info("LOCK RELEASED, lockKey={}", lockKey);
        }
    }

    /**
     * 尝试获取缓存重建锁。
     * redis的cli语句对应的是：SET lock:user:profile:1001 LOCK NX EX 10
     */
    private boolean tryLock(String lockKey) {
        Boolean success = objectRedisTemplate.opsForValue().setIfAbsent(lockKey, "LOCK", LOCK_TTL);
        return Boolean.TRUE.equals(success);
    }

    /**
     * 释放缓存重建锁。
     * <p>
     * 当前是教学版实现。
     * 后续分布式锁章节会升级为：
     * 唯一锁标识 + Lua 原子释放 / Redisson。
     */
    private void unlock(String lockKey) {
        objectRedisTemplate.delete(lockKey);
    }

    private Duration buildCacheTtl() {
        long jitterSeconds = ThreadLocalRandom.current().nextLong(0, CACHE_TTL_JITTER_SECONDS + 1);
        return CACHE_BASE_TTL.plusSeconds(jitterSeconds);
    }


    public void updateUser(Long id, String username, Integer age) {
        String key = "user:profile:" + id;
        UserProfile entity = new UserProfile();
        entity.setId(id);
        entity.setUsername(username);
        entity.setAge(age);
        log.info("UPDATE DATABASE, userId={}, username={}, age={}", id, username, age);
        int rows = userProfileMapper.updateById(entity); // UPDATE MySQL
        if (rows == 0) {
            throw new IllegalStateException("用户不存在, userId=" + id);
        }
        log.info("DATABASE UPDATED, userId={}", id);
        try {
            deleteCacheWithRetry(key);
        } catch (IllegalStateException e) {
            log.warn("SYNC CACHE INVALIDATION FAILED, SEND MQ, key={}", key);
            CacheInvalidationMessage message = new CacheInvalidationMessage(key);
            rabbitTemplate.convertAndSend(RabbitMqConfig.CACHE_INVALIDATION_QUEUE, message);
            log.info("CACHE INVALIDATION MESSAGE SENT, key={}", key);
        } // delete redis

        // 模拟 Redis 缓存删除失败
        // log.info("DATABASE UPDATED, userId={}", id);
        // log.error("SIMULATED CACHE DELETE FAILURE, key={}", key);
        // throw new IllegalStateException("模拟 Redis 缓存删除失败");
    }


    private void deleteCacheWithRetry(String key) {
        for (int attempt = 1; attempt <= CACHE_DELETE_MAX_ATTEMPTS; attempt++) {
            try {
                // 模拟第一次 Redis 删除失败
                /*if (attempt == 1) {
                    log.error("SIMULATED CACHE DELETE FAILURE, key={}, attempt={}", key, attempt);
                    throw new IllegalStateException("模拟第一次 Redis 删除失败");
                }*/
                if (attempt <= 3) {
                    log.error("SIMULATED CACHE DELETE FAILURE, key={}, attempt={}", key, attempt);
                    throw new IllegalStateException("模拟 Redis 删除失败, attempt=" + attempt);
                }
                Boolean deleted = objectRedisTemplate.delete(key);
                log.info("CACHE INVALIDATED, key={}, existed={}, attempt={}",
                        key,
                        deleted,
                        attempt
                );
                return;
            } catch (Exception e) {
                log.warn("CACHE DELETE FAILED, key={}, attempt={}/{}",
                        key,
                        attempt,
                        CACHE_DELETE_MAX_ATTEMPTS,
                        e
                );
                if (attempt == CACHE_DELETE_MAX_ATTEMPTS) {
                    throw new IllegalStateException("缓存删除重试仍然失败, key=" + key, e);
                }
                try {
                    Thread.sleep(CACHE_DELETE_RETRY_DELAY_MS);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("缓存删除重试等待被中断", interruptedException);
                }
            }
        }
    }
}