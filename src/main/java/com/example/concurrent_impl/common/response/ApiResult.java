package com.example.concurrent_impl.common.response;

import com.example.concurrent_impl.common.enums.ErrorCode;
import lombok.Data;

import java.io.Serializable;

/**
 * 【统一响应封装】API 响应结果封装类
 * 
 * 【业务背景】
 * 在企业级项目中，所有API接口都需要返回统一的响应格式，便于前端处理和错误追踪。
 * 
 * 【为什么这样写】
 * 1. 统一响应格式便于前端统一处理，减少重复代码
 * 2. 包含错误码和错误信息，便于问题定位
 * 3. 泛型设计支持返回不同类型的数据
 * 4. 实现Serializable接口，支持序列化传输
 * 
 * 【不遵守的后果】
 * 1. 不统一响应格式：前端需要针对每个接口单独处理，代码冗余
 * 2. 不包含错误码：出现问题时难以定位具体原因
 * 3. 不使用泛型：每个接口都要创建不同的响应类
 * 
 * 【正确示例】
 * 使用统一的ApiResult封装所有接口响应
 * 
 * 【错误示例】
 * 直接返回对象或Map，没有统一格式
 * 
 * @param <T> 响应数据类型
 */
@Data
public class ApiResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 状态码
     * 【为什么使用int】
     * 1. 便于前端判断成功/失败
     * 2. 可以定义业务错误码，快速定位问题
     */
    private int code;

    /**
     * 响应消息
     * 【为什么需要message】
     * 1. 成功时可以返回提示信息
     * 2. 失败时返回错误描述，便于调试
     */
    private String message;

    /**
     * 响应数据
     * 【为什么使用泛型】
     * 1. 支持返回任意类型的数据
     * 2. 编译时类型检查，避免ClassCastException
     */
    private T data;

    /**
     * 时间戳
     * 【为什么需要时间戳】
     * 1. 便于日志追踪和问题排查
     * 2. 可以用于接口响应时间统计
     */
    private long timestamp;

    public ApiResult() {
        this.timestamp = System.currentTimeMillis();
    }

    public ApiResult(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 成功响应（无数据）
     * 
     * 【使用场景】
     * 更新、删除等不需要返回数据的操作
     */
    public static <T> ApiResult<T> success() {
        return new ApiResult<>(200, "操作成功", null);
    }

    /**
     * 成功响应（带数据）
     * 
     * 【使用场景】
     * 查询、新增等需要返回数据的操作
     */
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(200, "操作成功", data);
    }

    /**
     * 成功响应（带消息和数据）
     */
    public static <T> ApiResult<T> success(String message, T data) {
        return new ApiResult<>(200, message, data);
    }

    /**
     * 失败响应（使用错误码枚举）
     * 
     * 【为什么推荐使用错误码枚举】
     * 1. 集中管理错误码，避免重复
     * 2. 错误码与错误信息绑定，保证一致性
     * 3. 便于国际化处理
     */
    public static <T> ApiResult<T> fail(ErrorCode errorCode) {
        return new ApiResult<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    /**
     * 失败响应（自定义错误消息）
     * 
     * 【使用场景】
     * 需要返回特定错误信息时，如参数校验失败的具体字段
     */
    public static <T> ApiResult<T> fail(int code, String message) {
        return new ApiResult<>(code, message, null);
    }

    /**
     * 失败响应（使用默认错误码）
     */
    public static <T> ApiResult<T> fail(String message) {
        return new ApiResult<>(500, message, null);
    }

    /**
     * 判断请求是否成功
     */
    public boolean isSuccess() {
        return this.code == 200;
    }
}
