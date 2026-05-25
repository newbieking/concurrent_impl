package com.example.concurrent_impl.mapper;

import com.example.concurrent_impl.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

/**
 * 【Mapper】用户数据访问层
 */
@Repository
public interface UserMapper extends JpaRepository<User, Long> {

    /**
     * 根据用户名查找用户
     */
    User findByUsername(String username);

    /**
     * 根据手机号查找用户
     */
    User findByPhone(String phone);

    /**
     * 使用乐观锁扣减余额
     *
     * @param id 用户ID
     * @param amount 扣减金额
     * @param version 期望版本号
     * @return 更新行数
     */
    @Modifying
    @Query("UPDATE User u SET u.balance = u.balance - :amount, u.version = u.version + 1 " +
           "WHERE u.id = :id AND u.version = :version AND u.balance >= :amount")
    int deductBalance(@Param("id") Long id, @Param("amount") BigDecimal amount, @Param("version") Integer version);

    /**
     * 使用乐观锁增加余额
     *
     * @param id 用户ID
     * @param amount 增加金额
     * @param version 期望版本号
     * @return 更新行数
     */
    @Modifying
    @Query("UPDATE User u SET u.balance = u.balance + :amount, u.version = u.version + 1 " +
           "WHERE u.id = :id AND u.version = :version")
    int addBalance(@Param("id") Long id, @Param("amount") BigDecimal amount, @Param("version") Integer version);
}
