package com.zjsu.nsq.enrollment.repository;

import com.zjsu.nsq.enrollment.model.Enrollment;
import com.zjsu.nsq.enrollment.model.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    // 按课程ID查询
    List<Enrollment> findByCourseId(String courseId);

    // 按用户ID查询
    List<Enrollment> findByUserId(String userId);

    // 检查用户是否已选某课程
    boolean existsByCourseIdAndUserId(String courseId, String userId);

    // 按课程ID和用户ID查询活跃选课
    Optional<Enrollment> findByCourseIdAndUserIdAndStatus(
            String courseId, String userId, EnrollmentStatus status);

    // 统计活跃选课数量
    long countByCourseIdAndStatus(String courseId, EnrollmentStatus status);

    // 按课程ID统计所有选课
    long countByCourseId(String courseId);

    // 按用户ID统计活跃选课
    long countByUserIdAndStatus(String userId, EnrollmentStatus status);

    // 查询用户的所有选课（包括状态）
    List<Enrollment> findByUserIdAndStatus(String userId, EnrollmentStatus status);

    // 查询课程的选课（包括状态）
    List<Enrollment> findByCourseIdAndStatus(String courseId, EnrollmentStatus status);

    // 按状态查询
    List<Enrollment> findByStatus(EnrollmentStatus status);

    // 查询用户的活跃课程ID列表
    @Query("SELECT DISTINCT e.courseId FROM Enrollment e WHERE e.userId = :userId AND e.status = 'ACTIVE'")
    List<String> findActiveCourseIdsByUser(@Param("userId") String userId);

    // 查询课程的所有活跃用户ID
    @Query("SELECT DISTINCT e.userId FROM Enrollment e WHERE e.courseId = :courseId AND e.status = 'ACTIVE'")
    List<String> findActiveUserIdsByCourse(@Param("courseId") String courseId);
}