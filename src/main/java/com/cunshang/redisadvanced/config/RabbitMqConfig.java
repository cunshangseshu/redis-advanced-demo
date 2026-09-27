package com.cunshang.redisadvanced.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    // ==================== 正常链路 ====================
    public static final String CACHE_INVALIDATION_EXCHANGE = "cache.invalidation.exchange";
    public static final String CACHE_INVALIDATION_QUEUE = "cache.invalidation.queue";
    public static final String CACHE_INVALIDATION_ROUTING_KEY = "cache.invalidation";


    // ==================== 失败链路 ====================
    public static final String CACHE_INVALIDATION_ERROR_EXCHANGE = "cache.invalidation.error.exchange";
    public static final String CACHE_INVALIDATION_ERROR_QUEUE = "cache.invalidation.error.queue";
    public static final String CACHE_INVALIDATION_ERROR_ROUTING_KEY = "cache.invalidation.error";


    // ==================== 正常 Exchange ====================
    @Bean
    public DirectExchange cacheInvalidationExchange() {
        return new DirectExchange(CACHE_INVALIDATION_EXCHANGE);
    }


    // ==================== 正常 Queue ====================
    @Bean
    public Queue cacheInvalidationQueue() {
        return QueueBuilder.durable(CACHE_INVALIDATION_QUEUE).build();
    }


    // ==================== 正常 Binding ====================
    @Bean
    public Binding cacheInvalidationBinding() {
        return BindingBuilder.bind(cacheInvalidationQueue()).to(cacheInvalidationExchange()).with(CACHE_INVALIDATION_ROUTING_KEY);
    }


    // ==================== Error Exchange ====================
    @Bean
    public DirectExchange cacheInvalidationErrorExchange() {
        return new DirectExchange(CACHE_INVALIDATION_ERROR_EXCHANGE);
    }


    // ==================== Error Queue ====================
    @Bean
    public Queue cacheInvalidationErrorQueue() {
        return QueueBuilder.durable(CACHE_INVALIDATION_ERROR_QUEUE).build();
    }


    // ==================== Error Binding ====================
    @Bean
    public Binding cacheInvalidationErrorBinding() {
        return BindingBuilder.bind(cacheInvalidationErrorQueue()).to(cacheInvalidationErrorExchange()).with(CACHE_INVALIDATION_ERROR_ROUTING_KEY);
    }


    // ==================== JSON Converter ====================
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }


    // ==================== Consumer Retry ====================
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter,
            RabbitTemplate rabbitTemplate
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        RepublishMessageRecoverer recoverer = new RepublishMessageRecoverer(
                rabbitTemplate,
                CACHE_INVALIDATION_ERROR_EXCHANGE,
                CACHE_INVALIDATION_ERROR_ROUTING_KEY
        );
        factory.setAdviceChain(RetryInterceptorBuilder.
                stateless().
                maxAttempts(3).
                backOffOptions(500, 2.0, 500).
                recoverer(recoverer).
                build()
        );
        return factory;
    }
}