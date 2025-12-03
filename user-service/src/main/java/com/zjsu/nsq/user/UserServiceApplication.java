package com.zjsu.nsq.user;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.core.env.Environment;
import java.util.Arrays;

@SpringBootApplication
@EnableDiscoveryClient
public class UserServiceApplication {

    @Autowired
    private Environment env;

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

    @PostConstruct
    public void printDb() {
        System.out.println("Active profile(s): " + Arrays.toString(env.getActiveProfiles()));
        System.out.println("Using DB URL: " + env.getProperty("spring.datasource.url"));
    }
}
