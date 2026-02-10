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
}