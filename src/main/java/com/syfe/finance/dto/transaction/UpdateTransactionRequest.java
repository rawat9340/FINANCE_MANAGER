package com.syfe.finance.dto.transaction;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Positive;
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
public class UpdateTransactionRequest {

    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    private String category;

    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
}
