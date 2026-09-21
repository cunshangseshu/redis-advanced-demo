package com.cunshang.redisadvanced.model;


// 请注意，这里是 record 类型的类，包含自动组装Javabean的方法
public record RedisUserProfile(Long id, String username, Integer age) {

}