package com.example.concurrent_impl.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 【异步线程池配置】配置异步任务的线程池
 * 
 * 【业务背景】
 * 在企业级项目中，很多操作需要异步执行，如：
 * 1. 发送邮件、短信通知
 * 2. 记录操作日志
 * 3. 生成报表
 * 4. 调用外部接口
 * 
 * 【为什么这样写】
 * 1. 使用线程池管理线程，避免频繁创建和销毁线程
 * 2. 合理配置线程池参数，避免资源耗尽
 * 3. 配置拒绝策略，处理线程池满的情况
 * 4. 使用有意义的线程名称，便于问题排查
 * 
 * 【不遵守的后果】
 * 1. 不使用线程池：每次异步操作都创建新线程，资源消耗大
 * 2. 线程池参数不合理：可能导致OOM或任务堆积
 * 3. 不配置拒绝策略：任务被拒绝时可能丢失
 * 4. 线程名称无意义：出现问题时难以定位
 * 
 * 【线程池参数说明】
 * 1. corePoolSize：核心线程数，线程池中一直存活的线程数
 * 2. maxPoolSize：最大线程数，线程池中允许的最大线程数
 * 3. queueCapacity：队列容量，当线程数达到核心线程数时，新任务会进入队列
 * 4. keepAliveSeconds：线程空闲时间，超过核心线程数的线程空闲超过此时间会被回收
 * 5. threadNamePrefix：线程名称前缀，便于日志追踪
 * 
 * 【拒绝策略】
 * 1. AbortPolicy：抛出异常（默认）
 * 2. CallerRunsPolicy：由调用线程执行
 * 3. DiscardPolicy：丢弃任务
 * 4. DiscardOldestPolicy：丢弃最旧的任务
 * 
 * 【实际案例】
 * 1. 订单创建后异步发送通知
 * 2. 支付成功后异步更新订单状态
 * 3. 用户注册后异步发送欢迎邮件
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 通用异步任务线程池
     * 
     * 【使用场景】
     * 一般的异步任务，如发送通知、记录日志等
     * 
     * 【参数设置依据】
     * 1. 核心线程数：CPU核心数，避免上下文切换
     * 2. 最大线程数：核心线程数的2倍，应对突发流量
     * 3. 队列容量：100，避免任务堆积
     * 4. 拒绝策略：CallerRunsPolicy，保证任务不丢失
     */
    @Bean("asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 【关键点1】核心线程数
        // 建议设置为CPU核心数
        // 过小：任务需要排队等待
        // 过大：线程上下文切换开销大
        executor.setCorePoolSize(5);
        
        // 【关键点2】最大线程数
        // 建议设置为核心线程数的2倍
        // 当队列满时，会创建新线程，直到达到最大线程数
        executor.setMaxPoolSize(10);
        
        // 【关键点3】队列容量
        // 当线程数达到核心线程数时，新任务会进入队列
        // 过小：容易触发拒绝策略
        // 过大：任务堆积，响应时间长
        executor.setQueueCapacity(100);
        
        // 【关键点4】线程空闲时间
        // 超过核心线程数的线程空闲超过此时间会被回收
        executor.setKeepAliveSeconds(60);
        
        // 【关键点5】线程名称前缀
        // 便于日志追踪和问题定位
        executor.setThreadNamePrefix("async-");
        
        // 【关键点6】拒绝策略
        // CallerRunsPolicy：由调用线程执行，保证任务不丢失
        // 注意：这会阻塞调用线程
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 【关键点7】等待所有任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // 【关键点8】等待时间
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        return executor;
    }

    /**
     * 订单处理线程池
     * 
     * 【使用场景】
     * 订单相关的异步任务，如订单超时取消、订单状态同步等
     * 
     * 【为什么单独配置】
     * 1. 订单任务较重，需要独立的线程池
     * 2. 避免与其他任务相互影响
     * 3. 便于单独监控和调优
     */
    @Bean("orderExecutor")
    public Executor orderExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("order-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * 通知处理线程池
     * 
     * 【使用场景】
     * 发送邮件、短信、推送等通知任务
     * 
     * 【为什么单独配置】
     * 1. 通知任务通常涉及网络IO，较慢
     * 2. 需要较多的线程来提高并发度
     * 3. 通知失败不影响主业务
     */
    @Bean("notifyExecutor")
    public Executor notifyExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(15);
        executor.setQueueCapacity(300);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("notify-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
