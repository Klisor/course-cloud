-- 课程管理系统数据库表结构
-- 适用于 H2 (开发环境) 和 MySQL (生产环境)

-- 创建学生表
CREATE TABLE IF NOT EXISTS students (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    major VARCHAR(50),
    grade INT,
    email VARCHAR(100) UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 索引
    INDEX idx_student_major (major),
    INDEX idx_student_grade (grade)
);

-- 创建课程表
CREATE TABLE IF NOT EXISTS courses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    title VARCHAR(100) NOT NULL,
    instructor_name VARCHAR(100),
    instructor_email VARCHAR(100),
    schedule_day VARCHAR(20),
    schedule_start_time VARCHAR(10),
    schedule_end_time VARCHAR(10),
    expected_attendance INT,
    capacity INT NOT NULL DEFAULT 0,
    enrolled INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 索引
    INDEX idx_course_instructor (instructor_name),
    INDEX idx_course_title (title)
);

-- 创建选课表
CREATE TABLE IF NOT EXISTS enrollments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    enrolled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- 唯一约束，防止重复选课
    UNIQUE KEY uk_course_student (course_id, student_id),

    -- 外键约束
    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,

    -- 索引
    INDEX idx_enrollment_student (student_id),
    INDEX idx_enrollment_course (course_id),
    INDEX idx_enrollment_status (status)
);