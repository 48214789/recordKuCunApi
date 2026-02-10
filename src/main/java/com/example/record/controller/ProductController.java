package com.example.record.controller;

import com.example.record.common.ApiResult;
import com.example.record.model.Category;
import com.example.record.model.Product;
import com.example.record.repository.CategoryRepository;
import com.example.record.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    @Autowired
    private ProductRepository productRepo;

    @Autowired
    private CategoryRepository categoryRepo;

    @Value("${app.base-url:}")
    private String baseUrlFromConfig;

    @PostMapping("/create")
    public ApiResult<Product> create(
            @RequestParam Long categoryId,
            @RequestParam String name,
            @RequestParam Long stock,
            @RequestParam(required = false) MultipartFile image,
            HttpServletRequest request) {
        try {
            System.out.println("开始创建商品: " + name);
            System.out.println("分类ID: " + categoryId + ", 库存: " + stock);

            String imageUrl = "";
            if (image != null && !image.isEmpty()) {
                System.out.println("接收到商品图片文件: " + image.getOriginalFilename());
                System.out.println("文件大小: " + image.getSize() + " bytes");
                System.out.println("Content-Type: " + image.getContentType());

                // 获取项目根目录
                String projectRoot = System.getProperty("user.dir");
                System.out.println("项目根目录: " + projectRoot);

                // 创建上传目录
                String uploadDir = projectRoot + File.separator + "uploads" + File.separator + "product";
                File dir = new File(uploadDir);
                if (!dir.exists()) {
                    boolean created = dir.mkdirs();
                    System.out.println("创建商品上传目录: " + created + ", 路径: " + uploadDir);
                }

                // 生成安全的文件名
                String originalFilename = image.getOriginalFilename();
                String safeFilename;

                if (originalFilename != null && !originalFilename.trim().isEmpty()) {
                    safeFilename = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
                } else {
                    safeFilename = "product_" + System.currentTimeMillis() + ".jpg";
                }

                String finalFilename = System.currentTimeMillis() + "_" + safeFilename;
                String filePath = uploadDir + File.separator + finalFilename;
                System.out.println("商品图片保存路径: " + filePath);

                // 保存文件
                try {
                    Path targetPath = Paths.get(filePath);
                    Files.copy(image.getInputStream(), targetPath);
                    System.out.println("商品图片保存成功");
                } catch (Exception e) {
                    System.out.println("使用Files.copy保存失败: " + e.getMessage());

                    // 备选方案：使用transferTo
                    File targetFile = new File(filePath);
                    image.transferTo(targetFile);
                    System.out.println("使用transferTo保存成功");
                }

                // 获取服务器基础URL
                String baseUrl = getBaseUrl(request);
                System.out.println("商品图片基础URL: " + baseUrl);

                // 构建完整的图片访问URL
                imageUrl = baseUrl + "/uploads/product/" + finalFilename;
                System.out.println("商品图片访问URL: " + imageUrl);

                // 验证文件是否保存成功
                File savedFile = new File(filePath);
                if (savedFile.exists()) {
                    System.out.println("✅ 商品图片保存验证成功，大小: " + savedFile.length() + " bytes");
                } else {
                    System.out.println("❌ 商品图片保存失败!");
                    return ApiResult.error("商品图片保存失败");
                }
            } else {
                System.out.println("没有上传商品图片");
            }

            // 验证分类是否存在
            Category category = categoryRepo.findById(categoryId);
            if (category == null) {
                System.out.println("❌ 分类不存在，ID: " + categoryId);
                return ApiResult.error("分类不存在");
            }

            System.out.println("找到分类: " + category.getName());

            // 保存商品信息
            Product product = productRepo.save(categoryId, name, imageUrl, stock);
            System.out.println("商品创建成功，ID: " + product.getId());

            // 更新分类库存总数
            Long currentTotal = category.getTotalCount() != null ? category.getTotalCount() : 0L;
            category.setTotalCount(currentTotal + stock);
            categoryRepo.update(category);
            System.out.println("分类库存总数更新为: " + category.getTotalCount());

            return ApiResult.ok(product);

        } catch (Exception e) {
            System.err.println("创建商品失败: " + e.getMessage());
            e.printStackTrace();
            return ApiResult.error("创建失败: " + e.getMessage());
        }
    }

    @GetMapping("/all")
    public ApiResult<Object> all(HttpServletRequest request) {
        try {
            System.out.println("获取所有商品");
            var products = productRepo.findAll();

            // 确保商品图片URL是正确的
            String baseUrl = getBaseUrl(request);
            for (Product product : products) {
                updateImageUrl(product, baseUrl);
            }

            System.out.println("返回商品数量: " + products.size());
            return ApiResult.ok(products);
        } catch (Exception e) {
            System.err.println("获取所有商品失败: " + e.getMessage());
            return ApiResult.error("获取失败: " + e.getMessage());
        }
    }

    @GetMapping("/list/{cid}")
    public ApiResult<Object> list(@PathVariable Long cid, HttpServletRequest request) {
        try {
            System.out.println("获取分类商品，分类ID: " + cid);

            // 验证分类是否存在
            Category category = categoryRepo.findById(cid);
            if (category == null) {
                System.out.println("分类不存在: " + cid);
                return ApiResult.error("分类不存在");
            }

            var products = productRepo.findByCategory(cid);

            // 确保商品图片URL是正确的
            String baseUrl = getBaseUrl(request);
            for (Product product : products) {
                updateImageUrl(product, baseUrl);
            }

            System.out.println("返回商品数量: " + products.size());
            return ApiResult.ok(products);
        } catch (Exception e) {
            System.err.println("获取分类商品失败: " + e.getMessage());
            return ApiResult.error("获取失败: " + e.getMessage());
        }
    }

    @PostMapping("/in")
    public ApiResult<Product> in(
            @RequestParam Long productId,
            @RequestParam Long count,
            HttpServletRequest request) {
        try {
            System.out.println("商品入库，商品ID: " + productId + ", 数量: " + count);

            if (count <= 0) {
                return ApiResult.error("入库数量必须大于0");
            }

            Product product = productRepo.findById(productId);
            if (product == null) {
                System.out.println("商品不存在: " + productId);
                return ApiResult.error("商品不存在");
            }

            Long oldStock = product.getStock();
            product.setStock(oldStock + count);
            productRepo.update(product);
            System.out.println("商品库存更新: " + oldStock + " -> " + product.getStock());

            Category category = categoryRepo.findById(product.getCategoryId());
            if (category != null) {
                Long oldTotal = category.getTotalCount() != null ? category.getTotalCount() : 0L;
                category.setTotalCount(oldTotal + count);
                categoryRepo.update(category);
                System.out.println("分类总库存更新: " + oldTotal + " -> " + category.getTotalCount());
            }

            // 确保图片URL正确
            updateImageUrl(product, getBaseUrl(request));

            return ApiResult.ok(product);

        } catch (Exception e) {
            System.err.println("商品入库失败: " + e.getMessage());
            e.printStackTrace();
            return ApiResult.error("入库失败: " + e.getMessage());
        }
    }

    @PostMapping("/out")
    public ApiResult<Product> out(
            @RequestParam Long productId,
            @RequestParam Long count,
            HttpServletRequest request) {
        try {
            System.out.println("商品出库，商品ID: " + productId + ", 数量: " + count);

            if (count <= 0) {
                return ApiResult.error("出库数量必须大于0");
            }

            Product product = productRepo.findById(productId);
            if (product == null) {
                System.out.println("商品不存在: " + productId);
                return ApiResult.error("商品不存在");
            }

            if (product.getStock() < count) {
                System.out.println("库存不足，当前库存: " + product.getStock() + ", 出库数量: " + count);
                return ApiResult.error("库存不足");
            }

            Long oldStock = product.getStock();
            product.setStock(oldStock - count);
            productRepo.update(product);
            System.out.println("商品库存更新: " + oldStock + " -> " + product.getStock());

            Category category = categoryRepo.findById(product.getCategoryId());
            if (category != null) {
                Long oldTotal = category.getTotalCount() != null ? category.getTotalCount() : 0L;
                category.setTotalCount(oldTotal - count);
                categoryRepo.update(category);
                System.out.println("分类总库存更新: " + oldTotal + " -> " + category.getTotalCount());
            }

            // 确保图片URL正确
            updateImageUrl(product, getBaseUrl(request));

            return ApiResult.ok(product);

        } catch (Exception e) {
            System.err.println("商品出库失败: " + e.getMessage());
            e.printStackTrace();
            return ApiResult.error("出库失败: " + e.getMessage());
        }
    }

    @PostMapping("/set")
    public ApiResult<Product> setStock(
            @RequestParam Long productId,
            @RequestParam Long newStock,
            HttpServletRequest request) {
        try {
            System.out.println("设置商品库存，商品ID: " + productId + ", 新库存: " + newStock);

            if (newStock < 0) {
                System.out.println("❌ 库存数量不能为负数: " + newStock);
                return ApiResult.error("库存数量不能为负数");
            }

            Product product = productRepo.findById(productId);
            if (product == null) {
                System.out.println("❌ 商品不存在，ID: " + productId);
                return ApiResult.error("商品不存在");
            }

            System.out.println("找到商品: " + product.getName() +
                    ", 当前库存: " + product.getStock() +
                    ", 分类ID: " + product.getCategoryId());

            // 计算库存变化量
            Long oldStock = product.getStock();
            Long stockChange = newStock - oldStock;
            System.out.println("库存变化量: " + stockChange + " (新库存 " + newStock + " - 旧库存 " + oldStock + ")");

            // 更新商品库存
            product.setStock(newStock);
            // 添加这一行，将商品更新保存到文件
            productRepo.update(product);
            System.out.println("✅ 商品库存更新成功: " + oldStock + " -> " + newStock);

            // 更新分类总库存（如果有变化）
            if (stockChange != 0) {
                Category category = categoryRepo.findById(product.getCategoryId());
                if (category != null) {
                    Long oldTotal = category.getTotalCount() != null ? category.getTotalCount() : 0L;
                    Long newTotal = oldTotal + stockChange;

                    // 确保分类总库存不会为负数
                    if (newTotal < 0) {
                        System.out.println("⚠️ 分类总库存不能为负数，重置为0");
                        newTotal = 0L;
                    }

                    category.setTotalCount(newTotal);
                    categoryRepo.update(category);
                    System.out.println("✅ 分类总库存更新: " + oldTotal + " -> " + newTotal);
                } else {
                    System.out.println("⚠️ 未找到分类，ID: " + product.getCategoryId());
                }
            } else {
                System.out.println("库存无变化，无需更新分类总库存");
            }

            // 确保图片URL正确
            updateImageUrl(product, getBaseUrl(request));

            return ApiResult.ok(product);

        } catch (Exception e) {
            System.err.println("❌ 设置商品库存失败: " + e.getMessage());
            e.printStackTrace();
            return ApiResult.error("设置库存失败: " + e.getMessage());
        }
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

    // 辅助方法：更新商品图片URL
    private void updateImageUrl(Product product, String baseUrl) {
        String imagePath = product.getImagePath();
        if (imagePath != null && !imagePath.isEmpty()) {
            // 如果是以/uploads/开头的相对路径，则构建完整URL
            if (imagePath.startsWith("/uploads/")) {
                product.setImagePath(baseUrl + imagePath);
            }
            // 如果已经是完整URL，则不需要处理
        }
    }
}