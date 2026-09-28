package com.cunshang.redisadvanced.config.mq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqPublishConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqPublishConfig.class);

    @Bean
    public RabbitTemplateCustomizer rabbitTemplateCustomizer() {
        return rabbitTemplate -> {
            // Publisher Confirm：
            // Broker 是否成功接收到消息
            rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
                if (ack) {
                    log.info("MQ PUBLISH CONFIRMED, correlationData={}", correlationData);
                } else {
                    log.error("MQ PUBLISH NOT CONFIRMED, correlationData={}, cause={}", correlationData, cause);
                }
            });
            // Publisher Return：
            // Exchange 收到了，但是没有路由到 Queue
            rabbitTemplate.setReturnsCallback(returned -> log.error("MQ MESSAGE RETURNED, exchange={}, routingKey={}, replyCode={}, replyText={}",
                    returned.getExchange(),
                    returned.getRoutingKey(),
                    returned.getReplyCode(),
                    returned.getReplyText()
            ));
        };
    }
}