package com.syfe.finance.controller;

import com.syfe.finance.dto.common.MessageResponse;
import com.syfe.finance.dto.goal.CreateGoalRequest;
import com.syfe.finance.dto.goal.GoalListResponse;
import com.syfe.finance.dto.goal.GoalResponse;
import com.syfe.finance.dto.goal.UpdateGoalRequest;
import com.syfe.finance.entity.User;
import com.syfe.finance.security.SecurityUtils;
import com.syfe.finance.service.SavingsGoalService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
@Tag(name = "Savings Goals", description = "Endpoints for managing savings goals and tracking progress")
public class SavingsGoalController {

    private final SavingsGoalService savingsGoalService;
    private final SecurityUtils securityUtils;

    @PostMapping
    @Operation(summary = "Create a savings goal", description = "Creates a new savings goal with target amount and date")
    public ResponseEntity<GoalResponse> createGoal(@Valid @RequestBody CreateGoalRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        GoalResponse response = savingsGoalService.createGoal(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all savings goals", description = "Retrieves all savings goals with dynamically calculated progress")
    public ResponseEntity<GoalListResponse> getGoals() {
        User currentUser = securityUtils.getCurrentUser();
        GoalListResponse response = savingsGoalService.getGoals(currentUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get savings goal by ID", description = "Retrieves a single savings goal by its ID")
    public ResponseEntity<GoalResponse> getGoalById(@PathVariable Long id) {
        User currentUser = securityUtils.getCurrentUser();
        GoalResponse response = savingsGoalService.getGoalById(id, currentUser);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update savings goal", description = "Updates target amount, target date, or name for an existing savings goal")
    public ResponseEntity<GoalResponse> updateGoal(
            @PathVariable Long id,
            @Valid @RequestBody UpdateGoalRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        GoalResponse response = savingsGoalService.updateGoal(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete savings goal", description = "Deletes a savings goal belonging to the authenticated user")
    public ResponseEntity<MessageResponse> deleteGoal(@PathVariable Long id) {
        User currentUser = securityUtils.getCurrentUser();
        MessageResponse response = savingsGoalService.deleteGoal(id, currentUser);
        return ResponseEntity.ok(response);
    }
}
