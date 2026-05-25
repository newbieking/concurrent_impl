package com.example.concurrent_impl.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 【错误码枚举】统一错误码定义
 * 
 * 【业务背景】
 * 在企业级项目中，需要统一定义错误码，便于前后端联调和问题定位。
 * 
 * 【为什么这样写】
 * 1. 集中管理错误码，避免分散在各处导致重复或冲突
 * 2. 错误码与错误信息绑定，保证一致性
 * 3. 使用枚举类型，编译时检查，避免字符串拼写错误
 * 4. 便于后续国际化处理
 * 
 * 【不遵守的后果】
 * 1. 错误码分散管理：容易出现重复或冲突，难以维护
 * 2. 使用魔法数字：代码可读性差，难以理解错误含义
 * 3. 不统一错误码：前端需要针对不同接口处理不同的错误码
 * 
 * 【错误码设计规范】
 * 1. 使用5位数字，便于分类
 * 2. 2开头表示成功
 * 3. 4开头表示客户端错误
 * 4. 5开头表示服务端错误
 * 5. 第3位表示模块，后2位表示具体错误
 * 
 * 【实际案例】
 * 阿里巴巴、美团等大厂都采用统一的错误码规范
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ==================== 成功 ====================
    SUCCESS(200, "操作成功"),

    // ==================== 客户端错误 4xx ====================
    // 通用错误
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权，请先登录"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "请求的资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    PARAM_ERROR(400, "参数校验失败"),

    // 用户相关错误 41x
    USER_NOT_FOUND(410, "用户不存在"),
    USER_ALREADY_EXISTS(411, "用户已存在"),
    PASSWORD_ERROR(412, "密码错误"),
    ACCOUNT_LOCKED(413, "账户已被锁定"),
    ACCOUNT_DISABLED(414, "账户已被禁用"),

    // 订单相关错误 42x
    ORDER_NOT_FOUND(420, "订单不存在"),
    ORDER_ALREADY_PAID(421, "订单已支付"),
    ORDER_ALREADY_CANCELLED(422, "订单已取消"),
    ORDER_STATUS_ERROR(423, "订单状态异常"),
    ORDER_CREATE_FAILED(424, "订单创建失败"),

    // 库存相关错误 43x
    STOCK_NOT_ENOUGH(430, "库存不足"),
    STOCK_LOCK_FAILED(431, "库存锁定失败"),
    STOCK_DEDUCT_FAILED(432, "库存扣减失败"),

    // 支付相关错误 44x
    PAYMENT_FAILED(440, "支付失败"),
    PAYMENT_TIMEOUT(441, "支付超时"),
    PAYMENT_AMOUNT_ERROR(442, "支付金额错误"),
    BALANCE_NOT_ENOUGH(443, "余额不足"),

    // 幂等相关错误 45x
    DUPLICATE_SUBMIT(450, "请勿重复提交"),
    IDEMPOTENT_KEY_EXPIRED(451, "幂等键已过期"),

    // 限流相关错误 46x
    RATE_LIMIT_EXCEEDED(460, "请求频率过高，请稍后重试"),

    // ==================== 服务端错误 5xx ====================
    // 通用错误
    INTERNAL_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂时不可用"),

    // 数据库相关错误 51x
    DB_ERROR(510, "数据库操作失败"),
    DB_DUPLICATE_KEY(511, "数据已存在"),

    // 缓存相关错误 52x
    CACHE_ERROR(520, "缓存操作失败"),
    CACHE_KEY_NOT_FOUND(521, "缓存key不存在"),

    // 消息队列相关错误 53x
    MQ_SEND_FAILED(530, "消息发送失败"),
    MQ_CONSUME_FAILED(531, "消息消费失败"),

    // 分布式锁相关错误 54x
    LOCK_ACQUIRE_FAILED(540, "获取锁失败"),
    LOCK_RELEASE_FAILED(541, "释放锁失败"),

    // 分布式事务相关错误 55x
    TRANSACTION_FAILED(550, "事务执行失败"),
    SAGA_COMPENSATE_FAILED(551, "Saga补偿失败"),
    TCC_CONFIRM_FAILED(552, "TCC确认失败"),
    TCC_CANCEL_FAILED(553, "TCC取消失败");

    /**
     * 错误码
     * 【为什么使用int类型】
     * 1. 数字便于前端判断和处理
     * 2. 可以按照规范进行分类
     * 3. 比字符串更节省传输空间
     */
    private final int code;

    /**
     * 错误信息
     * 【为什么使用中文】
     * 1. 便于开发人员理解
     * 2. 可以直接展示给用户
     * 3. 后续可以改为国际化key
     */
    private final String message;
}
