package com.cunshang.redisadvanced.task;

import com.cunshang.redisadvanced.entity.MqPublishFailure;
import com.cunshang.redisadvanced.model.message.CacheInvalidationMessage;
import com.cunshang.redisadvanced.model.mq.MqRetryCorrelationData;
import com.cunshang.redisadvanced.service.MqPublishFailureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class MqPublishFailureRetryTask {

    private static final Logger log = LoggerFactory.getLogger(MqPublishFailureRetryTask.class);

    private final MqPublishFailureService failureService;
    private final RabbitTemplate rabbitTemplate;

    public MqPublishFailureRetryTask(MqPublishFailureService failureService, RabbitTemplate rabbitTemplate) {
        this.failureService = failureService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelay = 5000)
    public void retry() {
        List<MqPublishFailure> tasks = failureService.findRetryableTasks();
        for (MqPublishFailure task : tasks) {
            retryOne(task);
        }
    }

    private void retryOne(MqPublishFailure task) {
        String messageId = UUID.randomUUID().toString();
        MqRetryCorrelationData correlationData = new MqRetryCorrelationData(messageId, task.getId());
        CacheInvalidationMessage message = new CacheInvalidationMessage(task.getBizKey());
        try {
            rabbitTemplate.convertAndSend(
                    task.getExchangeName(),
                    task.getRoutingKey(),
                    message,
                    rabbitMessage -> {
                        rabbitMessage.getMessageProperties().setMessageId(messageId);
                        rabbitMessage.getMessageProperties().setHeader("bizKey", task.getBizKey());
                        rabbitMessage.getMessageProperties().setHeader("retryTaskId", task.getId());
                        return rabbitMessage;
                    },
                    correlationData);

            // =========================================
            // 真正等待 RabbitMQ Publisher Confirm
            // =========================================
            CorrelationData.Confirm confirm = correlationData.getFuture().get(5, TimeUnit.SECONDS);
            // Broker NACK
            if (!confirm.isAck()) {
                failureService.markRetryFailed(task.getId(), "CONFIRM_NACK: " + confirm.getReason());
                log.error("MQ FAILURE TASK NACK, taskId={}, messageId={}, reason={}",
                        task.getId(),
                        messageId,
                        confirm.getReason()
                );
                return;
            }

            // =========================================
            // Confirm ACK 后继续检查 Return
            // =========================================
            ReturnedMessage returned = correlationData.getReturned();
            if (returned != null) {
                String reason = returned.getReplyCode() + " " + returned.getReplyText();
                failureService.markRetryFailed(task.getId(), reason);
                log.error("MQ FAILURE TASK RETURNED, taskId={}, messageId={}, reason={}",
                        task.getId(),
                        messageId,
                        reason
                );
                return;
            }

            // =========================================
            // ACK + 没有 Return
            // 才是真正成功
            // =========================================
            failureService.markSuccess(task.getId());
            log.info("MQ FAILURE TASK RETRY SUCCESS, taskId={}, messageId={}", task.getId(), messageId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            failureService.markRetryFailed(task.getId(), "线程被中断");
        } catch (Exception e) {
            failureService.markRetryFailed(task.getId(), e.getMessage());
            log.error("MQ FAILURE TASK RETRY FAILED, taskId={}", task.getId(), e);
        }
    }
}