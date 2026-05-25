package com.example.concurrent_impl.businesssecurity.idempotent.aspect;

import com.example.concurrent_impl.businesssecurity.idempotent.annotation.Idempotent;
import com.example.concurrent_impl.common.enums.ErrorCode;
import com.example.concurrent_impl.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.concurrent.TimeUnit;

/**
 * 【幂等切面】处理@Idempotent注解的AOP切面
 *
 * 【业务背景】
 * 通过AOP切面实现幂等性检查，避免在业务代码中重复编写幂等逻辑。
 *
 * 【实现原理】
 * 1. 拦截带有@Idempotent注解的方法
 * 2. 根据注解配置生成幂等键
 * 3. 检查幂等键是否已存在
 * 4. 如果存在，说明是重复请求，抛出异常
 * 5. 如果不存在，设置幂等键，执行业务逻辑
 * 6. 业务执行失败时，根据配置决定是否删除幂等键
 *
 * 【为什么这样写】
 * 1. 使用AOP切面，代码无侵入
 * 2. 使用SpEL表达式，支持灵活的key生成
 * 3. 使用Redis存储幂等键，支持分布式环境
 * 4. 支持业务失败后删除幂等键
 *
 * 【不遵守的后果】
 * 1. 不使用切面：每个接口都要写幂等逻辑，代码冗余
 * 2. 不使用SpEL：key生成方式不灵活
 * 3. 不使用Redis：不支持分布式环境
 *
 * 【正确示例】
 * 使用@Idempotent注解，切面自动处理
 *
 * 【错误示例】
 * 在每个接口中手动检查幂等
 *
 * @author concurrent_impl
 * @date 2024
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotentAspect {

    private final StringRedisTemplate redisTemplate;

    /**
     * SpEL表达式解析器
     */
    private final ExpressionParser parser = new SpelExpressionParser();

    /**
     * 环绕通知：处理幂等逻辑
     *
     * @param joinPoint 连接点
     * @param idempotent 幂等注解
     * @return 方法执行结果
     */
    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        // 【关键点1】生成幂等键
        String idempotentKey = generateIdempotentKey(joinPoint, idempotent);

        // 【关键点2】检查幂等键是否存在
        Boolean exists = redisTemplate.hasKey(idempotentKey);
        if (Boolean.TRUE.equals(exists)) {
            log.warn("重复提交: key={}", idempotentKey);
            throw new BusinessException(ErrorCode.DUPLICATE_SUBMIT, idempotent.message());
        }

        // 【关键点3】设置幂等键
        redisTemplate.opsForValue().set(idempotentKey, "1", idempotent.expireSeconds(), TimeUnit.SECONDS);

        try {
            // 【关键点4】执行业务逻辑
            Object result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            // 【关键点5】业务执行失败，根据配置决定是否删除幂等键
            if (idempotent.deleteKeyAfterFailure()) {
                redisTemplate.delete(idempotentKey);
                log.info("业务执行失败，删除幂等键: key={}", idempotentKey);
            }
            throw e;
        }
    }

    /**
     * 生成幂等键
     *
     * 【生成策略】
     * 1. 如果指定了SpEL表达式，使用SpEL解析
     * 2. 如果未指定，使用请求URI + 参数的MD5
     *
     * @param joinPoint 连接点
     * @param idempotent 幂等注解
     * @return 幂等键
     */
    private String generateIdempotentKey(ProceedingJoinPoint joinPoint, Idempotent idempotent) {
        String prefix = idempotent.prefix();
        String key;

        if (StringUtils.hasText(idempotent.key())) {
            // 使用SpEL表达式生成key
            key = parseSpEL(joinPoint, idempotent.key());
        } else {
            // 使用请求URI + 参数生成key
            key = generateDefaultKey(joinPoint);
        }

        return prefix + key;
    }

    /**
     * 解析SpEL表达式
     *
     * @param joinPoint 连接点
     * @param spelExpression SpEL表达式
     * @return 解析结果
     */
    private String parseSpEL(ProceedingJoinPoint joinPoint, String spelExpression) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        // 创建EvaluationContext
        EvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < parameters.length; i++) {
            context.setVariable(parameters[i].getName(), args[i]);
        }

        // 解析表达式
        return parser.parseExpression(spelExpression).getValue(context, String.class);
    }

    /**
     * 生成默认的幂等键（使用请求URI + 参数）
     *
     * @param joinPoint 连接点
     * @return 幂等键
     */
    private String generateDefaultKey(ProceedingJoinPoint joinPoint) {
        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            // 非Web环境，使用方法签名
            return joinPoint.getSignature().toString();
        }

        HttpServletRequest request = attributes.getRequest();
        String uri = request.getRequestURI();

        // 获取用户ID（从请求头或Session中获取）
        String userId = request.getHeader("X-User-Id");
        if (!StringUtils.hasText(userId)) {
            userId = "anonymous";
        }

        // 生成key: 用户ID + URI + 参数
        String argsStr = argsToString(joinPoint.getArgs());
        return userId + ":" + uri + ":" + argsStr.hashCode();
    }

    /**
     * 将参数转换为字符串
     *
     * @param args 参数数组
     * @return 字符串
     */
    private String argsToString(Object[] args) {
        if (args == null || args.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (Object arg : args) {
            if (arg != null) {
                sb.append(arg.toString()).append(":");
            }
        }
        return sb.toString();
    }
}
