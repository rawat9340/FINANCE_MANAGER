package com.syfe.finance.controller;

import com.syfe.finance.dto.common.MessageResponse;
import com.syfe.finance.dto.transaction.CreateTransactionRequest;
import com.syfe.finance.dto.transaction.TransactionListResponse;
import com.syfe.finance.dto.transaction.TransactionResponse;
import com.syfe.finance.dto.transaction.UpdateTransactionRequest;
import com.syfe.finance.entity.CategoryType;
import com.syfe.finance.entity.User;
import com.syfe.finance.security.SecurityUtils;
import com.syfe.finance.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction Management", description = "Endpoints for creating, filtering, updating, and deleting transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final SecurityUtils securityUtils;

    @PostMapping
    @Operation(summary = "Create a new transaction", description = "Creates an income or expense transaction; type is inferred from the category")
    public ResponseEntity<TransactionResponse> createTransaction(@Valid @RequestBody CreateTransactionRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        TransactionResponse response = transactionService.createTransaction(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get transactions", description = "Retrieves transactions sorted by newest date first, with composable filtering")
    public ResponseEntity<TransactionListResponse> getTransactions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) CategoryType type) {
        User currentUser = securityUtils.getCurrentUser();
        TransactionListResponse response = transactionService.getTransactions(startDate, endDate, categoryId, category, type, currentUser);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a transaction", description = "Updates transaction amount, category, or description. The date cannot be modified.")
    public ResponseEntity<TransactionResponse> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTransactionRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        TransactionResponse response = transactionService.updateTransaction(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a transaction", description = "Deletes a transaction and excludes it from reports and goal calculations")
    public ResponseEntity<MessageResponse> deleteTransaction(@PathVariable Long id) {
        User currentUser = securityUtils.getCurrentUser();
        MessageResponse response = transactionService.deleteTransaction(id, currentUser);
        return ResponseEntity.ok(response);
    }
}
