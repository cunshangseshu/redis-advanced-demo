package com.cunshang.redisadvanced.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("user_profile")
public class UserProfile {
    @TableId
    private Long id;
    private String username;
    private Integer age;
}