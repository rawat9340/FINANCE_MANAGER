package com.syfe.finance.service;

import com.syfe.finance.dto.category.CategoryListResponse;
import com.syfe.finance.dto.category.CategoryResponse;
import com.syfe.finance.dto.category.CreateCategoryRequest;
import com.syfe.finance.dto.common.MessageResponse;
import com.syfe.finance.entity.Category;
import com.syfe.finance.entity.User;
import com.syfe.finance.exception.BadRequestException;
import com.syfe.finance.exception.ConflictException;
import com.syfe.finance.exception.ForbiddenException;
import com.syfe.finance.exception.ResourceNotFoundException;
import com.syfe.finance.repository.CategoryRepository;
import com.syfe.finance.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public CategoryListResponse getCategories(User user) {
        List<Category> categories = categoryRepository.findAllAccessibleByUser(user);
        List<CategoryResponse> categoryResponses = categories.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return CategoryListResponse.builder()
                .categories(categoryResponses)
                .build();
    }

    @Transactional
    public CategoryResponse createCustomCategory(CreateCategoryRequest request, User user) {
        String trimmedName = request.getName().trim();

        if (categoryRepository.existsByNameIgnoreCaseAndIsCustomFalse(trimmedName)) {
            throw new ConflictException("Category with name '" + trimmedName + "' already exists as a default system category");
        }

        if (categoryRepository.existsByNameIgnoreCaseAndUserAndIsDeletedFalse(trimmedName, user)) {
            throw new ConflictException("Custom category with name '" + trimmedName + "' already exists for this user");
        }

        Category category = Category.builder()
                .name(trimmedName)
                .type(request.getType())
                .isCustom(true)
                .user(user)
                .isDeleted(false)
                .build();

        Category saved = categoryRepository.save(category);
        log.info("Created custom category {} for user {}", saved.getName(), user.getUsername());
        return mapToResponse(saved);
    }

    @Transactional
    public MessageResponse deleteCategory(String name, User user) {
        String trimmedName = name.trim();

        // Check if category is a default system category
        if (categoryRepository.existsByNameIgnoreCaseAndIsCustomFalse(trimmedName)) {
            throw new ForbiddenException("Default categories cannot be deleted");
        }

        Category category = categoryRepository.findByNameIgnoreCaseAndUserAndIsDeletedFalse(trimmedName, user)
                .orElseThrow(() -> new ResourceNotFoundException("Category '" + trimmedName + "' not found"));

        // Check if referenced by active transactions
        boolean inUse = transactionRepository.existsByCategoryAndIsDeletedFalse(category);
        if (inUse) {
            throw new BadRequestException("Category '" + trimmedName + "' cannot be deleted because it is referenced by active transactions");
        }

        category.setDeleted(true);
        categoryRepository.save(category);
        log.info("Soft-deleted custom category {} for user {}", trimmedName, user.getUsername());

        return MessageResponse.builder()
                .message("Category deleted successfully")
                .build();
    }

    public Category getAccessibleCategoryByName(String name, User user) {
        return categoryRepository.findAccessibleByNameIgnoreCase(name.trim(), user)
                .orElseThrow(() -> new BadRequestException("Category '" + name + "' does not exist or is not accessible"));
    }

    public CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .name(category.getName())
                .type(category.getType())
                .isCustom(category.isCustom())
                .build();
    }
}
