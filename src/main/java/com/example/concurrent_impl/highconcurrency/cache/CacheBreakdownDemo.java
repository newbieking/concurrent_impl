package com.example.concurrent_impl.highconcurrency.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 【缓存策略 - 缓存击穿】防护实现
 *
 * 【业务背景】
 * 缓存击穿是指热点key在失效的瞬间，
 * 大量并发请求同时查询这个key，
 * 导致所有请求都穿透到数据库。
 *
 * 【典型场景】
 * 1. 秒杀商品缓存过期
 * 2. 热点新闻缓存过期
 * 3. 热门商品详情缓存过期
 *
 * 【实现原理】
 * 1. 互斥锁：只允许一个线程查询数据库，其他线程等待
 * 2. 逻辑过期：缓存不设置过期时间，由业务逻辑判断是否过期
 * 3. 热点数据永不过期：对热点数据不设置过期时间
 *
 * 【为什么这样写】
 * 1. 互斥锁实现简单，效果好
 * 2. 逻辑过期可以避免缓存雪崩
 * 3. 热点数据永不过期保证高可用
 *
 * 【不遵守的后果】
 * 1. 不做防护：数据库被打垮
 * 2. 使用synchronized：性能差
 * 3. 不处理缓存重建失败：数据不一致
 *
 * 【正确示例】
 * 使用互斥锁或逻辑过期
 *
 * 【错误示例】
 * 直接查数据库，不做任何防护
 *
 * 【实际案例】
 * 1. 秒杀商品详情
 * 2. 热门文章内容
 * 3. 系统配置信息
 *
 * @author concurrent_impl
 * @date 2024
 */
public class CacheBreakdownDemo {

    /**
     * 模拟缓存
     */
    private static final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    /**
     * 模拟数据库
     */
    private static final ConcurrentHashMap<String, String> database = new ConcurrentHashMap<>();

    /**
     * 互斥锁
     */
    private static final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    /**
     * 数据库查询次数统计
     */
    private static final AtomicInteger dbQueryCount = new AtomicInteger(0);

    /**
     * 缓存重建次数统计
     */
    private static final AtomicInteger rebuildCount = new AtomicInteger(0);

    /**
     * 热点key
     */
    private static final String HOT_KEY = "hot_product";

    /**
     * 初始化数据
     */
    static {
        database.put(HOT_KEY, "热点商品数据");
    }

    /**
     * 不做防护的查询（有问题）
     *
     * 【问题】
     * 缓存失效时，所有请求同时查数据库
     */
    public static String queryWithoutProtection(String key) {
        // 先查缓存
        String value = cache.get(key);
        if (value != null) {
            return value;
        }

        // 【问题】缓存未命中，所有线程同时查数据库
        dbQueryCount.incrementAndGet();
        try {
            // 模拟数据库查询耗时
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        value = database.get(key);

        if (value != null) {
            cache.put(key, value);
        }

        return value;
    }

    /**
     * 使用互斥锁防护
     *
     * 【实现原理】
     * 1. 缓存未命中时，尝试获取锁
     * 2. 获取成功，查询数据库并重建缓存
     * 3. 获取失败，等待一段时间后重试查询缓存
     */
    public static String queryWithMutexLock(String key) {
        // 先查缓存
        String value = cache.get(key);
        if (value != null) {
            return value;
        }

        // 缓存未命中，尝试获取锁
        ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());

        if (lock.tryLock()) {
            try {
                // 【关键点1】获取锁成功，再次检查缓存（双重检查）
                value = cache.get(key);
                if (value != null) {
                    return value;
                }

                // 【关键点2】查询数据库并重建缓存
                dbQueryCount.incrementAndGet();
                rebuildCount.incrementAndGet();
                try {
                    Thread.sleep(100); // 模拟数据库查询
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                value = database.get(key);

                if (value != null) {
                    cache.put(key, value);
                }

                return value;
            } finally {
                lock.unlock();
            }
        } else {
            // 【关键点3】获取锁失败，等待后重试
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // 重试查询缓存
            return cache.get(key);
        }
    }

    /**
     * 使用逻辑过期防护
     *
     * 【实现原理】
     * 1. 缓存不设置过期时间
     * 2. 缓存中存储过期时间戳
     * 3. 查询时判断是否过期
     * 4. 过期时异步更新缓存
     */
    private static final ConcurrentHashMap<String, Long> expireTimes = new ConcurrentHashMap<>();
    private static final long LOGICAL_EXPIRE_SECONDS = 5000; // 5秒逻辑过期

    public static String queryWithLogicalExpire(String key) {
        // 先查缓存
        String value = cache.get(key);
        if (value == null) {
            // 缓存不存在，需要重建
            rebuildCache(key);
            return cache.get(key);
        }

        // 检查逻辑过期时间
        Long expireTime = expireTimes.get(key);
        if (expireTime != null && System.currentTimeMillis() > expireTime) {
            // 【关键点】已过期，异步更新缓存
            // 当前请求返回旧数据
            asyncRebuildCache(key);
        }

        return value;
    }

    /**
     * 重建缓存
     */
    private static void rebuildCache(String key) {
        ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());

        if (lock.tryLock()) {
            try {
                // 双重检查
                if (cache.get(key) != null) {
                    return;
                }

                dbQueryCount.incrementAndGet();
                rebuildCount.incrementAndGet();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                String value = database.get(key);
                if (value != null) {
                    cache.put(key, value);
                    // 设置逻辑过期时间
                    expireTimes.put(key, System.currentTimeMillis() + LOGICAL_EXPIRE_SECONDS);
                }
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * 异步重建缓存
     */
    private static void asyncRebuildCache(String key) {
        // 实际项目中应使用线程池异步执行
        new Thread(() -> rebuildCache(key)).start();
    }

    /**
     * 演示缓存击穿问题
     */
    public static void demonstrateBreakdown() throws InterruptedException {
        System.out.println("========== 缓存击穿问题演示 ==========");
        System.out.println();

        // 重置
        cache.clear();
        dbQueryCount.set(0);
        rebuildCount.set(0);

        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(threadCount);

        System.out.println("【场景】100个线程同时查询热点key（缓存已失效）");

        long start = System.currentTimeMillis();
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    queryWithoutProtection(HOT_KEY);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long time = System.currentTimeMillis() - start;

        System.out.println("耗时: " + time + "ms");
        System.out.println("数据库查询次数: " + dbQueryCount.get());
        System.out.println("【问题】所有请求都穿透到数据库！");
    }

    /**
     * 演示互斥锁防护
     */
    public static void demonstrateMutexLock() throws InterruptedException {
        System.out.println("========== 互斥锁防护演示 ==========");
        System.out.println();

        // 重置
        cache.clear();
        dbQueryCount.set(0);
        rebuildCount.set(0);

        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(threadCount);

        System.out.println("【场景】100个线程同时查询热点key（缓存已失效）");

        long start = System.currentTimeMillis();
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    queryWithMutexLock(HOT_KEY);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long time = System.currentTimeMillis() - start;

        System.out.println("耗时: " + time + "ms");
        System.out.println("数据库查询次数: " + dbQueryCount.get());
        System.out.println("缓存重建次数: " + rebuildCount.get());
        System.out.println("【效果】只有一个线程查询数据库，其他线程等待");
    }

    /**
     * 主方法
     */
    public static void main(String[] args) throws InterruptedException {
        demonstrateBreakdown();
        System.out.println("\n");
        demonstrateMutexLock();
    }
}
