package com.zjsu.nsq.enrollment.service;

import com.zjsu.nsq.enrollment.exception.ResourceNotFoundException;
import com.zjsu.nsq.enrollment.model.Enrollment;
import com.zjsu.nsq.enrollment.model.EnrollmentStatus;
import com.zjsu.nsq.enrollment.repository.EnrollmentRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
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

    private static final Logger log = LoggerFactory.getLogger(EnrollmentService.class);

    private final EnrollmentRepository enrollmentRepository;
    private final RestTemplate restTemplate;

    @Value("${USER_SERVICE_URL:http://user-service:8083}")
    private String userServiceUrl;

    @Value("${CATALOG_SERVICE_URL:http://catalog-service:8081}")
    private String catalogServiceUrl;

    public EnrollmentService(EnrollmentRepository enrollmentRepository, RestTemplate restTemplate) {
        this.enrollmentRepository = enrollmentRepository;
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    public void init() {
        log.info("=== Enrollment Service 初始化 ===");
        log.info("用户服务 URL: {}", userServiceUrl);
        log.info("课程服务 URL: {}", catalogServiceUrl);
        log.info("===============================");
    }

    // ==================== 查询方法 ====================

    @Transactional(readOnly = true)
    public List<Enrollment> findAll() {
        return enrollmentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Enrollment> findByCourse(String courseId) {
        return enrollmentRepository.findByCourseId(courseId);
    }

    @Transactional(readOnly = true)
    public List<Enrollment> findByUser(String userId) {
        return enrollmentRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<Enrollment> findByStatus(EnrollmentStatus status) {
        return enrollmentRepository.findAll().stream()
                .filter(e -> e.getStatus() == status)
                .toList();
    }

    @Transactional(readOnly = true)
    public Enrollment findById(Long id) {
        return enrollmentRepository.findById(id)
                .orElseThrow(() -> new EnrollmentNotFoundException("选课记录不存在，ID: " + id));
    }

    // ==================== 业务方法 ====================

    /**
     * 学生选课
     */
    public Enrollment enroll(String courseId, String userId) {
        log.info("🚀 开始选课流程 - courseId: {}, userId: {}", courseId, userId);

        // 1. 参数验证
        validateEnrollmentParameters(courseId, userId);

        // 2. 验证用户存在
        validateUserExists(userId);

        // 3. 获取课程信息并验证
        CourseInfo courseInfo = getAndValidateCourse(courseId);

        // 4. 检查重复选课
        checkDuplicateEnrollment(courseId, userId);

        // 5. 创建选课记录
        Enrollment enrollment = createEnrollment(courseId, userId);

        // 6. 异步更新课程已选人数（不阻塞主流程）
        updateCourseEnrollmentCountAsync(courseId, courseInfo.getEnrolled() + 1);

        log.info("✅ 选课成功 - enrollmentId: {}, courseId: {}, userId: {}",
                enrollment.getId(), courseId, userId);
        return enrollment;
    }

    /**
     * 按选课ID退课
     */
    public Enrollment drop(Long enrollmentId) {
        log.info("🔙 开始退课 - enrollmentId: {}", enrollmentId);

        // 1. 获取选课记录
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new EnrollmentNotFoundException("选课记录不存在，ID: " + enrollmentId));

        // 2. 验证选课状态
        validateActiveStatus(enrollment, "退课");

        // 3. 获取课程信息
        CourseInfo courseInfo = getCourseInfo(enrollment.getCourseId());

        // 4. 更新选课状态
        enrollment.setStatus(EnrollmentStatus.DROPPED);
        Enrollment updated = enrollmentRepository.save(enrollment);

        // 5. 异步更新课程已选人数
        updateCourseEnrollmentCountAsync(enrollment.getCourseId(), courseInfo.getEnrolled() - 1);

        log.info("✅ 退课成功 - enrollmentId: {}", enrollmentId);
        return updated;
    }

    /**
     * 按用户和课程退课
     */
    public Enrollment dropByUserAndCourse(String userId, String courseId) {
        log.info("🔙 按用户和课程退课 - userId: {}, courseId: {}", userId, courseId);

        Enrollment enrollment = enrollmentRepository
                .findByCourseIdAndUserIdAndStatus(courseId, userId, EnrollmentStatus.ACTIVE)
                .orElseThrow(() -> new EnrollmentNotFoundException(
                        "未找到用户[" + userId + "]的课程[" + courseId + "]活跃选课记录"));

        return drop(enrollment.getId());
    }

    /**
     * 标记课程完成
     */
    public Enrollment complete(Long enrollmentId) {
        log.info("🎓 标记课程完成 - enrollmentId: {}", enrollmentId);

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new EnrollmentNotFoundException("选课记录不存在，ID: " + enrollmentId));

        // 验证选课状态
        validateActiveStatus(enrollment, "标记完成");

        enrollment.setStatus(EnrollmentStatus.COMPLETED);
        Enrollment result = enrollmentRepository.save(enrollment);

        log.info("✅ 课程标记完成成功 - enrollmentId: {}", enrollmentId);
        return result;
    }

    /**
     * 删除选课记录（管理员操作）
     */
    public void delete(Long enrollmentId) {
        log.info("🗑️ 删除选课记录 - enrollmentId: {}", enrollmentId);

        if (!enrollmentRepository.existsById(enrollmentId)) {
            throw new EnrollmentNotFoundException("选课记录不存在，ID: " + enrollmentId);
        }

        enrollmentRepository.deleteById(enrollmentId);
        log.info("✅ 删除成功 - enrollmentId: {}", enrollmentId);
    }

    /**
     * 退课（删除并更新课程人数）
     */
    public void unenroll(Long enrollmentId) {
        log.info("🔙 退课操作 - enrollmentId: {}", enrollmentId);

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", enrollmentId.toString()));

        // 获取课程信息
        CourseInfo courseInfo = getCourseInfo(enrollment.getCourseId());

        // 删除选课记录
        enrollmentRepository.delete(enrollment);

        // 异步更新课程已选人数
        updateCourseEnrollmentCountAsync(enrollment.getCourseId(), courseInfo.getEnrolled() - 1);

        log.info("✅ 退课成功 - enrollmentId: {}", enrollmentId);
    }

    // ==================== 统计方法 ====================

    @Transactional(readOnly = true)
    public Long countActiveEnrollmentsByCourse(String courseId) {
        return enrollmentRepository.countByCourseIdAndStatus(courseId, EnrollmentStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public Long countActiveEnrollmentsByUser(String userId) {
        return enrollmentRepository.countByUserIdAndStatus(userId, EnrollmentStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public Long countTotalEnrollmentsByCourse(String courseId) {
        return enrollmentRepository.countByCourseId(courseId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getEnrollmentStats(String courseId) {
        long total = countTotalEnrollmentsByCourse(courseId);
        long active = countActiveEnrollmentsByCourse(courseId);
        long completed = enrollmentRepository.countByCourseIdAndStatus(courseId, EnrollmentStatus.COMPLETED);
        long dropped = enrollmentRepository.countByCourseIdAndStatus(courseId, EnrollmentStatus.DROPPED);

        return Map.of(
                "courseId", courseId,
                "total", total,
                "active", active,
                "completed", completed,
                "dropped", dropped
        );
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 验证选课参数
     */
    private void validateEnrollmentParameters(String courseId, String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new RuntimeException("userId 不能为空");
        }
        if (courseId == null || courseId.trim().isEmpty()) {
            throw new RuntimeException("courseId 不能为空");
        }
    }

    /**
     * 验证用户存在
     */
    private void validateUserExists(String userId) {
        try {
            String url = buildUserServiceUrl(userId);
            log.debug("🔍 调用用户服务验证用户 - URL: {}", url);

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("❌ 用户服务返回错误状态码: {}", response.getStatusCode());
                throw new StudentNotFoundException("用户服务返回错误: " + response.getStatusCode());
            }

            log.debug("✅ 用户验证成功 - userId: {}", userId);
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("⚠️ 用户不存在 - userId: {}", userId);
            throw new StudentNotFoundException("用户不存在，userId: " + userId);
        } catch (Exception e) {
            log.error("❌ 调用用户服务失败", e);
            throw new ServiceCallException("调用用户服务失败: " + e.getMessage());
        }
    }

    /**
     * 获取并验证课程信息
     */
    private CourseInfo getAndValidateCourse(String courseId) {
        CourseInfo courseInfo = getCourseInfo(courseId);

        // 检查课程容量
        if (courseInfo.getEnrolled() >= courseInfo.getCapacity()) {
            log.warn("⚠️ 课程已满 - courseId: {}, capacity: {}, enrolled: {}",
                    courseId, courseInfo.getCapacity(), courseInfo.getEnrolled());
            throw new CourseFullException(
                    String.format("课程已满（容量: %d，已选: %d）",
                            courseInfo.getCapacity(), courseInfo.getEnrolled()));
        }

        return courseInfo;
    }

    /**
     * 检查重复选课
     */
    private void checkDuplicateEnrollment(String courseId, String userId) {
        if (enrollmentRepository.existsByCourseIdAndUserId(courseId, userId)) {
            log.warn("⚠️ 重复选课 - courseId: {}, userId: {}", courseId, userId);
            throw new DuplicateEnrollmentException(
                    String.format("用户[%s]已选课程[%s]", userId, courseId));
        }
    }

    /**
     * 创建选课记录
     */
    private Enrollment createEnrollment(String courseId, String userId) {
        Enrollment enrollment = new Enrollment();
        enrollment.setCourseId(courseId);
        enrollment.setUserId(userId);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);

        return enrollmentRepository.save(enrollment);
    }

    /**
     * 验证选课状态为活跃
     */
    private void validateActiveStatus(Enrollment enrollment, String operation) {
        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            log.warn("⚠️ 无效的{}操作 - enrollmentId: {}, status: {}",
                    operation, enrollment.getId(), enrollment.getStatus());
            throw new InvalidEnrollmentOperationException(
                    String.format("仅活跃状态的选课可%s（当前状态: %s）",
                            operation, enrollment.getStatus()));
        }
    }

    /**
     * 获取课程信息
     */
    private CourseInfo getCourseInfo(String courseId) {
        try {
            String url = buildCatalogServiceUrl(courseId);
            log.debug("🔍 调用课程服务 - URL: {}", url);

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("❌ 课程服务返回错误状态码: {}", response.getStatusCode());
                throw new CourseNotFoundException("课程服务返回错误: " + response.getStatusCode());
            }

            Map<String, Object> body = response.getBody();
            if (body == null || body.get("data") == null) {
                log.error("❌ 课程服务响应格式错误");
                throw new ServiceCallException("课程服务响应格式错误");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> courseData = (Map<String, Object>) body.get("data");

            Integer capacity = (Integer) courseData.get("capacity");
            Integer enrolled = (Integer) courseData.get("enrolled");
            String code = (String) courseData.get("code");
            String title = (String) courseData.get("title");

            if (capacity == null || enrolled == null) {
                throw new ServiceCallException("课程数据不完整");
            }

            log.debug("✅ 获取课程信息成功 - courseId: {}, title: {}", courseId, title);
            return new CourseInfo(capacity, enrolled, code, title);

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("⚠️ 课程不存在 - courseId: {}", courseId);
            throw new CourseNotFoundException("课程不存在，ID: " + courseId);
        } catch (NumberFormatException e) {
            log.error("❌ 课程ID格式错误: {}", courseId);
            throw new CourseNotFoundException("课程ID必须是数字: " + courseId);
        } catch (Exception e) {
            log.error("❌ 调用课程服务失败", e);
            throw new ServiceCallException("调用课程服务失败: " + e.getMessage());
        }
    }

    /**
     * 异步更新课程已选人数
     */
    private void updateCourseEnrollmentCountAsync(String courseId, int newCount) {
        new Thread(() -> {
            try {
                String url = buildUpdateCourseUrl(courseId, newCount);
                log.debug("🔄 异步更新课程人数 - URL: {}", url);

                restTemplate.put(url, null);
                log.info("✅ 课程已选人数更新成功 - courseId: {}, newCount: {}", courseId, newCount);

            } catch (Exception e) {
                log.error("❌ 异步更新课程人数失败 - courseId: {}, error: {}", courseId, e.getMessage());
                // 这里可以记录到补偿表或发送到消息队列进行重试
            }
        }).start();
    }

    /**
     * 同步更新课程已选人数
     */
    private void updateCourseEnrollmentCountSync(String courseId, int newCount) {
        try {
            String url = buildUpdateCourseUrl(courseId, newCount);
            log.debug("🔄 同步更新课程人数 - URL: {}", url);

            restTemplate.put(url, null);
            log.info("✅ 课程已选人数更新成功 - courseId: {}, newCount: {}", courseId, newCount);

        } catch (Exception e) {
            log.error("❌ 更新课程人数失败", e);
            throw new ServiceCallException("更新课程人数失败: " + e.getMessage());
        }
    }

    // ==================== URL 构建方法 ====================

    private String buildUserServiceUrl(String userId) {
        String baseUrl = userServiceUrl.endsWith("/")
                ? userServiceUrl.substring(0, userServiceUrl.length() - 1)
                : userServiceUrl;
        return baseUrl + "/api/users/" + userId;
    }

    private String buildCatalogServiceUrl(String courseId) {
        String baseUrl = catalogServiceUrl.endsWith("/")
                ? catalogServiceUrl.substring(0, catalogServiceUrl.length() - 1)
                : catalogServiceUrl;
        return baseUrl + "/api/courses/" + courseId;
    }

    private String buildUpdateCourseUrl(String courseId, int newCount) {
        String baseUrl = catalogServiceUrl.endsWith("/")
                ? catalogServiceUrl.substring(0, catalogServiceUrl.length() - 1)
                : catalogServiceUrl;
        return baseUrl + "/api/courses/" + courseId + "/enrolled?count=" + newCount;
    }

    // ==================== 内部类 ====================

    /**
     * 课程信息封装类
     */
    private static class CourseInfo {
        private final int capacity;
        private final int enrolled;
        private final String code;
        private final String title;

        public CourseInfo(int capacity, int enrolled, String code, String title) {
            this.capacity = capacity;
            this.enrolled = enrolled;
            this.code = code;
            this.title = title;
        }

        public int getCapacity() { return capacity; }
        public int getEnrolled() { return enrolled; }
        public String getCode() { return code; }
        public String getTitle() { return title; }
    }

    // ==================== 自定义异常类 ====================

    public static class EnrollmentNotFoundException extends RuntimeException {
        public EnrollmentNotFoundException(String message) { super(message); }
    }

    public static class DuplicateEnrollmentException extends RuntimeException {
        public DuplicateEnrollmentException(String message) { super(message); }
    }

    public static class CourseFullException extends RuntimeException {
        public CourseFullException(String message) { super(message); }
    }

    public static class InvalidEnrollmentOperationException extends RuntimeException {
        public InvalidEnrollmentOperationException(String message) { super(message); }
    }

    public static class StudentNotFoundException extends RuntimeException {
        public StudentNotFoundException(String message) { super(message); }
    }

    public static class CourseNotFoundException extends RuntimeException {
        public CourseNotFoundException(String message) { super(message); }
    }

    public static class ServiceCallException extends RuntimeException {
        public ServiceCallException(String message) { super(message); }
    }
}