package com.example.concurrent_impl.businesssecurity.validation.validator;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 【自定义校验器】手机号校验注解
 *
 * 【业务背景】
 * 在企业级项目中，经常需要校验手机号格式，
 * JSR-303提供的@Pattern注解虽然可以实现，
 * 但自定义注解更加直观和灵活。
 *
 * 【实现原理】
 * 1. 定义注解@interface Mobile
 * 2. 实现ConstraintValidator接口
 * 3. 在isValid方法中编写校验逻辑
 * 4. 在实体类上使用@Mobile注解
 *
 * 【为什么这样写】
 * 1. 代码简洁，语义清晰
 * 2. 支持自定义错误提示
 * 3. 可以复用
 * 4. 支持国际化
 *
 * 【不遵守的后果】
 * 1. 使用@Pattern：正则表达式难以理解
 * 2. 手动校验：代码冗余
 * 3. 不做校验：非法手机号进入系统
 *
 * 【正确示例】
 * 使用@Mobile注解
 *
 * 【错误示例】
 * 使用@Pattern注解，正则表达式难以维护
 *
 * 【实际案例】
 * 1. 用户注册
 * 2. 绑定手机号
 * 3. 短信验证
 *
 * @author concurrent_impl
 * @date 2024
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = MobileValidator.class)
public @interface Mobile {

    /**
     * 错误提示信息
     */
    String message() default "手机号格式不正确";

    /**
     * 分组
     */
    Class<?>[] groups() default {};

    /**
     * 负载
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * 手机号正则表达式
     *
     * 【默认值】
     * 支持中国大陆手机号：
     * - 1开头
     * - 第二位是3-9
     * - 后面9位数字
     */
    String regexp() default "^1[3-9]\\d{9}$";

    /**
     * 是否允许为空
     *
     * 【使用场景】
     * 有些场景手机号是选填的
     */
    boolean nullable() default false;
}
