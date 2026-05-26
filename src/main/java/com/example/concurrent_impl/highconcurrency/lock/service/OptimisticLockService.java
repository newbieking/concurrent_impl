package com.example.concurrent_impl.highconcurrency.lock.service;

import com.example.concurrent_impl.entity.Product;
import com.example.concurrent_impl.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 【企业级】乐观锁服务
 *
 * 【业务背景】
 * 在电商秒杀场景中，大量用户同时抢购同一商品，
 * 需要保证库存扣减的原子性，防止超卖。
 *
 * 【实现原理】
 * 乐观锁假设不会发生冲突，只在提交更新时检查数据是否被修改。
 * 使用版本号机制：
 * 1. 读取数据时获取版本号
 * 2. 更新时带上版本号
 * 3. 如果版本号不匹配，说明数据被修改，更新失败
 *
 * @author concurrent_impl
 * @date 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OptimisticLockService {

    private final ProductMapper productMapper;

    /**
     * 使用乐观锁扣减库存
     *
     * @param productId 商品ID
     * @param quantity 扣减数量
     * @param maxRetries 最大重试次数
     * @return 是否扣减成功
     */
    public boolean deductStock(Long productId, int quantity, int maxRetries) {
        for (int i = 0; i < maxRetries; i++) {
            // 1. 查询当前商品信息（包含版本号）
            Product product = productMapper.findById(productId).orElse(null);
            if (product == null) {
                log.error("商品不存在: productId={}", productId);
                return false;
            }

            // 2. 检查库存
            if (product.getStock() < quantity) {
                log.warn("库存不足: productId={}, stock={}, requested={}", productId, product.getStock(), quantity);
                return false;
            }

            // 3. 使用乐观锁扣减库存
            int rows = productMapper.deductStock(productId, quantity, product.getVersion());
            if (rows > 0) {
                log.info("库存扣减成功: productId={}, quantity={}, newStock={}", productId, quantity, product.getStock() - quantity);
                return true;
            }

            // 4. 更新失败（版本冲突），重试
            log.warn("库存扣减失败（版本冲突），第{}次重试: productId={}", i + 1, productId);
        }

        log.error("库存扣减失败（超过最大重试次数）: productId={}", productId);
        return false;
    }

    /**
     * 使用乐观锁扣减库存（带事务）
     *
     * @param productId 商品ID
     * @param quantity 扣减数量
     * @return 是否扣减成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deductStockWithTransaction(Long productId, int quantity) {
        Product product = productMapper.findById(productId).orElse(null);
        if (product == null) {
            throw new RuntimeException("商品不存在: " + productId);
        }

        if (product.getStock() < quantity) {
            throw new RuntimeException("库存不足");
        }

        int rows = productMapper.deductStock(productId, quantity, product.getVersion());
        return rows > 0;
    }
}
