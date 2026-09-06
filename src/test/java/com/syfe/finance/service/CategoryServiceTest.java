package com.syfe.finance.service;

import com.syfe.finance.dto.category.CategoryListResponse;
import com.syfe.finance.dto.category.CategoryResponse;
import com.syfe.finance.dto.category.CreateCategoryRequest;
import com.syfe.finance.dto.common.MessageResponse;
import com.syfe.finance.entity.Category;
import com.syfe.finance.entity.CategoryType;
import com.syfe.finance.entity.User;
import com.syfe.finance.exception.BadRequestException;
import com.syfe.finance.exception.ConflictException;
import com.syfe.finance.exception.ForbiddenException;
import com.syfe.finance.exception.ResourceNotFoundException;
import com.syfe.finance.repository.CategoryRepository;
import com.syfe.finance.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private CategoryService categoryService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("user@example.com")
                .fullName("Test User")
                .build();
    }

    @Test
    @DisplayName("Should return all categories accessible by user")
    void getCategories_ReturnsAllAccessible() {
        Category salary = Category.builder().name("Salary").type(CategoryType.INCOME).isCustom(false).build();
        Category food = Category.builder().name("Food").type(CategoryType.EXPENSE).isCustom(false).build();
        Category freelance = Category.builder().name("Freelance").type(CategoryType.INCOME).isCustom(true).user(testUser).build();

        when(categoryRepository.findAllAccessibleByUserId(testUser.getId())).thenReturn(List.of(salary, food, freelance));

        CategoryListResponse response = categoryService.getCategories(testUser);

        assertNotNull(response);
        assertEquals(3, response.getCategories().size());
        assertEquals("Salary", response.getCategories().get(0).getName());
        assertFalse(response.getCategories().get(0).isCustom());
        assertTrue(response.getCategories().get(2).isCustom());
    }

    @Test
    @DisplayName("Should successfully create a new custom category")
    void createCustomCategory_Success() {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("Consulting")
                .type(CategoryType.INCOME)
                .build();

        when(categoryRepository.existsByNameIgnoreCaseAndIsCustomFalse("Consulting")).thenReturn(false);
        when(categoryRepository.existsByNameIgnoreCaseAndUserAndIsDeletedFalse("Consulting", testUser)).thenReturn(false);

        Category saved = Category.builder()
                .id(10L)
                .name("Consulting")
                .type(CategoryType.INCOME)
                .isCustom(true)
                .user(testUser)
                .build();

        when(categoryRepository.save(any(Category.class))).thenReturn(saved);

        CategoryResponse response = categoryService.createCustomCategory(request, testUser);

        assertNotNull(response);
        assertEquals("Consulting", response.getName());
        assertEquals(CategoryType.INCOME, response.getType());
        assertTrue(response.isCustom());
    }

    @Test
    @DisplayName("Should throw ConflictException when creating custom category that duplicates default category")
    void createCustomCategory_DuplicateWithDefault_ThrowsConflict() {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("Salary")
                .type(CategoryType.INCOME)
                .build();

        when(categoryRepository.existsByNameIgnoreCaseAndIsCustomFalse("Salary")).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> categoryService.createCustomCategory(request, testUser));
        assertTrue(ex.getMessage().contains("already exists as a default system category"));
    }

    @Test
    @DisplayName("Should throw ConflictException when creating custom category that duplicates existing user custom category")
    void createCustomCategory_DuplicateWithUserCustom_ThrowsConflict() {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("Freelance")
                .type(CategoryType.INCOME)
                .build();

        when(categoryRepository.existsByNameIgnoreCaseAndIsCustomFalse("Freelance")).thenReturn(false);
        when(categoryRepository.existsByNameIgnoreCaseAndUserAndIsDeletedFalse("Freelance", testUser)).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> categoryService.createCustomCategory(request, testUser));
        assertTrue(ex.getMessage().contains("already exists for this user"));
    }

    @Test
    @DisplayName("Should throw ForbiddenException when attempting to delete a default category")
    void deleteCategory_DefaultCategory_ThrowsForbidden() {
        when(categoryRepository.existsByNameIgnoreCaseAndIsCustomFalse("Food")).thenReturn(true);

        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> categoryService.deleteCategory("Food", testUser));
        assertEquals("Default categories cannot be deleted", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting non-existent category")
    void deleteCategory_NotFound_ThrowsNotFound() {
        when(categoryRepository.existsByNameIgnoreCaseAndIsCustomFalse("NonExistent")).thenReturn(false);
        when(categoryRepository.findByNameIgnoreCaseAndUserAndIsDeletedFalse("NonExistent", testUser))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.deleteCategory("NonExistent", testUser));
    }

    @Test
    @DisplayName("Should throw BadRequestException when deleting category referenced by active transactions")
    void deleteCategory_ReferencedByTransactions_ThrowsBadRequest() {
        Category customCat = Category.builder()
                .id(5L)
                .name("Gym")
                .type(CategoryType.EXPENSE)
                .isCustom(true)
                .user(testUser)
                .build();

        when(categoryRepository.existsByNameIgnoreCaseAndIsCustomFalse("Gym")).thenReturn(false);
        when(categoryRepository.findByNameIgnoreCaseAndUserAndIsDeletedFalse("Gym", testUser))
                .thenReturn(Optional.of(customCat));
        when(transactionRepository.existsByCategoryAndIsDeletedFalse(customCat)).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> categoryService.deleteCategory("Gym", testUser));
        assertTrue(ex.getMessage().contains("referenced by active transactions"));
    }

    @Test
    @DisplayName("Should successfully soft-delete an unused custom category")
    void deleteCategory_Success() {
        Category customCat = Category.builder()
                .id(5L)
                .name("Gym")
                .type(CategoryType.EXPENSE)
                .isCustom(true)
                .user(testUser)
                .isDeleted(false)
                .build();

        when(categoryRepository.existsByNameIgnoreCaseAndIsCustomFalse("Gym")).thenReturn(false);
        when(categoryRepository.findByNameIgnoreCaseAndUserAndIsDeletedFalse("Gym", testUser))
                .thenReturn(Optional.of(customCat));
        when(transactionRepository.existsByCategoryAndIsDeletedFalse(customCat)).thenReturn(false);

        MessageResponse response = categoryService.deleteCategory("Gym", testUser);

        assertNotNull(response);
        assertEquals("Category deleted successfully", response.getMessage());
        assertTrue(customCat.isDeleted());
        verify(categoryRepository).save(customCat);
    }
}
