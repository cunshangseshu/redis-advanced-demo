package com.cunshang.redisadvanced.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {
    public static final String CACHE_INVALIDATION_QUEUE = "cache.invalidation.queue";

    @Bean
    public Queue cacheInvalidationQueue() {
        return new Queue(CACHE_INVALIDATION_QUEUE, true);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
