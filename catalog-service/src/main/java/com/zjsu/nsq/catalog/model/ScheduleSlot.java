package com.zjsu.nsq.catalog.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class ScheduleSlot {
    private String dayOfWeek;
    private String startTime;
    private String endTime;
    private Integer expectedAttendance;

    // 默认构造函数
    public ScheduleSlot() {}

    public ScheduleSlot(String dayOfWeek, String startTime, String endTime, Integer expectedAttendance) {
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.expectedAttendance = expectedAttendance;
    }

    // Getter和Setter
    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public Integer getExpectedAttendance() { return expectedAttendance; }
    public void setExpectedAttendance(Integer expectedAttendance) { this.expectedAttendance = expectedAttendance; }
}