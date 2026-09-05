package com.syfe.finance.service;

import com.syfe.finance.dto.common.MessageResponse;
import com.syfe.finance.dto.goal.CreateGoalRequest;
import com.syfe.finance.dto.goal.GoalListResponse;
import com.syfe.finance.dto.goal.GoalResponse;
import com.syfe.finance.dto.goal.UpdateGoalRequest;
import com.syfe.finance.entity.Category;
import com.syfe.finance.entity.CategoryType;
import com.syfe.finance.entity.SavingsGoal;
import com.syfe.finance.entity.Transaction;
import com.syfe.finance.entity.User;
import com.syfe.finance.exception.BadRequestException;
import com.syfe.finance.exception.ResourceNotFoundException;
import com.syfe.finance.repository.SavingsGoalRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SavingsGoalServiceTest {

    @Mock
    private SavingsGoalRepository savingsGoalRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private SavingsGoalService savingsGoalService;

    private User testUser;
    private Category salaryCat;
    private Category rentCat;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).username("user@example.com").build();
        salaryCat = Category.builder().name("Salary").type(CategoryType.INCOME).build();
        rentCat = Category.builder().name("Rent").type(CategoryType.EXPENSE).build();
    }

    @Test
    @DisplayName("Should successfully create goal with calculated progress")
    void createGoal_Success() {
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate targetDate = LocalDate.now().plusMonths(6);

        CreateGoalRequest request = CreateGoalRequest.builder()
                .goalName("Emergency Fund")
                .targetAmount(new BigDecimal("5000.00"))
                .targetDate(targetDate)
                .startDate(startDate)
                .build();

        SavingsGoal savedGoal = SavingsGoal.builder()
                .id(1L)
                .goalName("Emergency Fund")
                .targetAmount(new BigDecimal("5000.00"))
                .targetDate(targetDate)
                .startDate(startDate)
                .user(testUser)
                .build();

        when(savingsGoalRepository.save(any(SavingsGoal.class))).thenReturn(savedGoal);

        // Mock 1 income and 1 expense transaction since startDate
        Transaction tx1 = Transaction.builder()
                .amount(new BigDecimal("3000.00"))
                .type(CategoryType.INCOME)
                .category(salaryCat)
                .build();

        Transaction tx2 = Transaction.builder()
                .amount(new BigDecimal("1000.00"))
                .type(CategoryType.EXPENSE)
                .category(rentCat)
                .build();

        when(transactionRepository.findByUserAndDateAfterOrEqualAndIsDeletedFalse(eq(testUser), eq(startDate)))
                .thenReturn(List.of(tx1, tx2));

        GoalResponse response = savingsGoalService.createGoal(request, testUser);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Emergency Fund", response.getGoalName());
        assertEquals(new BigDecimal("5000.00"), response.getTargetAmount());
        assertEquals(new BigDecimal("2000.00"), response.getCurrentProgress()); // 3000 - 1000 = 2000
        assertEquals(40.0, response.getProgressPercentage()); // (2000 / 5000) * 100 = 40.0%
        assertEquals(new BigDecimal("3000.00"), response.getRemainingAmount()); // 5000 - 2000 = 3000
    }

    @Test
    @DisplayName("Should default start date to today when not provided")
    void createGoal_DefaultStartDate_Success() {
        LocalDate targetDate = LocalDate.now().plusMonths(6);

        CreateGoalRequest request = CreateGoalRequest.builder()
                .goalName("Car Savings")
                .targetAmount(new BigDecimal("10000.00"))
                .targetDate(targetDate)
                .startDate(null)
                .build();

        SavingsGoal savedGoal = SavingsGoal.builder()
                .id(2L)
                .goalName("Car Savings")
                .targetAmount(new BigDecimal("10000.00"))
                .targetDate(targetDate)
                .startDate(LocalDate.now())
                .user(testUser)
                .build();

        when(savingsGoalRepository.save(any(SavingsGoal.class))).thenReturn(savedGoal);
        when(transactionRepository.findByUserAndDateAfterOrEqualAndIsDeletedFalse(any(), any()))
                .thenReturn(List.of());

        GoalResponse response = savingsGoalService.createGoal(request, testUser);

        assertNotNull(response);
        assertEquals(LocalDate.now(), response.getStartDate());
        assertEquals(new BigDecimal("0.00"), response.getCurrentProgress());
        assertEquals(0.0, response.getProgressPercentage());
        assertEquals(new BigDecimal("10000.00"), response.getRemainingAmount());
    }

    @Test
    @DisplayName("Should throw BadRequestException for invalid target amount or date")
    void createGoal_InvalidInputs_ThrowsBadRequest() {
        // Non-positive target amount
        CreateGoalRequest badAmount = CreateGoalRequest.builder()
                .goalName("Goal")
                .targetAmount(BigDecimal.ZERO)
                .targetDate(LocalDate.now().plusDays(10))
                .build();

        assertThrows(BadRequestException.class, () -> savingsGoalService.createGoal(badAmount, testUser));

        // Past target date
        CreateGoalRequest badDate = CreateGoalRequest.builder()
                .goalName("Goal")
                .targetAmount(new BigDecimal("1000.00"))
                .targetDate(LocalDate.now().minusDays(1))
                .build();

        assertThrows(BadRequestException.class, () -> savingsGoalService.createGoal(badDate, testUser));

        // Start date after target date
        CreateGoalRequest invertedDates = CreateGoalRequest.builder()
                .goalName("Goal")
                .targetAmount(new BigDecimal("1000.00"))
                .startDate(LocalDate.now().plusMonths(5))
                .targetDate(LocalDate.now().plusMonths(2))
                .build();

        assertThrows(BadRequestException.class, () -> savingsGoalService.createGoal(invertedDates, testUser));
    }

    @Test
    @DisplayName("Should correctly handle overachieved goals")
    void calculateAndMap_OverachievedGoal() {
        SavingsGoal goal = SavingsGoal.builder()
                .id(1L)
                .goalName("Fund")
                .targetAmount(new BigDecimal("1000.00"))
                .startDate(LocalDate.of(2025, 1, 1))
                .targetDate(LocalDate.now().plusYears(1))
                .user(testUser)
                .build();

        Transaction incomeTx = Transaction.builder()
                .amount(new BigDecimal("1500.00"))
                .type(CategoryType.INCOME)
                .build();

        when(transactionRepository.findByUserAndDateAfterOrEqualAndIsDeletedFalse(testUser, goal.getStartDate()))
                .thenReturn(List.of(incomeTx));

        GoalResponse response = savingsGoalService.calculateAndMap(goal, testUser);

        assertEquals(new BigDecimal("1500.00"), response.getCurrentProgress());
        assertEquals(150.0, response.getProgressPercentage());
        assertEquals(new BigDecimal("0.00"), response.getRemainingAmount());
    }

    @Test
    @DisplayName("Should update savings goal and recalculate progress")
    void updateGoal_Success() {
        LocalDate targetDate = LocalDate.now().plusMonths(6);
        SavingsGoal existing = SavingsGoal.builder()
                .id(1L)
                .goalName("Trip")
                .targetAmount(new BigDecimal("2000.00"))
                .targetDate(targetDate)
                .startDate(LocalDate.now().minusMonths(1))
                .user(testUser)
                .build();

        when(savingsGoalRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(existing));
        when(savingsGoalRepository.save(any(SavingsGoal.class))).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.findByUserAndDateAfterOrEqualAndIsDeletedFalse(any(), any()))
                .thenReturn(List.of());

        UpdateGoalRequest updateReq = UpdateGoalRequest.builder()
                .goalName("European Trip")
                .targetAmount(new BigDecimal("3000.00"))
                .targetDate(LocalDate.now().plusMonths(12))
                .build();

        GoalResponse response = savingsGoalService.updateGoal(1L, updateReq, testUser);

        assertNotNull(response);
        assertEquals("European Trip", response.getGoalName());
        assertEquals(new BigDecimal("3000.00"), response.getTargetAmount());
    }

    @Test
    @DisplayName("Should successfully delete savings goal")
    void deleteGoal_Success() {
        SavingsGoal existing = SavingsGoal.builder()
                .id(1L)
                .user(testUser)
                .build();

        when(savingsGoalRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(existing));

        MessageResponse response = savingsGoalService.deleteGoal(1L, testUser);

        assertNotNull(response);
        assertEquals("Goal deleted successfully", response.getMessage());
        verify(savingsGoalRepository).delete(existing);
    }
}
