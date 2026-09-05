package com.syfe.finance.config;

import com.syfe.finance.entity.Category;
import com.syfe.finance.entity.CategoryType;
import com.syfe.finance.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    private static final Map<String, CategoryType> DEFAULT_CATEGORIES = Map.of(
            "Salary", CategoryType.INCOME,
            "Food", CategoryType.EXPENSE,
            "Rent", CategoryType.EXPENSE,
            "Transportation", CategoryType.EXPENSE,
            "Entertainment", CategoryType.EXPENSE,
            "Healthcare", CategoryType.EXPENSE,
            "Utilities", CategoryType.EXPENSE
    );

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Initializing default system categories...");
        for (Map.Entry<String, CategoryType> entry : DEFAULT_CATEGORIES.entrySet()) {
            String name = entry.getKey();
            CategoryType type = entry.getValue();

            boolean exists = categoryRepository.existsByNameIgnoreCaseAndIsCustomFalse(name);
            if (!exists) {
                Category category = Category.builder()
                        .name(name)
                        .type(type)
                        .isCustom(false)
                        .user(null)
                        .isDeleted(false)
                        .build();
                categoryRepository.save(category);
                log.info("Created default category: {} ({})", name, type);
            }
        }
        log.info("Default categories initialization complete.");
    }
}
