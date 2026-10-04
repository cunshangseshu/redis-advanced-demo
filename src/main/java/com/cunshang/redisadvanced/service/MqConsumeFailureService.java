package com.cunshang.redisadvanced.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cunshang.redisadvanced.entity.MqConsumeFailure;
import com.cunshang.redisadvanced.mapper.MqConsumeFailureMapper;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MqConsumeFailureService {

    private final MqConsumeFailureMapper mapper;
    private static final int MAX_RETRY_COUNT = 3;

    public MqConsumeFailureService(MqConsumeFailureMapper mapper) {
        this.mapper = mapper;
    }

    public void record(String bizKey, String failureReason) {
        MqConsumeFailure failure = new MqConsumeFailure();
        failure.setBizKey(bizKey);
        failure.setFailureReason(failureReason);
        failure.setRetryCount(0);
        failure.setStatus("PENDING");
        failure.setNextRetryAt(LocalDateTime.now().plusSeconds(10));
        failure.setCreatedAt(LocalDateTime.now());
        failure.setUpdatedAt(LocalDateTime.now());
        mapper.insert(failure);
    }

    public List<MqConsumeFailure> findRetryableTasks() {
        return mapper.selectList(new LambdaQueryWrapper<MqConsumeFailure>().
                eq(MqConsumeFailure::getStatus, "PENDING").
                lt(MqConsumeFailure::getRetryCount, MAX_RETRY_COUNT).
                le(MqConsumeFailure::getNextRetryAt, LocalDateTime.now()));
    }

    public void markSuccess(Long id) {
        mapper.update(null,
                new LambdaUpdateWrapper<MqConsumeFailure>()
                        .eq(MqConsumeFailure::getId, id)
                        .set(MqConsumeFailure::getStatus, "SUCCESS")
                        .set(MqConsumeFailure::getNextRetryAt, null)
                        .set(MqConsumeFailure::getUpdatedAt, LocalDateTime.now())
        );
    }

    public void markRetryFailed(Long id, String reason) {
        MqConsumeFailure failure = mapper.selectById(id);
        int retryCount = failure.getRetryCount() + 1;
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<MqConsumeFailure> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(MqConsumeFailure::getId, id);
        wrapper.set(MqConsumeFailure::getRetryCount, retryCount);
        wrapper.set(MqConsumeFailure::getFailureReason, reason);
        wrapper.set(MqConsumeFailure::getUpdatedAt, now);
        if (retryCount >= MAX_RETRY_COUNT) {
            wrapper.set(MqConsumeFailure::getStatus, "FAILED");
            wrapper.set(MqConsumeFailure::getNextRetryAt, null);
        } else {
            wrapper.set(MqConsumeFailure::getStatus, "PENDING");
            wrapper.set(MqConsumeFailure::getNextRetryAt, now.plusSeconds(10));
        }
        mapper.update(null, wrapper);
    }
}