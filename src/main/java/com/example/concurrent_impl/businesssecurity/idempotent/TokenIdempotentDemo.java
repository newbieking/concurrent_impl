package com.example.concurrent_impl.businesssecurity.idempotent;

import lombok.Data;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 【幂等 - Token方式防重复提交】基于Token的幂等实现
 *
 * 【业务背景】
 * 在表单提交场景中，用户可能快速多次点击提交按钮，
 * 导致同一个请求被发送多次。
 *
 * 【实现原理】
 * 1. 页面加载时，服务端生成唯一的Token
 * 2. Token存储在服务端（Redis/Session）
 * 3. 提交时携带Token
 * 4. 服务端验证Token是否存在
 * 5. 验证通过后删除Token
 * 6. 重复提交时Token已不存在，拒绝请求
 *
 * 【为什么这样写】
 * 1. Token是一次性的，用完即删
 * 2. 支持分布式环境（使用Redis存储）
 * 3. 实现简单，效果好
 *
 * 【不遵守的后果】
 * 1. 不使用Token：无法防止重复提交
 * 2. Token不过期：占用存储空间
 * 3. Token不删除：无法防止重复提交
 *
 * 【正确示例】
 * 使用Token + 一次性验证
 *
 * 【错误示例】
 * 只检查Token是否存在，不删除
 *
 * 【实际案例】
 * 1. 表单提交
 * 2. 订单创建
 * 3. 支付确认
 *
 * @author concurrent_impl
 * @date 2024
 */
public class TokenIdempotentDemo {

    /**
     * 模拟Redis存储Token
     * 实际项目中应使用StringRedisTemplate
     */
    private static final ConcurrentHashMap<String, Boolean> tokenStore = new ConcurrentHashMap<>();

    /**
     * Token过期时间（秒）
     */
    private static final int TOKEN_EXPIRE_SECONDS = 300; // 5分钟

    /**
     * 生成Token
     *
     * 【使用场景】
     * 页面加载时调用，获取Token并返回给前端
     *
     * @param userId 用户ID
     * @return Token
     */
    public static String generateToken(String userId) {
        String token = UUID.randomUUID().toString();
        String key = "token:" + userId + ":" + token;

        // 存储Token（实际项目中设置过期时间）
        tokenStore.put(key, true);

        System.out.println("生成Token: " + token);
        return token;
    }

    /**
     * 验证并删除Token
     *
     * 【实现原理】
     * 1. 检查Token是否存在
     * 2. 如果存在，删除Token并返回true
     * 3. 如果不存在，返回false（重复提交）
     *
     * @param userId 用户ID
     * @param token Token
     * @return true-验证通过，false-重复提交
     */
    public static boolean validateAndRemoveToken(String userId, String token) {
        String key = "token:" + userId + ":" + token;

        // 【关键点】原子性操作：检查并删除
        // 实际项目中使用Redis的Lua脚本保证原子性
        Boolean exists = tokenStore.remove(key);

        if (Boolean.TRUE.equals(exists)) {
            System.out.println("Token验证通过: " + token);
            return true;
        }

        System.out.println("Token验证失败（重复提交）: " + token);
        return false;
    }

    /**
     * 模拟Token方式防重复提交
     */
    public static void demonstrateTokenIdempotent() {
        System.out.println("========== Token方式防重复提交演示 ==========");
        System.out.println();

        String userId = "user123";

        // 场景1：正常提交
        System.out.println("【场景1】正常提交");
        String token = generateToken(userId);
        boolean result = validateAndRemoveToken(userId, token);
        System.out.println("提交结果: " + (result ? "成功" : "失败"));
        System.out.println();

        // 场景2：重复提交
        System.out.println("【场景2】重复提交");
        token = generateToken(userId);
        result = validateAndRemoveToken(userId, token);
        System.out.println("第一次提交: " + (result ? "成功" : "失败"));

        result = validateAndRemoveToken(userId, token);
        System.out.println("第二次提交: " + (result ? "成功" : "失败"));
        System.out.println();

        // 场景3：伪造Token
        System.out.println("【场景3】伪造Token");
        result = validateAndRemoveToken(userId, "fake-token");
        System.out.println("伪造提交: " + (result ? "成功" : "失败"));
    }

    /**
     * 主方法
     */
    public static void main(String[] args) {
        demonstrateTokenIdempotent();
    }
}
