package com.example.concurrent_impl.common.exception;

import com.example.concurrent_impl.common.enums.ErrorCode;
import com.example.concurrent_impl.common.response.ApiResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * 【全局异常处理器】统一处理所有异常
 * 
 * 【业务背景】
 * 在企业级项目中，需要统一处理异常，避免将堆栈信息暴露给前端，
 * 同时提供友好的错误提示。
 * 
 * 【为什么这样写】
 * 1. 使用@RestControllerAdvice统一拦截所有Controller的异常
 * 2. 针对不同类型的异常进行分别处理
 * 3. 记录详细的日志便于问题定位
 * 4. 返回统一的响应格式，便于前端处理
 * 
 * 【不遵守的后果】
 * 1. 不统一处理异常：每个Controller都要写try-catch，代码冗余
 * 2. 暴露堆栈信息：存在安全风险，泄露系统实现细节
 * 3. 不记录日志：出现问题时难以定位
 * 4. 返回格式不统一：前端需要针对不同接口处理不同的响应格式
 * 
 * 【异常处理原则】
 * 1. 业务异常：返回具体的错误信息，便于前端展示
 * 2. 参数校验异常：返回具体的参数错误信息
 * 3. 系统异常：返回通用错误信息，不暴露系统细节
 * 4. 所有异常都要记录日志
 * 
 * 【实际案例】
 * 1. 参数校验失败：返回400和具体的字段错误信息
 * 2. 业务异常：返回对应的错误码和错误信息
 * 3. 系统异常：返回500和通用错误信息，同时记录堆栈日志
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     * 
     * 【业务背景】
     * 业务异常是可预期的，如库存不足、订单不存在等
     * 
     * 【为什么这样处理】
     * 1. 返回200状态码，业务错误码在body中
     * 2. 记录warn级别日志，便于监控业务异常频率
     * 3. 不记录堆栈信息，减少日志量
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResult<?> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常: uri={}, code={}, message={}", 
                request.getRequestURI(), e.getCode(), e.getMessage());
        return ApiResult.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常（@Valid注解触发）
     * 
     * 【业务背景】
     * 使用@Valid注解进行参数校验时，校验失败会抛出此异常
     * 
     * 【为什么这样处理】
     * 1. 返回400状态码，表示客户端请求参数错误
     * 2. 提取第一个校验失败的字段信息，便于前端定位问题
     * 3. 记录warn级别日志
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<?> handleValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验失败: {}", message);
        return ApiResult.fail(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 处理绑定异常
     * 
     * 【业务背景】
     * 表单绑定失败时会抛出此异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<?> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数绑定失败: {}", message);
        return ApiResult.fail(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 处理缺少请求参数异常
     * 
     * 【业务背景】
     * 客户端请求缺少必填参数时会抛出此异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<?> handleMissingParamsException(MissingServletRequestParameterException e) {
        log.warn("缺少请求参数: {}", e.getParameterName());
        return ApiResult.fail(ErrorCode.PARAM_ERROR.getCode(), "缺少参数: " + e.getParameterName());
    }

    /**
     * 处理参数类型不匹配异常
     * 
     * 【业务背景】
     * 客户端传递的参数类型与接口定义不匹配时会抛出此异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<?> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不匹配: {}", e.getName());
        return ApiResult.fail(ErrorCode.PARAM_ERROR.getCode(), "参数类型错误: " + e.getName());
    }

    /**
     * 处理请求方法不支持异常
     * 
     * 【业务背景】
     * 使用错误的HTTP方法访问接口时会抛出此异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ApiResult<?> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: {}", e.getMethod());
        return ApiResult.fail(ErrorCode.METHOD_NOT_ALLOWED.getCode(), "不支持的请求方法: " + e.getMethod());
    }

    /**
     * 处理404异常
     * 
     * 【业务背景】
     * 访问不存在的接口时会抛出此异常
     * 
     * 【配置说明】
     * 需要在配置文件中设置 throw-exception-if-no-handler-found: true
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResult<?> handleNoHandlerFoundException(NoHandlerFoundException e) {
        log.warn("请求地址不存在: {}", e.getRequestURL());
        return ApiResult.fail(ErrorCode.NOT_FOUND);
    }

    /**
     * 处理其他未知异常
     * 
     * 【业务背景】
     * 处理所有未被捕获的异常，作为最后的兜底处理
     * 
     * 【为什么这样处理】
     * 1. 返回500状态码，表示服务端错误
     * 2. 返回通用错误信息，不暴露系统细节
     * 3. 记录error级别日志，包含堆栈信息便于排查
     * 4. 可以同时发送告警通知
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<?> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常: uri={}", request.getRequestURI(), e);
        // TODO: 可以在这里发送告警通知，如钉钉、企业微信等
        return ApiResult.fail(ErrorCode.INTERNAL_ERROR);
    }
}
