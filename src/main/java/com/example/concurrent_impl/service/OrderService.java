package com.example.concurrent_impl.service;

import com.example.concurrent_impl.common.enums.ErrorCode;
import com.example.concurrent_impl.common.exception.BusinessException;
import com.example.concurrent_impl.entity.Order;
import com.example.concurrent_impl.entity.Product;
import com.example.concurrent_impl.entity.User;
import com.example.concurrent_impl.mapper.OrderMapper;
import com.example.concurrent_impl.mapper.ProductMapper;
import com.example.concurrent_impl.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 【Service】订单服务
 *
 * 【业务说明】
 * 处理订单相关的业务逻辑，包含：
 * 1. 创建订单
 * 2. 支付订单
 * 3. 取消订单
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final ProductMapper productMapper;

    /**
     * 创建订单
     *
     * 【业务流程】
     * 1. 检查用户是否存在
     * 2. 检查商品是否存在
     * 3. 检查库存是否充足
     * 4. 扣减库存（乐观锁）
     * 5. 创建订单
     *
     * 【并发安全】
     * 使用乐观锁保证库存扣减的原子性
     *
     * @param userId 用户ID
     * @param productId 商品ID
     * @param quantity 数量
     * @return 订单
     */
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(Long userId, Long productId, Integer quantity) {
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
        order.setTotalAmount(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
        order.setStatus(0); // 待支付

        order = orderMapper.save(order);
        log.info("订单创建成功: {}", order.getOrderNo());

        return order;
    }

    /**
     * 支付订单
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
        log.info("订单支付成功: {}", orderNo);

        return order;
    }

    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis() + String.format("%04d", (int) (Math.random() * 10000));
    }
}
