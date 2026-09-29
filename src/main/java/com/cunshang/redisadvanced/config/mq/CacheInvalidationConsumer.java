package com.cunshang.redisadvanced.config.mq;

import com.cunshang.redisadvanced.config.RabbitMqConfig;
import com.cunshang.redisadvanced.model.message.CacheInvalidationMessage;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Getter
@Component
public class CacheInvalidationConsumer {

    private static final Logger log = LoggerFactory.getLogger(CacheInvalidationConsumer.class);
    private final RedisTemplate<String, Object> objectRedisTemplate;

    public CacheInvalidationConsumer(@Qualifier("objectRedisTemplate") RedisTemplate<String, Object> objectRedisTemplate) {
        this.objectRedisTemplate = objectRedisTemplate;
    }

    @RabbitListener(queues = RabbitMqConfig.CACHE_INVALIDATION_QUEUE)
    public void consume(CacheInvalidationMessage message) {
        String key = message.key();
        log.info("MQ CACHE INVALIDATION RECEIVED, key={}", key);
        // Boolean deleted = objectRedisTemplate.delete(key);
        // 模拟 RabbitMQ 挂掉然后充分利用spring的重试机制；
        log.error("SIMULATED MQ CONSUMER FAILURE, key={}", key);
        throw new IllegalStateException("模拟 RabbitMQ Consumer 删除 Redis 失败");
        // log.info(" , key={}, existed={}", key, deleted);
    }

}