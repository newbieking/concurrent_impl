package com.example.concurrent_impl.businesssecurity.permission;

import java.util.*;

/**
 * 【权限控制 - 数据权限】数据权限控制实现
 *
 * 【业务背景】
 * 在企业级系统中，不同用户只能看到自己权限范围内的数据，
 * 如：销售只能看到自己的订单，经理可以看到部门所有订单。
 *
 * 【数据权限级别】
 * 1. 全部数据：管理员
 * 2. 部门数据：部门经理
 * 3. 部门及子部门数据：分公司经理
 * 4. 个人数据：普通员工
 * 5. 自定义数据：特殊权限
 *
 * 【实现原理】
 * 1. 在SQL查询时自动添加数据权限条件
 * 2. 使用AOP拦截器实现
 * 3. 根据用户角色和部门过滤数据
 *
 * 【为什么这样写】
 * 1. 自动添加权限条件，无需手动处理
 * 2. 支持多种数据权限级别
 * 3. 代码无侵入
 *
 * 【不遵守的后果】
 * 1. 不做数据权限：数据泄露
 * 2. 手动添加条件：容易遗漏
 * 3. 权限配置错误：越权访问
 *
 * 【正确示例】
 * 使用AOP自动添加数据权限条件
 *
 * 【错误示例】
 * 手动在SQL中添加WHERE条件
 *
 * 【实际案例】
 * 1. 订单查询：只能看到自己的订单
 * 2. 客户管理：只能看到自己负责的客户
 * 3. 报表统计：只能看到权限范围内的数据
 *
 * @author concurrent_impl
 * @date 2024
 */
public class DataPermissionDemo {

    /**
     * 数据权限类型
     */
    public enum DataScopeType {
        ALL(1, "全部数据"),
        DEPT(2, "部门数据"),
        DEPT_AND_CHILD(3, "部门及子部门数据"),
        SELF(4, "个人数据"),
        CUSTOM(5, "自定义数据");

        private final int code;
        private final String description;

