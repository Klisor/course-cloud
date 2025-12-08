package com.zjsu.nsq.catalog.repository;

import com.zjsu.nsq.catalog.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    // 按课程代码查询
    Optional<Course> findByCode(String code);

    // 按讲师姓名查询
    @Query("SELECT c FROM Course c WHERE c.instructor.name = :instructorName")
    List<Course> findByInstructorName(@Param("instructorName") String instructorName);

    // 按讲师邮箱查询
    @Query("SELECT c FROM Course c WHERE c.instructor.email = :instructorEmail")
    List<Course> findByInstructorEmail(@Param("instructorEmail") String instructorEmail);

    // 统计有剩余容量的课程
    @Query("SELECT c FROM Course c WHERE c.capacity > c.enrolled")
    List<Course> findCoursesWithAvailableCapacity();

    // 筛选有剩余容量的课程
    default List<Course> findAvailableCourses() {
        return findCoursesWithAvailableCapacity();
    }

    // 支持标题关键字模糊查询
    List<Course> findByTitleContainingIgnoreCase(String title);

    // 按课程代码存在性检查
    boolean existsByCode(String code);

    // 统计特定讲师的课程数量
    @Query("SELECT COUNT(c) FROM Course c WHERE c.instructor.name = :instructorName")
    Long countByInstructorName(@Param("instructorName") String instructorName);
}