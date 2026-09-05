package com.syfe.finance.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syfe.finance.dto.auth.LoginRequest;
import com.syfe.finance.dto.auth.RegisterRequest;
import com.syfe.finance.dto.category.CreateCategoryRequest;
import com.syfe.finance.dto.goal.CreateGoalRequest;
import com.syfe.finance.dto.goal.UpdateGoalRequest;
import com.syfe.finance.dto.transaction.CreateTransactionRequest;
import com.syfe.finance.dto.transaction.UpdateTransactionRequest;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DataIsolationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private MockHttpSession sessionUserA;
    private MockHttpSession sessionUserB;

    @BeforeEach
    void setUp() throws Exception {
        long timestamp = System.currentTimeMillis();

        // Register & Login User A
        String userA = "usera_" + timestamp + "@example.com";
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RegisterRequest.builder()
                        .username(userA)
                        .password("Password123")
                        .fullName("User A")
                        .phoneNumber("+1111111111")
                        .build())));

        MvcResult loginA = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(userA, "Password123"))))
                .andReturn();
        sessionUserA = (MockHttpSession) loginA.getRequest().getSession(false);

        // Register & Login User B
        String userB = "userb_" + timestamp + "@example.com";
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RegisterRequest.builder()
                        .username(userB)
                        .password("Password123")
                        .fullName("User B")
                        .phoneNumber("+2222222222")
                        .build())));

        MvcResult loginB = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(userB, "Password123"))))
                .andReturn();
        sessionUserB = (MockHttpSession) loginB.getRequest().getSession(false);
    }

    @Test
    @DisplayName("User B must NOT see User A's transactions")
    void transactions_UserIsolation_GetList() throws Exception {
        // User A creates transaction
        CreateTransactionRequest txA = CreateTransactionRequest.builder()
                .amount(new BigDecimal("500.00"))
                .date(LocalDate.now())
                .category("Salary")
                .description("User A salary")
                .build();

        mockMvc.perform(post("/api/transactions").session(sessionUserA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(txA)))
                .andExpect(status().isCreated());

        // User B gets transactions -> should be empty
        mockMvc.perform(get("/api/transactions").session(sessionUserB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions", hasSize(0)));
    }

    @Test
    @DisplayName("User B must NOT update or delete User A's transaction (returns 404)")
    void transactions_UserIsolation_UpdateAndDelete() throws Exception {
        CreateTransactionRequest txA = CreateTransactionRequest.builder()
                .amount(new BigDecimal("500.00"))
                .date(LocalDate.now())
                .category("Salary")
                .description("User A tx")
                .build();

        MvcResult createRes = mockMvc.perform(post("/api/transactions").session(sessionUserA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(txA)))
                .andExpect(status().isCreated())
                .andReturn();

        Long txIdA = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asLong();

        // User B attempts to update User A's transaction
        UpdateTransactionRequest updateReq = UpdateTransactionRequest.builder()
                .amount(new BigDecimal("9999.00"))
                .description("Hacked")
                .build();

        mockMvc.perform(put("/api/transactions/" + txIdA).session(sessionUserB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound());

        // User B attempts to delete User A's transaction
        mockMvc.perform(delete("/api/transactions/" + txIdA).session(sessionUserB))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("User B must NOT view, update, or delete User A's savings goal (returns 404)")
    void goals_UserIsolation_CRUD() throws Exception {
        CreateGoalRequest goalA = CreateGoalRequest.builder()
                .goalName("User A Secret Goal")
                .targetAmount(new BigDecimal("10000.00"))
                .targetDate(LocalDate.now().plusMonths(6))
                .build();

        MvcResult createRes = mockMvc.perform(post("/api/goals").session(sessionUserA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(goalA)))
                .andExpect(status().isCreated())
                .andReturn();

        Long goalIdA = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asLong();

        // User B tries to view goal A
        mockMvc.perform(get("/api/goals/" + goalIdA).session(sessionUserB))
                .andExpect(status().isNotFound());

        // User B tries to update goal A
        UpdateGoalRequest updateReq = UpdateGoalRequest.builder()
                .targetAmount(new BigDecimal("500.00"))
                .build();

        mockMvc.perform(put("/api/goals/" + goalIdA).session(sessionUserB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound());

        // User B tries to delete goal A
        mockMvc.perform(delete("/api/goals/" + goalIdA).session(sessionUserB))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("User B must NOT delete User A's custom category")
    void categories_UserIsolation_Delete() throws Exception {
        CreateCategoryRequest catA = CreateCategoryRequest.builder()
                .name("SecretConsulting")
                .type(CategoryType.INCOME)
                .build();

        mockMvc.perform(post("/api/categories").session(sessionUserA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(catA)))
                .andExpect(status().isCreated());

        // User B attempts to delete User A's custom category
        mockMvc.perform(delete("/api/categories/SecretConsulting").session(sessionUserB))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("User B's financial report must NOT include User A's income or expenses")
    void reports_UserIsolation() throws Exception {
        CreateTransactionRequest txA = CreateTransactionRequest.builder()
                .amount(new BigDecimal("8000.00"))
                .date(LocalDate.of(2024, 1, 10))
                .category("Salary")
                .build();

        mockMvc.perform(post("/api/transactions").session(sessionUserA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(txA)))
                .andExpect(status().isCreated());

        // User B queries monthly report for 2024/1 -> netSavings must be 0, no income
        mockMvc.perform(get("/api/reports/monthly/2024/1").session(sessionUserB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netSavings", is(0.00)))
                .andExpect(jsonPath("$.totalIncome").isEmpty());
    }
}