        DataScopeType(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public int getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 用户
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class User {
        private Long id;
        private String username;
        private Long deptId;
        private DataScopeType dataScope;
        private Set<Long> customDeptIds; // 自定义部门ID集合
    }

    /**
     * 部门
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class Dept {
        private Long id;
        private String deptName;
        private Long parentId;
    }

    /**
     * 订单
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class Order {
        private Long id;
        private String orderNo;
        private Long userId;
        private Long deptId;
        private String userName;
        private String deptName;
    }

    /**
     * 用户存储
     */
    private static final Map<Long, User> userStore = new HashMap<>();

    /**
     * 部门存储
     */
    private static final Map<Long, Dept> deptStore = new HashMap<>();

    /**
     * 订单存储
     */
    private static final List<Order> orderList = new ArrayList<>();

    /**
     * 初始化数据
     */
    static {
        // 初始化部门
        deptStore.put(1L, new Dept(1L, "总公司", 0L));
        deptStore.put(2L, new Dept(2L, "销售部", 1L));
        deptStore.put(3L, new Dept(3L, "技术部", 1L));
        deptStore.put(4L, new Dept(4L, "销售一组", 2L));
        deptStore.put(5L, new Dept(5L, "销售二组", 2L));

        // 初始化用户
        userStore.put(1L, new User(1L, "admin", 1L, DataScopeType.ALL, null));
        userStore.put(2L, new User(2L, "sales_manager", 2L, DataScopeType.DEPT, null));
        userStore.put(3L, new User(3L, "sales_user1", 4L, DataScopeType.SELF, null));
        userStore.put(4L, new User(4L, "sales_user2", 5L, DataScopeType.SELF, null));

        // 初始化订单
        orderList.add(new Order(1L, "ORD001", 3L, 4L, "sales_user1", "销售一组"));
        orderList.add(new Order(2L, "ORD002", 3L, 4L, "sales_user1", "销售一组"));
        orderList.add(new Order(3L, "ORD003", 4L, 5L, "sales_user2", "销售二组"));
        orderList.add(new Order(4L, "ORD004", 4L, 5L, "sales_user2", "销售二组"));
        orderList.add(new Order(5L, "ORD005", 2L, 2L, "sales_manager", "销售部"));
    }

    /**
     * 获取用户可见的部门ID集合
     *
     * @param user 用户
     * @return 部门ID集合
     */
    public static Set<Long> getVisibleDeptIds(User user) {
        Set<Long> deptIds = new HashSet<>();

        switch (user.getDataScope()) {
            case ALL:
                // 全部数据
                deptIds.addAll(deptStore.keySet());
                break;
            case DEPT:
                // 部门数据
                deptIds.add(user.getDeptId());
                break;
            case DEPT_AND_CHILD:
                // 部门及子部门数据
                deptIds.add(user.getDeptId());
                deptIds.addAll(getChildDeptIds(user.getDeptId()));
                break;
            case SELF:
                // 个人数据（返回空集合，后续按userId过滤）
                break;
            case CUSTOM:
                // 自定义数据
                if (user.getCustomDeptIds() != null) {
                    deptIds.addAll(user.getCustomDeptIds());
                }
                break;
        }

        return deptIds;
    }

    /**
     * 获取子部门ID集合
     *
     * @param parentId 父部门ID
     * @return 子部门ID集合
     */
    private static Set<Long> getChildDeptIds(Long parentId) {
        Set<Long> childIds = new HashSet<>();
        for (Dept dept : deptStore.values()) {
            if (dept.getParentId().equals(parentId)) {
                childIds.add(dept.getId());
                // 递归获取子部门
                childIds.addAll(getChildDeptIds(dept.getId()));
            }
        }
        return childIds;
    }

    /**
     * 根据数据权限过滤订单
     *
     * @param userId 用户ID
     * @return 过滤后的订单列表
     */
    public static List<Order> filterOrders(Long userId) {
        User user = userStore.get(userId);
        if (user == null) {
            return Collections.emptyList();
        }

        List<Order> result = new ArrayList<>();
        Set<Long> visibleDeptIds = getVisibleDeptIds(user);

        for (Order order : orderList) {
            if (user.getDataScope() == DataScopeType.SELF) {
                // 个人数据：只看自己的订单
                if (order.getUserId().equals(userId)) {
                    result.add(order);
                }
            } else if (visibleDeptIds.contains(order.getDeptId())) {
                // 部门数据：看部门内的订单
                result.add(order);
            }
        }

        return result;
    }

    /**
     * 生成数据权限SQL
     *
     * 【使用场景】
     * 在MyBatis拦截器中自动生成数据权限SQL
     *
     * @param userId 用户ID
     * @param alias 表别名
     * @return SQL条件
     */
    public static String generateDataPermissionSql(Long userId, String alias) {
        User user = userStore.get(userId);
        if (user == null) {
            return "1 = 0"; // 无权限
        }

        if (user.getDataScope() == DataScopeType.ALL) {
            return "1 = 1"; // 全部数据
        }

        if (user.getDataScope() == DataScopeType.SELF) {
            return alias + ".user_id = " + userId;
        }

        Set<Long> deptIds = getVisibleDeptIds(user);
        if (deptIds.isEmpty()) {
            return "1 = 0";
        }

        return alias + ".dept_id IN (" + String.join(",", deptIds.stream()
                .map(String::valueOf)
                .toArray(String[]::new)) + ")";
    }

    /**
     * 演示数据权限控制
     */
    public static void demonstrate() {
        System.out.println("========== 数据权限控制演示 ==========");
        System.out.println();

        // 管理员
        System.out.println("【管理员 admin】- 全部数据");
        List<Order> adminOrders = filterOrders(1L);
        System.out.println("可见订单数: " + adminOrders.size());
        System.out.println("SQL条件: " + generateDataPermissionSql(1L, "o"));
        System.out.println();

        // 销售经理
        System.out.println("【销售经理 sales_manager】- 部门数据");
        List<Order> managerOrders = filterOrders(2L);
        System.out.println("可见订单数: " + managerOrders.size());
        System.out.println("SQL条件: " + generateDataPermissionSql(2L, "o"));
        for (Order order : managerOrders) {
            System.out.println("  " + order.getOrderNo() + " - " + order.getUserName());
        }
        System.out.println();

        // 销售人员1
        System.out.println("【销售人员1 sales_user1】- 个人数据");
        List<Order> user1Orders = filterOrders(3L);
        System.out.println("可见订单数: " + user1Orders.size());
        System.out.println("SQL条件: " + generateDataPermissionSql(3L, "o"));
        for (Order order : user1Orders) {
            System.out.println("  " + order.getOrderNo() + " - " + order.getUserName());
        }
        System.out.println();

        // 销售人员2
        System.out.println("【销售人员2 sales_user2】- 个人数据");
        List<Order> user2Orders = filterOrders(4L);
        System.out.println("可见订单数: " + user2Orders.size());
        System.out.println("SQL条件: " + generateDataPermissionSql(4L, "o"));
        for (Order order : user2Orders) {
            System.out.println("  " + order.getOrderNo() + " - " + order.getUserName());
        }
    }

    /**
     * 主方法
     */
    public static void main(String[] args) {
        demonstrate();
    }
}
