package com.cunshang.redisadvanced.task;

import com.cunshang.redisadvanced.entity.MqConsumeFailure;
import com.cunshang.redisadvanced.service.MqConsumeFailureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class MqConsumeFailureRetryTask {

    private final MqConsumeFailureService failureService;
    private final RedisTemplate<String, Object> objectRedisTemplate;

    public MqConsumeFailureRetryTask(MqConsumeFailureService failureService, RedisTemplate<String, Object> objectRedisTemplate) {
        this.failureService = failureService;
        this.objectRedisTemplate = objectRedisTemplate;
    }

    @Scheduled(fixedDelay = 5000)
    public void retry() {
        List<MqConsumeFailure> tasks = failureService.findRetryableTasks();
        for (MqConsumeFailure task : tasks) {
            try {
                Boolean deleted = objectRedisTemplate.delete(task.getBizKey());
                failureService.markSuccess(task.getId());
                log.info("MQ CONSUME FAILURE RETRY SUCCESS, taskId={}, key={}, existed={}", task.getId(), task.getBizKey(), deleted);
            } catch (Exception e) {
                failureService.markRetryFailed(task.getId(), e.getMessage());
                log.error("MQ CONSUME FAILURE RETRY FAILED, taskId={}, key={}",
                        task.getId(),
                        task.getBizKey(),
                        e
                );
            }
        }
    }
}