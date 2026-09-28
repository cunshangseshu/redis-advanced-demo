package com.cunshang.redisadvanced.model.mq;

import lombok.Getter;
import org.springframework.amqp.rabbit.connection.CorrelationData;

@Getter
public class MqRetryCorrelationData extends CorrelationData {

    private final Long failureTaskId;

    public MqRetryCorrelationData(String id, Long failureTaskId) {
        super(id);
        this.failureTaskId = failureTaskId;
    }

}