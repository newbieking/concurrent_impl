package com.example.concurrent_impl.mapper;

import com.example.concurrent_impl.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 【Mapper】商品数据访问层
 */
@Repository
public interface ProductMapper extends JpaRepository<Product, Long> {

    /**
     * 使用乐观锁扣减库存
     *
     * @param id 商品ID
     * @param quantity 扣减数量
     * @param version 期望版本号
     * @return 更新行数
     */
    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock - :quantity, p.version = p.version + 1 " +
           "WHERE p.id = :id AND p.version = :version AND p.stock >= :quantity")
    int deductStock(@Param("id") Long id, @Param("quantity") Integer quantity, @Param("version") Integer version);

    /**
     * 使用乐观锁增加库存
     *
     * @param id 商品ID
     * @param quantity 增加数量
     * @param version 期望版本号
     * @return 更新行数
     */
    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock + :quantity, p.version = p.version + 1 " +
           "WHERE p.id = :id AND p.version = :version")
    int addStock(@Param("id") Long id, @Param("quantity") Integer quantity, @Param("version") Integer version);
}
