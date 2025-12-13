package com.zjsu.nsq.user.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "user_id"),
                @UniqueConstraint(columnNames = "email")
        },
        indexes = {
                @Index(name = "idx_user_role", columnList = "role"),
                @Index(name = "idx_user_grade", columnList = "grade")
        })
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", unique = true, nullable = false, length = 20)
    private String userId;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role; // STUDENT / TEACHER

    @Column(length = 50)
    private String major; // only for students

    @Column
    private Integer grade; // only for students

    @Column(unique = true, length = 100)
    private String email;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 添加密码字段
    @Column(nullable = false, length = 100)
    private String password = "123456"; // 默认密码

    // 添加用户名字段（使用 userId 作为用户名）
    @Transient  // 不持久化到数据库，从 userId 派生
    private String username;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        // 设置 username（使用 userId）
        this.username = this.userId;
    }

    // Getter & Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) {
        this.userId = userId;
        // 自动设置 username
        this.username = userId;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }

    public Integer getGrade() { return grade; }
    public void setGrade(Integer grade) { this.grade = grade; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // 新增的 getter 和 setter
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getUsername() {
        // 如果 username 为 null，则返回 userId
        return username != null ? username : userId;
    }
    public void setUsername(String username) { this.username = username; }
}