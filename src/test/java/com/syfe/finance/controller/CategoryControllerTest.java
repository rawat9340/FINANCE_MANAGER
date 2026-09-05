package com.syfe.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syfe.finance.dto.auth.LoginRequest;
import com.syfe.finance.dto.auth.RegisterRequest;
import com.syfe.finance.dto.category.CreateCategoryRequest;
import com.syfe.finance.dto.transaction.CreateTransactionRequest;
import com.syfe.finance.entity.CategoryType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private MockHttpSession session;

    @BeforeEach
    void setUp() throws Exception {
        String username = "catuser" + System.currentTimeMillis() + "@example.com";
        RegisterRequest regReq = RegisterRequest.builder()
                .username(username)
                .password("Password123")
                .fullName("Category User")
                .phoneNumber("+1234567890")
                .build();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(regReq)));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(username, "Password123"))))
                .andReturn();

        session = (MockHttpSession) loginResult.getRequest().getSession(false);
    }

    @Test
    @DisplayName("Should return default categories on startup")
    void getCategories_ReturnsDefaultCategories() throws Exception {
        mockMvc.perform(get("/api/categories").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories").isArray())
                .andExpect(jsonPath("$.categories[*].name", hasItem("Salary")))
                .andExpect(jsonPath("$.categories[*].name", hasItem("Food")))
                .andExpect(jsonPath("$.categories[*].name", hasItem("Rent")));
    }

    @Test
    @DisplayName("Should create custom category and retrieve it in category list")
    void createCustomCategory_Success() throws Exception {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("Freelancing")
                .type(CategoryType.INCOME)
                .build();

        mockMvc.perform(post("/api/categories").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Freelancing")))
                .andExpect(jsonPath("$.type", is("INCOME")))
                .andExpect(jsonPath("$.isCustom", is(true)));

        // Retrieve list, should now have Freelancing
        mockMvc.perform(get("/api/categories").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories[*].name", hasItem("Freelancing")));
    }

    @Test
    @DisplayName("Should return 403 when attempting to delete a default category")
    void deleteCategory_DefaultCategory_Returns403() throws Exception {
        mockMvc.perform(delete("/api/categories/Food").session(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is("Default categories cannot be deleted")));
    }

    @Test
    @DisplayName("Should return 400 when attempting to delete category in use by transactions")
    void deleteCategory_InUse_Returns400() throws Exception {
        // Create custom category
        CreateCategoryRequest catReq = CreateCategoryRequest.builder()
                .name("GymSubscription")
                .type(CategoryType.EXPENSE)
                .build();

        mockMvc.perform(post("/api/categories").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(catReq)))
                .andExpect(status().isCreated());

        // Create transaction using that category
        CreateTransactionRequest txReq = CreateTransactionRequest.builder()
                .amount(new BigDecimal("50.00"))
                .date(LocalDate.now())
                .category("GymSubscription")
                .description("Monthly gym")
                .build();

        mockMvc.perform(post("/api/transactions").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(txReq)))
                .andExpect(status().isCreated());

        // Attempt delete -> 400
        mockMvc.perform(delete("/api/categories/GymSubscription").session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Category 'GymSubscription' cannot be deleted because it is referenced by active transactions")));
    }

    @Test
    @DisplayName("Should successfully delete unused custom category")
    void deleteCategory_Success() throws Exception {
        CreateCategoryRequest catReq = CreateCategoryRequest.builder()
                .name("UnusedCategory")
                .type(CategoryType.EXPENSE)
                .build();

        mockMvc.perform(post("/api/categories").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(catReq)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/categories/UnusedCategory").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Category deleted successfully")));
    }
}
