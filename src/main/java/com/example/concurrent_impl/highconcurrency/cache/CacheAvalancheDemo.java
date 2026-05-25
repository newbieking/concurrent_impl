package com.example.concurrent_impl.highconcurrency.cache;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 【缓存策略 - 缓存雪崩】防护实现
 *
 * 【业务背景】
 * 缓存雪崩是指大量缓存key在同一时间失效，
 * 导致大量请求同时穿透到数据库，造成数据库压力骤增。
 *
 * 【典型场景】
 * 1. 系统重启后，大量缓存同时失效
 * 2. 设置了相同的过期时间
 * 3. Redis服务宕机
 *
 * 【实现原理】
 * 1. 随机过期时间：避免大量key同时失效
 * 2. 多级缓存：本地缓存 + Redis缓存
 * 3. 限流降级：限制查询数据库的并发数
 * 4. 缓存预热：系统启动时提前加载热点数据
 *
 * 【为什么这样写】
 * 1. 随机过期时间实现简单，效果好
 * 2. 多级缓存提高可用性
 * 3. 限流降级保护数据库
 *
 * 【不遵守的后果】
 * 1. 不做防护：数据库被打垮
 * 2. 过期时间相同：雪崩效应
 * 3. 不做限流：数据库连接池耗尽
 *
 * 【正确示例】
 * 使用随机过期时间 + 多级缓存
 *
 * 【错误示例】
 * 所有key设置相同的过期时间
 *
 * 【实际案例】
 * 1. 商品列表缓存
 * 2. 首页推荐数据
 * 3. 系统配置信息
 *
 * @author concurrent_impl
 * @date 2024
 */
public class CacheAvalancheDemo {

    /**
     * 模拟缓存（key -> value）
     */
    private static final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    /**
     * 模拟缓存过期时间（key -> 过期时间戳）
     */
    private static final ConcurrentHashMap<String, Long> cacheExpireTimes = new ConcurrentHashMap<>();

    /**
     * 模拟数据库
     */
    private static final ConcurrentHashMap<String, String> database = new ConcurrentHashMap<>();

    /**
     * 数据库查询次数统计
     */
    private static final AtomicInteger dbQueryCount = new AtomicInteger(0);

    /**
     * 基础过期时间（毫秒）
     */
    private static final long BASE_EXPIRE_MS = 5000;

    /**
     * 随机过期时间范围（毫秒）
     */
    private static final long RANDOM_EXPIRE_MS = 3000;

    /**
     * 初始化数据
     */
    static {
        for (int i = 1; i <= 100; i++) {
            database.put("product:" + i, "商品" + i);
        }
    }

    /**
     * 初始化缓存（使用固定过期时间 - 有问题）
     *
     * 【问题】
     * 所有key设置相同的过期时间，会同时失效
     */
    public static void initCacheWithFixedExpire() {
        cache.clear();
        cacheExpireTimes.clear();

        long expireTime = System.currentTimeMillis() + BASE_EXPIRE_MS;
        for (int i = 1; i <= 100; i++) {
            String key = "product:" + i;
            cache.put(key, database.get(key));
            cacheExpireTimes.put(key, expireTime);
        }
    }

    /**
     * 初始化缓存（使用随机过期时间 - 正确）
     *
     * 【关键点】
     * 在基础过期时间上加一个随机值，避免同时失效
     */
    public static void initCacheWithRandomExpire() {
        cache.clear();
        cacheExpireTimes.clear();

        Random random = new Random();
        for (int i = 1; i <= 100; i++) {
            String key = "product:" + i;
            cache.put(key, database.get(key));
            // 【关键点】随机过期时间
            long expireTime = System.currentTimeMillis() + BASE_EXPIRE_MS + random.nextInt((int) RANDOM_EXPIRE_MS);
            cacheExpireTimes.put(key, expireTime);
        }
    }

