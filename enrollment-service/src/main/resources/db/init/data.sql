-- 课程管理系统测试数据
-- 清空现有数据（可选）
-- DELETE FROM enrollments;
-- DELETE FROM courses;
-- DELETE FROM students;

-- 插入学生数据 (匹配实体类字段)
INSERT INTO students (student_id, name, major, grade, email, created_at) VALUES
('2023001001', '张三', '计算机科学', 3, 'zhangsan@zjsu.edu.cn', NOW()),
('2023001002', '李四', '计算机科学', 3, 'lisi@zjsu.edu.cn', NOW()),
('2023001003', '王五', '计算机科学', 3, 'wangwu@zjsu.edu.cn', NOW()),
('2023002001', '赵六', '软件工程', 2, 'zhaoliu@zjsu.edu.cn', NOW()),
('2023002002', '钱七', '软件工程', 2, 'qianqi@zjsu.edu.cn', NOW()),
('2023003001', '孙八', '数据科学', 4, 'sunba@zjsu.edu.cn', NOW()),
('2023003002', '周九', '数据科学', 4, 'zhoujiu@zjsu.edu.cn', NOW()),
('2023004001', '吴十', '人工智能', 1, 'wushi@zjsu.edu.cn', NOW());

-- 插入课程数据 (匹配实体类字段)
INSERT INTO courses (code, title, instructor_name, instructor_email, schedule_day, schedule_start_time, schedule_end_time, capacity, enrolled, created_at) VALUES
('CS101', 'Java程序设计', '张教授', 'zhang@zjsu.edu.cn', 'MONDAY', '08:00', '09:40', 50, 3, NOW()),
('CS102', '数据库原理', '李教授', 'li@zjsu.edu.cn', 'TUESDAY', '10:00', '11:40', 40, 2, NOW()),
('CS201', 'Web开发技术', '王教授', 'wang@zjsu.edu.cn', 'WEDNESDAY', '14:00', '15:40', 45, 2, NOW()),
('CS301', '人工智能基础', '赵教授', 'zhao@zjsu.edu.cn', 'THURSDAY', '16:00', '17:40', 35, 2, NOW()),
('MATH101', '高等数学', '钱教授', 'qian@zjsu.edu.cn', 'FRIDAY', '08:00', '09:40', 60, 1, NOW()),
('CS401', '分布式系统', '周教授', 'zhou@zjsu.edu.cn', 'MONDAY', '14:00', '15:40', 30, 2, NOW());

-- 插入选课数据
INSERT INTO enrollments (course_id, student_id, status, enrolled_at) VALUES
(1, 1, 'ACTIVE', NOW()),
(1, 2, 'ACTIVE', NOW()),
(1, 3, 'ACTIVE', NOW()),
(2, 1, 'ACTIVE', NOW()),
(2, 4, 'ACTIVE', NOW()),
(3, 2, 'ACTIVE', NOW()),
(3, 5, 'ACTIVE', NOW()),
(4, 6, 'ACTIVE', NOW()),
(4, 7, 'ACTIVE', NOW()),
(5, 8, 'ACTIVE', NOW()),
(6, 3, 'ACTIVE', NOW()),
(6, 7, 'ACTIVE', NOW());

-- 输出数据初始化完成信息
SELECT '测试数据初始化完成' AS message;
SELECT
    (SELECT COUNT(*) FROM students) AS student_count,
    (SELECT COUNT(*) FROM courses) AS course_count,
    (SELECT COUNT(*) FROM enrollments) AS enrollment_count;