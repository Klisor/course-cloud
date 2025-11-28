package com.zjsu.nsq.catalog.service;

import com.zjsu.nsq.catalog.model.Course;
import com.zjsu.nsq.catalog.repository.CourseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CourseService {
    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @Transactional(readOnly = true)
    public List<Course> findAll() {
        return courseRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Course> findById(Long id) {
        return courseRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Course> findByCode(String code) {
        return courseRepository.findByCode(code);
    }

    @Transactional(readOnly = true)
    public List<Course> findByTitleContaining(String title) {
        return courseRepository.findByTitleContainingIgnoreCase(title);
    }

    @Transactional(readOnly = true)
    public List<Course> findAvailableCourses() {
        return courseRepository.findAvailableCourses();
    }

    @Transactional(readOnly = true)
    public List<Course> findByInstructorName(String instructorName) {
        return courseRepository.findByInstructorName(instructorName);
    }

    public Course create(Course course) {
        // 检查课程代码是否已存在
        if (courseRepository.existsByCode(course.getCode())) {
            throw new CourseAlreadyExistsException("课程代码已存在: " + course.getCode());
        }

        // 设置默认值
        if (course.getEnrolled() == null) {
            course.setEnrolled(0);
        }
        if (course.getCapacity() == null) {
            course.setCapacity(0);
        }

        return courseRepository.save(course);
    }

    public Course update(Long id, Course course) {
        // 检查课程是否存在
        Course existingCourse = courseRepository.findById(id)
                .orElseThrow(() -> new CourseNotFoundException("课程不存在，ID: " + id));

        // 检查课程代码是否被其他课程使用
        if (!existingCourse.getCode().equals(course.getCode()) &&
                courseRepository.existsByCode(course.getCode())) {
            throw new CourseAlreadyExistsException("课程代码已被其他课程使用: " + course.getCode());
        }

        // 更新字段
        existingCourse.setCode(course.getCode());
        existingCourse.setTitle(course.getTitle());
        existingCourse.setInstructor(course.getInstructor());
        existingCourse.setSchedule(course.getSchedule());
        existingCourse.setCapacity(course.getCapacity());
        existingCourse.setEnrolled(course.getEnrolled());

        return courseRepository.save(existingCourse);
    }

    // 🔥 新增：专门用于更新课程选课人数的方法（供 enrollment-service 调用）
    public Course updateEnrolledCount(Long courseId, Integer newEnrolledCount) {
        // 1. 检查课程是否存在（复用现有异常类）
        Course existingCourse = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException("课程不存在，ID: " + courseId));

        // 2. 校验选课人数合法性（避免负数或超出容量，保持数据一致性）
        if (newEnrolledCount < 0) {
            throw new IllegalArgumentException("选课人数不能为负数");
        }
        // 若课程容量已设置，确保选课人数不超过容量（兼容你原有 create 方法的默认值 0）
        if (existingCourse.getCapacity() != null && newEnrolledCount > existingCourse.getCapacity()) {
            throw new IllegalArgumentException("选课人数超出课程容量（容量：" + existingCourse.getCapacity() + "）");
        }

        // 3. 仅更新选课人数字段（不改动其他字段，高效）
        existingCourse.setEnrolled(newEnrolledCount);

        // 4. 保存并返回更新后的课程
        return courseRepository.save(existingCourse);
    }

    public void delete(Long id) {
        // 检查课程是否存在
        if (!courseRepository.existsById(id)) {
            throw new CourseNotFoundException("课程不存在，ID: " + id);
        }

        // 检查是否有选课记录（这里需要 EnrollmentRepository）
        // 这个检查将在 EnrollmentService 中实现更完整的关联检查

        courseRepository.deleteById(id);
    }

    // 自定义异常
    public static class CourseNotFoundException extends RuntimeException {
        public CourseNotFoundException(String message) {
            super(message);
        }
    }

    public static class CourseAlreadyExistsException extends RuntimeException {
        public CourseAlreadyExistsException(String message) {
            super(message);
        }
    }
}