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

    public ProductRepository() {
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
            if (c != 0) return c;
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
}