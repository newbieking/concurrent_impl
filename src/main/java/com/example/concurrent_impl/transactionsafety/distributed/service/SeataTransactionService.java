package com.example.concurrent_impl.transactionsafety.distributed.service;

import io.seata.core.context.RootContext;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 【企业级】Seata全局事务服务
 *
 * 【业务背景】
 * 在微服务架构中，一个业务操作可能涉及多个服务，
 * 每个服务有自己的数据库，无法使用本地事务。
 * Seata是一个开源的分布式事务解决方案，提供了AT、TCC、Saga、XA等模式。
 *
 * 【Seata AT模式原理】
 * 1. 一阶段：
 *    - 拦截业务SQL，解析SQL语义
 *    - 记录before image和after image
 *    - 执行业务SQL
 *    - 提交本地事务
 * 2. 二阶段-提交：
 *    - 删除undo_log
 * 3. 二阶段-回滚：
 *    - 根据undo_log反向补偿
 *
 * 【为什么使用Seata】
 * 1. 对业务无侵入
 * 2. 支持多种模式（AT、TCC、Saga、XA）
 * 3. 支持全局事务和分支事务
 * 4. 提供高可用方案
 *
 * @author concurrent_impl
 * @date 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeataTransactionService {

    /**
     * 转账接口（Seata全局事务）
     *
     * 【实现原理】
     * 1. @GlobalTransactional开启全局事务
     * 2. 调用远程服务（订单服务、账户服务、库存服务）
     * 3. 如果任何服务失败，全局回滚
     * 4. 如果所有服务成功，全局提交
     *
     * @param fromUserId 转出用户ID
     * @param toUserId 转入用户ID
     * @param amount 转账金额
     * @return 是否转账成功
     */
    @GlobalTransactional(name = "seata-transfer-transaction", timeoutMills = 30000)
    public boolean transfer(Long fromUserId, Long toUserId, BigDecimal amount) {
        log.info("开始Seata全局事务: fromUserId={}, toUserId={}, amount={}, xid={}",
                fromUserId, toUserId, amount, RootContext.getXID());

        try {
            // 1. 扣减转出账户余额
            boolean deductResult = deductBalance(fromUserId, amount);
            if (!deductResult) {
                throw new RuntimeException("扣减转出账户余额失败");
            }

            // 2. 增加转入账户余额
            boolean addResult = addBalance(toUserId, amount);
            if (!addResult) {
                throw new RuntimeException("增加转入账户余额失败");
            }

            // 3. 记录转账流水
            saveTransferLog(fromUserId, toUserId, amount);

            log.info("Seata全局事务成功: xid={}", RootContext.getXID());
            return true;

        } catch (Exception e) {
            log.error("Seata全局事务失败，触发回滚: xid={}", RootContext.getXID(), e);
            throw e; // 抛出异常触发全局回滚
        }
    }

    /**
     * 扣减余额（分支事务）
     *
     * 【实现方式】
     * 方式1：本地方法调用（单体应用）
     * 方式2：Feign远程调用（微服务）
     * 方式3：MQ消息（最终一致性）
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deductBalance(Long userId, BigDecimal amount) {
        log.info("扣减余额: userId={}, amount={}", userId, amount);
        // 实际项目中调用账户服务
        // accountService.deduct(userId, amount);
        return true;
    }

    /**
     * 增加余额（分支事务）
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean addBalance(Long userId, BigDecimal amount) {
        log.info("增加余额: userId={}, amount={}", userId, amount);
        // 实际项目中调用账户服务
        // accountService.add(userId, amount);
        return true;
    }

    /**
     * 保存转账流水
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveTransferLog(Long fromUserId, Long toUserId, BigDecimal amount) {
        log.info("保存转账流水: fromUserId={}, toUserId={}, amount={}", fromUserId, toUserId, amount);
        // 实际项目中保存流水记录
        // transferLogMapper.insert(transferLog);
    }

    /**
     * 下单接口（Seata全局事务）
     *
     * 【业务流程】
     * 1. 创建订单
     * 2. 扣减库存
     * 3. 扣减余额
     * 4. 发送通知
     *
     * @param userId 用户ID
     * @param productId 商品ID
     * @param quantity 数量
     * @return 订单号
     */
    @GlobalTransactional(name = "seata-create-order", timeoutMills = 60000)
    public String createOrder(Long userId, Long productId, Integer quantity) {
        log.info("开始创建订单（Seata全局事务）: userId={}, productId={}, quantity={}, xid={}",
                userId, productId, quantity, RootContext.getXID());

        try {
            // 1. 创建订单
            String orderNo = doCreateOrder(userId, productId, quantity);

            // 2. 扣减库存
            boolean deductResult = deductStock(productId, quantity);
            if (!deductResult) {
                throw new RuntimeException("扣减库存失败");
            }

            // 3. 扣减余额（模拟）
            BigDecimal amount = BigDecimal.valueOf(100); // 简化处理
            boolean payResult = deductBalance(userId, amount);
            if (!payResult) {
                throw new RuntimeException("扣减余额失败");
            }

            log.info("创建订单成功（Seata全局事务）: orderNo={}, xid={}", orderNo, RootContext.getXID());
            return orderNo;

        } catch (Exception e) {
            log.error("创建订单失败，触发回滚: xid={}", RootContext.getXID(), e);
            throw e;
        }
    }

    /**
     * 创建订单（内部方法）
     */
    private String doCreateOrder(Long userId, Long productId, Integer quantity) {
        String orderNo = "ORD" + System.currentTimeMillis();
        log.info("创建订单: orderNo={}, userId={}, productId={}, quantity={}", orderNo, userId, productId, quantity);
        // 实际项目中调用订单服务
        // orderService.create(orderNo, userId, productId, quantity);
        return orderNo;
    }

    /**
     * 扣减库存（分支事务）
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deductStock(Long productId, Integer quantity) {
        log.info("扣减库存: productId={}, quantity={}", productId, quantity);
        // 实际项目中调用库存服务
        // stockService.deduct(productId, quantity);
        return true;
    }
}
