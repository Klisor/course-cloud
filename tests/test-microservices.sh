#!/bin/bash
# test-microservices.sh
set -e

echo "=== 测试微服务接口 ==="

# -------------------------
# 1. 测试课程目录服务 - 创建课程
# -------------------------
echo -e "\n1. 创建课程"
COURSE_JSON='{
  "code": "CS101",
  "title": "计算机科学导论",
  "instructor": {
    "id": "T001",
    "name": "张教授",
    "email": "zhang@example.edu.cn"
  },
  "schedule": {
    "dayOfWeek": "MONDAY",
    "startTime": "08:00",
    "endTime": "10:00",
    "expectedAttendance": 50
  },
  "capacity": 60,
  "enrolled": 0
}'
curl -s -X POST http://localhost:8081/api/courses \
  -H "Content-Type: application/json" \
  -d "$COURSE_JSON" | jq

# -------------------------
# 2. 获取所有课程
# -------------------------
echo -e "\n2. 获取所有课程"
curl -s http://localhost:8081/api/courses | jq

# -------------------------
# 3. 测试学生服务 - 创建学生
# -------------------------
echo -e "\n3. 创建学生"
STUDENT_JSON='{
  "studentId": "2024001",
  "name": "张三",
  "major": "计算机科学与技术",
  "grade": 2024,
  "email": "zhangsan@example.edu.cn"
}'
curl -s -X POST http://localhost:8083/api/students \
  -H "Content-Type: application/json" \
  -d "$STUDENT_JSON" | jq

# -------------------------
# 4. 获取所有学生
# -------------------------
echo -e "\n4. 获取所有学生"
curl -s http://localhost:8083/api/students | jq

# -------------------------
# 5. 测试选课
# -------------------------
echo -e "\n5. 学生选课"
COURSE_ID=$(curl -s http://localhost:8081/api/courses | jq -r '.data[0].id')
curl -s -X POST http://localhost:8082/api/enrollments \
  -H "Content-Type: application/json" \
  -d "{
    \"courseId\": \"$COURSE_ID\",
    \"userId\": \"2024001\"
  }" | jq

# -------------------------
# 6. 查询选课记录
# -------------------------
echo -e "\n6. 查询选课记录"
curl -s http://localhost:8082/api/enrollments | jq

# -------------------------
# 7. 测试重复选课
# -------------------------
echo -e "\n7. 测试重复选课"
curl -s -X POST http://localhost:8082/api/enrollments \
  -H "Content-Type: application/json" \
  -d "{
    \"courseId\": \"$COURSE_ID\",
    \"userId\": \"2024001\"
  }" | jq

# -------------------------
# 8. 测试课程不存在
# -------------------------
echo -e "\n8. 测试选课失败（课程不存在）"
curl -s -X POST http://localhost:8082/api/enrollments \
  -H "Content-Type: application/json" \
  -d '{
    "courseId": "999999",
    "userId": "2024001"
  }' | jq

# -------------------------
# 9. 测试退课
# -------------------------
echo -e "\n9. 测试退课"
ENROLL_ID=$(curl -s http://localhost:8082/api/enrollments | jq -r '.data[0].id')
curl -s -X PUT http://localhost:8082/api/enrollments/$ENROLL_ID/drop | jq

# -------------------------
# 10. 查询选课记录（退课后）
# -------------------------
echo -e "\n10. 查询选课记录（退课后）"
curl -s http://localhost:8082/api/enrollments | jq

echo -e "\n=== 测试完成 ==="
