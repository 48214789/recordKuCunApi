package com.example.record.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;
import com.example.record.model.Category;
import com.example.record.model.Product;
import com.example.record.repository.CategoryRepository;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class ProductRepository {

    private static final String ROOT = "data/products";
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicLong idGen = new AtomicLong(1000);

    private final CategoryRepository categoryRepository;

    public ProductRepository(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
        new File(ROOT).mkdirs();
        initId();
    }

    private void initId() {
        File root = new File(ROOT);
        File[] dirs = root.listFiles(File::isDirectory);
        long max = 0;
        if (dirs != null) {
            for (File dir : dirs) {
                File[] fs = dir.listFiles();
                if (fs == null)
                    continue;
                for (File f : fs) {
                    try {
                        max = Math.max(max, Long.parseLong(f.getName().replace(".json", "")));
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        idGen.set(max + 1);
    }

    public Product save(Long categoryId, String name, String imagePath, Long stock) {
        Product p = new Product();
        p.setId(idGen.getAndIncrement());
        p.setCategoryId(categoryId);
        p.setName(name);
        p.setImagePath(imagePath); // 保存完整URL或相对路径
        p.setStock(stock);
        write(p);
        return p;
    }

    public Product findById(Long pid) {
        File[] dirs = new File(ROOT).listFiles(File::isDirectory);
        if (dirs == null)
            return null;
        for (File d : dirs) {
            File f = new File(d, pid + ".json");
            if (f.exists())
                try {
                    return mapper.readValue(f, Product.class);
                } catch (Exception ignored) {
                }
        }
        return null;
    }

    public List<Product> findByCategory(Long cid) {
        List<Product> list = new ArrayList<>();
        File dir = new File(ROOT, "category_" + cid);
        File[] files = dir.listFiles();
        if (files == null)
            return list;
        for (File f : files) {
            try {
                list.add(mapper.readValue(f, Product.class));
            } catch (Exception ignored) {
            }
        }
        return list;
    }

    public List<Product> findAll() {
        List<Product> list = new ArrayList<>();

        File root = new File(ROOT);
        File[] dirs = root.listFiles(File::isDirectory);
        if (dirs == null)
            return list;

        for (File dir : dirs) {
            File[] files = dir.listFiles();
            if (files == null)
                continue;

            for (File f : files) {
                try {
                    list.add(mapper.readValue(f, Product.class));
                } catch (Exception ignored) {
                }
            }
        }

        // 可选：按 categoryId + id 排序，方便前端联动
        list.sort((a, b) -> {
            int c = a.getCategoryId().compareTo(b.getCategoryId());
            if (c != 0)
                return c;
            return a.getId().compareTo(b.getId());
        });

        return list;
    }

    public void update(Product p) {
        write(p);
    }

    private void write(Product p) {
        File dir = new File(ROOT, "category_" + p.getCategoryId());
        dir.mkdirs();
        try {
            mapper.writeValue(new File(dir, p.getId() + ".json"), p);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean delete(Long id) {
        try {
            Product product = findById(id);
            if (product == null) {
                return false;
            }

            // 1. 删除JSON文件
            deleteProductJsonFile(product);

            // 2. 删除图片
            deleteProductImage(product);

            // 3. 更新分类库存
            updateCategoryStockAfterDelete(product);

            return true;

        } catch (Exception e) {
            System.err.println("删除商品失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void deleteProductJsonFile(Product product) {
        // 找到对应的JSON文件
        File dir = new File(ROOT, "category_" + product.getCategoryId());
        File jsonFile = new File(dir, product.getId() + ".json");

        if (jsonFile.exists()) {
            boolean jsonDeleted = jsonFile.delete();
            System.out.println("删除商品JSON文件: " + jsonFile.getPath() + ", 结果: " + jsonDeleted);

            // 如果目录为空，删除目录
            if (dir.exists() && dir.isDirectory() && dir.listFiles().length == 0) {
                boolean dirDeleted = dir.delete();
                System.out.println("删除空商品目录: " + dir.getPath() + ", 结果: " + dirDeleted);
            }
        }
    }

    private void deleteProductImage(Product product) {
        if (product.getImagePath() != null && !product.getImagePath().isEmpty()) {
            try {
                String imagePath = product.getImagePath();

                // 从URL或路径中提取本地文件路径
                if (imagePath.startsWith("http")) {
                    // 从URL中提取文件名
                    String[] parts = imagePath.split("/uploads/");
                    if (parts.length > 1) {
                        String relativePath = parts[1];
                        String projectRoot = System.getProperty("user.dir");
                        String fullPath = projectRoot + File.separator + "uploads" + File.separator + relativePath;
                        deleteImageFile(fullPath);
                    }
                } else if (imagePath.startsWith("/uploads/")) {
                    // 相对路径
                    String projectRoot = System.getProperty("user.dir");
                    String fullPath = projectRoot + imagePath.replace("/", File.separator);
                    deleteImageFile(fullPath);
                } else {
                    // 直接路径
                    deleteImageFile(imagePath);
                }
            } catch (Exception e) {
                System.err.println("删除商品图片失败: " + e.getMessage());
            }
        }
    }

    private void updateCategoryStockAfterDelete(Product product) {
        try {
            Category category = categoryRepository.findById(product.getCategoryId());
            if (category != null) {
                Long oldTotal = category.getTotalCount() != null ? category.getTotalCount() : 0L;

                Long newTotal = oldTotal - product.getStock();

                if (newTotal < 0) {
                    newTotal = 0L; // 防止负数
                }

                category.setTotalCount(newTotal);
                categoryRepository.update(category);

                System.out.println("分类库存更新: " + oldTotal + " -> " + newTotal);
            } else {
                System.out.println("未找到分类，无法更新库存");
            }
        } catch (Exception e) {
            System.err.println("更新分类库存失败: " + e.getMessage());
        }
    }

    public void deleteAll() {
        try {
            // 获取所有商品
            List<Product> products = findAll();

            // 删除每个商品的图片
            for (Product product : products) {
                deleteProductImage(product);
            }

            // 删除所有商品目录
            File rootDir = new File(ROOT);
            if (rootDir.exists() && rootDir.isDirectory()) {
                File[] categoryDirs = rootDir.listFiles(File::isDirectory);
                if (categoryDirs != null) {
                    for (File categoryDir : categoryDirs) {
                        deleteDirectory(categoryDir);
                    }
                }
            }

            // 重置ID生成器
            idGen.set(1000);
            System.out.println("已删除所有商品和图片");

        } catch (Exception e) {
            System.err.println("删除所有商品失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void deleteDirectory(File dir) {
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            dir.delete();
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
}