package com.syfe.finance.service;

import com.syfe.finance.dto.common.MessageResponse;
import com.syfe.finance.dto.goal.CreateGoalRequest;
import com.syfe.finance.dto.goal.GoalListResponse;
import com.syfe.finance.dto.goal.GoalResponse;
import com.syfe.finance.dto.goal.UpdateGoalRequest;
import com.syfe.finance.entity.CategoryType;
import com.syfe.finance.entity.SavingsGoal;
import com.syfe.finance.entity.Transaction;
import com.syfe.finance.entity.User;
import com.syfe.finance.exception.BadRequestException;
import com.syfe.finance.exception.ResourceNotFoundException;
import com.syfe.finance.repository.SavingsGoalRepository;
import com.syfe.finance.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SavingsGoalService {

    private final SavingsGoalRepository savingsGoalRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public GoalResponse createGoal(CreateGoalRequest request, User user) {
        if (request.getTargetAmount() == null || request.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Target amount must be positive");
        }

        LocalDate now = LocalDate.now();
        if (request.getTargetDate() == null || !request.getTargetDate().isAfter(now)) {
            throw new BadRequestException("Target date must be in the future");
        }

        LocalDate startDate = request.getStartDate() != null ? request.getStartDate() : now;
        if (startDate.isAfter(request.getTargetDate())) {
            throw new BadRequestException("Start date cannot be after target date");
        }

        SavingsGoal goal = SavingsGoal.builder()
                .goalName(request.getGoalName().trim())
                .targetAmount(request.getTargetAmount().setScale(2, RoundingMode.HALF_UP))
                .targetDate(request.getTargetDate())
                .startDate(startDate)
                .user(user)
                .build();

        SavingsGoal saved = savingsGoalRepository.save(goal);
        log.info("Created savings goal id {} for user {}", saved.getId(), user.getUsername());
        return calculateAndMap(saved, user);
    }

    @Transactional(readOnly = true)
    public GoalListResponse getGoals(User user) {
        List<SavingsGoal> goals = savingsGoalRepository.findAllByUserOrderByIdAsc(user);
        List<GoalResponse> responses = goals.stream()
                .map(goal -> calculateAndMap(goal, user))
                .collect(Collectors.toList());

        return GoalListResponse.builder()
                .goals(responses)
                .build();
    }

    @Transactional(readOnly = true)
    public GoalResponse getGoalById(Long id, User user) {
        SavingsGoal goal = savingsGoalRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Savings goal not found with id: " + id));

        return calculateAndMap(goal, user);
    }

    @Transactional
    public GoalResponse updateGoal(Long id, UpdateGoalRequest request, User user) {
        SavingsGoal goal = savingsGoalRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Savings goal not found with id: " + id));

        if (request.getGoalName() != null && !request.getGoalName().isBlank()) {
            goal.setGoalName(request.getGoalName().trim());
        }

        if (request.getTargetAmount() != null) {
            if (request.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Target amount must be positive");
            }
            goal.setTargetAmount(request.getTargetAmount().setScale(2, RoundingMode.HALF_UP));
        }

        if (request.getTargetDate() != null) {
            if (!request.getTargetDate().isAfter(LocalDate.now())) {
                throw new BadRequestException("Target date must be in the future");
            }
            if (request.getTargetDate().isBefore(goal.getStartDate())) {
                throw new BadRequestException("Target date cannot be before goal start date");
            }
            goal.setTargetDate(request.getTargetDate());
        }

        SavingsGoal updated = savingsGoalRepository.save(goal);
        log.info("Updated savings goal id {} for user {}", updated.getId(), user.getUsername());
        return calculateAndMap(updated, user);
    }

    @Transactional
    public MessageResponse deleteGoal(Long id, User user) {
        SavingsGoal goal = savingsGoalRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Savings goal not found with id: " + id));

        savingsGoalRepository.delete(goal);
        log.info("Deleted savings goal id {} for user {}", id, user.getUsername());

        return MessageResponse.builder()
                .message("Goal deleted successfully")
                .build();
    }

    public GoalResponse calculateAndMap(SavingsGoal goal, User user) {
        List<Transaction> transactions = transactionRepository
                .findByUserAndDateAfterOrEqualAndIsDeletedFalse(user, goal.getStartDate());

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;

        for (Transaction t : transactions) {
            if (t.getType() == CategoryType.INCOME) {
                totalIncome = totalIncome.add(t.getAmount());
            } else if (t.getType() == CategoryType.EXPENSE) {
                totalExpenses = totalExpenses.add(t.getAmount());
            }
        }

        BigDecimal currentProgress = totalIncome.subtract(totalExpenses).setScale(2, RoundingMode.HALF_UP);
        BigDecimal targetAmount = goal.getTargetAmount().setScale(2, RoundingMode.HALF_UP);

        double progressPercentage = 0.0;
        if (targetAmount.compareTo(BigDecimal.ZERO) > 0) {
            progressPercentage = currentProgress
                    .divide(targetAmount, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        BigDecimal remainingAmount = targetAmount.subtract(currentProgress).setScale(2, RoundingMode.HALF_UP);
        if (remainingAmount.compareTo(BigDecimal.ZERO) < 0) {
            remainingAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return GoalResponse.builder()
                .id(goal.getId())
                .goalName(goal.getGoalName())
                .targetAmount(targetAmount)
                .targetDate(goal.getTargetDate())
                .startDate(goal.getStartDate())
                .currentProgress(currentProgress)
                .progressPercentage(progressPercentage)
                .remainingAmount(remainingAmount)
                .build();
    }
}
