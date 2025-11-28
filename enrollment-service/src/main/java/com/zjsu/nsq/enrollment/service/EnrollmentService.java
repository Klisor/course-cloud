package com.zjsu.nsq.enrollment.service;

import com.zjsu.nsq.enrollment.exception.ResourceNotFoundException;
import com.zjsu.nsq.enrollment.model.Enrollment;
import com.zjsu.nsq.enrollment.model.EnrollmentStatus;
import com.zjsu.nsq.enrollment.model.Student;
import com.zjsu.nsq.enrollment.repository.EnrollmentRepository;
import com.zjsu.nsq.enrollment.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional
public class EnrollmentService {
    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final RestTemplate restTemplate;

    @Value("${catalog-service.url}")
    private String catalogServiceUrl;

    public EnrollmentService(EnrollmentRepository enrollmentRepository,
                             StudentRepository studentRepository,
                             RestTemplate restTemplate) {
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.restTemplate = restTemplate;
    }

    @Transactional(readOnly = true)
    public List<Enrollment> findAll() {
        return enrollmentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Enrollment> findByCourse(String courseId) {
        return enrollmentRepository.findByCourseId(courseId);
    }

    @Transactional(readOnly = true)
    public List<Enrollment> findByStudent(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("学生不存在，ID: " + studentId));
        return enrollmentRepository.findByStudent(student);
    }

    @Transactional(readOnly = true)
    public List<Enrollment> findByStatus(EnrollmentStatus status) {
        return enrollmentRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<Enrollment> findActiveByCourse(String courseId) {
        return enrollmentRepository.findByCourseIdAndStatus(courseId, EnrollmentStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<Enrollment> findActiveByStudent(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("学生不存在，ID: " + studentId));
        return enrollmentRepository.findByStudentAndStatus(student, EnrollmentStatus.ACTIVE);
    }

    public Enrollment enroll(String courseId, Long studentId) {
        // 1. 验证学生是否存在（按学号查询）
        Student student = studentRepository.findByStudentId(studentId.toString())
                .orElseThrow(() -> new StudentNotFoundException("学生不存在，学号: " + studentId));

        // 2. 调用课程服务获取课程信息（优化：复用工具方法，避免重复代码）
        Map<String, Object> courseData = getCourseFromCatalogService(courseId);

        // 3. 提取课程容量和已选人数（优化：添加非空校验，避免空指针）
        Integer capacity = Objects.requireNonNull((Integer) courseData.get("capacity"), "课程容量不能为空");
        Integer enrolled = Objects.requireNonNull((Integer) courseData.get("enrolled"), "已选人数不能为空");

        // 4. 业务校验
        if (enrolled >= capacity) {
            throw new CourseFullException("课程已满（当前容量: " + capacity + "，已选: " + enrolled + "）");
        }
        if (enrollmentRepository.existsByCourseIdAndStudentId(courseId, studentId.toString())) {
            throw new DuplicateEnrollmentException("学生[" + studentId + "]已选课程[" + courseId + "]");
        }

        // 5. 创建选课记录
        Enrollment enrollment = new Enrollment();
        enrollment.setCourseId(courseId);
        enrollment.setStudent(student);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment.setEnrolledAt(LocalDateTime.now());
        Enrollment saved = enrollmentRepository.save(enrollment);

        // 6. 更新课程已选人数（服务间调用）
        updateCourseEnrolledCount(courseId, enrolled + 1);

        return saved;
    }

    public Enrollment drop(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new EnrollmentNotFoundException("选课记录不存在，ID: " + enrollmentId));

        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new InvalidEnrollmentOperationException("仅活跃状态的选课可退选（当前状态: " + enrollment.getStatus() + "）");
        }

        // 获取课程信息并更新人数
        Map<String, Object> courseData = getCourseFromCatalogService(enrollment.getCourseId());
        Integer enrolled = Objects.requireNonNull((Integer) courseData.get("enrolled"), "已选人数不能为空");

        enrollment.setStatus(EnrollmentStatus.DROPPED);
        Enrollment updated = enrollmentRepository.save(enrollment);

        updateCourseEnrolledCount(enrollment.getCourseId(), enrolled - 1);
        return updated;
    }

    public Enrollment dropByStudentAndCourse(Long studentId, String courseId) {
        Enrollment enrollment = enrollmentRepository
                .findByCourseIdAndStudentIdAndStatus(courseId, studentId.toString(), EnrollmentStatus.ACTIVE)
                .orElseThrow(() -> new EnrollmentNotFoundException(
                        "未找到学生[" + studentId + "]的课程[" + courseId + "]活跃选课记录"));

        // 获取课程信息并更新人数
        Map<String, Object> courseData = getCourseFromCatalogService(courseId);
        Integer enrolled = Objects.requireNonNull((Integer) courseData.get("enrolled"), "已选人数不能为空");

        enrollment.setStatus(EnrollmentStatus.DROPPED);
        Enrollment updated = enrollmentRepository.save(enrollment);

        updateCourseEnrolledCount(courseId, enrolled - 1);
        return updated;
    }

