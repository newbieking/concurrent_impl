package com.example.concurrent_impl.businesssecurity.permission;

import java.util.*;

/**
 * 【权限控制 - RBAC模型】基于角色的访问控制实现
 *
 * 【业务背景】
 * 在企业级系统中，需要对不同用户分配不同的权限，
 * RBAC（Role-Based Access Control）是最常用的权限模型。
 *
 * 【RBAC模型】
 * 用户 -> 角色 -> 权限
 * 1. 一个用户可以有多个角色
 * 2. 一个角色可以有多个权限
 * 3. 通过角色间接分配权限
 *
 * 【为什么这样写】
 * 1. 用户和权限解耦，便于管理
 * 2. 角色可以复用，减少重复配置
 * 3. 支持动态权限变更
 *
 * 【不遵守的后果】
 * 1. 不使用RBAC：权限管理混乱
 * 2. 用户直接关联权限：难以维护
 * 3. 不做权限校验：越权访问
 *
 * 【正确示例】
 * 使用RBAC模型，通过角色分配权限
 *
 * 【错误示例】
 * 用户直接关联权限，没有角色概念
 *
 * 【实际案例】
 * 1. 后台管理系统
 * 2. 多租户系统
 * 3. 企业OA系统
 *
 * @author concurrent_impl
 * @date 2024
 */
public class RBACDemo {

    /**
     * 用户
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class User {
        private Long id;
        private String username;
        private Set<Long> roleIds;
    }

    /**
     * 角色
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class Role {
        private Long id;
        private String roleName;
        private String description;
        private Set<Long> permissionIds;
    }

    /**
     * 权限
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class Permission {
        private Long id;
        private String permissionCode;
        private String permissionName;
        private String url;
    }

    /**
     * 用户存储
     */
    private static final Map<Long, User> userStore = new HashMap<>();

    /**
     * 角色存储
     */
    private static final Map<Long, Role> roleStore = new HashMap<>();

    /**
     * 权限存储
     */
    private static final Map<Long, Permission> permissionStore = new HashMap<>();

    /**
     * 初始化数据
     */
    static {
        // 初始化权限
        permissionStore.put(1L, new Permission(1L, "user:view", "查看用户", "/api/users"));
        permissionStore.put(2L, new Permission(2L, "user:create", "创建用户", "/api/users"));
        permissionStore.put(3L, new Permission(3L, "user:update", "更新用户", "/api/users/{id}"));
        permissionStore.put(4L, new Permission(4L, "user:delete", "删除用户", "/api/users/{id}"));
        permissionStore.put(5L, new Permission(5L, "order:view", "查看订单", "/api/orders"));
        permissionStore.put(6L, new Permission(6L, "order:create", "创建订单", "/api/orders"));
        permissionStore.put(7L, new Permission(7L, "order:cancel", "取消订单", "/api/orders/{id}/cancel"));

        // 初始化角色
        Set<Long> adminPermissions = new HashSet<>(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L));
        Set<Long> userPermissions = new HashSet<>(Arrays.asList(5L, 6L, 7L));
        Set<Long> viewerPermissions = new HashSet<>(Arrays.asList(1L, 5L));

        roleStore.put(1L, new Role(1L, "ADMIN", "管理员", adminPermissions));
        roleStore.put(2L, new Role(2L, "USER", "普通用户", userPermissions));
        roleStore.put(3L, new Role(3L, "VIEWER", "只读用户", viewerPermissions));

        // 初始化用户
        Set<Long> adminRoles = new HashSet<>(Collections.singletonList(1L));
        Set<Long> userRoles = new HashSet<>(Collections.singletonList(2L));
        Set<Long> viewerRoles = new HashSet<>(Collections.singletonList(3L));

        userStore.put(1L, new User(1L, "admin", adminRoles));
        userStore.put(2L, new User(2L, "zhangsan", userRoles));
        userStore.put(3L, new User(3L, "lisi", viewerRoles));
    }

    /**
     * 获取用户的所有角色
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    public static List<Role> getUserRoles(Long userId) {
        User user = userStore.get(userId);
        if (user == null) {
            return Collections.emptyList();
        }

        List<Role> roles = new ArrayList<>();
        for (Long roleId : user.getRoleIds()) {
            Role role = roleStore.get(roleId);
            if (role != null) {
                roles.add(role);
            }
        }
        return roles;
    }

    /**
     * 获取用户的所有权限
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    public static List<Permission> getUserPermissions(Long userId) {
        User user = userStore.get(userId);
        if (user == null) {
            return Collections.emptyList();
        }

        Set<Long> permissionIds = new HashSet<>();
        for (Long roleId : user.getRoleIds()) {
            Role role = roleStore.get(roleId);
            if (role != null) {
                permissionIds.addAll(role.getPermissionIds());
            }
        }

        List<Permission> permissions = new ArrayList<>();
        for (Long permissionId : permissionIds) {
            Permission permission = permissionStore.get(permissionId);
            if (permission != null) {
                permissions.add(permission);
            }
        }
        return permissions;
    }

    /**
     * 检查用户是否有指定权限
     *
     * @param userId 用户ID
     * @param permissionCode 权限编码
     * @return 是否有权限
     */
    public static boolean hasPermission(Long userId, String permissionCode) {
        List<Permission> permissions = getUserPermissions(userId);
        for (Permission permission : permissions) {
            if (permission.getPermissionCode().equals(permissionCode)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查用户是否有指定角色
     *
     * @param userId 用户ID
     * @param roleName 角色名称
     * @return 是否有角色
     */
    public static boolean hasRole(Long userId, String roleName) {
        List<Role> roles = getUserRoles(userId);
        for (Role role : roles) {
            if (role.getRoleName().equals(roleName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 演示RBAC权限控制
     */
    public static void demonstrate() {
        System.out.println("========== RBAC权限控制演示 ==========");
        System.out.println();

        // 管理员用户
        System.out.println("【管理员用户 admin】");
        System.out.println("角色: " + getUserRoles(1L));
        System.out.println("权限: " + getUserPermissions(1L));
        System.out.println("是否有user:delete权限: " + hasPermission(1L, "user:delete"));
        System.out.println();

        // 普通用户
        System.out.println("【普通用户 zhangsan】");
        System.out.println("角色: " + getUserRoles(2L));
        System.out.println("权限: " + getUserPermissions(2L));
        System.out.println("是否有user:delete权限: " + hasPermission(2L, "user:delete"));
        System.out.println("是否有order:create权限: " + hasPermission(2L, "order:create"));
        System.out.println();

        // 只读用户
        System.out.println("【只读用户 lisi】");
        System.out.println("角色: " + getUserRoles(3L));
        System.out.println("权限: " + getUserPermissions(3L));
        System.out.println("是否有user:view权限: " + hasPermission(3L, "user:view"));
        System.out.println("是否有order:create权限: " + hasPermission(3L, "order:create"));
        System.out.println();

        // 权限校验演示
        System.out.println("【权限校验演示】");
        System.out.println("admin访问DELETE /api/users/1: " +
                (hasPermission(1L, "user:delete") ? "允许" : "拒绝"));
        System.out.println("zhangsan访问DELETE /api/users/1: " +
                (hasPermission(2L, "user:delete") ? "允许" : "拒绝"));
        System.out.println("lisi访问POST /api/orders: " +
                (hasPermission(3L, "order:create") ? "允许" : "拒绝"));
    }

    /**
     * 主方法
     */
    public static void main(String[] args) {
        demonstrate();
    }
}
