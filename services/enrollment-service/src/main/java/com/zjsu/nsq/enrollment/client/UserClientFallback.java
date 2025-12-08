package com.zjsu.nsq.enrollment.client;

import com.zjsu.nsq.enrollment.dto.StudentDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class UserClientFallback implements UserClient {

    // 重点：确保这里有Logger定义
    private static final Logger log = LoggerFactory.getLogger(UserClientFallback.class);

    @Override
    public StudentDto getStudent(Long id) {
        System.err.println("\n\n");
        System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        System.err.println("!!! 🚨🚨🚨 作业熔断测试：UserClientFallback被调用！ 🚨🚨🚨 !!!");
        System.err.println("!!! 学生ID: " + id);
        System.err.println("!!! 时间: " + java.time.LocalDateTime.now());
        System.err.println("!!! 请截图此日志作为作业提交证据！");
        System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        System.err.println("\n");

        // 同时记录到日志文件
        log.error("\n\n");
        log.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        log.error("!!! 🚨🚨🚨 作业熔断测试：UserClientFallback被调用！ 🚨🚨🚨 !!!");
        log.error("!!! 学生ID: {}", id);
        log.error("!!! 时间: {}", java.time.LocalDateTime.now());
        log.error("!!! 请截图此日志作为作业提交证据！");
        log.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        log.error("\n");

        // 创建明显的降级响应
        StudentDto studentDto = new StudentDto();
        studentDto.setCode(503);
        studentDto.setMessage("【作业熔断降级】用户服务不可用");

        StudentDto.Data data = new StudentDto.Data();
        data.setId(-1L);
        data.setName("【作业测试】熔断降级用户");
        data.setUserId("FALLBACK_TEST_" + System.currentTimeMillis());

        studentDto.setData(data);

        return studentDto;
    }
}