package com.zjsu.nsq.catalog.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "courses",
        uniqueConstraints = @UniqueConstraint(columnNames = "code"),
        indexes = {
                @Index(name = "idx_course_instructor", columnList = "instructor_name"),
                @Index(name = "idx_course_title", columnList = "title")
        })
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", unique = true, nullable = false, length = 20)
    private String code;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "name", column = @Column(name = "instructor_name")),
            @AttributeOverride(name = "email", column = @Column(name = "instructor_email"))
    })
    private Instructor instructor;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "dayOfWeek", column = @Column(name = "schedule_day")),
            @AttributeOverride(name = "startTime", column = @Column(name = "schedule_start_time")),
            @AttributeOverride(name = "endTime", column = @Column(name = "schedule_end_time")),
            @AttributeOverride(name = "expectedAttendance", column = @Column(name = "expected_attendance"))
    })
    private ScheduleSlot schedule;

    @Column(name = "capacity", nullable = false)
    private Integer capacity = 0;

    @Column(name = "enrolled")
    private Integer enrolled = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 生命周期回调
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (enrolled == null) enrolled = 0;
        if (capacity == null) capacity = 0;
    }

    // Getter和Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Instructor getInstructor() { return instructor; }
    public void setInstructor(Instructor instructor) { this.instructor = instructor; }

    public ScheduleSlot getSchedule() { return schedule; }
    public void setSchedule(ScheduleSlot schedule) { this.schedule = schedule; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public Integer getEnrolled() { return enrolled; }
    public void setEnrolled(Integer enrolled) { this.enrolled = enrolled; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}