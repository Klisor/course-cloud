package com.zjsu.nsq.enrollment.repository;

import com.zjsu.nsq.enrollment.model.Enrollment;
import com.zjsu.nsq.enrollment.model.EnrollmentStatus;
import com.zjsu.nsq.enrollment.model.Student;
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

    // 按学生查询
    List<Enrollment> findByStudent(Student student);

    // 按状态查询
    List<Enrollment> findByStatus(EnrollmentStatus status);

    // 按课程ID和状态查询
    List<Enrollment> findByCourseIdAndStatus(String courseId, EnrollmentStatus status);

    // 按学生和状态查询
    List<Enrollment> findByStudentAndStatus(Student student, EnrollmentStatus status);

    // 核心修复：JPQL 字段名从 e.student.id → e.student.studentId（匹配 Student 实体的 studentId 字段）
    @Query("SELECT EXISTS(SELECT 1 FROM Enrollment e WHERE e.courseId = :courseId AND e.student.studentId = :studentId)")
    boolean existsByCourseIdAndStudentId(@Param("courseId") String courseId, @Param("studentId") String studentId);
    // 查找特定课程和学生的选课记录
    @Query("SELECT e FROM Enrollment e WHERE e.courseId = :courseId AND e.student.id = :studentId AND e.status = :status")
    Optional<Enrollment> findByCourseIdAndStudentIdAndStatus(
            @Param("courseId") String courseId,
            @Param("studentId") String studentId,
            @Param("status") EnrollmentStatus status);

    // 统计课程的活跃选课数量
    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.courseId = :courseId AND e.status = 'ACTIVE'")
    Long countByCourseIdAndStatus(@Param("courseId") String courseId, EnrollmentStatus status);

    // 统计学生的活跃选课数量
    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.student = :student AND e.status = :status")
    Long countByStudentAndStatus(@Param("student") Student student, @Param("status") EnrollmentStatus status);
}