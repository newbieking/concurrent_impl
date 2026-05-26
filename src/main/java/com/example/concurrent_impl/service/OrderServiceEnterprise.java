package com.example.concurrent_impl.service;

import com.example.concurrent_impl.common.enums.ErrorCode;
import com.example.concurrent_impl.common.exception.BusinessException;
import com.example.concurrent_impl.entity.LocalMessage;
import com.example.concurrent_impl.entity.Order;
import com.example.concurrent_impl.entity.Product;
import com.example.concurrent_impl.entity.User;
import com.example.concurrent_impl.entity.enums.MessageStatus;
import com.example.concurrent_impl.highconcurrency.lock.service.RedisLockService;
import com.example.concurrent_impl.mapper.LocalMessageMapper;
import com.example.concurrent_impl.mapper.OrderMapper;
import com.example.concurrent_impl.mapper.ProductMapper;
import com.example.concurrent_impl.mapper.UserMapper;
import com.example.concurrent_impl.transactionsafety.distributed.service.LocalMessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 【企业级】订单服务
 *
 * 【业务说明】
 * 处理订单相关的业务逻辑，包含：
 * 1. 创建订单（带分布式锁）
 * 2. 支付订单（带乐观锁）
 * 3. 取消订单
 * 4. 订单消息通知
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceEnterprise {

    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final ProductMapper productMapper;
    private final LocalMessageMapper localMessageMapper;
    private final RedisLockService redisLockService;
    private final LocalMessageService localMessageService;
    private final ObjectMapper objectMapper;

    /**
     * 创建订单（带分布式锁）
     *
     * 【业务流程】
     * 1. 获取分布式锁
     * 2. 检查用户是否存在
     * 3. 检查商品是否存在
     * 4. 检查库存是否充足
     * 5. 扣减库存（乐观锁）
     * 6. 创建订单
     * 7. 保存本地消息
     * 8. 释放锁
     *
     * 【并发安全】
     * 1. 使用分布式锁防止重复下单
     * 2. 使用乐观锁保证库存扣减的原子性
     *
     * @param userId 用户ID
     * @param productId 商品ID
     * @param quantity 数量
     * @return 订单
     */
    public Order createOrder(Long userId, Long productId, Integer quantity) {
        // 生成锁的key（用户+商品）
        String lockKey = "order:create:" + userId + ":" + productId;

        // 使用分布式锁
        return redisLockService.executeWithLock(lockKey, 30, () -> {
            return doCreateOrder(userId, productId, quantity);
        });
    }

    /**
     * 创建订单（内部实现）
     */
    @Transactional(rollbackFor = Exception.class)
    protected Order doCreateOrder(Long userId, Long productId, Integer quantity) {
        // 1. 检查用户
        User user = userMapper.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 2. 检查商品
        Product product = productMapper.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        // 3. 检查库存
        if (product.getStock() < quantity) {
            throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH);
        }

        // 4. 扣减库存（乐观锁）
        int rows = productMapper.deductStock(productId, quantity, product.getVersion());
        if (rows == 0) {
            throw new BusinessException(ErrorCode.STOCK_DEDUCT_FAILED, "库存扣减失败，请重试");
        }

        // 5. 创建订单
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setProductId(productId);
        order.setQuantity(quantity);
        order.setTotalAmount(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
        order.setStatus(0); // 待支付

        order = orderMapper.save(order);
        log.info("订单创建成功: orderNo={}, userId={}, productId={}, quantity={}",
                order.getOrderNo(), userId, productId, quantity);

        // 6. 保存本地消息（用于通知库存服务等）
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("orderId", order.getId());
            message.put("orderNo", order.getOrderNo());
            message.put("userId", userId);
            message.put("productId", productId);
            message.put("quantity", quantity);

            String content = objectMapper.writeValueAsString(message);
            localMessageService.saveMessage("order.exchange", "order.create", content);
        } catch (Exception e) {
            log.error("保存本地消息失败", e);
            // 不影响订单创建
        }

        return order;
    }

    /**
     * 支付订单（带乐观锁）
     *
     * 【业务流程】
     * 1. 检查订单是否存在
     * 2. 检查订单状态
     * 3. 检查用户余额
     * 4. 扣减余额（乐观锁）
     * 5. 更新订单状态
     *
     * @param orderNo 订单号
     * @return 订单
     */
    @Transactional(rollbackFor = Exception.class)
    public Order payOrder(String orderNo) {
        // 1. 检查订单
        Order order = orderMapper.findByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 2. 检查状态
        if (order.getStatus() != 0) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "订单状态异常");
        }

        // 3. 检查余额
        User user = userMapper.findById(order.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getBalance().compareTo(order.getTotalAmount()) < 0) {
            throw new BusinessException(ErrorCode.BALANCE_NOT_ENOUGH);
        }

        // 4. 扣减余额（乐观锁）
        int rows = userMapper.deductBalance(order.getUserId(), order.getTotalAmount(), user.getVersion());
        if (rows == 0) {
            throw new BusinessException(ErrorCode.PAYMENT_FAILED, "扣款失败，请重试");
        }

        // 5. 更新订单状态
        order.setStatus(1); // 已支付
        order = orderMapper.save(order);
        log.info("订单支付成功: orderNo={}", orderNo);

        // 6. 保存本地消息
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("orderId", order.getId());
            message.put("orderNo", orderNo);
            message.put("userId", order.getUserId());
            message.put("amount", order.getTotalAmount());

            String content = objectMapper.writeValueAsString(message);
            localMessageService.saveMessage("order.exchange", "order.paid", content);
        } catch (Exception e) {
            log.error("保存本地消息失败", e);
        }

        return order;
    }

    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis() + String.format("%04d", (int) (Math.random() * 10000));
    }
}
