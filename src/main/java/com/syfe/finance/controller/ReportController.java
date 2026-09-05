package com.syfe.finance.controller;

import com.syfe.finance.dto.report.MonthlyReportResponse;
import com.syfe.finance.dto.report.YearlyReportResponse;
import com.syfe.finance.entity.User;
import com.syfe.finance.security.SecurityUtils;
import com.syfe.finance.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Financial Reports", description = "Endpoints for monthly and yearly financial reports and net savings")
public class ReportController {

    private final ReportService reportService;
    private final SecurityUtils securityUtils;

    @GetMapping("/monthly/{year}/{month}")
    @Operation(summary = "Monthly financial report", description = "Aggregates income and expenses grouped by category for a specific month")
    public ResponseEntity<MonthlyReportResponse> getMonthlyReport(
            @PathVariable int year,
            @PathVariable int month) {
        User currentUser = securityUtils.getCurrentUser();
        MonthlyReportResponse response = reportService.getMonthlyReport(year, month, currentUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/yearly/{year}")
    @Operation(summary = "Yearly financial report", description = "Aggregates income and expenses grouped by category for an entire year")
    public ResponseEntity<YearlyReportResponse> getYearlyReport(@PathVariable int year) {
        User currentUser = securityUtils.getCurrentUser();
        YearlyReportResponse response = reportService.getYearlyReport(year, currentUser);
        return ResponseEntity.ok(response);
    }
}
