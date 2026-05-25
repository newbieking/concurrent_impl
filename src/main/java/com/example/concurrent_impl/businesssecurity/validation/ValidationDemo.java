package com.example.concurrent_impl.businesssecurity.validation;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 【参数校验】JSR-303校验示例
 *
 * 【业务背景】
 * 在企业级项目中，需要对用户输入进行严格校验，
 * 防止非法数据进入系统。
 *
 * 【实现原理】
 * 使用JSR-303注解进行参数校验：
 * 1. 在实体类上添加校验注解
 * 2. 在Controller方法参数上添加@Valid注解
 * 3. 校验失败时抛出MethodArgumentNotValidException
 * 4. 全局异常处理器统一处理
 *
 * 【为什么这样写】
 * 1. 使用注解方式，代码简洁
 * 2. 支持分组校验
 * 3. 支持自定义校验器
 * 4. 支持嵌套校验
 *
 * 【不遵守的后果】
 * 1. 不做参数校验：非法数据进入系统，导致业务异常
 * 2. 只依赖前端校验：可以通过Postman等工具绕过
 * 3. 不统一处理校验异常：错误信息不友好
 *
 * 【正确示例】
 * 使用@Valid注解 + JSR-303注解
 *
 * 【错误示例】
 * 手动校验参数，代码冗余
 *
 * 【实际案例】
 * 1. 用户注册（用户名、密码、手机号）
 * 2. 订单创建（商品ID、数量、地址）
 * 3. 支付请求（金额、支付方式）
 *
 * @author concurrent_impl
 * @date 2024
 */
public class ValidationDemo {

    /**
     * 用户注册请求
     *
     * 【校验规则】
     * 1. 用户名：必填，长度3-20
     * 2. 密码：必填，长度6-20
     * 3. 手机号：必填，格式正确
     * 4. 邮箱：格式正确
     * 5. 年龄：1-150
     * 6. 余额：大于等于0
     */
    @Data
    public static class UserRegisterRequest {

        /**
         * 用户名
         * 【校验规则】
         * 1. 不能为空
         * 2. 长度3-20个字符
         */
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 20, message = "用户名长度必须在3-20之间")
        private String username;

