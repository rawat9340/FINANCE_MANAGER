package com.syfe.finance.dto.category;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.syfe.finance.entity.CategoryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryResponse {

    private String name;
    private CategoryType type;

    @JsonProperty("isCustom")
    private boolean isCustom;
}
