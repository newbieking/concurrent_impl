package com.example.concurrent_impl.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 【实体类】订单
 *
 * 【业务说明】
 * 存储订单主信息
 *
 * 【状态流转】
 * 0-待支付 -> 1-已支付 -> 2-已发货 -> 3-已完成
 * 0-待支付 -> 4-已取消
 * 1-已支付 -> 5-已退款
 */
@Data
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String orderNo;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal payAmount;

    @Column
    private Integer status = 0;

    @Column
    private Integer payType;

    @Column
    private LocalDateTime payTime;

    @Column
    private LocalDateTime shipTime;

    @Column
    private LocalDateTime receiveTime;

    @Column
    private LocalDateTime cancelTime;

    @Column(length = 200)
    private String cancelReason;

    @Column(unique = true, length = 64)
    private String idempotentKey;

    @Column(length = 500)
    private String remark;

    @Version
    private Integer version = 0;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
