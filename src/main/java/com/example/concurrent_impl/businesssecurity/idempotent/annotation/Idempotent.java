package com.example.concurrent_impl.businesssecurity.idempotent.annotation;

import java.lang.annotation.*;

/**
 * 【幂等注解】防止重复提交的注解
 *
 * 【业务背景】
 * 在企业级项目中，由于网络抖动、用户重复点击等原因，
 * 同一个请求可能会被发送多次，需要保证接口的幂等性。
 *
 * 【使用方式】
 * 在Controller方法上添加@Idempotent注解即可
 *
 * 【为什么这样写】
 * 1. 使用注解方式，代码简洁，无侵入
 * 2. 支持自定义幂等键生成策略
 * 3. 支持自定义过期时间
 * 4. 支持自定义错误提示
 *
 * 【不遵守的后果】
 * 1. 不做幂等处理：重复请求导致重复扣款、重复下单等问题
 * 2. 使用硬编码：难以维护，容易遗漏
 * 3. 不使用注解：代码冗余，逻辑分散
 *
 * 【正确示例】
 * @Idempotent(key = "#request.orderNo", expireSeconds = 5)
 * public ApiResult createOrder(OrderRequest request) { ... }
 *
 * 【错误示例】
 * 不做任何幂等处理
 *
 * 【实际案例】
 * 1. 订单创建接口
 * 2. 支付接口
 * 3. 退款接口
 *
 * @author concurrent_impl
 * @date 2024
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * 幂等键的SpEL表达式
     *
     * 【使用示例】
     * 1. 使用请求参数：key = "#request.orderNo"
     * 2. 使用请求头：key = "#request.getHeader('X-Idempotent-Key')"
     * 3. 使用多个参数：key = "#userId + ':' + #orderId"
     *
     * 【默认值】
     * 如果不指定，使用请求URI + 参数的MD5作为幂等键
     */
    String key() default "";

    /**
     * 幂等键过期时间（秒）
     *
     * 【为什么设置默认值】
     * 1. 大部分接口在5秒内可以完成
     * 2. 太短：请求未完成就过期，无法防重复
     * 3. 太长：占用存储空间
     */
    int expireSeconds() default 5;

    /**
     * 重复提交时的错误提示
     */
    String message() default "请勿重复提交";

    /**
     * 幂等键前缀
     *
     * 【使用场景】
     * 不同接口使用不同的前缀，避免key冲突
     */
    String prefix() default "idempotent:";

    /**
     * 是否在业务执行失败后删除幂等键
     *
     * 【使用场景】
     * 如果业务执行失败，允许用户重新提交
     *
     * 【默认值】true
     * 业务失败后删除幂等键，允许重新提交
     */
    boolean deleteKeyAfterFailure() default true;
}
