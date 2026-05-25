package com.example.concurrent_impl.mapper;

import com.example.concurrent_impl.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 【Mapper】订单数据访问层
 */
@Repository
public interface OrderMapper extends JpaRepository<Order, Long> {

    /**
     * 根据订单号查找订单
     */
    Order findByOrderNo(String orderNo);

    /**
     * 根据用户ID查找订单
     */
    List<Order> findByUserId(Long userId);

    /**
     * 根据幂等键查找订单
     */
    Order findByIdempotentKey(String idempotentKey);

    /**
     * 根据用户ID和状态查找订单
     */
    List<Order> findByUserIdAndStatus(Long userId, Integer status);
}
