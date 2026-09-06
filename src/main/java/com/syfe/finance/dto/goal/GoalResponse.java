package com.syfe.finance.dto.goal;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.syfe.finance.dto.common.ZeroFlexibleBigDecimalSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalResponse {

    private Long id;
    private String goalName;
    private BigDecimal targetAmount;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate targetDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonSerialize(using = ZeroFlexibleBigDecimalSerializer.class)
    private BigDecimal currentProgress;
    private Double progressPercentage;
    private BigDecimal remainingAmount;
}
