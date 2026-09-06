package com.syfe.finance.service;

import com.syfe.finance.dto.common.MessageResponse;
import com.syfe.finance.dto.transaction.CreateTransactionRequest;
import com.syfe.finance.dto.transaction.TransactionListResponse;
import com.syfe.finance.dto.transaction.TransactionResponse;
import com.syfe.finance.dto.transaction.UpdateTransactionRequest;
import com.syfe.finance.entity.Category;
import com.syfe.finance.entity.CategoryType;
import com.syfe.finance.entity.Transaction;
import com.syfe.finance.entity.User;
import com.syfe.finance.exception.BadRequestException;
import com.syfe.finance.exception.ResourceNotFoundException;
import com.syfe.finance.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private TransactionService transactionService;

    private User testUser;
    private Category salaryCat;
    private Category foodCat;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("user@example.com")
                .fullName("Test User")
                .build();

        salaryCat = Category.builder()
                .id(1L)
                .name("Salary")
                .type(CategoryType.INCOME)
                .isCustom(false)
                .build();

        foodCat = Category.builder()
                .id(2L)
                .name("Food")
                .type(CategoryType.EXPENSE)
                .isCustom(false)
                .build();
    }

    @Test
    @DisplayName("Should successfully create an income transaction with derived type")
    void createTransaction_Income_Success() {
        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .amount(new BigDecimal("50000.00"))
                .date(LocalDate.of(2024, 1, 15))
                .category("Salary")
                .description("January Salary")
                .build();

        when(categoryService.getAccessibleCategoryByName("Salary", testUser)).thenReturn(salaryCat);

        Transaction savedTx = Transaction.builder()
                .id(1L)
                .amount(new BigDecimal("50000.00"))
                .date(LocalDate.of(2024, 1, 15))
                .category(salaryCat)
                .type(CategoryType.INCOME)
                .user(testUser)
                .description("January Salary")
                .isDeleted(false)
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTx);

        TransactionResponse response = transactionService.createTransaction(request, testUser);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(new BigDecimal("50000.00"), response.getAmount());
        assertEquals(LocalDate.of(2024, 1, 15), response.getDate());
        assertEquals("Salary", response.getCategory());
        assertEquals(CategoryType.INCOME, response.getType());
        assertEquals("January Salary", response.getDescription());
    }

    @Test
    @DisplayName("Should successfully create an expense transaction with derived type")
    void createTransaction_Expense_Success() {
        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .amount(new BigDecimal("150.00"))
                .date(LocalDate.of(2024, 1, 16))
                .category("Food")
                .description("Dinner")
                .build();

        when(categoryService.getAccessibleCategoryByName("Food", testUser)).thenReturn(foodCat);

        Transaction savedTx = Transaction.builder()
                .id(2L)
                .amount(new BigDecimal("150.00"))
                .date(LocalDate.of(2024, 1, 16))
                .category(foodCat)
                .type(CategoryType.EXPENSE)
                .user(testUser)
                .description("Dinner")
                .isDeleted(false)
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTx);

        TransactionResponse response = transactionService.createTransaction(request, testUser);

        assertNotNull(response);
        assertEquals(CategoryType.EXPENSE, response.getType());
        assertEquals(new BigDecimal("150.00"), response.getAmount());
    }

    @Test
    @DisplayName("Should throw BadRequestException when date is in the future")
    void createTransaction_FutureDate_ThrowsBadRequest() {
        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .amount(new BigDecimal("500.00"))
                .date(LocalDate.now().plusDays(1))
                .category("Salary")
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> transactionService.createTransaction(request, testUser));
        assertEquals("Transaction date cannot be in the future", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw BadRequestException when amount is zero or negative")
    void createTransaction_InvalidAmount_ThrowsBadRequest() {
        CreateTransactionRequest zeroRequest = CreateTransactionRequest.builder()
                .amount(BigDecimal.ZERO)
                .date(LocalDate.now())
                .category("Salary")
                .build();

        assertThrows(BadRequestException.class,
                () -> transactionService.createTransaction(zeroRequest, testUser));

        CreateTransactionRequest negRequest = CreateTransactionRequest.builder()
                .amount(new BigDecimal("-50.00"))
                .date(LocalDate.now())
                .category("Salary")
                .build();

        assertThrows(BadRequestException.class,
                () -> transactionService.createTransaction(negRequest, testUser));
    }

    @Test
    @DisplayName("Should return transactions sorted by newest date first")
    void getTransactions_Success() {
        Transaction tx1 = Transaction.builder()
                .id(1L)
                .amount(new BigDecimal("100.00"))
                .date(LocalDate.of(2024, 1, 10))
                .category(foodCat)
                .type(CategoryType.EXPENSE)
                .user(testUser)
                .build();

        when(transactionRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(tx1));

        TransactionListResponse response = transactionService.getTransactions(null, null, null, null, testUser);

        assertNotNull(response);
        assertEquals(1, response.getTransactions().size());
        assertEquals(new BigDecimal("100.00"), response.getTransactions().get(0).getAmount());
    }

    @Test
    @DisplayName("Should throw BadRequestException when startDate is after endDate")
    void getTransactions_InvalidDateRange_ThrowsBadRequest() {
        LocalDate start = LocalDate.of(2024, 2, 1);
        LocalDate end = LocalDate.of(2024, 1, 1);

        assertThrows(BadRequestException.class,
                () -> transactionService.getTransactions(start, end, null, null, testUser));
    }

    @Test
    @DisplayName("Should successfully update permitted fields of transaction")
    void updateTransaction_Success() {
        Transaction existing = Transaction.builder()
                .id(1L)
                .amount(new BigDecimal("50000.00"))
                .date(LocalDate.of(2024, 1, 15))
                .category(salaryCat)
                .type(CategoryType.INCOME)
                .description("Old Salary")
                .user(testUser)
                .isDeleted(false)
                .build();

        when(transactionRepository.findByIdAndUserAndIsDeletedFalse(1L, testUser))
                .thenReturn(Optional.of(existing));

        UpdateTransactionRequest updateReq = UpdateTransactionRequest.builder()
                .amount(new BigDecimal("60000.00"))
                .description("Updated January Salary")
                .date(LocalDate.of(2024, 1, 15)) // same date allowed
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        TransactionResponse response = transactionService.updateTransaction(1L, updateReq, testUser);

        assertNotNull(response);
        assertEquals(new BigDecimal("60000.00"), response.getAmount());
        assertEquals("Updated January Salary", response.getDescription());
    }

    @Test
    @DisplayName("Should ignore date field if provided during update and retain original date")
    void updateTransaction_WithDate_IgnoresDate() {
        Transaction existing = Transaction.builder()
                .id(1L)
                .amount(new BigDecimal("50000.00"))
                .date(LocalDate.of(2024, 1, 15))
                .category(salaryCat)
                .type(CategoryType.INCOME)
                .user(testUser)
                .build();

        when(transactionRepository.findByIdAndUserAndIsDeletedFalse(1L, testUser))
                .thenReturn(Optional.of(existing));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateTransactionRequest updateReq = UpdateTransactionRequest.builder()
                .amount(new BigDecimal("55000.00"))
                .date(LocalDate.of(2024, 1, 20)) // different date!
                .build();

        TransactionResponse response = transactionService.updateTransaction(1L, updateReq, testUser);
        assertEquals(LocalDate.of(2024, 1, 15), response.getDate());
        assertEquals(new BigDecimal("55000.00"), response.getAmount());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating non-existent transaction")
    void updateTransaction_NotFound_ThrowsNotFound() {
        when(transactionRepository.findByIdAndUserAndIsDeletedFalse(99L, testUser))
                .thenReturn(Optional.empty());

        UpdateTransactionRequest updateReq = UpdateTransactionRequest.builder()
                .amount(new BigDecimal("100.00"))
                .build();

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.updateTransaction(99L, updateReq, testUser));
    }

    @Test
    @DisplayName("Should successfully soft-delete transaction")
    void deleteTransaction_Success() {
        Transaction existing = Transaction.builder()
                .id(1L)
                .amount(new BigDecimal("50000.00"))
                .date(LocalDate.of(2024, 1, 15))
                .category(salaryCat)
                .user(testUser)
                .isDeleted(false)
                .build();

        when(transactionRepository.findByIdAndUserAndIsDeletedFalse(1L, testUser))
                .thenReturn(Optional.of(existing));

        MessageResponse response = transactionService.deleteTransaction(1L, testUser);

        assertNotNull(response);
        assertEquals("Transaction deleted successfully", response.getMessage());
        assertTrue(existing.isDeleted());
        verify(transactionRepository).save(existing);
    }
}
