package com.syfe.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syfe.finance.dto.auth.LoginRequest;
import com.syfe.finance.dto.auth.RegisterRequest;
import com.syfe.finance.dto.goal.CreateGoalRequest;
import com.syfe.finance.dto.goal.UpdateGoalRequest;
import com.syfe.finance.dto.transaction.CreateTransactionRequest;
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

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
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
class SavingsGoalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private MockHttpSession session;

    @BeforeEach
    void setUp() throws Exception {
        String username = "goaluser" + System.currentTimeMillis() + "@example.com";
        RegisterRequest regReq = RegisterRequest.builder()
                .username(username)
                .password("Password123")
                .fullName("Goal User")
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
    @DisplayName("Should create goal and accurately calculate progress from transactions")
    void createAndGetGoals_Success() throws Exception {
        // Create an income transaction of 5000
        CreateTransactionRequest income = CreateTransactionRequest.builder()
                .amount(new BigDecimal("5000.00"))
                .date(LocalDate.now())
                .category("Salary")
                .build();
        mockMvc.perform(post("/api/transactions").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(income)))
                .andExpect(status().isCreated());

        // Create an expense transaction of 1000
        CreateTransactionRequest expense = CreateTransactionRequest.builder()
                .amount(new BigDecimal("1000.00"))
                .date(LocalDate.now())
                .category("Rent")
                .build();
        mockMvc.perform(post("/api/transactions").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(expense)))
                .andExpect(status().isCreated());

        // Create goal with target 10000
        CreateGoalRequest goalReq = CreateGoalRequest.builder()
                .goalName("Car Fund")
                .targetAmount(new BigDecimal("10000.00"))
                .targetDate(LocalDate.now().plusYears(1))
                .startDate(LocalDate.now().minusDays(1))
                .build();

        MvcResult createGoalRes = mockMvc.perform(post("/api/goals").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(goalReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.goalName", is("Car Fund")))
                .andExpect(jsonPath("$.targetAmount", is(10000.00)))
                .andExpect(jsonPath("$.currentProgress", is(4000.00))) // 5000 - 1000 = 4000
                .andExpect(jsonPath("$.progressPercentage", is(40.0)))
                .andExpect(jsonPath("$.remainingAmount", is(6000.00)))
                .andReturn();

        Long goalId = objectMapper.readTree(createGoalRes.getResponse().getContentAsString()).get("id").asLong();

        // Get single goal by ID
        mockMvc.perform(get("/api/goals/" + goalId).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(goalId.intValue())))
                .andExpect(jsonPath("$.currentProgress", is(4000.00)));

        // Update goal target amount
        UpdateGoalRequest updateReq = UpdateGoalRequest.builder()
                .targetAmount(new BigDecimal("8000.00"))
                .build();

        mockMvc.perform(put("/api/goals/" + goalId).session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetAmount", is(8000.00)))
                .andExpect(jsonPath("$.progressPercentage", is(50.0))) // 4000 / 8000 = 50%
                .andExpect(jsonPath("$.remainingAmount", is(4000.00)));

        // Delete goal
        mockMvc.perform(delete("/api/goals/" + goalId).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Goal deleted successfully")));

        // After deletion -> 404
        mockMvc.perform(get("/api/goals/" + goalId).session(session))
                .andExpect(status().isNotFound());
    }
}
