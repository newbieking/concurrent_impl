package com.example.concurrent_impl.highconcurrency.cache;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 【缓存策略 - 缓存穿透】防护实现
 *
 * 【业务背景】
 * 缓存穿透是指查询一个一定不存在的数据，
 * 由于缓存不命中，每次都要查数据库，导致数据库压力增大。
 *
 * 【攻击场景】
 * 恶意用户构造大量不存在的ID进行查询，
 * 如：id=-1, id=999999999等
 *
 * 【实现原理】
 * 1. 缓存空值：将查询结果为空的key也缓存起来
 * 2. 布隆过滤器：在缓存前加一层布隆过滤器
 * 3. 参数校验：拦截非法参数
 *
 * 【为什么这样写】
 * 1. 缓存空值实现简单，效果好
 * 2. 布隆过滤器空间效率高
 * 3. 参数校验是第一道防线
 *
 * 【不遵守的后果】
 * 1. 不做防护：数据库被打垮
 * 2. 缓存空值不过期：占用大量内存
 * 3. 不使用布隆过滤器：无法应对大量随机key
 *
 * 【正确示例】
 * 使用缓存空值 + 布隆过滤器
 *
 * 【错误示例】
 * 不缓存空值，每次都查数据库
 *
 * 【实际案例】
 * 1. 用户查询（用户ID不存在）
 * 2. 商品查询（商品ID不存在）
 * 3. 订单查询（订单号不存在）
 *
 * @author concurrent_impl
 * @date 2024
 */
public class CachePenetrationDemo {

    /**
     * 模拟缓存（key -> value）
     */
    private static final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    /**
     * 模拟数据库
     */
    private static final ConcurrentHashMap<String, String> database = new ConcurrentHashMap<>();

    /**
     * 模拟布隆过滤器（使用Set模拟，实际应使用Redis的布隆过滤器）
     */
    private static final Set<String> bloomFilter = new HashSet<>();

    /**
     * 数据库查询次数统计
     */
    private static final AtomicInteger dbQueryCount = new AtomicInteger(0);

    /**
     * 缓存命中次数统计
     */
    private static final AtomicInteger cacheHitCount = new AtomicInteger(0);

    /**
     * 空值缓存过期时间（秒）
     */
    private static final int NULL_CACHE_EXPIRE_SECONDS = 60;

    /**
     * 空值标记
     */
    private static final String NULL_VALUE = "NULL";

    /**
     * 初始化数据
     */
    static {
        // 模拟数据库中有100条数据
        for (int i = 1; i <= 100; i++) {
            String key = "user:" + i;
            database.put(key, "用户" + i);
            bloomFilter.add(key);
        }
    }

    /**
     * 不做防护的查询（有问题）
     *
     * 【问题】
     * 查询不存在的数据时，每次都穿透到数据库
     */
    public static String queryWithoutProtection(String key) {
        // 先查缓存
        String value = cache.get(key);
        if (value != null) {
            cacheHitCount.incrementAndGet();
            return value;
        }

        // 缓存未命中，查数据库
        dbQueryCount.incrementAndGet();
        value = database.get(key);

        if (value != null) {
            // 数据库有数据，写入缓存
            cache.put(key, value);
        }
        // 【问题】数据库没有数据时，不缓存，下次还会查数据库

        return value;
    }

    /**
     * 使用缓存空值防护
     *
     * 【实现原理】
     * 查询结果为空时，也将空值缓存起来，设置较短的过期时间
     */
    public static String queryWithNullCache(String key) {
        // 先查缓存
        String value = cache.get(key);
        if (value != null) {
            cacheHitCount.incrementAndGet();
            // 判断是否是空值缓存
            if (NULL_VALUE.equals(value)) {
                return null;
            }
            return value;
        }

        // 缓存未命中，查数据库
        dbQueryCount.incrementAndGet();
        value = database.get(key);

        if (value != null) {
            // 数据库有数据，写入缓存
            cache.put(key, value);
        } else {
            // 【关键点】数据库没有数据，缓存空值
            cache.put(key, NULL_VALUE);
            // 实际项目中应设置过期时间
            // redisTemplate.opsForValue().set(key, NULL_VALUE, 60, TimeUnit.SECONDS);
        }

        return value;
    }

