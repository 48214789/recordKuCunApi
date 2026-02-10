package com.example.record.controller;

import com.example.record.common.ApiResult;
import com.example.record.model.Category;
import com.example.record.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/category")
public class CategoryController {

    @Autowired
    private CategoryRepository repo;

    @Autowired
    private ResourceLoader resourceLoader;
    
    // 添加默认值，避免在properties中未配置时报错
    @Value("${server.address:}")
    private String serverAddress;
    
    @Value("${server.port:8080}")
    private String serverPort;
    
    @Value("${app.base-url:}")
    private String baseUrlFromConfig;
    
    @PostMapping("/create")
    public ApiResult<Category> create(
            @RequestParam String name,
            @RequestParam(required = false) MultipartFile image,
            HttpServletRequest request) {
        try {
            System.out.println("开始创建分类: " + name);
            
            String imageUrl = "";
            if (image != null && !image.isEmpty()) {
                System.out.println("接收到图片文件: " + image.getOriginalFilename());
                
                // 获取项目根目录
                String projectRoot = System.getProperty("user.dir");
                System.out.println("项目根目录: " + projectRoot);
                
                // 创建上传目录
                String uploadDir = projectRoot + File.separator + "uploads" + File.separator + "category";
                File dir = new File(uploadDir);
                if (!dir.exists()) {
                    boolean created = dir.mkdirs();
                    System.out.println("创建上传目录: " + created + ", 路径: " + uploadDir);
                }
                
                // 生成安全的文件名
                String originalFilename = image.getOriginalFilename();
                String safeFilename;
                
                if (originalFilename != null && !originalFilename.trim().isEmpty()) {
                    safeFilename = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
                } else {
                    safeFilename = "category_" + System.currentTimeMillis() + ".jpg";
                }
                
                String finalFilename = System.currentTimeMillis() + "_" + safeFilename;
                String filePath = uploadDir + File.separator + finalFilename;
                System.out.println("文件保存路径: " + filePath);
                
                // 保存文件
                Path targetPath = Paths.get(filePath);
                Files.copy(image.getInputStream(), targetPath);
                System.out.println("文件保存成功");
                
                // 获取服务器基础URL
                String baseUrl;
                
                // 优先级：配置文件 > 从请求中构建
                if (baseUrlFromConfig != null && !baseUrlFromConfig.isEmpty()) {
                    baseUrl = baseUrlFromConfig;
                    System.out.println("使用配置文件中的baseUrl: " + baseUrl);
                } else {
                    // 从请求中构建基础URL
                    String scheme = request.getScheme(); // http 或 https
                    String serverName = request.getServerName();
                    int serverPort = request.getServerPort();
                    
                    // 构建基础URL
                    StringBuilder urlBuilder = new StringBuilder();
                    urlBuilder.append(scheme).append("://").append(serverName);
                    
                    // 如果是默认端口，可以省略
                    if (("http".equals(scheme) && serverPort != 80) || 
                        ("https".equals(scheme) && serverPort != 443)) {
                        urlBuilder.append(":").append(serverPort);
                    }
                    
                    baseUrl = urlBuilder.toString();
                    System.out.println("从请求构建baseUrl: " + baseUrl);
                }
                
                // 构建完整的图片访问URL
                imageUrl = baseUrl + "/uploads/category/" + finalFilename;
                System.out.println("图片访问URL: " + imageUrl);
                
                // 验证文件是否可以访问（可选）
                File savedFile = new File(filePath);
                if (savedFile.exists()) {
                    System.out.println("✅ 文件保存验证成功，大小: " + savedFile.length() + " bytes");
                }
            } else {
                System.out.println("没有上传图片");
            }
            
            // 保存分类信息
            Category category = repo.save(name, imageUrl);
            System.out.println("分类创建成功，ID: " + category.getId());
            return ApiResult.ok(category);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("创建失败: " + e.getMessage());
        }
    }

    @GetMapping("/list")
    public ApiResult<Object> list(HttpServletRequest request) {
        List<Category> categories = repo.findAll();
        
        // 确保图片URL是正确的（如果数据库中存储的是相对路径，则构建完整URL）
        String baseUrl = getBaseUrl(request);
        
        for (Category category : categories) {
            String imagePath = category.getImagePath();
            if (imagePath != null && !imagePath.isEmpty()) {
                // 如果是以/uploads/开头的相对路径，则构建完整URL
                if (imagePath.startsWith("/uploads/")) {
                    category.setImagePath(baseUrl + imagePath);
                }
                // 注意：如果imagePath已经是完整URL（例如在create时已构建），则不需要处理
            }
        }
        
        return ApiResult.ok(categories);
    }
    
    // 辅助方法：获取基础URL
    private String getBaseUrl(HttpServletRequest request) {
        if (baseUrlFromConfig != null && !baseUrlFromConfig.isEmpty()) {
            return baseUrlFromConfig;
        }
        
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(scheme).append("://").append(serverName);
        
        if (("http".equals(scheme) && serverPort != 80) || 
            ("https".equals(scheme) && serverPort != 443)) {
            urlBuilder.append(":").append(serverPort);
        }
        
        return urlBuilder.toString();
    }
}