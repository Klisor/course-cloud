package com.zjsu.nsq.enrollment.client;

import com.zjsu.nsq.enrollment.dto.CourseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CatalogClientFallback implements CatalogClient {

    private static final Logger log = LoggerFactory.getLogger(CatalogClientFallback.class);

    @Override
    public CourseDto getCourse(Long id) {
        // 重点：这里用ERROR级别，更容易在日志中看到
        log.error("🚨🚨🚨 CATALOG SERVICE FALLBACK TRIGGERED! 课程ID: {}", id);
        log.error("堆栈信息:", new RuntimeException("Fallback triggered"));

        // 创建降级数据，不要抛出异常！
        CourseDto courseDto = new CourseDto();
        courseDto.setCode(503);
        courseDto.setMessage("课程服务不可用（熔断降级）");

        if (courseDto.getData() == null) {
            CourseDto.Data data = new CourseDto.Data();
            data.setId(-1L);
            data.setCode("FALLBACK_COURSE");
            data.setTitle("【熔断降级】课程服务不可用");
            data.setDescription("服务暂时不可用，请稍后再试");
            courseDto.setData(data);
        } else {
            courseDto.getData().setTitle("【熔断降级】课程服务不可用");
        }

        return courseDto;
    }

    @Override
    public void updateCourseEnrollment(Long id, int count) {
        log.warn("CatalogClient fallback triggered for update enrollment, course: {}, count: {}", id, count);
        // 对于更新操作，只记录日志
    }
}