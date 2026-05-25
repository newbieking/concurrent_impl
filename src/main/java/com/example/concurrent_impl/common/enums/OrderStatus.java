package com.example.concurrent_impl.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 【订单状态枚举】定义订单的生命周期状态
 * 
 * 【业务背景】
 * 订单状态是订单系统的核心，通过状态机管理订单的生命周期，
 * 确保状态流转的合法性和数据的一致性。
 * 
 * 【为什么这样写】
 * 1. 使用枚举定义状态，避免魔法数字
 * 2. 状态值有明确的业务含义
 * 3. 支持通过code查询枚举，便于数据库存储和反序列化
 * 
 * 【不遵守的后果】
 * 1. 使用魔法数字：代码可读性差，难以维护
 * 2. 不统一状态定义：不同地方使用不同的状态值，导致数据不一致
 * 3. 不使用状态机：状态流转混乱，可能出现非法状态
 * 
 * 【状态流转规则】
 * 待支付 -> 已支付 -> 已发货 -> 已完成
 * 待支付 -> 已取消
 * 已支付 -> 已退款
 * 
 * 【实际案例】
 * 1. 用户下单后，订单状态为"待支付"
 * 2. 用户支付成功后，订单状态变为"已支付"
 * 3. 商家发货后，订单状态变为"已发货"
 * 4. 用户确认收货后，订单状态变为"已完成"
 */
@Getter
@AllArgsConstructor
public enum OrderStatus {

    /**
     * 待支付
     * 【业务含义】用户已提交订单，等待支付
     * 【可流转状态】已支付、已取消
     */
    PENDING_PAYMENT(0, "待支付"),

    /**
     * 已支付
     * 【业务含义】用户已完成支付，等待商家发货
     * 【可流转状态】已发货、已退款
     */
    PAID(1, "已支付"),

    /**
     * 已发货
     * 【业务含义】商家已发货，等待用户确认收货
     * 【可流转状态】已完成
     */
    SHIPPED(2, "已发货"),

    /**
     * 已完成
     * 【业务含义】用户已确认收货，订单完成
     * 【可流转状态】无（终态）
     */
    COMPLETED(3, "已完成"),

    /**
     * 已取消
     * 【业务含义】订单已取消（用户主动取消或超时取消）
     * 【可流转状态】无（终态）
     */
    CANCELLED(4, "已取消"),

    /**
     * 已退款
     * 【业务含义】订单已退款
     * 【可流转状态】无（终态）
     */
    REFUNDED(5, "已退款");

    /**
     * 状态码
     * 【为什么使用int类型】
     * 1. 便于数据库存储，占用空间小
     * 2. 便于前端判断和处理
     */
    private final int code;

    /**
     * 状态描述
     * 【为什么使用中文】
     * 1. 便于开发人员理解
     * 2. 可以直接展示给用户
     */
    private final String description;

    /**
     * 根据状态码获取枚举
     * 
     * 【使用场景】
     * 从数据库读取状态码后转换为枚举
     * 
     * @param code 状态码
     * @return 对应的枚举，如果不存在返回null
     */
    public static OrderStatus fromCode(int code) {
        for (OrderStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }

    /**
     * 判断是否可以流转到目标状态
     * 
     * 【使用场景】
     * 更新订单状态前，先判断状态流转是否合法
     * 
     * @param targetStatus 目标状态
     * @return 是否可以流转
     */
    public boolean canTransitTo(OrderStatus targetStatus) {
        return switch (this) {
            case PENDING_PAYMENT -> targetStatus == PAID || targetStatus == CANCELLED;
            case PAID -> targetStatus == SHIPPED || targetStatus == REFUNDED;
            case SHIPPED -> targetStatus == COMPLETED;
            case COMPLETED, CANCELLED, REFUNDED -> false; // 终态，不能流转
        };
    }
}
