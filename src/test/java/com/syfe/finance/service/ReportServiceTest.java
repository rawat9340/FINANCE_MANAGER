package com.syfe.finance.service;

import com.syfe.finance.dto.report.MonthlyReportResponse;
import com.syfe.finance.dto.report.YearlyReportResponse;
import com.syfe.finance.entity.Category;
import com.syfe.finance.entity.CategoryType;
import com.syfe.finance.entity.Transaction;
import com.syfe.finance.entity.User;
import com.syfe.finance.exception.BadRequestException;
import com.syfe.finance.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private ReportService reportService;

    private User testUser;
    private Category salaryCat;
    private Category freelanceCat;
    private Category foodCat;
    private Category rentCat;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).username("user@example.com").build();
        salaryCat = Category.builder().name("Salary").type(CategoryType.INCOME).build();
        freelanceCat = Category.builder().name("Freelance").type(CategoryType.INCOME).build();
        foodCat = Category.builder().name("Food").type(CategoryType.EXPENSE).build();
        rentCat = Category.builder().name("Rent").type(CategoryType.EXPENSE).build();
    }

    @Test
    @DisplayName("Should successfully generate monthly report with category groupings and net savings")
    void getMonthlyReport_Success() {
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);

        Transaction tx1 = Transaction.builder()
                .amount(new BigDecimal("3000.00"))
                .category(salaryCat)
                .type(CategoryType.INCOME)
                .build();

        Transaction tx2 = Transaction.builder()
                .amount(new BigDecimal("500.00"))
                .category(freelanceCat)
                .type(CategoryType.INCOME)
                .build();

        Transaction tx3 = Transaction.builder()
                .amount(new BigDecimal("400.00"))
                .category(foodCat)
                .type(CategoryType.EXPENSE)
                .build();

        Transaction tx4 = Transaction.builder()
                .amount(new BigDecimal("1200.00"))
                .category(rentCat)
                .type(CategoryType.EXPENSE)
                .build();

        when(transactionRepository.findByUserAndDateBetweenAndIsDeletedFalse(eq(testUser), eq(startDate), eq(endDate)))
                .thenReturn(List.of(tx1, tx2, tx3, tx4));

        MonthlyReportResponse report = reportService.getMonthlyReport(2024, 1, testUser);

        assertNotNull(report);
        assertEquals(1, report.getMonth());
        assertEquals(2024, report.getYear());

        // Total Income map
        assertEquals(2, report.getTotalIncome().size());
        assertEquals(new BigDecimal("3000.00"), report.getTotalIncome().get("Salary"));
        assertEquals(new BigDecimal("500.00"), report.getTotalIncome().get("Freelance"));

        // Total Expenses map
        assertEquals(2, report.getTotalExpenses().size());
        assertEquals(new BigDecimal("400.00"), report.getTotalExpenses().get("Food"));
        assertEquals(new BigDecimal("1200.00"), report.getTotalExpenses().get("Rent"));

        // Net savings = (3000 + 500) - (400 + 1200) = 3500 - 1600 = 1900
        assertEquals(new BigDecimal("1900.00"), report.getNetSavings());
    }

    @Test
    @DisplayName("Should throw BadRequestException for invalid month or year")
    void getMonthlyReport_InvalidInputs_ThrowsBadRequest() {
        assertThrows(BadRequestException.class, () -> reportService.getMonthlyReport(2024, 0, testUser));
        assertThrows(BadRequestException.class, () -> reportService.getMonthlyReport(2024, 13, testUser));
        assertThrows(BadRequestException.class, () -> reportService.getMonthlyReport(1899, 1, testUser));
    }

    @Test
    @DisplayName("Should return empty maps and zero net savings when no transactions exist")
    void getMonthlyReport_EmptyTransactions() {
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);

        when(transactionRepository.findByUserAndDateBetweenAndIsDeletedFalse(eq(testUser), eq(startDate), eq(endDate)))
                .thenReturn(List.of());

        MonthlyReportResponse report = reportService.getMonthlyReport(2024, 1, testUser);

        assertNotNull(report);
        assertTrue(report.getTotalIncome().isEmpty());
        assertTrue(report.getTotalExpenses().isEmpty());
        assertEquals(new BigDecimal("0.00"), report.getNetSavings());
    }

    @Test
    @DisplayName("Should successfully generate yearly report with annual aggregation")
    void getYearlyReport_Success() {
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 12, 31);

        Transaction tx1 = Transaction.builder()
                .amount(new BigDecimal("36000.00"))
                .category(salaryCat)
                .type(CategoryType.INCOME)
                .build();

        Transaction tx2 = Transaction.builder()
                .amount(new BigDecimal("4800.00"))
                .category(foodCat)
                .type(CategoryType.EXPENSE)
                .build();

        when(transactionRepository.findByUserAndDateBetweenAndIsDeletedFalse(eq(testUser), eq(startDate), eq(endDate)))
                .thenReturn(List.of(tx1, tx2));

        YearlyReportResponse report = reportService.getYearlyReport(2024, testUser);

        assertNotNull(report);
        assertEquals(2024, report.getYear());
        assertEquals(new BigDecimal("36000.00"), report.getTotalIncome().get("Salary"));
        assertEquals(new BigDecimal("4800.00"), report.getTotalExpenses().get("Food"));
        assertEquals(new BigDecimal("31200.00"), report.getNetSavings());
    }
}
