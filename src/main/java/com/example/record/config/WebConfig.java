package com.example.record.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 获取项目根目录
        String projectRoot = System.getProperty("user.dir");
        
        // 配置/uploads/**映射到文件系统的uploads目录
        String uploadPath = "file:" + projectRoot + "/uploads/";
        
        System.out.println("配置静态资源映射，项目根目录: " + projectRoot);
        System.out.println("上传文件访问路径: " + uploadPath);
        
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath)
                .setCachePeriod(3600); // 缓存1小时
    }
}