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
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService;

    @Transactional
    public TransactionResponse createTransaction(CreateTransactionRequest request, User user) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be positive");
        }

        if (request.getDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("Transaction date cannot be in the future");
        }

        Category category = categoryService.getAccessibleCategoryByName(request.getCategory(), user);

        Transaction transaction = Transaction.builder()
                .amount(request.getAmount().setScale(2, RoundingMode.HALF_UP))
                .date(request.getDate())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .category(category)
                .type(category.getType())
                .user(user)
                .isDeleted(false)
                .build();

        Transaction saved = transactionRepository.save(transaction);
        log.info("Created transaction id {} for user {}", saved.getId(), user.getUsername());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public TransactionListResponse getTransactions(LocalDate startDate, LocalDate endDate, Long categoryId, CategoryType type, User user) {
        return getTransactions(startDate, endDate, categoryId, null, type, user);
    }

    @Transactional(readOnly = true)
    public TransactionListResponse getTransactions(LocalDate startDate, LocalDate endDate, Long categoryId, String category, CategoryType type, User user) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("Start date cannot be after end date");
        }

        Specification<Transaction> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("user"), user));
            predicates.add(criteriaBuilder.isFalse(root.get("isDeleted")));

            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("date"), startDate));
            }
            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("date"), endDate));
            }
            if (categoryId != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("id"), categoryId));
            }
            if (category != null && !category.isBlank()) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("category").get("name")),
                        category.trim().toLowerCase()
                ));
            }
            if (type != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), type));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = Sort.by(Sort.Direction.DESC, "date").and(Sort.by(Sort.Direction.DESC, "id"));
        List<Transaction> transactions = transactionRepository.findAll(spec, sort);

        List<TransactionResponse> responses = transactions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return TransactionListResponse.builder()
                .transactions(responses)
                .build();
    }

    @Transactional
    public TransactionResponse updateTransaction(Long id, UpdateTransactionRequest request, User user) {
        Transaction transaction = transactionRepository.findByIdAndUserAndIsDeletedFalse(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));

        // Important: date cannot be modified - ignore date field if provided

        if (request.getAmount() != null) {
            if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Amount must be positive");
            }
            transaction.setAmount(request.getAmount().setScale(2, RoundingMode.HALF_UP));
        }

        if (request.getDescription() != null) {
            transaction.setDescription(request.getDescription().trim());
        }

        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            Category category = categoryService.getAccessibleCategoryByName(request.getCategory(), user);
            transaction.setCategory(category);
            transaction.setType(category.getType());
        }

        Transaction updated = transactionRepository.save(transaction);
        log.info("Updated transaction id {} for user {}", updated.getId(), user.getUsername());
        return mapToResponse(updated);
    }

    @Transactional
    public MessageResponse deleteTransaction(Long id, User user) {
        Transaction transaction = transactionRepository.findByIdAndUserAndIsDeletedFalse(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));

        transaction.setDeleted(true);
        transactionRepository.save(transaction);
        log.info("Deleted transaction id {} for user {}", id, user.getUsername());

        return MessageResponse.builder()
                .message("Transaction deleted successfully")
                .build();
    }

    public TransactionResponse mapToResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .amount(transaction.getAmount().setScale(2, RoundingMode.HALF_UP))
                .date(transaction.getDate())
                .category(transaction.getCategory().getName())
                .description(transaction.getDescription())
                .type(transaction.getType())
                .build();
    }
}
