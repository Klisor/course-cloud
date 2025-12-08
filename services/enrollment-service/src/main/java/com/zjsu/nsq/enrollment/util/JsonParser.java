// JsonParser.java
package com.zjsu.nsq.enrollment.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonParser {
    private static final Logger log = LoggerFactory.getLogger(JsonParser.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 解析User Service响应
     */
    public static StudentData parseUserResponse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            Integer code = root.get("code").asInt();
            String message = root.get("message").asText();
            JsonNode dataNode = root.get("data");

            if (code != 200 || dataNode == null || dataNode.isNull()) {
                return null;
            }

            StudentData studentData = new StudentData();
            studentData.setId(dataNode.get("id").asLong());
            studentData.setUserId(dataNode.get("userId").asText());
            studentData.setName(dataNode.get("name").asText());
            studentData.setRole(dataNode.get("role").asText());
            studentData.setMajor(dataNode.get("major").asText());
            studentData.setGrade(dataNode.get("grade").asInt());
            studentData.setEmail(dataNode.get("email").asText());
            studentData.setCreatedAt(dataNode.get("createdAt").asText());

            return studentData;

        } catch (Exception e) {
            log.error("解析User响应失败: {}", json, e);
            return null;
        }
    }

    /**
     * 解析Catalog Service响应
     */
    public static CourseData parseCourseResponse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            Integer code = root.get("code").asInt();
            String message = root.get("message").asText();
            JsonNode dataNode = root.get("data");

            if (code != 200 || dataNode == null || dataNode.isNull()) {
                return null;
            }

            CourseData courseData = new CourseData();
            courseData.setId(dataNode.get("id").asLong());
            courseData.setCode(dataNode.get("code").asText());
            courseData.setTitle(dataNode.get("title").asText());
            courseData.setDescription(dataNode.has("description") ? dataNode.get("description").asText() : null);
            courseData.setInstructorName(dataNode.has("instructorName") ? dataNode.get("instructorName").asText() : null);
            courseData.setInstructorEmail(dataNode.has("instructorEmail") ? dataNode.get("instructorEmail").asText() : null);
            courseData.setScheduleDay(dataNode.has("scheduleDay") ? dataNode.get("scheduleDay").asText() : null);
            courseData.setScheduleStartTime(dataNode.has("scheduleStartTime") ? dataNode.get("scheduleStartTime").asText() : null);
            courseData.setScheduleEndTime(dataNode.has("scheduleEndTime") ? dataNode.get("scheduleEndTime").asText() : null);
            courseData.setCapacity(dataNode.get("capacity").asInt());
            courseData.setEnrolled(dataNode.get("enrolled").asInt());
            courseData.setCreatedAt(dataNode.get("createdAt").asText());

            return courseData;

        } catch (Exception e) {
            log.error("解析Course响应失败: {}", json, e);
            return null;
        }
    }

    // 简单的数据类
    public static class StudentData {
        private Long id;
        private String userId;
        private String name;
        private String role;
        private String major;
        private Integer grade;
        private String email;
        private String createdAt;

        // Getter和Setter
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public String getMajor() { return major; }
        public void setMajor(String major) { this.major = major; }

        public Integer getGrade() { return grade; }
        public void setGrade(Integer grade) { this.grade = grade; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }

    public static class CourseData {
        private Long id;
        private String code;
        private String title;
        private String description;
        private String instructorName;
        private String instructorEmail;
        private String scheduleDay;
        private String scheduleStartTime;
        private String scheduleEndTime;
        private Integer capacity;
        private Integer enrolled;
        private String createdAt;

        // Getter和Setter
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getInstructorName() { return instructorName; }
        public void setInstructorName(String instructorName) { this.instructorName = instructorName; }

        public String getInstructorEmail() { return instructorEmail; }
        public void setInstructorEmail(String instructorEmail) { this.instructorEmail = instructorEmail; }

        public String getScheduleDay() { return scheduleDay; }
        public void setScheduleDay(String scheduleDay) { this.scheduleDay = scheduleDay; }

        public String getScheduleStartTime() { return scheduleStartTime; }
        public void setScheduleStartTime(String scheduleStartTime) { this.scheduleStartTime = scheduleStartTime; }

        public String getScheduleEndTime() { return scheduleEndTime; }
        public void setScheduleEndTime(String scheduleEndTime) { this.scheduleEndTime = scheduleEndTime; }

        public Integer getCapacity() { return capacity; }
        public void setCapacity(Integer capacity) { this.capacity = capacity; }

        public Integer getEnrolled() { return enrolled; }
        public void setEnrolled(Integer enrolled) { this.enrolled = enrolled; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }
}