    /**
     * 查询（带过期检查）
     */
    public static String query(String key) {
        // 检查缓存是否存在且未过期
        String value = cache.get(key);
        Long expireTime = cacheExpireTimes.get(key);

        if (value != null && expireTime != null && System.currentTimeMillis() < expireTime) {
            return value;
        }

        // 缓存不存在或已过期，查数据库
        dbQueryCount.incrementAndGet();
        try {
            // 模拟数据库查询耗时
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        value = database.get(key);
        if (value != null) {
            cache.put(key, value);
            cacheExpireTimes.put(key, System.currentTimeMillis() + BASE_EXPIRE_MS);
        }

        return value;
    }

    /**
     * 演示缓存雪崩问题
     */
    public static void demonstrateAvalanche() throws InterruptedException {
        System.out.println("========== 缓存雪崩问题演示 ==========");
        System.out.println();

        // 使用固定过期时间初始化
        initCacheWithFixedExpire();

        // 等待缓存过期
        System.out.println("等待缓存过期...");
        Thread.sleep(BASE_EXPIRE_MS + 1000);

        // 重置统计
        dbQueryCount.set(0);

        int threadCount = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(threadCount);

        System.out.println("【场景】" + threadCount + "个请求同时查询（缓存已全部过期）");

        long start = System.currentTimeMillis();
        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    query("product:" + ((idx % 100) + 1));
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long time = System.currentTimeMillis() - start;

        System.out.println("耗时: " + time + "ms");
        System.out.println("数据库查询次数: " + dbQueryCount.get());
        System.out.println("【问题】大量请求同时穿透到数据库！");
    }

    /**
     * 演示随机过期时间防护
     */
    public static void demonstrateRandomExpire() throws InterruptedException {
        System.out.println("========== 随机过期时间防护演示 ==========");
        System.out.println();

        // 使用随机过期时间初始化
        initCacheWithRandomExpire();

        // 等待部分缓存过期
        System.out.println("等待部分缓存过期...");
        Thread.sleep(BASE_EXPIRE_MS + 500);

        // 重置统计
        dbQueryCount.set(0);

        int threadCount = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(threadCount);

        System.out.println("【场景】" + threadCount + "个请求同时查询");

        long start = System.currentTimeMillis();
        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    query("product:" + ((idx % 100) + 1));
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long time = System.currentTimeMillis() - start;

        System.out.println("耗时: " + time + "ms");
        System.out.println("数据库查询次数: " + dbQueryCount.get());
        System.out.println("【效果】由于随机过期时间，缓存不会同时失效");
    }

    /**
     * 演示多级缓存防护
     */
    public static void demonstrateMultiLevelCache() {
        System.out.println("========== 多级缓存防护演示 ==========");
        System.out.println();
        System.out.println("【多级缓存架构】");
        System.out.println("1. L1缓存：本地缓存（Caffeine/Guava）");
        System.out.println("2. L2缓存：Redis缓存");
        System.out.println("3. 数据库");
        System.out.println();
        System.out.println("【查询流程】");
        System.out.println("1. 先查L1缓存");
        System.out.println("2. L1未命中，查L2缓存");
        System.out.println("3. L2未命中，查数据库");
        System.out.println("4. 查询结果写入L1和L2");
        System.out.println();
        System.out.println("【优点】");
        System.out.println("1. L1缓存速度快，减少网络开销");
        System.out.println("2. L2缓存容量大，支持分布式");
        System.out.println("3. 即使Redis宕机，本地缓存仍可用");
        System.out.println();
        System.out.println("【代码示例】");
        System.out.println("```java");
        System.out.println("public String query(String key) {");
        System.out.println("    // 1. 查L1缓存");
        System.out.println("    String value = localCache.get(key);");
        System.out.println("    if (value != null) return value;");
        System.out.println();
        System.out.println("    // 2. 查L2缓存");
        System.out.println("    value = redisTemplate.opsForValue().get(key);");
        System.out.println("    if (value != null) {");
        System.out.println("        localCache.put(key, value);");
        System.out.println("        return value;");
        System.out.println("    }");
        System.out.println();
        System.out.println("    // 3. 查数据库");
        System.out.println("    value = database.get(key);");
        System.out.println("    if (value != null) {");
        System.out.println("        redisTemplate.opsForValue().set(key, value, 1, TimeUnit.HOURS);");
        System.out.println("        localCache.put(key, value);");
        System.out.println("    }");
        System.out.println("    return value;");
        System.out.println("}");
        System.out.println("```");
    }

    /**
     * 演示缓存预热
     */
    public static void demonstrateCacheWarmUp() {
        System.out.println("========== 缓存预热演示 ==========");
        System.out.println();
        System.out.println("【缓存预热策略】");
        System.out.println("1. 系统启动时加载热点数据");
        System.out.println("2. 定时任务更新热点数据");
        System.out.println("3. 用户行为预测预加载");
        System.out.println();
        System.out.println("【实现方式】");
        System.out.println("1. @PostConstruct 注解");
        System.out.println("2. CommandLineRunner 接口");
        System.out.println("3. ApplicationListener 接口");
        System.out.println();
        System.out.println("【代码示例】");
        System.out.println("```java");
        System.out.println("@Component");
        System.out.println("public class CacheWarmUp implements CommandLineRunner {");
        System.out.println("    @Override");
        System.out.println("    public void run(String... args) {");
        System.out.println("        // 加载热点商品到缓存");
        System.out.println("        List<Product> hotProducts = productMapper.findHotProducts();");
        System.out.println("        for (Product product : hotProducts) {");
        System.out.println("            redisTemplate.opsForValue().set(");
        System.out.println("                \"product:\" + product.getId(),");
        System.out.println("                product,");
        System.out.println("                1, TimeUnit.HOURS");
        System.out.println("            );");
        System.out.println("        }");
        System.out.println("    }");
        System.out.println("}");
        System.out.println("```");
    }

    /**
     * 主方法
     */
    public static void main(String[] args) throws InterruptedException {
        demonstrateAvalanche();
        System.out.println("\n");
        demonstrateRandomExpire();
        System.out.println("\n");
        demonstrateMultiLevelCache();
        System.out.println("\n");
        demonstrateCacheWarmUp();
    }
}
