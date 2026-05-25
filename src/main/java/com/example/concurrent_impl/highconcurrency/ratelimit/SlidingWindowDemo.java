package com.example.concurrent_impl.highconcurrency.ratelimit;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 【限流 - 滑动窗口算法】精确限流实现
 *
 * 【业务背景】
 * 在需要精确统计请求次数的场景中，
 * 固定窗口算法存在临界点突发问题，
 * 滑动窗口可以解决这个问题。
 *
 * 【实现原理】
 * 滑动窗口算法：
 * 1. 将时间划分为多个小窗口
 * 2. 每个小窗口有独立的计数器
 * 3. 统计当前时间窗口内的总请求数
 * 4. 窗口随时间滑动，旧窗口自动过期
 *
 * 【为什么这样写】
 * 1. 滑动窗口解决了固定窗口的临界点问题
 * 2. 统计更精确，限流更平滑
 * 3. 适合需要精确限流的场景
 *
 * 【不遵守的后果】
 * 1. 使用固定窗口：临界点可能通过2倍流量
 * 2. 窗口太小：统计不准确
 * 3. 窗口太大：限流不及时
 *
 * 【正确示例】
 * 使用滑动窗口，精确统计请求数
 *
 * 【错误示例】
 * 使用固定计数器，存在临界点问题
 *
 * 【实际案例】
 * 1. API接口精确限流
 * 2. 用户行为统计
 * 3. 系统监控指标
 *
 * 【算法对比】
 * 固定窗口：
 * - 优点：实现简单
 * - 缺点：临界点可能通过2倍流量
 *
 * 滑动窗口：
 * - 优点：统计精确，限流平滑
 * - 缺点：实现复杂，内存占用稍大
 *
 * @author concurrent_impl
 * @date 2024
 */
public class SlidingWindowDemo {

    /**
     * 窗口大小（毫秒）
     */
    private final long windowSize;

    /**
     * 窗口内允许的最大请求数
     */
    private final int maxRequests;

    /**
     * 子窗口数量
     */
    private final int subWindowCount;

    /**
     * 子窗口大小（毫秒）
     */
    private final long subWindowSize;

    /**
     * 子窗口计数器
     */
    private final AtomicInteger[] subWindowCounters;

    /**
     * 子窗口起始时间
     */
    private final long[] subWindowStartTimes;

    /**
     * 当前子窗口索引
     */
    private volatile int currentSubWindowIndex = 0;

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
     * @param windowSize 窗口大小（毫秒）
     * @param maxRequests 窗口内允许的最大请求数
     * @param subWindowCount 子窗口数量
     */
    public SlidingWindowDemo(long windowSize, int maxRequests, int subWindowCount) {
        this.windowSize = windowSize;
        this.maxRequests = maxRequests;
        this.subWindowCount = subWindowCount;
        this.subWindowSize = windowSize / subWindowCount;

        this.subWindowCounters = new AtomicInteger[subWindowCount];
        this.subWindowStartTimes = new long[subWindowCount];

        long now = System.currentTimeMillis();
        for (int i = 0; i < subWindowCount; i++) {
            subWindowCounters[i] = new AtomicInteger(0);
            subWindowStartTimes[i] = now;
        }
    }

