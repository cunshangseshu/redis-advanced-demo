package com.cunshang.redisadvanced.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mq_publish_failure")
public class MqPublishFailure {

    @TableId
    private Long id;

    private String messageId;
    private String bizKey;
    private String exchangeName;
    private String routingKey;

    private String failureType;
    private String failureReason;

    private Integer retryCount;
    private String status;

    private LocalDateTime nextRetryAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}