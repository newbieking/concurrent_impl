package com.example.concurrent_impl.businesssecurity.permission.service;

import com.example.concurrent_impl.entity.User;
import com.example.concurrent_impl.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 【企业级】权限服务
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
 * @author concurrent_impl
 * @date 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final StringRedisTemplate redisTemplate;
    private final UserMapper userMapper;

    /**
     * 角色权限缓存前缀
     */
    private static final String ROLE_PERMISSION_PREFIX = "permission:role:";

    /**
     * 用户角色缓存前缀
     */
    private static final String USER_ROLE_PREFIX = "permission:user:role:";

    /**
     * 缓存过期时间（秒）
     */
    private static final int CACHE_EXPIRE_SECONDS = 3600; // 1小时

    /**
     * 检查用户是否有指定权限
     *
     * @param userId 用户ID
     * @param permissionCode 权限编码
     * @return 是否有权限
     */
    public boolean hasPermission(Long userId, String permissionCode) {
        // 1. 获取用户的所有权限
        Set<String> permissions = getUserPermissions(userId);

        // 2. 检查是否有指定权限
        boolean hasPermission = permissions.contains(permissionCode);

        log.debug("权限检查: userId={}, permissionCode={}, hasPermission={}", userId, permissionCode, hasPermission);
        return hasPermission;
    }

    /**
     * 检查用户是否有指定角色
     *
     * @param userId 用户ID
     * @param roleName 角色名称
     * @return 是否有角色
     */
    public boolean hasRole(Long userId, String roleName) {
        Set<String> roles = getUserRoles(userId);
        return roles.contains(roleName);
    }

    /**
     * 获取用户的所有角色
     *
     * @param userId 用户ID
     * @return 角色集合
     */
    public Set<String> getUserRoles(Long userId) {
        String key = USER_ROLE_PREFIX + userId;

        // 从缓存获取
        Set<String> roles = redisTemplate.opsForSet().members(key);
        if (roles != null && !roles.isEmpty()) {
            return roles;
        }

        // 从数据库获取（示例：根据用户类型分配角色）
        roles = loadUserRolesFromDb(userId);

        // 写入缓存
        if (!roles.isEmpty()) {
            redisTemplate.opsForSet().add(key, roles.toArray(new String[0]));
            redisTemplate.expire(key, CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
        }

        return roles;
    }

    /**
     * 获取用户的所有权限
     *
     * @param userId 用户ID
     * @return 权限集合
     */
    public Set<String> getUserPermissions(Long userId) {
        Set<String> roles = getUserRoles(userId);
        Set<String> permissions = new HashSet<>();

        for (String role : roles) {
            Set<String> rolePermissions = getRolePermissions(role);
            permissions.addAll(rolePermissions);
        }

        return permissions;
    }

    /**
     * 获取角色的所有权限
     *
     * @param roleName 角色名称
     * @return 权限集合
     */
    public Set<String> getRolePermissions(String roleName) {
        String key = ROLE_PERMISSION_PREFIX + roleName;

        // 从缓存获取
        Set<String> permissions = redisTemplate.opsForSet().members(key);
        if (permissions != null && !permissions.isEmpty()) {
            return permissions;
        }

        // 从数据库获取
        permissions = loadRolePermissionsFromDb(roleName);

        // 写入缓存
        if (!permissions.isEmpty()) {
            redisTemplate.opsForSet().add(key, permissions.toArray(new String[0]));
            redisTemplate.expire(key, CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
        }

        return permissions;
    }

    /**
     * 从数据库加载用户角色
     *
     * @param userId 用户ID
     * @return 角色集合
     */
    private Set<String> loadUserRolesFromDb(Long userId) {
        // 示例：根据用户类型分配角色
        User user = userMapper.findById(userId).orElse(null);
        if (user == null) {
            return Collections.emptySet();
        }

        Set<String> roles = new HashSet<>();
        // 简单示例：根据用户ID分配角色
        if (userId == 1L) {
            roles.add("ADMIN");
        } else {
            roles.add("USER");
        }

        return roles;
    }

    /**
     * 从数据库加载角色权限
     *
     * @param roleName 角色名称
     * @return 权限集合
     */
    private Set<String> loadRolePermissionsFromDb(String roleName) {
        Set<String> permissions = new HashSet<>();

        // 简单示例：根据角色分配权限
        switch (roleName) {
            case "ADMIN":
                permissions.add("user:view");
                permissions.add("user:create");
                permissions.add("user:update");
                permissions.add("user:delete");
                permissions.add("order:view");
                permissions.add("order:create");
                permissions.add("order:cancel");
                break;
            case "USER":
                permissions.add("order:view");
                permissions.add("order:create");
                permissions.add("order:cancel");
                break;
            case "VIEWER":
                permissions.add("user:view");
                permissions.add("order:view");
                break;
        }

        return permissions;
    }

    /**
     * 清除用户权限缓存
     *
     * @param userId 用户ID
     */
    public void clearUserCache(Long userId) {
        String key = USER_ROLE_PREFIX + userId;
        redisTemplate.delete(key);
        log.info("清除用户权限缓存: userId={}", userId);
    }

    /**
     * 清除角色权限缓存
     *
     * @param roleName 角色名称
     */
    public void clearRoleCache(String roleName) {
        String key = ROLE_PERMISSION_PREFIX + roleName;
        redisTemplate.delete(key);
        log.info("清除角色权限缓存: roleName={}", roleName);
    }
}
