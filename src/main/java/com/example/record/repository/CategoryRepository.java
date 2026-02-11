package com.example.record.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;
import com.example.record.model.Category;
import com.example.record.model.Product;
import com.example.record.repository.ProductRepository;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class CategoryRepository {

    private static final String DIR = "data/categories";
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicLong idGen = new AtomicLong(1);

    public CategoryRepository() {
        new File(DIR).mkdirs();
        initId();
    }

    private void initId() {
        File[] files = new File(DIR).listFiles();
        long max = 0;
        if (files != null) {
            for (File f : files) {
                try {
                    max = Math.max(max, Long.parseLong(f.getName().replace(".json", "")));
                } catch (Exception ignored) {
                }
            }
        }
        idGen.set(max + 1);
    }

    public Category save(String name, String imagePath) {
        Category c = new Category();
        c.setId(idGen.getAndIncrement());
        c.setName(name);
        c.setImagePath(imagePath);
        c.setTotalCount(0L);
        write(c);
        return c;
    }

    public void update(Category c) {
        write(c);
    }

    public Category findById(Long id) {
        File f = new File(DIR, id + ".json");
        try {
            return f.exists() ? mapper.readValue(f, Category.class) : null;
        } catch (Exception e) {
            return null;
        }
    }

    public List<Category> findAll() {
        List<Category> list = new ArrayList<>();
        File[] files = new File(DIR).listFiles();
        if (files == null)
            return list;
        for (File f : files) {
            try {
                list.add(mapper.readValue(f, Category.class));
            } catch (Exception ignored) {
            }
        }
        return list;
    }

    private void write(Category c) {
        try {
            mapper.writeValue(new File(DIR, c.getId() + ".json"), c);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean delete(Long id) {
        try {
            // 先读取分类信息，获取图片路径
            Category category = findById(id);
            if (category == null) {
                return false; // 分类不存在
            }
            
            // 删除对应的JSON文件
            File jsonFile = new File(DIR, id + ".json");
            if (jsonFile.exists()) {
                boolean jsonDeleted = jsonFile.delete();
                System.out.println("删除分类JSON文件: " + jsonFile.getPath() + ", 结果: " + jsonDeleted);
            }
            
            // 尝试删除对应的图片文件
            deleteCategoryImage(category);
            
            return true;
        } catch (Exception e) {
            System.err.println("删除分类失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private void deleteCategoryImage(Category category) {
        if (category.getImagePath() != null && !category.getImagePath().isEmpty()) {
            try {
                // 从完整的URL中提取相对路径
                String imagePath = category.getImagePath();
                
                // 如果是URL，尝试提取本地文件路径
                if (imagePath.startsWith("http")) {
                    // 从URL中提取文件名部分
                    // 例如: http://localhost:8080/uploads/category/1234567890_image.jpg
                    // 提取为: category/1234567890_image.jpg
                    String[] parts = imagePath.split("/uploads/");
                    if (parts.length > 1) {
                        String relativePath = parts[1];
                        String projectRoot = System.getProperty("user.dir");
                        String fullPath = projectRoot + File.separator + "uploads" + File.separator + relativePath;
                        deleteImageFile(fullPath);
                    }
                } else if (imagePath.startsWith("/uploads/")) {
                    // 相对路径的情况
                    String projectRoot = System.getProperty("user.dir");
                    String fullPath = projectRoot + imagePath.replace("/", File.separator);
                    deleteImageFile(fullPath);
                } else {
                    // 直接是文件路径的情况
                    deleteImageFile(imagePath);
                }
            } catch (Exception e) {
                System.err.println("删除分类图片失败: " + e.getMessage());
            }
        }
    }
    
    private void deleteImageFile(String filePath) {
        try {
            File imageFile = new File(filePath);
            if (imageFile.exists()) {
                boolean deleted = imageFile.delete();
                System.out.println("删除图片文件: " + filePath + ", 结果: " + deleted);
                
                // 尝试删除可能存在的空目录
                File parentDir = imageFile.getParentFile();
                if (parentDir != null && parentDir.isDirectory() && parentDir.listFiles().length == 0) {
                    boolean dirDeleted = parentDir.delete();
                    System.out.println("删除空目录: " + parentDir.getPath() + ", 结果: " + dirDeleted);
                }
            } else {
                System.out.println("图片文件不存在: " + filePath);
            }
        } catch (Exception e) {
            System.err.println("删除文件异常: " + e.getMessage());
        }
    }
    
    public void deleteAll() {
        try {
            // 获取所有分类
            List<Category> categories = findAll();
            
            // 删除每个分类
            for (Category category : categories) {
                // 先删除图片
                deleteCategoryImage(category);
                
                // 再删除JSON文件
                File jsonFile = new File(DIR, category.getId() + ".json");
                if (jsonFile.exists()) {
                    jsonFile.delete();
                }
            }
            
            // 重置ID生成器
            idGen.set(1);
            System.out.println("已删除全部分类和图片");
            
        } catch (Exception e) {
            System.err.println("删除全部分类失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}