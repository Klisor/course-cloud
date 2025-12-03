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

    private final CourseRepository repo;

    public CourseService(CourseRepository repo) {
        this.repo = repo;
    }

    public List<Course> findAll() {
        return repo.findAll();
    }

    public Optional<Course> findById(Long id) {
        return repo.findById(id);
    }

    public Optional<Course> findByCode(String code) {
        return repo.findByCode(code);
    }

    public Course create(Course course) {
        // 检查课程代码是否已存在
        if (repo.existsByCode(course.getCode())) {
            throw new CourseAlreadyExistsException("课程代码已存在: " + course.getCode());
        }

        // 设置默认值
        if (course.getEnrolled() == null) {
            course.setEnrolled(0);
        }

        if (course.getCapacity() == null || course.getCapacity() <= 0) {
            course.setCapacity(50); // 默认容量
        }

        return repo.save(course);
    }

    public Course update(Long id, Course course) {
        Course existingCourse = repo.findById(id)
                .orElseThrow(() -> new CourseNotFoundException("课程不存在，ID: " + id));

        // 检查课程代码是否重复（如果修改了课程代码）
        if (!existingCourse.getCode().equals(course.getCode()) &&
                repo.existsByCode(course.getCode())) {
            throw new CourseAlreadyExistsException("课程代码已存在: " + course.getCode());
        }

        // 更新字段
        existingCourse.setCode(course.getCode());
        existingCourse.setTitle(course.getTitle());
        existingCourse.setInstructor(course.getInstructor());
        existingCourse.setSchedule(course.getSchedule());
        existingCourse.setCapacity(course.getCapacity());

        // 注意：更新时不允许直接修改已选人数，只能通过专门的接口修改
        // existingCourse.setEnrolled(course.getEnrolled());

        return repo.save(existingCourse);
    }

    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new CourseNotFoundException("课程不存在，ID: " + id);
        }
        repo.deleteById(id);
    }

    public List<Course> findByTitleContaining(String title) {
        return repo.findByTitleContainingIgnoreCase(title);
    }

    public List<Course> findByInstructorName(String instructorName) {
        return repo.findByInstructorName(instructorName);
    }

    public List<Course> findAvailableCourses() {
        return repo.findAvailableCourses();
    }

    // 🔥 新增：更新课程选课人数（供 enrollment-service 调用）
    @Transactional
    public Course updateEnrolledCount(Long courseId, Integer newEnrolledCount) {
        Course course = repo.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException("课程不存在，ID: " + courseId));

        // 验证新的选课人数是否有效
        if (newEnrolledCount == null) {
            throw new InvalidCourseDataException("选课人数不能为空");
        }

        if (newEnrolledCount < 0) {
            throw new InvalidCourseDataException("选课人数不能为负数: " + newEnrolledCount);
        }

        if (newEnrolledCount > course.getCapacity()) {
            throw new InvalidCourseDataException(
                    String.format("选课人数超过课程容量（容量: %d，请求: %d）",
                            course.getCapacity(), newEnrolledCount)
            );
        }

        course.setEnrolled(newEnrolledCount);
        return repo.save(course);
    }

    // 🔥 新增：原子操作 - 增加选课人数（加1）
    @Transactional
    public Course incrementEnrolledCount(Long courseId) {
        Course course = repo.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException("课程不存在，ID: " + courseId));

        if (course.getEnrolled() >= course.getCapacity()) {
            throw new CourseFullException("课程已满，无法增加选课人数");
        }

        course.setEnrolled(course.getEnrolled() + 1);
        return repo.save(course);
    }

    // 🔥 新增：原子操作 - 减少选课人数（减1）
    @Transactional
    public Course decrementEnrolledCount(Long courseId) {
        Course course = repo.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException("课程不存在，ID: " + courseId));

        if (course.getEnrolled() <= 0) {
            throw new InvalidCourseDataException("选课人数已为0，无法再减少");
        }

        course.setEnrolled(course.getEnrolled() - 1);
        return repo.save(course);
    }

    // 🔥 新增：检查课程是否还有容量
    @Transactional(readOnly = true)
    public boolean hasAvailableCapacity(Long courseId) {
        Course course = repo.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException("课程不存在，ID: " + courseId));
        return course.getEnrolled() < course.getCapacity();
    }

    // 🔥 新增：获取课程剩余容量
    @Transactional(readOnly = true)
    public int getAvailableCapacity(Long courseId) {
        Course course = repo.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException("课程不存在，ID: " + courseId));
        return course.getCapacity() - course.getEnrolled();
    }

    // ==================== 异常类 ====================

    public static class CourseNotFoundException extends RuntimeException {
        public CourseNotFoundException(String message) { super(message); }
    }

    public static class CourseAlreadyExistsException extends RuntimeException {
        public CourseAlreadyExistsException(String message) { super(message); }
    }

    public static class InvalidCourseDataException extends RuntimeException {
        public InvalidCourseDataException(String message) { super(message); }
    }

    public static class CourseFullException extends RuntimeException {
        public CourseFullException(String message) { super(message); }
    }
}