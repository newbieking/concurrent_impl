package com.example.concurrent_impl.highconcurrency.ratelimit.aspect;

import com.example.concurrent_impl.common.enums.ErrorCode;
import com.example.concurrent_impl.common.exception.BusinessException;
import com.example.concurrent_impl.highconcurrency.ratelimit.annotation.RateLimit;
import com.example.concurrent_impl.highconcurrency.ratelimit.service.RedisRateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 【企业级】限流切面
 *
 * 【业务背景】
 * 通过AOP切面实现限流，避免在业务代码中重复编写限流逻辑。
 *
 * @author concurrent_impl
 * @date 2024
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RedisRateLimitService rateLimitService;

    /**
     * 前置通知：处理限流逻辑
     */
    @Before("@annotation(rateLimit)")
    public void before(JoinPoint joinPoint, RateLimit rateLimit) {
        // 生成限流key
        String key = generateKey(joinPoint, rateLimit);

        // 执行限流检查
        boolean allowed;
        switch (rateLimit.type()) {
            case IP:
                allowed = rateLimitService.slidingWindowRateLimit(key, rateLimit.windowMs(), rateLimit.limit());
                break;
            case USER:
                allowed = rateLimitService.slidingWindowRateLimit(key, rateLimit.windowMs(), rateLimit.limit());
                break;
            case METHOD:
                allowed = rateLimitService.fixedWindowRateLimit(key, rateLimit.limit(), (int) (rateLimit.windowMs() / 1000));
                break;
            default:
                allowed = true;
        }

        if (!allowed) {
            log.warn("限流触发: key={}", key);
            throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED, "请求过于频繁，请稍后再试");
        }
    }

    /**
     * 生成限流key
     */
    private String generateKey(JoinPoint joinPoint, RateLimit rateLimit) {
        StringBuilder key = new StringBuilder(rateLimit.prefix());

        switch (rateLimit.type()) {
            case IP:
                key.append(getClientIp());
                break;
            case USER:
                key.append(getUserId());
                break;
            case METHOD:
                MethodSignature signature = (MethodSignature) joinPoint.getSignature();
                Method method = signature.getMethod();
                key.append(method.getDeclaringClass().getName())
                   .append(".")
                   .append(method.getName());
                break;
        }

        return key.toString();
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }

        HttpServletRequest request = attributes.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * 获取用户ID
     */
    private String getUserId() {
        // 从请求头或Session中获取用户ID
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "anonymous";
        }

        HttpServletRequest request = attributes.getRequest();
        String userId = request.getHeader("X-User-Id");
        if (userId == null || userId.isEmpty()) {
            userId = "anonymous";
        }
        return userId;
    }
}
