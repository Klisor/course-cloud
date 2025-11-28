package com.zjsu.nsq.enrollment.repository;

import com.zjsu.nsq.enrollment.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    // 按学号唯一查询
    Optional<Student> findByStudentId(String studentId);

    // 按邮箱唯一查询
    Optional<Student> findByEmail(String email);

    // 判重检查 - 学号
    boolean existsByStudentId(String studentId);

    // 判重检查 - 邮箱
    boolean existsByEmail(String email);

    // 按专业筛选
    List<Student> findByMajor(String major);

    // 按年级筛选
    List<Student> findByGrade(Integer grade);

    // 按专业和年级组合筛选
    List<Student> findByMajorAndGrade(String major, Integer grade);

    // 按姓名模糊查询
    List<Student> findByNameContainingIgnoreCase(String name);

    // 统计专业人数
    @Query("SELECT COUNT(s) FROM Student s WHERE s.major = :major")
    Long countByMajor(@Param("major") String major);

    // 统计年级人数
    @Query("SELECT COUNT(s) FROM Student s WHERE s.grade = :grade")
    Long countByGrade(@Param("grade") Integer grade);
}