package com.syfe.finance.service;

import com.syfe.finance.dto.report.MonthlyReportResponse;
import com.syfe.finance.dto.report.YearlyReportResponse;
import com.syfe.finance.entity.CategoryType;
import com.syfe.finance.entity.Transaction;
import com.syfe.finance.entity.User;
import com.syfe.finance.exception.BadRequestException;
import com.syfe.finance.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public MonthlyReportResponse getMonthlyReport(int year, int month, User user) {
        validateYear(year);
        if (month < 1 || month > 12) {
            throw new BadRequestException("Month must be between 1 and 12");
        }

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        List<Transaction> transactions = transactionRepository
                .findByUserAndDateBetweenAndIsDeletedFalse(user, startDate, endDate);

        Map<String, BigDecimal> totalIncome = new TreeMap<>();
        Map<String, BigDecimal> totalExpenses = new TreeMap<>();
        BigDecimal sumIncome = BigDecimal.ZERO;
        BigDecimal sumExpenses = BigDecimal.ZERO;

        for (Transaction t : transactions) {
            String categoryName = t.getCategory().getName();
            BigDecimal amount = t.getAmount().setScale(2, RoundingMode.HALF_UP);

            if (t.getType() == CategoryType.INCOME) {
                totalIncome.put(categoryName, totalIncome.getOrDefault(categoryName, BigDecimal.ZERO).add(amount));
                sumIncome = sumIncome.add(amount);
            } else if (t.getType() == CategoryType.EXPENSE) {
                totalExpenses.put(categoryName, totalExpenses.getOrDefault(categoryName, BigDecimal.ZERO).add(amount));
                sumExpenses = sumExpenses.add(amount);
            }
        }

        BigDecimal netSavings = sumIncome.subtract(sumExpenses).setScale(2, RoundingMode.HALF_UP);

        return MonthlyReportResponse.builder()
                .month(month)
                .year(year)
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .netSavings(netSavings)
                .build();
    }

    @Transactional(readOnly = true)
    public YearlyReportResponse getYearlyReport(int year, User user) {
        validateYear(year);

        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        List<Transaction> transactions = transactionRepository
                .findByUserAndDateBetweenAndIsDeletedFalse(user, startDate, endDate);

        Map<String, BigDecimal> totalIncome = new TreeMap<>();
        Map<String, BigDecimal> totalExpenses = new TreeMap<>();
        BigDecimal sumIncome = BigDecimal.ZERO;
        BigDecimal sumExpenses = BigDecimal.ZERO;

        for (Transaction t : transactions) {
            String categoryName = t.getCategory().getName();
            BigDecimal amount = t.getAmount().setScale(2, RoundingMode.HALF_UP);

            if (t.getType() == CategoryType.INCOME) {
                totalIncome.put(categoryName, totalIncome.getOrDefault(categoryName, BigDecimal.ZERO).add(amount));
                sumIncome = sumIncome.add(amount);
            } else if (t.getType() == CategoryType.EXPENSE) {
                totalExpenses.put(categoryName, totalExpenses.getOrDefault(categoryName, BigDecimal.ZERO).add(amount));
                sumExpenses = sumExpenses.add(amount);
            }
        }

        BigDecimal netSavings = sumIncome.subtract(sumExpenses).setScale(2, RoundingMode.HALF_UP);

        return YearlyReportResponse.builder()
                .year(year)
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .netSavings(netSavings)
                .build();
    }

    private void validateYear(int year) {
        if (year < 1900 || year > 2100) {
            throw new BadRequestException("Year must be between 1900 and 2100");
        }
    }
}
