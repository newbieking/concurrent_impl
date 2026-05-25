package com.example.concurrent_impl.highconcurrency.ratelimit;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 【限流 - 令牌桶算法】平滑限流实现
 *
 * 【业务背景】
 * 在高并发系统中，需要对请求进行限流保护，
 * 防止系统过载导致服务不可用。
 *
 * 【实现原理】
 * 令牌桶算法：
 * 1. 系统以固定速率向桶中添加令牌
 * 2. 桶有最大容量，满了就丢弃新令牌
 * 3. 请求到来时，从桶中取一个令牌
 * 4. 如果桶中有令牌，请求通过；否则拒绝
 *
 * 【为什么这样写】
 * 1. 令牌桶允许突发流量（桶中有积累的令牌）
 * 2. 长期来看，平均速率等于令牌生成速率
 * 3. 适合需要平滑限流的场景
 *
 * 【不遵守的后果】
 * 1. 不限流：系统过载，响应变慢甚至崩溃
 * 2. 限流太严：正常请求被拒绝，用户体验差
 * 3. 使用固定窗口：存在临界点突发问题
 *
 * 【正确示例】
 * 使用令牌桶算法，允许一定的突发流量
 *
 * 【错误示例】
 * 使用固定计数器限流，存在临界点问题
 *
 * 【实际案例】
 * 1. API接口限流
 * 2. 短信发送限流
 * 3. 第三方接口调用限流
 *
 * 【算法对比】
 * 令牌桶：
 * - 优点：允许突发流量，平滑限流
 * - 缺点：实现相对复杂
 *
 * 漏桶：
 * - 优点：强制平滑输出
 * - 缺点：不允许突发流量
 *
 * @author concurrent_impl
 * @date 2024
 */
public class TokenBucketDemo {

    /**
     * 桶的最大容量
     */
    private final int bucketCapacity;

    /**
     * 令牌生成速率（每秒生成的令牌数）
     */
    private final int refillRate;

    /**
     * 当前桶中的令牌数
     */
    private final AtomicInteger tokens;

    /**
     * 上次补充令牌的时间戳
     */
    private final AtomicLong lastRefillTime;

    /**
     * 统计：通过的请求数
     */
    private final AtomicInteger passCount = new AtomicInteger(0);

    /**
     * 统计：拒绝的请求数
     */
    private final AtomicInteger rejectCount = new AtomicInteger(0);

    /**
     * 构造方法
     *
     * @param bucketCapacity 桶的最大容量
     * @param refillRate 令牌生成速率（每秒）
     */
    public TokenBucketDemo(int bucketCapacity, int refillRate) {
        this.bucketCapacity = bucketCapacity;
        this.refillRate = refillRate;
        this.tokens = new AtomicInteger(bucketCapacity);
        this.lastRefillTime = new AtomicLong(System.currentTimeMillis());
    }

    /**
     * 尝试获取令牌
     *
     * 【实现原理】
     * 1. 计算距离上次补充的时间差
     * 2. 根据时间差计算应该补充的令牌数
     * 3. 更新桶中的令牌数（不超过容量）
     * 4. 尝试消费一个令牌
     *
     * @return true-获取成功，false-获取失败（被限流）
     */
    public synchronized boolean tryAcquire() {
        // 【关键点1】补充令牌
        refillTokens();

        // 【关键点2】尝试消费令牌
        int currentTokens = tokens.get();
        if (currentTokens > 0) {
            tokens.decrementAndGet();
            passCount.incrementAndGet();
            return true;
        }

        // 桶中没有令牌，拒绝请求
        rejectCount.incrementAndGet();
        return false;
    }

    /**
     * 补充令牌
     *
     * 【计算公式】
     * 新令牌数 = (当前时间 - 上次时间) * 速率 / 1000
     */
    private void refillTokens() {
        long now = System.currentTimeMillis();
        long lastTime = lastRefillTime.get();

        // 计算时间差（毫秒）
        long elapsed = now - lastTime;
        if (elapsed <= 0) {
            return;
        }

        // 计算应该补充的令牌数
        int newTokens = (int) (elapsed * refillRate / 1000);
        if (newTokens > 0) {
            // 更新令牌数，不超过容量
            int currentTokens = tokens.get();
            int updatedTokens = Math.min(currentTokens + newTokens, bucketCapacity);

            // 使用CAS更新
            if (tokens.compareAndSet(currentTokens, updatedTokens)) {
                lastRefillTime.set(now);
            }
        }
    }

    /**
     * 获取当前令牌数
     */
    public int getAvailableTokens() {
        refillTokens();
        return tokens.get();
    }

    /**
     * 获取通过的请求数
     */
    public int getPassCount() {
        return passCount.get();
    }

    /**
     * 获取拒绝的请求数
     */
    public int getRejectCount() {
        return rejectCount.get();
    }

    /**
     * 重置统计
     */
    public void resetStats() {
        passCount.set(0);
        rejectCount.set(0);
    }

    /**
     * 演示令牌桶算法
     */
    public static void demonstrate() throws InterruptedException {
        System.out.println("========== 令牌桶算法演示 ==========");
        System.out.println();

        // 创建令牌桶：容量10，每秒生成5个令牌
        TokenBucketDemo bucket = new TokenBucketDemo(10, 5);

        System.out.println("配置：桶容量=10，生成速率=5/秒");
        System.out.println();

        // 场景1：突发流量
        System.out.println("【场景1】突发流量（瞬间20个请求）");
        for (int i = 0; i < 20; i++) {
            boolean pass = bucket.tryAcquire();
            System.out.println("请求" + (i + 1) + ": " + (pass ? "通过" : "拒绝") +
                    "，剩余令牌: " + bucket.getAvailableTokens());
        }
        System.out.println("通过: " + bucket.getPassCount() + "，拒绝: " + bucket.getRejectCount());
        System.out.println();

        // 等待令牌补充
        System.out.println("等待2秒，让令牌补充...");
        Thread.sleep(2000);
        bucket.resetStats();

        // 场景2：平稳流量
        System.out.println();
        System.out.println("【场景2】平稳流量（每200ms一个请求）");
        for (int i = 0; i < 20; i++) {
            boolean pass = bucket.tryAcquire();
            System.out.println("请求" + (i + 1) + ": " + (pass ? "通过" : "拒绝") +
                    "，剩余令牌: " + bucket.getAvailableTokens());
            Thread.sleep(200);
        }
        System.out.println("通过: " + bucket.getPassCount() + "，拒绝: " + bucket.getRejectCount());
    }

    /**
     * 主方法
     */
    public static void main(String[] args) throws InterruptedException {
        demonstrate();
    }
}
