package com.example.concurrent_impl.controller;

import com.example.concurrent_impl.common.response.ApiResult;
import com.example.concurrent_impl.entity.Order;
import com.example.concurrent_impl.entity.Product;
import com.example.concurrent_impl.entity.User;
import com.example.concurrent_impl.mapper.ProductMapper;
import com.example.concurrent_impl.mapper.UserMapper;
import com.example.concurrent_impl.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 【Controller】订单接口
 *
 * 【业务说明】
 * 提供订单相关的REST API接口
 */
@Tag(name = "订单管理", description = "订单相关接口")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserMapper userMapper;
    private final ProductMapper productMapper;

    /**
     * 创建订单
     */
    @Operation(summary = "创建订单")
    @PostMapping
    public ApiResult<Order> createOrder(@RequestParam Long userId,
                                         @RequestParam Long productId,
                                         @RequestParam Integer quantity) {
        Order order = orderService.createOrder(userId, productId, quantity);
        return ApiResult.success(order);
    }

    /**
     * 支付订单
     */
    @Operation(summary = "支付订单")
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
}
