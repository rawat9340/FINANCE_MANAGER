package com.syfe.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syfe.finance.dto.auth.LoginRequest;
import com.syfe.finance.dto.auth.RegisterRequest;
import com.syfe.finance.dto.transaction.CreateTransactionRequest;
import com.syfe.finance.dto.transaction.UpdateTransactionRequest;
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
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private MockHttpSession session;

    @BeforeEach
    void setUp() throws Exception {
        String username = "txuser" + System.currentTimeMillis() + "@example.com";
        RegisterRequest regReq = RegisterRequest.builder()
                .username(username)
                .password("Password123")
                .fullName("Tx User")
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
    @DisplayName("Should successfully create transaction with category-derived type")
    void createTransaction_Success() throws Exception {
        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .amount(new BigDecimal("50000.00"))
                .date(LocalDate.of(2024, 1, 15))
                .category("Salary")
                .description("January Salary")
                .build();

        mockMvc.perform(post("/api/transactions").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount", is(50000.00)))
                .andExpect(jsonPath("$.date", is("2024-01-15")))
                .andExpect(jsonPath("$.category", is("Salary")))
                .andExpect(jsonPath("$.type", is("INCOME")))
                .andExpect(jsonPath("$.description", is("January Salary")));
    }

    @Test
    @DisplayName("Should filter transactions by date range and type")
    void getTransactions_WithFilter_Success() throws Exception {
        CreateTransactionRequest tx1 = CreateTransactionRequest.builder()
                .amount(new BigDecimal("3000.00"))
                .date(LocalDate.of(2024, 1, 10))
                .category("Salary")
                .build();

        CreateTransactionRequest tx2 = CreateTransactionRequest.builder()
                .amount(new BigDecimal("200.00"))
                .date(LocalDate.of(2024, 1, 20))
                .category("Food")
                .build();

        mockMvc.perform(post("/api/transactions").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tx1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/transactions").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tx2)))
                .andExpect(status().isCreated());

        // Filter for EXPENSE only
        mockMvc.perform(get("/api/transactions?type=EXPENSE").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions", hasSize(1)))
                .andExpect(jsonPath("$.transactions[0].category", is("Food")));

        // Filter date range
        mockMvc.perform(get("/api/transactions?startDate=2024-01-01&endDate=2024-01-15").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions", hasSize(1)))
                .andExpect(jsonPath("$.transactions[0].category", is("Salary")));
    }

    @Test
    @DisplayName("Should successfully update transaction and reject date change")
    void updateTransaction_SuccessAndDateImmutable() throws Exception {
        CreateTransactionRequest createReq = CreateTransactionRequest.builder()
                .amount(new BigDecimal("50000.00"))
                .date(LocalDate.of(2024, 1, 15))
                .category("Salary")
                .description("Old Salary")
                .build();

        MvcResult createRes = mockMvc.perform(post("/api/transactions").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long id = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asLong();

        // Valid update: amount and description
        UpdateTransactionRequest updateReq = UpdateTransactionRequest.builder()
                .amount(new BigDecimal("60000.00"))
                .description("Updated January Salary")
                .build();

        mockMvc.perform(put("/api/transactions/" + id).session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount", is(60000.00)))
                .andExpect(jsonPath("$.description", is("Updated January Salary")));

        // Invalid update: attempt to modify date
        UpdateTransactionRequest badDateReq = UpdateTransactionRequest.builder()
                .date(LocalDate.of(2024, 1, 20))
                .build();

        mockMvc.perform(put("/api/transactions/" + id).session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badDateReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("The date field cannot be modified")));
    }

    @Test
    @DisplayName("Should successfully delete transaction and exclude from future queries")
    void deleteTransaction_Success() throws Exception {
        CreateTransactionRequest createReq = CreateTransactionRequest.builder()
                .amount(new BigDecimal("100.00"))
                .date(LocalDate.of(2024, 1, 15))
                .category("Food")
                .build();

        MvcResult createRes = mockMvc.perform(post("/api/transactions").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long id = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/transactions/" + id).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Transaction deleted successfully")));

        // Trying to delete again -> 404
        mockMvc.perform(delete("/api/transactions/" + id).session(session))
                .andExpect(status().isNotFound());
    }
}
