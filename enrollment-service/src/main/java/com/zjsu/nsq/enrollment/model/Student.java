package com.zjsu.nsq.enrollment.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "students",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "student_id"),
                @UniqueConstraint(columnNames = "email")
        },
        indexes = {
                @Index(name = "idx_student_major", columnList = "major"),
                @Index(name = "idx_student_grade", columnList = "grade")
        })
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", unique = true, nullable = false, length = 20)
    private String studentId;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "major", length = 50)
    private String major;

    @Column(name = "grade")
    private Integer grade;

    @Column(name = "email", unique = true, length = 100)
    private String email;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 生命周期回调
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getter和Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }

    public Integer getGrade() { return grade; }
    public void setGrade(Integer grade) { this.grade = grade; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}