package com.syfe.finance.controller;

import com.syfe.finance.dto.category.CategoryListResponse;
import com.syfe.finance.dto.category.CategoryResponse;
import com.syfe.finance.dto.category.CreateCategoryRequest;
import com.syfe.finance.dto.common.MessageResponse;
import com.syfe.finance.entity.User;
import com.syfe.finance.security.SecurityUtils;
import com.syfe.finance.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Category Management", description = "Endpoints for managing default and custom categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @Operation(summary = "Get all categories", description = "Retrieves all default system categories and authenticated user's custom categories")
    public ResponseEntity<CategoryListResponse> getCategories() {
        User currentUser = securityUtils.getCurrentUser();
        CategoryListResponse response = categoryService.getCategories(currentUser);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Create custom category", description = "Creates a new custom category for the authenticated user")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        CategoryResponse response = categoryService.createCustomCategory(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{name}")
    @Operation(summary = "Delete custom category", description = "Deletes a custom category if not in use by any transactions")
    public ResponseEntity<MessageResponse> deleteCategory(@PathVariable String name) {
        User currentUser = securityUtils.getCurrentUser();
        MessageResponse response = categoryService.deleteCategory(name, currentUser);
        return ResponseEntity.ok(response);
    }
}