    public Enrollment complete(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new EnrollmentNotFoundException("选课记录不存在，ID: " + enrollmentId));

        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new InvalidEnrollmentOperationException("仅活跃状态的选课可标记完成（当前状态: " + enrollment.getStatus() + "）");
        }

        enrollment.setStatus(EnrollmentStatus.COMPLETED);
        return enrollmentRepository.save(enrollment);
    }

    public void delete(Long enrollmentId) {
        if (!enrollmentRepository.existsById(enrollmentId)) {
            throw new EnrollmentNotFoundException("选课记录不存在，ID: " + enrollmentId);
        }
        enrollmentRepository.deleteById(enrollmentId);
    }

    @Transactional(readOnly = true)
    public Long countActiveEnrollmentsByCourse(String courseId) {
        return enrollmentRepository.countByCourseIdAndStatus(courseId, EnrollmentStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public Long countActiveEnrollmentsByStudent(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("学生不存在，ID: " + studentId));
        return enrollmentRepository.countByStudentAndStatus(student, EnrollmentStatus.ACTIVE);
    }

    // 🌟 优化1：添加unenroll方法的ResourceNotFoundException导入（避免编译错误）
    public void unenroll(Long enrollmentId) {
        // 1. 查找选课记录（抛出独立的ResourceNotFoundException，供Controller捕获）
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", enrollmentId.toString()));

        // 2. 获取课程信息（复用工具方法，添加异常处理）
        Map<String, Object> courseData = getCourseFromCatalogService(enrollment.getCourseId());
        Integer enrolled = Objects.requireNonNull((Integer) courseData.get("enrolled"), "已选人数不能为空");

        // 3. 删除选课记录
        enrollmentRepository.delete(enrollment);

        // 4. 更新课程已选人数（-1）
        updateCourseEnrolledCount(enrollment.getCourseId(), enrolled - 1);
    }

    // 🌟 优化2：工具方法 - 从课程服务获取课程信息（添加非空校验，避免空指针）
    // 🌟 修正：String 类型 courseId 转为 Long，匹配 catalog 的接口要求
    private Map<String, Object> getCourseFromCatalogService(String courseId) {
        try {
            // 1. 字符串转 Long（确保 courseId 是数字字符串，如 "3"）
            Long courseIdLong = Long.valueOf(courseId);

            // 2. 调用 catalog 的按 ID 查询接口（传递 Long 类型 ID）
            String url = catalogServiceUrl + "/api/courses/" + courseIdLong;

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            // 3. 校验响应和 data 字段非空
            if (response == null || response.get("data") == null) {
                throw new RuntimeException("课程服务响应格式错误，无有效数据");
            }

            return (Map<String, Object>) response.get("data");
        } catch (NumberFormatException e) {
            throw new CourseNotFoundException("课程 ID 必须是数字字符串（如 \"3\"），当前值：" + courseId);
        } catch (HttpClientErrorException.NotFound e) {
            throw new CourseNotFoundException("课程不存在，ID: " + courseId);
        } catch (Exception e) {
            throw new RuntimeException("调用课程服务失败: " + e.getMessage() + "（URL: " + catalogServiceUrl + "/api/courses/" + courseId + "）");
        }
    }
    // 🌟 关键修改：调用 catalog 专门的更新人数接口，而非通用 PUT 接口
// 🌟 最终修复：用 restTemplate.put() 替代 putForObject，避免 responseType 问题
    private void updateCourseEnrolledCount(String courseId, int newCount) {
        try {
            // 1. String 转 Long（匹配 catalog 的课程 ID 类型）
            Long courseIdLong = Long.valueOf(courseId);

            // 2. 拼接 catalog 专门的更新人数接口 URL（确保参数名是 count，和 catalog 接口一致）
            String updateUrl = catalogServiceUrl + "/api/courses/" + courseIdLong + "/enrolled?count=" + newCount;
            System.out.println("调用 catalog 更新人数接口：" + updateUrl); // 打印 URL，方便调试

            // 3. 发送 PUT 请求（无请求体，无需接收返回值）
            restTemplate.put(updateUrl, null); // 关键修改：用 put() 替代 putForObject()

            // 4. 若没报错，说明更新成功（catalog 接口会自动处理参数校验）
            System.out.println("课程[" + courseId + "]人数更新成功，新人数：" + newCount);
        } catch (NumberFormatException e) {
            throw new RuntimeException("courseId 必须是数字字符串（如 \"3\"），当前值：" + courseId);
        } catch (HttpClientErrorException e) {
            // 捕获 catalog 接口返回的 404/409 等错误，友好提示
            String errorMsg = "调用 catalog 接口失败：" + e.getStatusCode() + "，原因：" + e.getResponseBodyAsString();
            System.err.println(errorMsg);
            throw new RuntimeException(errorMsg);
        } catch (Exception e) {
            String errorMsg = "更新课程[" + courseId + "]已选人数失败: " + e.getMessage();
            System.err.println(errorMsg);
            throw new RuntimeException("选课失败：" + errorMsg);
        }
    }
    // 自定义内部异常类（与Controller引用完全匹配）
    public static class EnrollmentNotFoundException extends RuntimeException {
        public EnrollmentNotFoundException(String message) {
            super(message);
        }
    }

    public static class DuplicateEnrollmentException extends RuntimeException {
        public DuplicateEnrollmentException(String message) {
            super(message);
        }
    }

    public static class CourseFullException extends RuntimeException {
        public CourseFullException(String message) {
            super(message);
        }
    }

    public static class InvalidEnrollmentOperationException extends RuntimeException {
        public InvalidEnrollmentOperationException(String message) {
            super(message);
        }
    }

    public static class StudentNotFoundException extends RuntimeException {
        public StudentNotFoundException(String message) {
            super(message);
        }
    }

    public static class CourseNotFoundException extends RuntimeException {
        public CourseNotFoundException(String message) {
            super(message);
        }
    }
}