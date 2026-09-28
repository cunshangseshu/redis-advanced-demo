package com.cunshang.redisadvanced.config.mq;

import com.cunshang.redisadvanced.model.mq.MqRetryCorrelationData;
import com.cunshang.redisadvanced.service.MqPublishFailureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqPublishConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqPublishConfig.class);

    @Bean
    public RabbitTemplateCustomizer rabbitTemplateCustomizer(MqPublishFailureService failureService) {
        return rabbitTemplate -> {
            // ==================== Confirm ====================
            rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
                if (ack) {
                    log.info("MQ PUBLISH CONFIRMED, correlationData={}", correlationData);
                    return;
                }
                String messageId = correlationData == null ? null : correlationData.getId();
                log.error("MQ PUBLISH NOT CONFIRMED, messageId={}, cause={}", messageId, cause);
                // 自动重试消息由 RetryTask 自己处理结果
                // 不要再次创建新的失败任务
                if (correlationData instanceof MqRetryCorrelationData) {
                    return;
                }
                failureService.record(messageId, null, "cache.invalidation.exchange", "cache.invalidation", "CONFIRM_NACK", cause);
            });
            // ==================== Return ====================
            rabbitTemplate.setReturnsCallback(returned -> {
                String messageId = returned.getMessage().getMessageProperties().getMessageId();
                Object bizKey = returned.getMessage().getMessageProperties().getHeaders().get("bizKey");
                Object retryTaskId = returned.getMessage().getMessageProperties().getHeaders().get("retryTaskId");
                log.error("MQ MESSAGE RETURNED, messageId={}, exchange={}, routingKey={}, replyCode={}, replyText={}", messageId, returned.getExchange(), returned.getRoutingKey(), returned.getReplyCode(), returned.getReplyText());
                // 自动重试消息：
                // RetryTask 会根据 CorrelationData.getReturned()
                // 更新原来的数据库任务
                if (retryTaskId != null) {
                    return;
                }
                // 第一次发送失败才新增失败任务
                failureService.record(messageId, bizKey == null ? null : bizKey.toString(), returned.getExchange(), returned.getRoutingKey(), "RETURN", returned.getReplyCode() + " " + returned.getReplyText());
            });
        };
    }
}