package com.example.concurrent_impl.controller;

import com.example.concurrent_impl.common.response.ApiResult;
import com.example.concurrent_impl.entity.Order;
import com.example.concurrent_impl.entity.Product;
import com.example.concurrent_impl.entity.User;
import com.example.concurrent_impl.highconcurrency.lock.service.RedisLockService;
import com.example.concurrent_impl.highconcurrency.ratelimit.service.RedisRateLimitService;
import com.example.concurrent_impl.mapper.ProductMapper;
import com.example.concurrent_impl.mapper.UserMapper;
import com.example.concurrent_impl.service.OrderServiceEnterprise;
import com.example.concurrent_impl.transactionsafety.distributed.service.LocalMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 【企业级Controller】订单接口
 *
 * 【业务说明】
 * 提供订单相关的REST API接口，包含：
 * 1. 创建订单（带限流）
 * 2. 支付订单
 * 3. 查询订单
 * 4. 用户和商品查询
 */
@Tag(name = "订单管理", description = "订单相关接口")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderControllerEnterprise {

    private final OrderServiceEnterprise orderService;
    private final UserMapper userMapper;
    private final ProductMapper productMapper;
    private final RedisRateLimitService rateLimitService;
    private final RedisLockService lockService;
    private final LocalMessageService localMessageService;

    /**
     * 创建订单
     *
     * 【接口说明】
     * 1. 使用分布式锁防止重复下单
     * 2. 使用限流保护接口
     * 3. 使用乐观锁保证库存扣减
     */
    @Operation(summary = "创建订单", description = "创建订单，带分布式锁和限流保护")
    @PostMapping
    public ApiResult<Order> createOrder(@RequestParam Long userId,
                                         @RequestParam Long productId,
                                         @RequestParam Integer quantity,
                                         HttpServletRequest request) {
        // 限流检查（每秒最多10个请求）
        String clientIp = getClientIp(request);
        if (!rateLimitService.slidingWindowRateLimit("order:create:" + clientIp, 1000, 10)) {
            return ApiResult.fail(429, "请求过于频繁，请稍后再试");
        }

        Order order = orderService.createOrder(userId, productId, quantity);
        return ApiResult.success(order);
    }

    /**
     * 支付订单
     */
    @Operation(summary = "支付订单", description = "支付订单，带乐观锁")
    @PostMapping("/{orderNo}/pay")
    public ApiResult<Order> payOrder(@PathVariable String orderNo) {
        Order order = orderService.payOrder(orderNo);
        return ApiResult.success(order);
    }

    /**
     * 获取用户列表
     */
    @Operation(summary = "获取用户列表")
    @GetMapping("/users")
    public ApiResult<List<User>> getUsers() {
        return ApiResult.success(userMapper.findAll());
    }

    /**
     * 获取商品列表
     */
    @Operation(summary = "获取商品列表")
    @GetMapping("/products")
    public ApiResult<List<Product>> getProducts() {
        return ApiResult.success(productMapper.findAll());
    }

    /**
     * 获取消息统计
     */
    @Operation(summary = "获取消息统计", description = "获取本地消息表统计信息")
    @GetMapping("/messages/statistics")
    public ApiResult<LocalMessageService.MessageStatistics> getMessageStatistics() {
        return ApiResult.success(localMessageService.getStatistics());
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