        /**
         * 密码
         * 【校验规则】
         * 1. 不能为空
         * 2. 长度6-20个字符
         */
        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 20, message = "密码长度必须在6-20之间")
        private String password;

        /**
         * 手机号
         * 【校验规则】
         * 1. 不能为空
         * 2. 格式正确（11位数字）
         */
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        private String phone;

        /**
         * 邮箱
         * 【校验规则】
         * 1. 格式正确
         */
        @Email(message = "邮箱格式不正确")
        private String email;

        /**
         * 年龄
         * 【校验规则】
         * 1. 最小1岁
         * 2. 最大150岁
         */
        @Min(value = 1, message = "年龄最小为1岁")
        @Max(value = 150, message = "年龄最大为150岁")
        private Integer age;

        /**
         * 余额
         * 【校验规则】
         * 1. 大于等于0
         */
        @DecimalMin(value = "0", message = "余额不能为负数")
        private BigDecimal balance;
    }

    /**
     * 订单创建请求
     *
     * 【校验规则】
     * 1. 商品ID：必填
     * 2. 数量：大于0，小于100
     * 3. 地址：必填，长度不超过200
     */
    @Data
    public static class OrderCreateRequest {

        /**
         * 商品ID
         */
        @NotNull(message = "商品ID不能为空")
        @Positive(message = "商品ID必须为正数")
        private Long productId;

        /**
         * 数量
         */
        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "数量最少为1")
        @Max(value = 100, message = "数量最多为100")
        private Integer quantity;

        /**
         * 收货地址
         */
        @NotBlank(message = "收货地址不能为空")
        @Size(max = 200, message = "收货地址长度不能超过200")
        private String address;

        /**
         * 备注
         */
        @Size(max = 500, message = "备注长度不能超过500")
        private String remark;
    }

    /**
     * 支付请求
     *
     * 【校验规则】
     * 1. 订单号：必填
     * 2. 金额：大于0
     * 3. 支付方式：1-支付宝，2-微信，3-银行卡
     */
    @Data
    public static class PaymentRequest {

        /**
         * 订单号
         */
        @NotBlank(message = "订单号不能为空")
        @Size(min = 10, max = 32, message = "订单号长度必须在10-32之间")
        private String orderNo;

        /**
         * 支付金额
         */
        @NotNull(message = "支付金额不能为空")
        @DecimalMin(value = "0.01", message = "支付金额必须大于0")
        @DecimalMax(value = "999999.99", message = "支付金额不能超过999999.99")
        private BigDecimal amount;

        /**
         * 支付方式
         * 【校验规则】
         * 只能是1、2、3中的一个
         */
        @NotNull(message = "支付方式不能为空")
        @Min(value = 1, message = "支付方式不正确")
        @Max(value = 3, message = "支付方式不正确")
        private Integer payType;
    }

    /**
     * 演示参数校验
     */
    public static void demonstrateValidation() {
        System.out.println("========== 参数校验演示 ==========");
        System.out.println();

        // 场景1：正常数据
        System.out.println("【场景1】正常数据");
        UserRegisterRequest validUser = new UserRegisterRequest();
        validUser.setUsername("zhangsan");
        validUser.setPassword("123456");
        validUser.setPhone("13800138000");
        validUser.setEmail("zhangsan@example.com");
        validUser.setAge(25);
        validUser.setBalance(new BigDecimal("1000.00"));
        System.out.println("数据: " + validUser);
        System.out.println("校验结果: 通过");
        System.out.println();

        // 场景2：非法数据
        System.out.println("【场景2】非法数据");
        UserRegisterRequest invalidUser = new UserRegisterRequest();
        invalidUser.setUsername("");  // 空用户名
        invalidUser.setPassword("123");  // 密码太短
        invalidUser.setPhone("123456");  // 手机号格式错误
        invalidUser.setEmail("invalid-email");  // 邮箱格式错误
        invalidUser.setAge(200);  // 年龄超范围
        invalidUser.setBalance(new BigDecimal("-100"));  // 余额为负
        System.out.println("数据: " + invalidUser);
        System.out.println("校验结果: 失败");
        System.out.println();

        // 演示校验注解
        System.out.println("【常用校验注解】");
        System.out.println("@NotNull - 不能为null");
        System.out.println("@NotEmpty - 不能为null或空（集合、数组、字符串）");
        System.out.println("@NotBlank - 不能为null或空白字符串");
        System.out.println("@Size - 大小限制（字符串长度、集合大小）");
        System.out.println("@Min - 最小值");
        System.out.println("@Max - 最大值");
        System.out.println("@Pattern - 正则表达式匹配");
        System.out.println("@Email - 邮箱格式");
        System.out.println("@DecimalMin - 最小值（小数）");
        System.out.println("@DecimalMax - 最大值（小数）");
        System.out.println("@Positive - 正数");
        System.out.println("@PositiveOrZero - 正数或0");
    }

    /**
     * 演示分组校验
     */
    public static void demonstrateGroupValidation() {
        System.out.println("========== 分组校验演示 ==========");
        System.out.println();
        System.out.println("【使用场景】");
        System.out.println("不同接口对同一实体有不同的校验规则");
        System.out.println();
        System.out.println("【示例】");
        System.out.println("1. 创建用户：需要校验所有字段");
        System.out.println("2. 更新用户：只需要校验修改的字段");
        System.out.println("3. 登录：只需要校验用户名和密码");
        System.out.println();
        System.out.println("【代码示例】");
        System.out.println("```java");
        System.out.println("// 定义分组");
        System.out.println("public interface CreateGroup {}");
        System.out.println("public interface UpdateGroup {}");
        System.out.println("public interface LoginGroup {}");
        System.out.println();
        System.out.println("// 使用分组");
        System.out.println("@NotBlank(message = \"用户名不能为空\", groups = {CreateGroup.class, LoginGroup.class})");
        System.out.println("private String username;");
        System.out.println();
        System.out.println("// Controller中指定分组");
        System.out.println("@PostMapping(\"/user\")");
        System.out.println("public ApiResult createUser(@Validated(CreateGroup.class) UserRequest request) { ... }");
        System.out.println("```");
    }

    /**
     * 演示嵌套校验
     */
    public static void demonstrateNestedValidation() {
        System.out.println("========== 嵌套校验演示 ==========");
        System.out.println();
        System.out.println("【使用场景】");
        System.out.println("对象中包含其他对象，需要校验内部对象的字段");
        System.out.println();
        System.out.println("【代码示例】");
        System.out.println("```java");
        System.out.println("@Data");
        System.out.println("public class OrderRequest {");
        System.out.println("    @NotNull(message = \"用户信息不能为空\")");
        System.out.println("    @Valid  // 【关键点】添加@Valid注解");
        System.out.println("    private UserInfo userInfo;");
        System.out.println();
        System.out.println("    @NotNull(message = \"商品列表不能为空\")");
        System.out.println("    @Valid  // 【关键点】添加@Valid注解");
        System.out.println("    private List<ProductInfo> products;");
        System.out.println("}");
        System.out.println("```");
    }

    /**
     * 主方法
     */
    public static void main(String[] args) {
        demonstrateValidation();
        System.out.println("\n");
        demonstrateGroupValidation();
        System.out.println("\n");
        demonstrateNestedValidation();
    }
}
