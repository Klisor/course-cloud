package com.zjsu.nsq.enrollment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StudentDto {
    private Integer code;
    private String message;
    private Data data;

    // 内部数据类
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
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