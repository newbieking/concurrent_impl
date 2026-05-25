package com.example.concurrent_impl.businesssecurity.idempotent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 【幂等 - 幂等键方式防重复提交】基于业务幂等键的实现
 *
 * 【业务背景】
 * 在某些场景下，需要根据业务特征判断是否重复，
 * 如：同一个用户对同一个商品只能下一单。
 *
 * 【实现原理】
 * 1. 根据业务特征生成幂等键（如：用户ID + 商品ID）
 * 2. 提交前检查幂等键是否存在
 * 3. 如果存在，说明已处理过，直接返回之前的结果
 * 4. 如果不存在，执行业务逻辑并记录幂等键
 *
 * 【为什么这样写】
 * 1. 幂等键与业务关联，更准确
 * 2. 支持返回之前的结果
 * 3. 适合需要业务唯一性的场景
 *
 * 【不遵守的后果】
 * 1. 不使用幂等键：可能出现重复订单
 * 2. 幂等键设计不合理：无法正确判断重复
 * 3. 不存储结果：无法返回之前的结果
 *
 * 【正确示例】
 * 使用业务特征生成幂等键
 *
 * 【错误示例】
 * 使用随机数作为幂等键
 *
 * 【实际案例】
 * 1. 订单创建（用户 + 商品 + 时间窗口）
 * 2. 支付回调（订单号）
 * 3. 退款申请（订单号 + 退款原因）
 *
 * @author concurrent_impl
 * @date 2024
 */
public class KeyIdempotentDemo {

    /**
     * 模拟Redis存储幂等键和结果
     * 实际项目中应使用StringRedisTemplate
     */
    private static final ConcurrentHashMap<String, IdempotentResult> idempotentStore = new ConcurrentHashMap<>();

    /**
     * 幂等键过期时间（秒）
     */
    private static final int KEY_EXPIRE_SECONDS = 86400; // 24小时

    /**
     * 幂等结果
     */
    @lombok.Data
    public static class IdempotentResult {
        private Object data;
        private long timestamp;
        private boolean success;

        public IdempotentResult(Object data, boolean success) {
            this.data = data;
            this.success = success;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * 生成订单幂等键
     *
     * 【幂等键设计】
     * 用户ID + 商品ID + 日期（同一天同一用户同一商品只能下一单）
     *
     * @param userId 用户ID
     * @param productId 商品ID
     * @return 幂等键
     */
    public static String generateOrderKey(String userId, String productId) {
        // 获取当前日期（年月日）
        String date = java.time.LocalDate.now().toString();
        return "order:" + userId + ":" + productId + ":" + date;
    }

    /**
     * 检查幂等键是否存在
     *
     * @param idempotentKey 幂等键
     * @return 是否存在
     */
    public static boolean exists(String idempotentKey) {
        IdempotentResult result = idempotentStore.get(idempotentKey);
        if (result == null) {
            return false;
        }

        // 检查是否过期
        if (System.currentTimeMillis() - result.getTimestamp() > KEY_EXPIRE_SECONDS * 1000L) {
            idempotentStore.remove(idempotentKey);
            return false;
        }

        return true;
    }

    /**
     * 获取幂等结果
     *
     * @param idempotentKey 幂等键
     * @return 幂等结果
     */
    public static IdempotentResult getResult(String idempotentKey) {
        return idempotentStore.get(idempotentKey);
    }

    /**
     * 设置幂等结果
     *
     * @param idempotentKey 幂等键
     * @param result 结果
     */
    public static void setResult(String idempotentKey, Object result) {
        idempotentStore.put(idempotentKey, new IdempotentResult(result, true));
    }

    /**
     * 设置幂等结果（失败）
     *
     * @param idempotentKey 幂等键
     * @param result 结果
     */
    public static void setFailureResult(String idempotentKey, Object result) {
        idempotentStore.put(idempotentKey, new IdempotentResult(result, false));
    }

    /**
     * 删除幂等键
     *
     * @param idempotentKey 幂等键
     */
    public static void remove(String idempotentKey) {
        idempotentStore.remove(idempotentKey);
    }

    /**
     * 模拟订单创建（带幂等检查）
     *
     * @param userId 用户ID
     * @param productId 商品ID
     * @param quantity 数量
     * @return 订单结果
     */
    public static String createOrder(String userId, String productId, int quantity) {
        String idempotentKey = generateOrderKey(userId, productId);

        // 【关键点1】检查幂等键
        if (exists(idempotentKey)) {
            IdempotentResult existingResult = getResult(idempotentKey);
            System.out.println("订单已存在，返回之前的结果: " + existingResult.getData());
            return (String) existingResult.getData();
        }

        // 【关键点2】执行业务逻辑
        String orderId = "ORD" + System.currentTimeMillis();
        System.out.println("创建订单: " + orderId);

        // 模拟业务处理
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 【关键点3】存储结果
        setResult(idempotentKey, orderId);

        return orderId;
    }

    /**
     * 演示幂等键方式防重复提交
     */
    public static void demonstrateKeyIdempotent() {
        System.out.println("========== 幂等键方式防重复提交演示 ==========");
        System.out.println();

        String userId = "user123";
        String productId = "product456";

        // 场景1：正常下单
        System.out.println("【场景1】正常下单");
        String orderId1 = createOrder(userId, productId, 1);
        System.out.println("订单ID: " + orderId1);
        System.out.println();

        // 场景2：重复下单
        System.out.println("【场景2】重复下单（同一用户同一商品）");
        String orderId2 = createOrder(userId, productId, 1);
        System.out.println("订单ID: " + orderId2);
        System.out.println("是否同一订单: " + orderId1.equals(orderId2));
        System.out.println();

        // 场景3：不同用户下单
        System.out.println("【场景3】不同用户下单（同一商品）");
        String orderId3 = createOrder("user789", productId, 1);
        System.out.println("订单ID: " + orderId3);
        System.out.println("是否不同订单: " + !orderId1.equals(orderId3));
        System.out.println();

        // 场景4：不同商品下单
        System.out.println("【场景4】不同商品下单（同一用户）");
        String orderId4 = createOrder(userId, "product999", 1);
        System.out.println("订单ID: " + orderId4);
        System.out.println("是否不同订单: " + !orderId1.equals(orderId4));
    }

    /**
     * 主方法
     */
    public static void main(String[] args) {
        demonstrateKeyIdempotent();
    }
}
