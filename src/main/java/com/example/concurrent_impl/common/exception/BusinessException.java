package com.example.concurrent_impl.common.exception;

import com.example.concurrent_impl.common.enums.ErrorCode;
import lombok.Getter;

/**
 * 【业务异常】自定义业务异常类
 * 
 * 【业务背景】
 * 在企业级项目中，需要区分业务异常和系统异常。
 * 业务异常是可预期的，如参数错误、库存不足等；
 * 系统异常是不可预期的，如数据库连接失败、网络超时等。
 * 
 * 【为什么这样写】
 * 1. 继承RuntimeException，无需显式捕获，便于异常传播
 * 2. 包含错误码，便于前端统一处理
 * 3. 支持自定义错误消息，灵活性更强
 * 4. 使用@Getter自动生成getter方法
 * 
 * 【不遵守的后果】
 * 1. 不区分业务异常和系统异常：导致所有异常都返回500，用户体验差
 * 2. 使用checked exception：代码中充斥着try-catch，可读性差
 * 3. 不包含错误码：前端无法根据不同错误进行不同处理
 * 
 * 【正确示例】
 * 抛出业务异常：throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH);
 * 
 * 【错误示例】
 * 1. 抛出RuntimeException：throw new RuntimeException("库存不足");
 * 2. 返回null：return null; // 前端无法知道具体错误原因
 * 
 * 【实际案例】
 * 1. 库存不足时抛出STOCK_NOT_ENOUGH异常
 * 2. 订单不存在时抛出ORDER_NOT_FOUND异常
 * 3. 用户余额不足时抛出BALANCE_NOT_ENOUGH异常
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     * 【为什么需要错误码】
     * 1. 前端可以根据错误码进行不同的处理
     * 2. 便于日志追踪和问题定位
     * 3. 可以与错误码枚举对应
     */
    private final int code;

    /**
     * 错误码枚举
     * 【为什么保留枚举】
     * 1. 便于程序中判断异常类型
     * 2. 可以获取预定义的错误信息
     */
    private final ErrorCode errorCode;

    /**
     * 构造方法：使用错误码枚举
     * 
     * 【使用场景】
     * 抛出预定义的业务异常
     * 
     * @param errorCode 错误码枚举
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.errorCode = errorCode;
    }

    /**
     * 构造方法：使用错误码枚举和自定义消息
     * 
     * 【使用场景】
     * 需要返回特定错误信息时，如参数校验失败的具体字段
     * 
     * @param errorCode 错误码枚举
     * @param message 自定义错误消息
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.errorCode = errorCode;
    }

    /**
     * 构造方法：使用自定义错误码和消息
     * 
     * 【使用场景】
     * 错误码枚举中没有定义的错误
     * 
     * @param code 错误码
     * @param message 错误消息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.errorCode = null;
    }

    /**
     * 构造方法：使用错误码枚举和原因
     * 
     * 【使用场景】
     * 需要保留原始异常信息时
     * 
     * @param errorCode 错误码枚举
     * @param cause 原始异常
     */
    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode.getCode();
        this.errorCode = errorCode;
    }
}
