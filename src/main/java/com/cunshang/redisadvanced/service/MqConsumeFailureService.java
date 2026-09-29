package com.cunshang.redisadvanced.service;

import com.cunshang.redisadvanced.entity.MqConsumeFailure;
import com.cunshang.redisadvanced.mapper.MqConsumeFailureMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class MqConsumeFailureService {

    private final MqConsumeFailureMapper mapper;

    public MqConsumeFailureService(MqConsumeFailureMapper mapper) {
        this.mapper = mapper;
    }

    public void record(String bizKey, String failureReason) {
        MqConsumeFailure failure = new MqConsumeFailure();
        failure.setBizKey(bizKey);
        failure.setFailureReason(failureReason);
        failure.setStatus("PENDING");
        failure.setCreatedAt(LocalDateTime.now());
        failure.setUpdatedAt(LocalDateTime.now());
        mapper.insert(failure);
    }
}