package com.example.concurrent_impl.common.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 【企业级】Redisson配置类
 *
 * 【业务背景】
 * Redisson是一个高级的分布式协调工具库，
 * 提供了分布式锁、分布式集合、分布式服务等功能。
 *
 * 【为什么使用Redisson】
 * 1. 支持可重入锁、公平锁、联锁、红锁等
 * 2. 支持锁续期（Watch Dog机制）
 * 3. 支持异步和响应式编程
 * 4. 支持Redis集群、哨兵、主从等模式
 *
 * @author concurrent_impl
 * @date 2024
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.database:0}")
    private int database;

    /**
     * Redisson客户端配置
     *
     * 【配置说明】
     * 1. 单节点模式：适用于开发环境
     * 2. 集群模式：适用于生产环境
     * 3. 哨兵模式：适用于高可用环境
     */
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();

        // 单节点配置
        String address = "redis://" + redisHost + ":" + redisPort;
        config.useSingleServer().setAddress(address);

        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.useSingleServer().setPassword(redisPassword);
        }

        config.useSingleServer().setDatabase(database);

        // 连接池配置
        config.useSingleServer().setConnectionMinimumIdleSize(5);
        config.useSingleServer().setConnectionPoolSize(20);

        // 超时配置
        config.useSingleServer().setConnectTimeout(10000);
        config.useSingleServer().setTimeout(10000);

        return Redisson.create(config);
    }
}
