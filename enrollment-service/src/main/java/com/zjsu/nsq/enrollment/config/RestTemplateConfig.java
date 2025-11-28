package com.zjsu.nsq.enrollment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration // 标识为配置类
public class RestTemplateConfig {
    // 手动创建 RestTemplate Bean，供 EnrollmentService 注入
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}