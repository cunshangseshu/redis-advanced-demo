package com.cunshang.redisadvanced.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cunshang.redisadvanced.entity.MqPublishFailure;
import com.cunshang.redisadvanced.mapper.MqPublishFailureMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MqPublishFailureService {

    private static final int MAX_RETRY_COUNT = 3;

    private final MqPublishFailureMapper mapper;

    public MqPublishFailureService(MqPublishFailureMapper mapper) {
        this.mapper = mapper;
    }

    public void record(String messageId, String bizKey, String exchange, String routingKey, String failureType, String failureReason) {
        MqPublishFailure failure = new MqPublishFailure();
        failure.setMessageId(messageId);
        failure.setBizKey(bizKey);
        failure.setExchangeName(exchange);
        failure.setRoutingKey(routingKey);
        failure.setFailureType(failureType);
        failure.setFailureReason(failureReason);
        failure.setRetryCount(0);
        failure.setStatus("PENDING");
        failure.setNextRetryAt(LocalDateTime.now().plusSeconds(10));
        failure.setCreatedAt(LocalDateTime.now());
        failure.setUpdatedAt(LocalDateTime.now());
        mapper.insert(failure);
    }

    public List<MqPublishFailure> findRetryableTasks() {
        return mapper.selectList(new LambdaQueryWrapper<MqPublishFailure>().eq(MqPublishFailure::getStatus, "PENDING").lt(MqPublishFailure::getRetryCount, MAX_RETRY_COUNT).le(MqPublishFailure::getNextRetryAt, LocalDateTime.now()));
    }

    public void markSuccess(Long id) {
        MqPublishFailure failure = mapper.selectById(id);
        failure.setStatus("SUCCESS");
        failure.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(failure);
    }

    public void markRetryFailed(Long id, String reason) {
        MqPublishFailure failure = mapper.selectById(id);
        int retryCount = failure.getRetryCount() + 1;
        failure.setRetryCount(retryCount);
        failure.setFailureReason(reason);
        failure.setUpdatedAt(LocalDateTime.now());
        if (retryCount >= MAX_RETRY_COUNT) {
            failure.setStatus("FAILED");
            failure.setNextRetryAt(null);
        } else {
            failure.setStatus("PENDING");
            failure.setNextRetryAt(LocalDateTime.now().plusSeconds(10));
        }
        mapper.updateById(failure);
    }
}