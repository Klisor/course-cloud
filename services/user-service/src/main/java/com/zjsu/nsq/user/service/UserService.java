package com.zjsu.nsq.user.service;

import com.zjsu.nsq.user.model.User;
import com.zjsu.nsq.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.regex.Pattern;

@Service
@Transactional
public class UserService {
    private final UserRepository repo;

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    private static final String EMAIL_REGEX =
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    // 修改：查找用户（使用 userId 作为用户名）
    public User findByUsername(String username) {
        // 使用 userId 作为用户名进行查找
        return repo.findByUserId(username).orElse(null);
    }

    // 原有方法保持不变
    public List<User> findAll() { return repo.findAll(); }
    public Optional<User> findById(Long id) { return repo.findById(id); }
    public Optional<User> findByUserId(String userId) { return repo.findByUserId(userId); }
    public Optional<User> findByEmail(String email) { return repo.findByEmail(email); }

    public User create(User u) {
        if (u.getUserId() == null || u.getUserId().trim().isEmpty())
            throw new InvalidUserDataException("用户ID不能为空");

        if (repo.existsByUserId(u.getUserId()))
            throw new UserAlreadyExistsException("用户ID已存在: " + u.getUserId());

        // 如果 username 为空，使用 userId 作为 username
        if (u.getUsername() == null || u.getUsername().trim().isEmpty()) {
            u.setUsername(u.getUserId());
        }

        if (u.getName() == null || u.getName().trim().isEmpty())
            throw new InvalidUserDataException("姓名不能为空");

        if (u.getRole() == null)
            throw new InvalidUserDataException("用户角色不能为空");

        if (u.getEmail() != null && !u.getEmail().isEmpty()) {
            if (!EMAIL_PATTERN.matcher(u.getEmail()).matches())
                throw new InvalidUserDataException("邮箱格式不正确: " + u.getEmail());
            if (repo.existsByEmail(u.getEmail()))
                throw new UserAlreadyExistsException("邮箱已存在: " + u.getEmail());
        }

        // 设置默认密码
        if (u.getPassword() == null || u.getPassword().isEmpty()) {
            u.setPassword("123456");
        }

        return repo.save(u);
    }

    public User update(Long id, User user) {
        User exist = repo.findById(id)
                .orElseThrow(() -> new UserNotFoundException("用户不存在: " + id));

        if (!exist.getUserId().equals(user.getUserId())
                && repo.existsByUserId(user.getUserId()))
            throw new UserAlreadyExistsException("用户ID已存在: " + user.getUserId());

        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            if (!EMAIL_PATTERN.matcher(user.getEmail()).matches())
                throw new InvalidUserDataException("邮箱格式不正确: " + user.getEmail());
            if (!user.getEmail().equals(exist.getEmail())
                    && repo.existsByEmail(user.getEmail()))
                throw new UserAlreadyExistsException("邮箱已存在: " + user.getEmail());
        }

        exist.setUserId(user.getUserId());

        // 更新 username
        if (user.getUsername() != null && !user.getUsername().isEmpty()) {
            exist.setUsername(user.getUsername());
        }

        exist.setName(user.getName());
        exist.setRole(user.getRole());
        exist.setMajor(user.getMajor());
        exist.setGrade(user.getGrade());
        exist.setEmail(user.getEmail());

        // 如果有新密码，更新密码
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            exist.setPassword(user.getPassword());
        }

        return repo.save(exist);
    }

    public void delete(Long id) {
        User u = repo.findById(id)
                .orElseThrow(() -> new UserNotFoundException("用户不存在: " + id));

        repo.deleteById(id);
    }

    // 验证密码
    public boolean validatePassword(String userId, String password) {
        Optional<User> userOptional = repo.findByUserId(userId);
        if (userOptional.isEmpty()) {
            return false;
        }
        User user = userOptional.get();
        return user.getPassword().equals(password);
    }

    // 保存用户（用于注册）
    public User saveUser(User user) {
        return create(user);
    }

    // Custom Exceptions
    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String m) { super(m); }
    }

    public static class UserAlreadyExistsException extends RuntimeException {
        public UserAlreadyExistsException(String m) { super(m); }
    }

    public static class InvalidUserDataException extends RuntimeException {
        public InvalidUserDataException(String m) { super(m); }
    }
}