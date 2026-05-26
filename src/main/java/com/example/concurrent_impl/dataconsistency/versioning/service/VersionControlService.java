package com.example.concurrent_impl.dataconsistency.versioning.service;

import com.example.concurrent_impl.entity.Product;
import com.example.concurrent_impl.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 【企业级】版本控制服务
 *
 * 【业务背景】
 * 在并发更新场景中，需要使用版本控制来保证数据一致性，
 * 防止数据被覆盖。
 *
 * 【实现原理】
 * 1. 每条数据有一个版本号
 * 2. 更新时带上版本号
 * 3. 如果版本号不匹配，更新失败
 * 4. 更新成功后版本号+1
 *
 * @author concurrent_impl
 * @date 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VersionControlService {

    private final ProductMapper productMapper;

    /**
     * 使用乐观锁更新商品价格
     *
     * @param productId 商品ID
     * @param newPrice 新价格
     * @param maxRetries 最大重试次数
     * @return 是否更新成功
     */
    public boolean updateProductPrice(Long productId, BigDecimal newPrice, int maxRetries) {
        for (int i = 0; i < maxRetries; i++) {
            // 1. 查询当前商品信息（包含版本号）
            Product product = productMapper.findById(productId).orElse(null);
            if (product == null) {
                log.error("商品不存在: productId={}", productId);
                return false;
            }

            // 2. 更新价格（使用乐观锁）
            int rows = updatePriceWithVersion(productId, newPrice, product.getVersion());
            if (rows > 0) {
                log.info("商品价格更新成功: productId={}, newPrice={}, oldVersion={}, newVersion={}",
                        productId, newPrice, product.getVersion(), product.getVersion() + 1);
                return true;
            }

            // 3. 更新失败（版本冲突），重试
            log.warn("商品价格更新失败（版本冲突），第{}次重试: productId={}", i + 1, productId);
        }

        log.error("商品价格更新失败（超过最大重试次数）: productId={}", productId);
        return false;
    }

    /**
     * 使用乐观锁更新商品库存
     *
     * @param productId 商品ID
     * @param newStock 新库存
     * @param maxRetries 最大重试次数
     * @return 是否更新成功
     */
    public boolean updateProductStock(Long productId, int newStock, int maxRetries) {
        for (int i = 0; i < maxRetries; i++) {
            Product product = productMapper.findById(productId).orElse(null);
            if (product == null) {
                log.error("商品不存在: productId={}", productId);
                return false;
            }

            int rows = updateStockWithVersion(productId, newStock, product.getVersion());
            if (rows > 0) {
                log.info("商品库存更新成功: productId={}, newStock={}", productId, newStock);
                return true;
            }

            log.warn("商品库存更新失败（版本冲突），第{}次重试: productId={}", i + 1, productId);
        }

        return false;
    }

    /**
     * 使用乐观锁更新价格（内部方法）
     */
    private int updatePriceWithVersion(Long productId, BigDecimal newPrice, Integer version) {
        // 使用JPA的@Version注解自动处理版本号
        Product product = productMapper.findById(productId).orElse(null);
        if (product == null || !product.getVersion().equals(version)) {
            return 0;
        }

        product.setPrice(newPrice);
        productMapper.save(product);
        return 1;
    }

    /**
     * 使用乐观锁更新库存（内部方法）
     */
    private int updateStockWithVersion(Long productId, int newStock, Integer version) {
        Product product = productMapper.findById(productId).orElse(null);
        if (product == null || !product.getVersion().equals(version)) {
            return 0;
        }

        product.setStock(newStock);
        productMapper.save(product);
        return 1;
    }
}
