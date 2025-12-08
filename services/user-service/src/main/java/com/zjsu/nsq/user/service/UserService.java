

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

    public List<User> findAll() { return repo.findAll(); }

    public Optional<User> findById(Long id) { return repo.findById(id); }

    public Optional<User> findByUserId(String userId) { return repo.findByUserId(userId); }

    public Optional<User> findByEmail(String email) { return repo.findByEmail(email); }

    public User create(User u) {

        if (u.getUserId() == null || u.getUserId().trim().isEmpty())
            throw new com.zjsu.nsq.user.service.UserService.InvalidUserDataException("用户ID不能为空");

        if (repo.existsByUserId(u.getUserId()))
            throw new com.zjsu.nsq.user.service.UserService.UserAlreadyExistsException("用户ID已存在: " + u.getUserId());

        if (u.getName() == null || u.getName().trim().isEmpty())
            throw new com.zjsu.nsq.user.service.UserService.InvalidUserDataException("姓名不能为空");

        if (u.getRole() == null)
            throw new com.zjsu.nsq.user.service.UserService.InvalidUserDataException("用户角色不能为空");

        if (u.getEmail() != null && !u.getEmail().isEmpty()) {
            if (!EMAIL_PATTERN.matcher(u.getEmail()).matches())
                throw new com.zjsu.nsq.user.service.UserService.InvalidUserDataException("邮箱格式不正确: " + u.getEmail());
            if (repo.existsByEmail(u.getEmail()))
                throw new com.zjsu.nsq.user.service.UserService.UserAlreadyExistsException("邮箱已存在: " + u.getEmail());
        }

        return repo.save(u);
    }

    public User update(Long id, User user) {
        User exist = repo.findById(id)
                .orElseThrow(() -> new com.zjsu.nsq.user.service.UserService.UserNotFoundException("用户不存在: " + id));

        if (!exist.getUserId().equals(user.getUserId())
                && repo.existsByUserId(user.getUserId()))
            throw new com.zjsu.nsq.user.service.UserService.UserAlreadyExistsException("用户ID已存在: " + user.getUserId());

        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            if (!EMAIL_PATTERN.matcher(user.getEmail()).matches())
                throw new com.zjsu.nsq.user.service.UserService.InvalidUserDataException("邮箱格式不正确: " + user.getEmail());
            if (!user.getEmail().equals(exist.getEmail())
                    && repo.existsByEmail(user.getEmail()))
                throw new com.zjsu.nsq.user.service.UserService.UserAlreadyExistsException("邮箱已存在: " + user.getEmail());
        }

        exist.setUserId(user.getUserId());
        exist.setName(user.getName());
        exist.setRole(user.getRole());
        exist.setMajor(user.getMajor());
        exist.setGrade(user.getGrade());
        exist.setEmail(user.getEmail());

        return repo.save(exist);
    }

    public void delete(Long id) {
        User u = repo.findById(id)
                .orElseThrow(() -> new com.zjsu.nsq.user.service.UserService.UserNotFoundException("用户不存在: " + id));

        repo.deleteById(id);
    }

    // Custom Exceptions
    public static class UserNotFoundException extends RuntimeException { public UserNotFoundException(String m){super(m);} }
    public static class UserAlreadyExistsException extends RuntimeException { public UserAlreadyExistsException(String m){super(m);} }
    public static class InvalidUserDataException extends RuntimeException { public InvalidUserDataException(String m){super(m);} }
}
