package com.syfe.finance.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityHandlerTest {

    @Test
    @DisplayName("CustomAuthenticationEntryPoint should write 401 JSON error response")
    void commence_Writes401Json() throws Exception {
        CustomAuthenticationEntryPoint entryPoint = new CustomAuthenticationEntryPoint();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/protected");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("Unauth"));

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("401"));
        assertTrue(response.getContentAsString().contains("/api/protected"));
    }

    @Test
    @DisplayName("CustomAccessDeniedHandler should write 403 JSON error response")
    void handle_Writes403Json() throws Exception {
        CustomAccessDeniedHandler handler = new CustomAccessDeniedHandler();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/forbidden");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("Forbidden"));

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("403"));
        assertTrue(response.getContentAsString().contains("/api/forbidden"));
    }
}
