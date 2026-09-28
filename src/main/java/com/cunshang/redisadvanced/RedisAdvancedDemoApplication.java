package com.cunshang.redisadvanced;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RedisAdvancedDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                RedisAdvancedDemoApplication.class,
                args
        );
    }

}