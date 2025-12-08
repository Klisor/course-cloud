package com.zjsu.nsq.enrollment.service;

import com.zjsu.nsq.enrollment.client.UserClient;
import com.zjsu.nsq.enrollment.client.CatalogClient;
import com.zjsu.nsq.enrollment.dto.StudentDto;
import com.zjsu.nsq.enrollment.dto.CourseDto;
import com.zjsu.nsq.enrollment.exception.ResourceNotFoundException;
import com.zjsu.nsq.enrollment.exception.ServiceUnavailableException;
import com.zjsu.nsq.enrollment.model.Enrollment;
import com.zjsu.nsq.enrollment.model.EnrollmentStatus;
import com.zjsu.nsq.enrollment.repository.EnrollmentRepository;
import com.zjsu.nsq.enrollment.util.JsonParser;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class EnrollmentService {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentService.class);

    private final EnrollmentRepository enrollmentRepository;
    private final LoadBalancerClient loadBalancerClient;
    private final DiscoveryClient discoveryClient;
    private final UserClient userClient;
    private final CatalogClient catalogClient;

    @Value("${USER_SERVICE_URL:http://user-service:8083}")
    private String userServiceUrl;

    @Value("${CATALOG_SERVICE_URL:http://catalog-service:8081}")
    private String catalogServiceUrl;

    public EnrollmentService(EnrollmentRepository enrollmentRepository,
                             LoadBalancerClient loadBalancerClient,
                             DiscoveryClient discoveryClient,
                             UserClient userClient,
                             CatalogClient catalogClient) {
        this.enrollmentRepository = enrollmentRepository;
        this.loadBalancerClient = loadBalancerClient;
        this.discoveryClient = discoveryClient;
        this.userClient = userClient;
        this.catalogClient = catalogClient;
    }

    @PostConstruct
    public void init() {
        log.info("=== Enrollment Service 初始化 ===");
        log.info("用户服务 URL: {}", userServiceUrl);
        log.info("课程服务 URL: {}", catalogServiceUrl);
        log.info("使用 OpenFeign 进行服务间通信");

        // 检查服务发现
        List<String> services = discoveryClient.getServices();
        log.info("已注册的服务: {}", services);

        log.info("===============================");
    }

    // ==================== 查询方法（保持不变） ====================

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
        return enrollmentRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public Enrollment findById(Long id) {
        return enrollmentRepository.findById(id)
                .orElseThrow(() -> new EnrollmentNotFoundException("选课记录不存在，ID: " + id));
    }

    // ==================== 核心业务方法 ====================

    /**
     * 学生选课 - 使用新的 Feign 客户端
     */
    public Enrollment enroll(String courseIdStr, String userIdStr) {
        log.info("🚀 开始选课流程 - courseId: {}, userId: {}", courseIdStr, userIdStr);

        // 1. 参数验证
        validateEnrollmentParameters(courseIdStr, userIdStr);

        // 2. 将字符串参数转换为Long
        Long courseId = convertToLong(courseIdStr, "课程ID");
        Long userId = convertToLong(userIdStr, "用户ID");

        // 3. 验证用户存在 - 使用新的 Feign 客户端
        validateUserExists(userId);

        // 4. 获取课程信息并验证 - 使用新的 Feign 客户端
        CourseDto courseDto = getAndValidateCourse(courseId);

        // 5. 检查重复选课
        checkDuplicateEnrollment(courseIdStr, userIdStr);

        // 6. 创建选课记录
        Enrollment enrollment = createEnrollment(courseIdStr, userIdStr);

        // 7. 异步更新课程已选人数
        updateCourseEnrollmentCountAsync(courseId, courseDto.getData().getEnrolled() + 1);

        log.info("✅ 选课成功 - enrollmentId: {}, courseId: {}, userId: {}",
                enrollment.getId(), courseIdStr, userIdStr);
        return enrollment;
    }

    /**
     * 验证用户存在 - 使用新的 Feign 客户端
     */
    // ==================== 核心业务方法 ====================

    /**
     * 验证用户存在 - 使用新的 Feign 客户端
     */
    private void validateUserExists(Long userId) {
        try {
            log.info("🔍 使用 OpenFeign 调用用户服务 - userId: {}", userId);

            // 修改：Feign客户端现在直接返回StudentDto
            StudentDto studentDto = userClient.getStudent(userId);

            if (studentDto == null || studentDto.getData() == null || studentDto.getData().getId() == null) {
                log.error("❌ 用户不存在 - userId: {}", userId);
                throw new StudentNotFoundException("用户不存在，userId: " + userId);
            }

            log.info("✅ 用户验证成功 - userId: {}, username: {}", userId, studentDto.getData().getName());

        } catch (Exception e) {
            log.error("❌ 调用用户服务失败", e);
            throw new StudentNotFoundException("用户服务调用失败，userId: " + userId);
        }
    }

    /**
     * 获取并验证课程信息
     */
    private CourseDto getAndValidateCourse(Long courseId) {
        try {
            log.info("🔍 使用 OpenFeign 调用课程服务 - courseId: {}", courseId);

            // 修改：Feign客户端现在直接返回CourseDto
            CourseDto courseDto = catalogClient.getCourse(courseId);

            if (courseDto == null || courseDto.getData() == null || courseDto.getData().getId() == null) {
                log.error("❌ 课程不存在 - courseId: {}", courseId);
                throw new CourseNotFoundException("课程不存在，courseId: " + courseId);
            }

            CourseDto.Data courseData = courseDto.getData();

            // 检查课程容量
            if (courseData.getEnrolled() >= courseData.getCapacity()) {
                log.warn("⚠️ 课程已满 - courseId: {}, capacity: {}, enrolled: {}",
                        courseId, courseData.getCapacity(), courseData.getEnrolled());
                throw new CourseFullException(
                        String.format("课程已满（容量: %d，已选: %d）",
                                courseData.getCapacity(), courseData.getEnrolled()));
            }

            log.info("✅ 课程验证成功 - courseId: {}, title: {}", courseId, courseData.getTitle());
            return courseDto;

        } catch (CourseNotFoundException | CourseFullException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ 调用课程服务失败", e);
            throw new CourseNotFoundException("课程服务调用失败，courseId: " + courseId);
        }
    }

    private CourseDto getCourseInfo(Long courseId) {
        try {
            log.info("🔍 使用 OpenFeign 获取课程信息 - courseId: {}", courseId);

            // 修改：Feign客户端现在直接返回CourseDto
            CourseDto courseDto = catalogClient.getCourse(courseId);

            if (courseDto == null || courseDto.getData() == null || courseDto.getData().getId() == null) {
                log.error("❌ 课程不存在 - courseId: {}", courseId);
                throw new CourseNotFoundException("课程不存在，courseId: " + courseId);
            }

            log.info("✅ 获取课程信息成功 - courseId: {}, title: {}", courseId, courseDto.getData().getTitle());
            return courseDto;

        } catch (Exception e) {
            log.error("❌ 调用课程服务失败", e);
            throw new CourseNotFoundException("课程服务调用失败，courseId: " + courseId);
        }
    }
    /**
     * 字符串转Long的辅助方法
     */
    private Long convertToLong(String value, String fieldName) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " 必须是数字: " + value);
        }
    }

    // ==================== 其他业务方法 ====================

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
        Long courseId = convertToLong(enrollment.getCourseId(), "课程ID");
        CourseDto courseDto = getCourseInfo(courseId);

        // 4. 更新选课状态
        enrollment.setStatus(EnrollmentStatus.DROPPED);
        Enrollment updated = enrollmentRepository.save(enrollment);

        // 5. 异步更新课程已选人数
        updateCourseEnrollmentCountAsync(courseId, courseDto.getData().getEnrolled() - 1);

        log.info("✅ 退课成功 - enrollmentId: {}", enrollmentId);
        return updated;
    }

    // 修改unenroll方法
    public void unenroll(Long enrollmentId) {
        log.info("🔙 退课操作 - enrollmentId: {}", enrollmentId);

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("选课记录", enrollmentId.toString()));

        // 获取课程信息
        Long courseId = convertToLong(enrollment.getCourseId(), "课程ID");
        CourseDto courseDto = getCourseInfo(courseId);

        // 删除选课记录
        enrollmentRepository.delete(enrollment);

        // 异步更新课程已选人数
        updateCourseEnrollmentCountAsync(courseId, courseDto.getData().getEnrolled() - 1);

        log.info("✅ 退课成功 - enrollmentId: {}", enrollmentId);
    }

    /**
     * 按用户和课程退课
     */
    public Enrollment dropByUserAndCourse(String userId, String courseId) {
        log.info("🔙 按用户和课程退课 - userId: {}, courseId: {}", userId, courseId);

        Optional<Enrollment> enrollmentOpt = enrollmentRepository
                .findByCourseIdAndUserIdAndStatus(courseId, userId, EnrollmentStatus.ACTIVE);

        Enrollment enrollment = enrollmentOpt.orElseThrow(() -> new EnrollmentNotFoundException(
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

    // ==================== 统计方法（保持不变） ====================

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
     * 检查重复选课
     */
    private void checkDuplicateEnrollment(String courseId, String userId) {
        Optional<Enrollment> existingEnrollment = enrollmentRepository
                .findByCourseIdAndUserIdAndStatus(courseId, userId, EnrollmentStatus.ACTIVE);

        if (existingEnrollment.isPresent()) {
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
     * 更新课程已选人数 - 使用新的 Feign 客户端
     */
    private void updateCourseEnrollmentCountAsync(Long courseId, int newCount) {
        new Thread(() -> {
            try {
                log.info("🔄 使用 OpenFeign 更新课程人数 - courseId: {}, newCount: {}", courseId, newCount);

                catalogClient.updateCourseEnrollment(courseId, newCount);

                log.info("✅ 课程已选人数更新成功 - courseId: {}, newCount: {}", courseId, newCount);

            } catch (Exception e) {
                log.error("❌ 异步更新课程人数失败 - courseId: {}, error: {}", courseId, e.getMessage());
            }
        }).start();
    }

    /**
     * 同步更新课程已选人数 - 使用新的 Feign 客户端
     */
    private void updateCourseEnrollmentCountSync(Long courseId, int newCount) {
        try {
            log.info("🔄 同步更新课程人数 - courseId: {}, newCount: {}", courseId, newCount);

            catalogClient.updateCourseEnrollment(courseId, newCount);

            log.info("✅ 课程已选人数更新成功 - courseId: {}, newCount: {}", courseId, newCount);

        } catch (Exception e) {
            log.error("❌ 更新课程人数失败", e);
            throw new ServiceCallException("更新课程人数失败: " + e.getMessage());
        }
    }

    // ==================== 测试服务发现方法 ====================

    @Autowired
    private Environment environment;

    public Map<String, Object> testServiceDiscovery() {
        log.info("开始服务发现测试...");
        Map<String, Object> result = new HashMap<>();

        result.put("feignEnabled", true);
        result.put("port", environment.getProperty("local.server.port"));
        result.put("service", "enrollment-service");

        // 测试 UserClient
        Map<String, Object> userClientResult = new HashMap<>();
        try {
            // 修改：Feign客户端现在直接返回StudentDto
            StudentDto studentDto = userClient.getStudent(1L);

            if (studentDto != null && studentDto.getData() != null) {
                userClientResult.put("success", true);
                userClientResult.put("status", "connected");
                userClientResult.put("data", studentDto.getData());
            } else {
                userClientResult.put("success", false);
                userClientResult.put("status", "failed");
                userClientResult.put("error", "用户数据解析失败");
            }

        } catch (Exception e) {
            userClientResult.put("success", false);
            userClientResult.put("status", "error");
            userClientResult.put("error", e.getMessage());
        }


        // 测试 CatalogClient
        // 测试 CatalogClient
        Map<String, Object> catalogClientResult = new HashMap<>();
        try {
            // 修改：Feign客户端现在直接返回CourseDto
            CourseDto courseDto = catalogClient.getCourse(1L);

            if (courseDto != null && courseDto.getData() != null) {
                catalogClientResult.put("success", true);
                catalogClientResult.put("status", "connected");
                catalogClientResult.put("data", courseDto.getData());
            } else {
                catalogClientResult.put("success", false);
                catalogClientResult.put("status", "failed");
                catalogClientResult.put("error", "课程数据解析失败");
            }

        } catch (Exception e) {
            catalogClientResult.put("success", false);
            catalogClientResult.put("status", "error");
            catalogClientResult.put("error", e.getMessage());
        }

        result.put("userClient", userClientResult);
        result.put("catalogClient", catalogClientResult);
        result.put("timestamp", System.currentTimeMillis());

        log.info("服务发现和Feign测试完成");
        return result;
    }


    // ==================== 测试方法 ====================

    public StudentDto testUserClient() {
        try {
            return userClient.getStudent(1L);
        } catch (Exception e) {
            log.error("测试用户服务失败", e);
            return null;
        }
    }

    public CourseDto testCatalogClient() {
        try {
            return catalogClient.getCourse(1L);
        } catch (Exception e) {
            log.error("测试课程服务失败", e);
            return null;
        }
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