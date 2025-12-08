package com.zjsu.nsq.enrollment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CourseDto {
    private Integer code;
    private String message;
    private Data data;

    // 内部数据类
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
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

    // Getter和Setter
    public Integer getCode() { return code; }
    public void setCode(Integer code) { this.code = code; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Data getData() { return data; }
    public void setData(Data data) { this.data = data; }

    public boolean isSuccess() {
        return code != null && code == 200;
    }
}