package com.example.concurrent_impl.highconcurrency.ratelimit.annotation;

import java.lang.annotation.*;

/**
 * 【企业级】限流注解
 *
 * 【业务背景】
 * 在高并发系统中，需要对API接口进行限流保护，
 * 防止系统过载导致服务不可用。
 *
 * 【使用方式】
 * 在Controller方法上添加@RateLimit注解即可
 *
 * @author concurrent_impl
 * @date 2024
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 限流key前缀
     */
    String prefix() default "rate_limit:";

    /**
     * 窗口大小（毫秒）
     */
    long windowMs() default 1000;

    /**
     * 窗口内允许的最大请求数
     */
    int limit() default 10;

    /**
     * 限流类型
     */
    LimitType type() default LimitType.IP;

    /**
     * 限流类型枚举
     */
    enum LimitType {
        /**
         * 按IP限流
         */
        IP,
        /**
         * 按用户限流
         */
        USER,
        /**
         * 按接口限流
         */
        METHOD
    }
}