    /**
     * 尝试获取许可
     *
     * 【实现原理】
     * 1. 计算当前时间所在的子窗口
     * 2. 清理过期的子窗口
     * 3. 统计当前窗口内的总请求数
     * 4. 判断是否超过限制
     *
     * @return true-获取成功，false-被限流
     */
    public synchronized boolean tryAcquire() {
        long now = System.currentTimeMillis();

        // 【关键点1】计算当前时间所在的子窗口
        int newSubWindowIndex = (int) ((now / subWindowSize) % subWindowCount);

        // 【关键点2】清理过期的子窗口
        if (newSubWindowIndex != currentSubWindowIndex) {
            // 清理从当前窗口到新窗口之间的所有窗口
            int start = (currentSubWindowIndex + 1) % subWindowCount;
            int end = newSubWindowIndex;

            if (start <= end) {
                for (int i = start; i <= end; i++) {
                    subWindowCounters[i].set(0);
                    subWindowStartTimes[i] = now;
                }
            } else {
                // 跨越数组边界
                for (int i = start; i < subWindowCount; i++) {
                    subWindowCounters[i].set(0);
                    subWindowStartTimes[i] = now;
                }
                for (int i = 0; i <= end; i++) {
                    subWindowCounters[i].set(0);
                    subWindowStartTimes[i] = now;
                }
            }
            currentSubWindowIndex = newSubWindowIndex;
        }

        // 【关键点3】统计当前窗口内的总请求数
        int totalCount = 0;
        for (int i = 0; i < subWindowCount; i++) {
            totalCount += subWindowCounters[i].get();
        }

        // 【关键点4】判断是否超过限制
        if (totalCount < maxRequests) {
            subWindowCounters[newSubWindowIndex].incrementAndGet();
            passCount.incrementAndGet();
            return true;
        }

        // 超过限制，拒绝请求
        rejectCount.incrementAndGet();
        return false;
    }

    /**
     * 获取当前窗口内的总请求数
     */
    public int getCurrentCount() {
        int totalCount = 0;
        for (int i = 0; i < subWindowCount; i++) {
            totalCount += subWindowCounters[i].get();
        }
        return totalCount;
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
     * 演示滑动窗口算法
     */
    public static void demonstrate() throws InterruptedException {
        System.out.println("========== 滑动窗口算法演示 ==========");
        System.out.println();

        // 创建滑动窗口：1秒窗口，最多10个请求，5个子窗口
        SlidingWindowDemo window = new SlidingWindowDemo(1000, 10, 5);

        System.out.println("配置：窗口大小=1秒，最大请求数=10，子窗口数=5");
        System.out.println();

        // 场景1：突发流量
        System.out.println("【场景1】突发流量（瞬间20个请求）");
        for (int i = 0; i < 20; i++) {
            boolean pass = window.tryAcquire();
            System.out.println("请求" + (i + 1) + ": " + (pass ? "通过" : "拒绝") +
                    "，当前窗口请求数: " + window.getCurrentCount());
        }
        System.out.println("通过: " + window.getPassCount() + "，拒绝: " + window.getRejectCount());
        System.out.println();

        // 等待窗口滑动
        System.out.println("等待1.5秒，让窗口滑动...");
        Thread.sleep(1500);

        // 场景2：平稳流量
        System.out.println();
        System.out.println("【场景2】平稳流量（每100ms一个请求）");
        window.passCount.set(0);
        window.rejectCount.set(0);

        for (int i = 0; i < 30; i++) {
            boolean pass = window.tryAcquire();
            System.out.println("请求" + (i + 1) + ": " + (pass ? "通过" : "拒绝") +
                    "，当前窗口请求数: " + window.getCurrentCount());
            Thread.sleep(100);
        }
        System.out.println("通过: " + window.getPassCount() + "，拒绝: " + window.getRejectCount());
    }

    /**
     * 演示固定窗口的临界点问题
     */
    public static void demonstrateFixedWindowProblem() {
        System.out.println("========== 固定窗口临界点问题演示 ==========");
        System.out.println();
        System.out.println("【问题描述】");
        System.out.println("假设限制：每秒10个请求");
        System.out.println();
        System.out.println("【场景】");
        System.out.println("1. 在第0.9秒时，来了10个请求 -> 通过");
        System.out.println("2. 在第1.0秒时，又来了10个请求 -> 通过");
        System.out.println("结果：在0.1秒内通过了20个请求，超过限制！");
        System.out.println();
        System.out.println("【解决方案】");
        System.out.println("使用滑动窗口，将窗口划分为多个小窗口，");
        System.out.println("统计当前时间点往前一个窗口内的请求数。");
    }

    /**
     * 主方法
     */
    public static void main(String[] args) throws InterruptedException {
        demonstrateFixedWindowProblem();
        System.out.println("\n");
        demonstrate();
    }
}
