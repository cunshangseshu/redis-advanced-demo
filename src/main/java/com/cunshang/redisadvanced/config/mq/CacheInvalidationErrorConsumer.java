package com.cunshang.redisadvanced.config.mq;

import com.cunshang.redisadvanced.config.RabbitMqConfig;
import com.cunshang.redisadvanced.model.message.CacheInvalidationMessage;
import com.cunshang.redisadvanced.service.MqConsumeFailureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class CacheInvalidationErrorConsumer {

    private static final Logger log = LoggerFactory.getLogger(CacheInvalidationErrorConsumer.class);

    private final MqConsumeFailureService failureService;

    public CacheInvalidationErrorConsumer(MqConsumeFailureService failureService) {
        this.failureService = failureService;
    }

    @RabbitListener(queues = RabbitMqConfig.CACHE_INVALIDATION_ERROR_QUEUE)
    public void consume(CacheInvalidationMessage message) {
        String key = message.key();
        log.error("MQ ERROR MESSAGE RECEIVED, key={}", key);
        failureService.record(key, "Consumer retry exhausted");
        log.warn("MQ CONSUME FAILURE RECORDED, key={}", key);
    }
}