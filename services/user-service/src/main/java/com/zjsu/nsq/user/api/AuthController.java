package com.zjsu.nsq.user.api;

import com.zjsu.nsq.user.model.Role;
import com.zjsu.nsq.user.model.User;
import com.zjsu.nsq.user.service.UserService;
import com.zjsu.nsq.user.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    // 登录请求内部类
    public static class LoginRequest {
        private String userId;  // 使用 userId 作为用户名
        private String password;

        public LoginRequest() {}

        public LoginRequest(String userId, String password) {
            this.userId = userId;
            this.password = password;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    // 登录响应内部类
    // 登录响应内部类
    public static class LoginResponse {
        private String token;
        private User user;
        private String message;

        public LoginResponse() {
            this.message = "操作成功";
        }

        public LoginResponse(String token, User user) {
            this.token = token;
            this.user = user;
            this.message = "登录成功";  // 默认消息
        }

        public LoginResponse(String message) {
            this.message = message;
        }

        public LoginResponse(String token, User user, String message) {
            this.token = token;
            this.user = user;
            this.message = message;
        }

        // getter 和 setter
        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public User getUser() {
            return user;
        }

        public void setUser(User user) {
            this.user = user;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        System.out.println("登录请求 - 用户ID: " + request.getUserId());

        try {
            // 1. 验证用户ID和密码
            // 修改：使用 userId 直接查找用户
            User user = userService.findByUserId(request.getUserId()).orElse(null);
            if (user == null) {
                System.out.println("用户不存在: " + request.getUserId());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new LoginResponse("用户ID或密码错误"));
            }

            // 验证密码
            if (!user.getPassword().equals(request.getPassword())) {
                System.out.println("密码错误 - 用户ID: " + request.getUserId());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new LoginResponse("用户ID或密码错误"));
            }

            // 2. 生成 JWT Token
            String token = jwtUtil.generateToken(
                    user.getUserId(),
                    user.getUsername(), // 使用 username 字段
                    user.getRole().name()

            );
            System.out.print("！！！！！！！打印userId = " +   user.getUserId()  );
            System.out.println("用户登录成功: " + user.getUsername() + " - " + user.getRole());

            // 3. 返回 Token 和用户信息（不包含密码）
            user.setPassword(null); // 不返回密码
            return ResponseEntity.ok(new LoginResponse(token, user));

        } catch (Exception e) {
            System.err.println("登录过程发生错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new LoginResponse("登录失败，请稍后重试"));
        }
    }

    // 注册请求内部类
    public static class RegisterRequest {
        private String userId;
        private String password;
        private String name;
        private String role; // "STUDENT", "TEACHER", "ADMIN"
        private String major;
        private Integer grade;
        private String email;

        // 构造函数
        public RegisterRequest() {}

        // getter和setter
        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getMajor() {
            return major;
        }

        public void setMajor(String major) {
            this.major = major;
        }

        public Integer getGrade() {
            return grade;
        }

        public void setGrade(Integer grade) {
            this.grade = grade;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@RequestBody RegisterRequest request) {
        System.out.println("注册请求 - 用户ID: " + request.getUserId());

        try {
            // 检查用户ID是否已存在
            if (userService.findByUserId(request.getUserId()).isPresent()) {
                System.out.println("用户ID已存在: " + request.getUserId());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new LoginResponse("用户ID已存在"));
            }

            // 创建用户对象
            User user = new User();
            user.setUserId(request.getUserId());
            user.setPassword(request.getPassword() != null ? request.getPassword() : "123456");
            user.setName(request.getName());

            // 设置角色
            if (request.getRole() != null) {
                try {
                    user.setRole(Role.valueOf(request.getRole().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    // 默认使用 STUDENT
                    user.setRole(Role.STUDENT);
                }
            } else {
                user.setRole(Role.STUDENT);
            }

            user.setMajor(request.getMajor());
            user.setGrade(request.getGrade());
            user.setEmail(request.getEmail());

            // 保存用户
            User savedUser = userService.create(user);
            savedUser.setPassword(null); // 不返回密码

            // 生成 Token（注册后自动登录）
            String token = jwtUtil.generateToken(
                    savedUser.getUserId(),
                    savedUser.getUserId(), // 使用 userId 作为用户名
                    savedUser.getRole().name()

            );

            System.out.println("用户注册成功: " + savedUser.getUserId());

            // 创建响应，明确显示是注册成功
            LoginResponse response = new LoginResponse(token, savedUser);
            response.setMessage("注册成功，已自动登录");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("注册过程发生错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new LoginResponse("注册失败，请稍后重试: " + e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("User Auth Service is healthy");
    }
}