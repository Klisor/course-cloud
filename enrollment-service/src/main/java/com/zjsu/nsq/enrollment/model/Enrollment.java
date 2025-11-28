package com.zjsu.nsq.enrollment.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "enrollments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"course_id", "student_id"}),
        indexes = {
                @Index(name = "idx_enrollment_student", columnList = "student_id"),
                @Index(name = "idx_enrollment_course", columnList = "course_id"),
                @Index(name = "idx_enrollment_status", columnList = "status")
        })
public class Enrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 课程ID作为普通字段，不再关联课程实体
    @Column(name = "course_id", nullable = false)
    private String courseId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnrollmentStatus status;

    @Column(name = "enrolled_at", nullable = false)
    private LocalDateTime enrolledAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 生命周期回调
    @PrePersist
    protected void onCreate() {
        enrolledAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = EnrollmentStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getter和Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public EnrollmentStatus getStatus() { return status; }
    public void setStatus(EnrollmentStatus status) { this.status = status; }

    public LocalDateTime getEnrolledAt() { return enrolledAt; }
    public void setEnrolledAt(LocalDateTime enrolledAt) { this.enrolledAt = enrolledAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