    /**
     * 使用布隆过滤器防护
     *
     * 【实现原理】
     * 1. 查询前先检查布隆过滤器
     * 2. 如果布隆过滤器判断不存在，直接返回
     * 3. 如果布隆过滤器判断存在，再查缓存和数据库
     *
     * 【注意】
     * 布隆过滤器存在误判（判断存在可能不存在），
     * 但不会漏判（判断不存在一定不存在）
     */
    public static String queryWithBloomFilter(String key) {
        // 【关键点1】先检查布隆过滤器
        if (!bloomFilter.contains(key)) {
            // 布隆过滤器判断不存在，直接返回
            // 实际项目中：bloomFilter.exists(key)
            return null;
        }

        // 布隆过滤器判断存在，查缓存
        String value = cache.get(key);
        if (value != null) {
            cacheHitCount.incrementAndGet();
            if (NULL_VALUE.equals(value)) {
                return null;
            }
            return value;
        }

        // 缓存未命中，查数据库
        dbQueryCount.incrementAndGet();
        value = database.get(key);

        if (value != null) {
            cache.put(key, value);
        } else {
            cache.put(key, NULL_VALUE);
        }

        return value;
    }

    /**
     * 演示缓存穿透问题
     */
    public static void demonstratePenetration() {
        System.out.println("========== 缓存穿透问题演示 ==========");
        System.out.println();

        // 重置统计
        cache.clear();
        dbQueryCount.set(0);
        cacheHitCount.set(0);

        // 模拟攻击：查询不存在的数据
        System.out.println("【攻击场景】查询1000个不存在的key");
        for (int i = 1001; i <= 2000; i++) {
            queryWithoutProtection("user:" + i);
        }

        System.out.println("数据库查询次数: " + dbQueryCount.get());
        System.out.println("缓存命中次数: " + cacheHitCount.get());
        System.out.println("【问题】所有请求都穿透到数据库！");
    }

    /**
     * 演示缓存空值防护
     */
    public static void demonstrateNullCache() {
        System.out.println("========== 缓存空值防护演示 ==========");
        System.out.println();

        // 重置
        cache.clear();
        dbQueryCount.set(0);
        cacheHitCount.set(0);

        // 第一轮：查询不存在的数据
        System.out.println("【第一轮】查询1000个不存在的key");
        for (int i = 1001; i <= 2000; i++) {
            queryWithNullCache("user:" + i);
        }
        System.out.println("数据库查询次数: " + dbQueryCount.get());

        // 第二轮：再次查询同样的数据
        System.out.println();
        System.out.println("【第二轮】再次查询同样的key");
        dbQueryCount.set(0);
        for (int i = 1001; i <= 2000; i++) {
            queryWithNullCache("user:" + i);
        }
        System.out.println("数据库查询次数: " + dbQueryCount.get());
        System.out.println("【效果】第二次查询都命中了缓存（空值）");
    }

    /**
     * 演示布隆过滤器防护
     */
    public static void demonstrateBloomFilter() {
        System.out.println("========== 布隆过滤器防护演示 ==========");
        System.out.println();

        // 重置
        cache.clear();
        dbQueryCount.set(0);
        cacheHitCount.set(0);

        // 查询不存在的数据
        System.out.println("【测试】查询1000个不存在的key");
        for (int i = 1001; i <= 2000; i++) {
            queryWithBloomFilter("user:" + i);
        }
        System.out.println("数据库查询次数: " + dbQueryCount.get());
        System.out.println("【效果】布隆过滤器直接拦截，没有查数据库");

        // 查询存在的数据
        System.out.println();
        System.out.println("【测试】查询100个存在的key");
        dbQueryCount.set(0);
        for (int i = 1; i <= 100; i++) {
            queryWithBloomFilter("user:" + i);
        }
        System.out.println("数据库查询次数: " + dbQueryCount.get());
        System.out.println("【效果】存在的数据正常查询");
    }

    /**
     * 主方法
     */
    public static void main(String[] args) {
        demonstratePenetration();
        System.out.println("\n");
        demonstrateNullCache();
        System.out.println("\n");
        demonstrateBloomFilter();
    }
}
