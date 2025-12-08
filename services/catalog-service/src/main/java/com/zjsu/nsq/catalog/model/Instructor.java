package com.zjsu.nsq.catalog.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class Instructor {
    private String name;
    private String email;

    // 默认构造函数
    public Instructor() {}

    public Instructor(String name, String email) {
        this.name = name;
        this.email = email;
    }

    // Getter和Setter
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}