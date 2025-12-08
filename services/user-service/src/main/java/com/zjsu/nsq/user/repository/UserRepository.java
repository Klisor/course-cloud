package com.zjsu.nsq.user.repository;

import com.zjsu.nsq.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 根据 userId（学号）查找用户
    Optional<User> findByUserId(String userId);

    // 根据 email 查找用户
    Optional<User> findByEmail(String email);

    // 检查 userId 是否存在
    boolean existsByUserId(String userId);

    // 检查 email 是否存在
    boolean existsByEmail(String email);

    // 如果需要按学号查询的别名
    default Optional<User> findByStudentId(String studentId) {
        return findByUserId(studentId);
    }
}

