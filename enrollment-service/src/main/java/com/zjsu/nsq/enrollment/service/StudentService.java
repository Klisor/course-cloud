package com.zjsu.nsq.enrollment.service;

import com.zjsu.nsq.enrollment.model.Student;
import com.zjsu.nsq.enrollment.repository.EnrollmentRepository;
import com.zjsu.nsq.enrollment.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@Transactional
public class StudentService {
    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;

    // 邮箱格式正则表达式
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    public StudentService(StudentRepository studentRepository, EnrollmentRepository enrollmentRepository) {
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional(readOnly = true)
    public List<Student> findAll() {
        return studentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Student> findById(Long id) {
        return studentRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Student> findByStudentId(String studentId) {
        return studentRepository.findByStudentId(studentId);
    }

    @Transactional(readOnly = true)
    public Optional<Student> findByEmail(String email) {
        return studentRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public List<Student> findByMajor(String major) {
        return studentRepository.findByMajor(major);
    }

    @Transactional(readOnly = true)
    public List<Student> findByGrade(Integer grade) {
        return studentRepository.findByGrade(grade);
    }

    @Transactional(readOnly = true)
    public List<Student> findByNameContaining(String name) {
        return studentRepository.findByNameContainingIgnoreCase(name);
    }

    public Student create(Student student) {
        // 验证学号
        if (student.getStudentId() == null || student.getStudentId().trim().isEmpty()) {
            throw new InvalidStudentDataException("学号不能为空");
        }
        if (studentRepository.existsByStudentId(student.getStudentId())) {
            throw new StudentAlreadyExistsException("学号已存在: " + student.getStudentId());
        }

        // 验证姓名
        if (student.getName() == null || student.getName().trim().isEmpty()) {
            throw new InvalidStudentDataException("学生姓名不能为空");
        }

        // 验证邮箱格式和唯一性
        if (student.getEmail() != null && !student.getEmail().trim().isEmpty()) {
            if (!isValidEmail(student.getEmail())) {
                throw new InvalidStudentDataException("邮箱格式不正确: " + student.getEmail());
            }
            if (studentRepository.existsByEmail(student.getEmail())) {
                throw new StudentAlreadyExistsException("邮箱已存在: " + student.getEmail());
            }
        }

        return studentRepository.save(student);
    }

    public Student update(Long id, Student student) {
        Student existingStudent = studentRepository.findById(id)
                .orElseThrow(() -> new StudentNotFoundException("学生不存在，ID: " + id));

        // 验证学号唯一性（排除自己）
        if (!existingStudent.getStudentId().equals(student.getStudentId())) {
            studentRepository.findByStudentId(student.getStudentId())
                    .ifPresent(s -> {
                        if (!s.getId().equals(id)) {
                            throw new StudentAlreadyExistsException("学号已被其他学生使用: " + student.getStudentId());
                        }
                    });
        }

        // 验证邮箱唯一性（排除自己）
        if (student.getEmail() != null && !student.getEmail().trim().isEmpty()) {
            if (!isValidEmail(student.getEmail())) {
                throw new InvalidStudentDataException("邮箱格式不正确: " + student.getEmail());
            }
            if (!student.getEmail().equals(existingStudent.getEmail())) {
                studentRepository.findByEmail(student.getEmail())
                        .ifPresent(s -> {
                            if (!s.getId().equals(id)) {
                                throw new StudentAlreadyExistsException("邮箱已被其他学生使用: " + student.getEmail());
                            }
                        });
            }
        }

        // 更新字段
        existingStudent.setStudentId(student.getStudentId());
        existingStudent.setName(student.getName());
        existingStudent.setMajor(student.getMajor());
        existingStudent.setGrade(student.getGrade());
        existingStudent.setEmail(student.getEmail());

        return studentRepository.save(existingStudent);
    }

    public void delete(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new StudentNotFoundException("学生不存在，ID: " + id));

        // 检查学生是否有活跃的选课记录
        long activeEnrollments = enrollmentRepository.countByStudentAndStatus(student, com.zjsu.nsq.enrollment.model.EnrollmentStatus.ACTIVE);
        if (activeEnrollments > 0) {
            throw new StudentHasActiveEnrollmentsException("该学生存在活跃的选课记录，无法删除");
        }

        studentRepository.deleteById(id);
    }

    // 邮箱格式验证
    private boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

    // 自定义异常
    public static class StudentNotFoundException extends RuntimeException {
        public StudentNotFoundException(String message) {
            super(message);
        }
    }

    public static class StudentAlreadyExistsException extends RuntimeException {
        public StudentAlreadyExistsException(String message) {
            super(message);
        }
    }

    public static class InvalidStudentDataException extends RuntimeException {
        public InvalidStudentDataException(String message) {
            super(message);
        }
    }

    public static class StudentHasActiveEnrollmentsException extends RuntimeException {
        public StudentHasActiveEnrollmentsException(String message) {
            super(message);
        }
    }
}