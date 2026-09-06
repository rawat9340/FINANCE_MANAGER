package com.syfe.finance.dto.report;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.syfe.finance.dto.common.ZeroFlexibleBigDecimalSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyReportResponse {

    private int month;
    private int year;
    private Map<String, BigDecimal> totalIncome;
    private Map<String, BigDecimal> totalExpenses;

    @JsonSerialize(using = ZeroFlexibleBigDecimalSerializer.class)
    private BigDecimal netSavings;
}
