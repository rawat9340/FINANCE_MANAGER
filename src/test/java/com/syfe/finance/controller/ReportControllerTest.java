package com.syfe.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syfe.finance.dto.auth.LoginRequest;
import com.syfe.finance.dto.auth.RegisterRequest;
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

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private MockHttpSession session;

    @BeforeEach
    void setUp() throws Exception {
        String username = "reportuser" + System.currentTimeMillis() + "@example.com";
        RegisterRequest regReq = RegisterRequest.builder()
                .username(username)
                .password("Password123")
                .fullName("Report User")
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
    @DisplayName("Should return accurate monthly report matching specification JSON structure")
    void getMonthlyReport_Success() throws Exception {
        CreateTransactionRequest income = CreateTransactionRequest.builder()
                .amount(new BigDecimal("3000.00"))
                .date(LocalDate.of(2024, 1, 15))
                .category("Salary")
                .build();
        mockMvc.perform(post("/api/transactions").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(income)))
                .andExpect(status().isCreated());

        CreateTransactionRequest expense = CreateTransactionRequest.builder()
                .amount(new BigDecimal("400.00"))
                .date(LocalDate.of(2024, 1, 18))
                .category("Food")
                .build();
        mockMvc.perform(post("/api/transactions").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(expense)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/reports/monthly/2024/1").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month", is(1)))
                .andExpect(jsonPath("$.year", is(2024)))
                .andExpect(jsonPath("$.totalIncome.Salary", is(3000.00)))
                .andExpect(jsonPath("$.totalExpenses.Food", is(400.00)))
                .andExpect(jsonPath("$.netSavings", is(2600.00)));
    }

    @Test
    @DisplayName("Should return accurate yearly report matching specification JSON structure")
    void getYearlyReport_Success() throws Exception {
        CreateTransactionRequest income = CreateTransactionRequest.builder()
                .amount(new BigDecimal("36000.00"))
                .date(LocalDate.of(2024, 5, 20))
                .category("Salary")
                .build();
        mockMvc.perform(post("/api/transactions").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(income)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/reports/yearly/2024").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year", is(2024)))
                .andExpect(jsonPath("$.totalIncome.Salary", is(36000.00)))
                .andExpect(jsonPath("$.netSavings", is(36000.00)));
    }

    @Test
    @DisplayName("Should return 400 when invalid month or year is requested")
    void getMonthlyReport_InvalidParams_Returns400() throws Exception {
        mockMvc.perform(get("/api/reports/monthly/2024/13").session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Month must be between 1 and 12")));
    }
}
