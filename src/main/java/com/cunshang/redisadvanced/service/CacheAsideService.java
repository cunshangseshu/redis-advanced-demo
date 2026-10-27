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
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.amqp.rabbit.connection.CorrelationData;

import java.util.List;
import java.util.UUID;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Redis Cache Aside 示例服务：用户缓存查询、缓存重建互斥锁、数据更新与缓存失效。
 *
 * <p>维护时间：2026-10-27 14:32（UTC+08:00）
 * <p>当前学习阶段：Redis 分布式锁（UUID Token + Lua 原子解锁）。
 * <p>读路径：先查 Redis；未命中时抢锁、二次检查、查 MySQL 并重建缓存。
 * <p>写路径：先更新 MySQL，再尝试删除缓存；同步删除连续失败则发送 RabbitMQ 消息。
 *
 * <p>边界：锁 TTL 固定为 10 秒，尚无自动续期或可重入能力；Lua 仅保证
 * 校验 Token 与删除锁的原子性，不保证超时后的业务处理仍受锁保护。
 */
@Service
public class CacheAsideService {

    private static final Logger log = LoggerFactory.getLogger(CacheAsideService.class);

    private final RedisTemplate<String, Object> objectRedisTemplate;
    private final UserProfileMapper userProfileMapper;
    private static final String NULL_CACHE_VALUE = "NULL";
    private static final String LOCK_KEY_PREFIX = "lock:user:profile:";
    // 缓存重建锁最长持有 10 秒：避免异常请求永久占锁；超时业务存在锁提前过期风险。
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);
    /**
     * Lua 解锁脚本：classpath:lua/unlock.lua。
     * KEYS[1] 为锁 Key；ARGV[1] 为请求持有的 UUID Token。
     * 返回 1：Token 匹配且删除成功；返回 0：未删除（过期/不属于本请求）。
     */
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    // 在类初始化时配置 Lua 资源路径与 Long 返回类型；执行由 RedisTemplate 负责。
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("lua/unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    private static final Duration CACHE_BASE_TTL = Duration.ofMinutes(10);
    private static final long CACHE_TTL_JITTER_SECONDS = 120;
    private static final int CACHE_DELETE_MAX_ATTEMPTS = 3;
    private static final long CACHE_DELETE_RETRY_DELAY_MS = 200;
    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    public CacheAsideService(@Qualifier("objectRedisTemplate") RedisTemplate<String, Object> objectRedisTemplate, UserProfileMapper userProfileMapper, RabbitTemplate rabbitTemplate, StringRedisTemplate stringRedisTemplate) {
        this.objectRedisTemplate = objectRedisTemplate;
        this.userProfileMapper = userProfileMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 读取用户信息：缓存命中直接返回，未命中时由持锁请求查询 MySQL 并重建。
     *
     * <p>处理流程：缓存读取 -> UUID 锁竞争 -> Double Check -> MySQL 查询
     * -> 写入正常/空值缓存 -> finally 执行 Lua 原子解锁。
     * <p>抢锁失败时每 50 毫秒重试，并在等待期间检查缓存是否已被其他请求重建。
     * <p>当前未设置最大等待时间；该实现仅用于 Redis 学习和演示。
     *
     * @param id 用户 ID
     * @return 缓存对象；数据库中无此用户时返回 null
     */
    public RedisUserProfile getUser(Long id) {
        String key = "user:profile:" + id;
        String lockKey = LOCK_KEY_PREFIX + id;
        // 1. 首次读 Redis：命中用户对象直接返回；命中空值标记时不查询 MySQL。
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
        // 2. 热点缓存未命中：使用 SET NX + TTL 争抢 UUID 锁；失败请求间隔 50ms 再试。
        String lockToken;
        while ((lockToken = tryLock(lockKey)) == null) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("等待缓存重建锁时线程被中断", e);
            }
            // 等待时复查缓存：如果其他持锁请求已重建，直接读取新缓存，避免多次回源。
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
            // 3. Double Check：获得锁之后再查一次 Redis，避免并发请求重复访问数据库。
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
            // 4. 两次缓存检查均未命中，当前请求持锁执行 MySQL 查询。
            log.info("QUERY DATABASE, userId={}", id);
            // 历史 JMeter 实验：以下 500ms 延迟已被注释，不属于当前执行路径。
            /*try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }*/
            UserProfile entity = userProfileMapper.selectById(id);
            // 5. MySQL 记录不存在：缓存 NULL 标记 2 分钟，防止相同无效 ID 反复穿透数据库。
            if (entity == null) {
                objectRedisTemplate.opsForValue().set(key, NULL_CACHE_VALUE, Duration.ofMinutes(2));
                log.info("CACHE NULL REBUILD, key={}", key);
                return null;
            }
            RedisUserProfile profile = new RedisUserProfile(entity.getId(), entity.getUsername(), entity.getAge());
            // 6. MySQL 查询成功：写入用户缓存，TTL = 10 分钟 + 0~120 秒随机抖动。
            Duration ttl = buildCacheTtl();
            objectRedisTemplate.opsForValue().set(key, profile, ttl);
            log.info("CACHE REBUILD, key={}, ttl={}s", key, ttl.toSeconds());
            return profile;
        } finally {
            // 7. finally 尝试 Lua 解锁；released=false 可能代表锁超时或所有权已变化，不应记录为释放成功。
            boolean released = unlock(lockKey, lockToken);
            log.info("LOCK RELEASE RESULT, lockKey={}, released={}", lockKey, released);
        }
    }

    /**
     * 尝试获取当前用户缓存的重建锁（Redis SET NX + TTL）。
     *
     * <p>每次尝试生成一个 UUID，仅在 SET NX 成功时把它交给调用方作为锁所有权凭证。
     * Redis Key 使用字符串序列化，Value 也是 UUID 字符串。
     * <p>锁初始 TTL 为 10 秒，当前不会续期；不要把获取成功视为无限期持锁。
     *
     * <p>等效 Redis 命令：{@code SET lock:user:profile:1001 UUID_TOKEN NX EX 10}。
     *
     * @param lockKey 缓存重建锁 Key，如 lock:user:profile:1001
     * @return 抢锁成功返回 UUID Token；Key 已被占用时返回 null
     */
    private String tryLock(String lockKey) {
        String token = UUID.randomUUID().toString();
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, token, LOCK_TTL);
        return Boolean.TRUE.equals(success) ? token : null;
    }

    /**
     * 使用 Lua 在 Redis 服务端原子检查 Token 并删除缓存重建锁。
     *
     * <p>脚本路径：classpath:lua/unlock.lua。
     * KEYS[1] = lockKey，ARGV[1] = 持锁请求得到的 UUID Token。
     * 校验通过后删除 Key；校验失败/锁已过期时保持 Key 不变。
     * <p>脚本原子性只解决“校验后误删新持有者的锁”，不提供锁续期或可重入能力。
     * Redis 执行脚本失败时异常会向调用方传播，不能误判为释放成功。
     *
     * @param lockKey 需要释放的锁 Key
     * @param token   本次请求成功加锁时得到的 UUID Token
     * @return true 表示删除成功（Lua 返回 1）；false 表示未删除（Lua 返回 0）
     */
    private boolean unlock(String lockKey, String token) {
        Long result = stringRedisTemplate.execute(
                UNLOCK_SCRIPT,
                List.of(lockKey),
                token
        );
        return Long.valueOf(1L).equals(result);
    }

    /**
     * 为正常用户缓存生成随机过期时间，避免大量缓存同时到期导致雪崩。
     * 基础 TTL = 10 分钟；追加 0~120 秒抖动（包含边界），实际范围 10~12 分钟。
     *
     * @return 用于缓存写入的 TTL
     */
    private Duration buildCacheTtl() {
        long jitterSeconds = ThreadLocalRandom.current().nextLong(0, CACHE_TTL_JITTER_SECONDS + 1);
        return CACHE_BASE_TTL.plusSeconds(jitterSeconds);
    }


    /**
     * 更新 MySQL 用户记录并尽力删除对应 Redis 缓存（Cache Aside 写路径）。
     *
     * <p>先执行 MySQL UPDATE，成功后直接删除缓存；仅在删除抛异常且重试耗尽时，
     * 才向 RabbitMQ 发送缓存失效消息。消息投递与消费属于后续异步流程，
     * 此方法不保证缓存已经被最终删除。
     *
     * @param id       待更新用户 ID
     * @param username 新用户名
     * @param age      新年龄
     */
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
            String messageId = UUID.randomUUID().toString();
            CorrelationData correlationData = new CorrelationData(messageId);
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.CACHE_INVALIDATION_EXCHANGE,
                    RabbitMqConfig.CACHE_INVALIDATION_ROUTING_KEY,
                    message, rabbitMessage -> {
                        rabbitMessage.getMessageProperties().setMessageId(messageId);
                        rabbitMessage.getMessageProperties().setHeader("bizKey", key);
                        return rabbitMessage;
                    },
                    correlationData
            );
            log.info("CACHE INVALIDATION MESSAGE SENT, key={}, messageId={}", key, messageId);
        } // 同步缓存删除连续失败时走 MQ 异步补偿；正常删除时不发送消息。

        // 历史失败场景教学代码（已注释、不执行）：如需复现实验，应在隔离环境中手动启用。
        // log.info("DATABASE UPDATED, userId={}", id);
        // log.error("SIMULATED CACHE DELETE FAILURE, key={}", key);
        // throw new IllegalStateException("模拟 Redis 缓存删除失败");
    }

    /**
     * 同步删除用户缓存并进行有界重试（最多 3 次，失败后等待 200ms 再试）。
     *
     * <p>仅当 Redis 操作抛出异常时重试；返回 false 表示 Key 不存在，
     * 仍视为当前缓存失效目标已满足。
     * <p>最终失败时抛出 IllegalStateException，由上层 updateUser() 触发 MQ 补偿。
     *
     * @param key 需要失效的 Redis 用户缓存 Key
     */
    private void deleteCacheWithRetry(String key) {
        for (int attempt = 1; attempt <= CACHE_DELETE_MAX_ATTEMPTS; attempt++) {
            try {
                Boolean deleted = objectRedisTemplate.delete(key);
                log.info(
                        "CACHE INVALIDATED, key={}, existed={}, attempt={}",
                        key,
                        deleted,
                        attempt
                );
                return;
            } catch (Exception e) {
                log.warn(
                        "CACHE DELETE FAILED, key={}, attempt={}/{}",
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