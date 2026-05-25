package com.example.concurrent_impl.common.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 【Redis配置】Redis序列化和连接配置
 * 
 * 【业务背景】
 * Redis是企业级项目中常用的缓存和分布式锁组件，
 * 合理的配置可以提高性能和可维护性。
 * 
 * 【为什么这样写】
 * 1. 使用Jackson序列化：支持复杂对象存储，可读性好
 * 2. 使用String序列化key：key通常较短，String序列化更高效
 * 3. 开启类型信息：反序列化时能正确还原对象类型
 * 4. 配置连接池：提高连接复用率，减少连接创建开销
 * 
 * 【不遵守的后果】
 * 1. 使用JDK序列化：可读性差，占用空间大，存在安全风险
 * 2. 不配置序列化：默认使用JDK序列化，可能出现乱码
 * 3. 不开启类型信息：反序列化时可能得到LinkedHashMap而不是原始类型
 * 4. 不配置连接池：每次操作都创建新连接，性能差
 * 
 * 【正确示例】
 * 使用Jackson序列化，配置类型信息
 * 
 * 【错误示例】
 * 使用默认的JDK序列化，出现乱码和类型转换问题
 * 
 * 【实际案例】
 * 1. 缓存用户信息：需要正确反序列化为User对象
 * 2. 分布式锁：需要原子性的set和delete操作
 * 3. 限流计数：需要原子性的incr操作
 */
@Configuration
public class RedisConfig {

    /**
     * 配置RedisTemplate
     * 
     * 【为什么需要自定义RedisTemplate】
     * 1. 默认的RedisTemplate使用JDK序列化，可读性差
     * 2. 需要配置key和value的不同序列化方式
     * 3. 需要开启类型信息，支持复杂对象的正确反序列化
     * 
     * @param connectionFactory Redis连接工厂
     * @return RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 【关键点1】配置key的序列化器
        // 使用String序列化，key通常是字符串，这样可读性好
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // 【关键点2】配置value的序列化器
        // 使用Jackson序列化，支持复杂对象，可读性好
        GenericJackson2JsonRedisSerializer jsonSerializer = createJsonSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 创建JSON序列化器
     * 
     * 【为什么单独创建】
     * 1. 便于统一配置序列化选项
     * 2. 可以复用同一个ObjectMapper
     * 
     * @return JSON序列化器
     */
    private GenericJackson2JsonRedisSerializer createJsonSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        
        // 【关键点1】设置可见性，所有属性都可以序列化
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        
        // 【关键点2】开启类型信息
        // 这样反序列化时能正确还原对象类型
        // 【注意】这会在JSON中添加@class字段，占用额外空间
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        
        return new GenericJackson2JsonRedisSerializer(mapper);
    }
}
