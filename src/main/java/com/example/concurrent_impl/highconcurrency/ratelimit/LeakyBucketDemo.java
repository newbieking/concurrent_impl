package com.example.concurrent_impl.highconcurrency.ratelimit;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 【限流 - 漏桶算法】强制平滑限流实现
 *
 * 【业务背景】
 * 在某些场景下，需要强制平滑请求速率，
 * 保证下游服务接收到的请求是均匀的。
 *
 * 【实现原理】
 * 漏桶算法：
 * 1. 请求进入漏桶（队列）
 * 2. 漏桶以固定速率处理请求
 * 3. 如果桶满了，新请求被拒绝
 * 4. 输出速率是恒定的，不会突发
 *
 * 【为什么这样写】
 * 1. 漏桶强制平滑输出，保护下游服务
 * 2. 适合对下游服务有严格速率要求的场景
 * 3. 使用队列实现，简单可靠
 *
 * 【不遵守的后果】
 * 1. 不限流：下游服务被突发流量压垮
 * 2. 使用令牌桶：可能产生突发流量
 * 3. 桶容量太小：正常请求被拒绝
 *
 * 【正确示例】
 * 使用漏桶算法，强制平滑输出
 *
 * 【错误示例】
 * 直接转发请求，不做限流
 *
 * 【实际案例】
 * 1. 第三方API调用（有严格的QPS限制）
 * 2. 消息推送（平滑发送）
 * 3. 数据同步（控制写入速率）
 *
 * 【漏桶 vs 令牌桶】
 * 漏桶：
 * - 优点：强制平滑输出
 * - 缺点：不允许突发流量
 *
 * 令牌桶：
 * - 优点：允许突发流量
 * - 缺点：实现相对复杂
 *
 * @author concurrent_impl
 * @date 2024
 */
public class LeakyBucketDemo {

    /**
     * 桶的最大容量
     */
    private final int bucketCapacity;

    /**
     * 漏水速率（每秒处理的请求数）
     */
    private final int leakRate;

    /**
     * 漏桶（使用队列模拟）
     */
    private final LinkedBlockingQueue<Runnable> bucket;

    /**
     * 统计：通过的请求数
     */
    private final AtomicInteger passCount = new AtomicInteger(0);

    /**
     * 统计：拒绝的请求数
     */
    private final AtomicInteger rejectCount = new AtomicInteger(0);

    /**
     * 是否正在运行
     */
    private volatile boolean running = true;

    /**
     * 构造方法
     *
     * @param bucketCapacity 桶的最大容量
     * @param leakRate 漏水速率（每秒）
     */
    public LeakyBucketDemo(int bucketCapacity, int leakRate) {
        this.bucketCapacity = bucketCapacity;
        this.leakRate = leakRate;
        this.bucket = new LinkedBlockingQueue<>(bucketCapacity);

        // 启动漏水线程
        startLeakThread();
    }

    /**
     * 启动漏水线程
     *
     * 【实现原理】
     * 以固定速率从队列中取出请求并处理
     */
    private void startLeakThread() {
        Thread leakThread = new Thread(() -> {
            // 计算每次漏水的间隔时间（毫秒）
            long interval = 1000 / leakRate;

            while (running) {
                try {
                    // 从队列中取出请求
                    Runnable task = bucket.poll();
                    if (task != null) {
                        // 处理请求
                        task.run();
                        passCount.incrementAndGet();
                    }

                    // 等待固定间隔
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        leakThread.setDaemon(true);
        leakThread.setName("leaky-bucket-thread");
        leakThread.start();
    }

    /**
     * 尝试提交请求
     *
     * @param task 请求任务
     * @return true-提交成功，false-桶已满（被限流）
     */
    public boolean tryAcquire(Runnable task) {
        // 【关键点】尝试将请求放入桶中
        boolean success = bucket.offer(task);
        if (success) {
            return true;
        }

        // 桶已满，拒绝请求
        rejectCount.incrementAndGet();
        return false;
    }

    /**
     * 获取当前桶中的请求数
     */
    public int getCurrentSize() {
        return bucket.size();
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
     * 停止漏桶
     */
    public void stop() {
        running = false;
    }

    /**
     * 演示漏桶算法
     */
    public static void demonstrate() throws InterruptedException {
        System.out.println("========== 漏桶算法演示 ==========");
        System.out.println();

        // 创建漏桶：容量10，每秒处理5个请求
        LeakyBucketDemo bucket = new LeakyBucketDemo(10, 5);

        System.out.println("配置：桶容量=10，漏水速率=5/秒");
        System.out.println();

        // 场景1：突发流量
        System.out.println("【场景1】突发流量（瞬间20个请求）");
        for (int i = 0; i < 20; i++) {
            final int requestId = i + 1;
            boolean success = bucket.tryAcquire(() -> {
                // 模拟业务处理
            });
            System.out.println("请求" + requestId + ": " +
                    (success ? "进入桶中" : "被拒绝") +
                    "，桶中请求数: " + bucket.getCurrentSize());
        }
        System.out.println("进入桶中: " + (bucket.getPassCount() + bucket.getCurrentSize()) +
                "，被拒绝: " + bucket.getRejectCount());
        System.out.println();

        // 等待桶中的请求处理完
        System.out.println("等待桶中的请求处理...");
        Thread.sleep(3000);
        System.out.println("处理完成，通过: " + bucket.getPassCount());
        System.out.println();

        // 场景2：平稳流量
        System.out.println("【场景2】平稳流量（每200ms一个请求）");
        bucket.passCount.set(0);
        bucket.rejectCount.set(0);

        for (int i = 0; i < 20; i++) {
            final int requestId = i + 1;
            boolean success = bucket.tryAcquire(() -> {
                // 模拟业务处理
            });
            System.out.println("请求" + requestId + ": " +
                    (success ? "进入桶中" : "被拒绝") +
                    "，桶中请求数: " + bucket.getCurrentSize());
            Thread.sleep(200);
        }
        System.out.println("通过: " + bucket.getPassCount() + "，拒绝: " + bucket.getRejectCount());

        bucket.stop();
    }

    /**
     * 主方法
     */
    public static void main(String[] args) throws InterruptedException {
        demonstrate();
    }
}
