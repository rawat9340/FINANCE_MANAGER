package com.syfe.finance.exception;

import com.syfe.finance.dto.common.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    @Test
    @DisplayName("Should return 400 for BadRequestException")
    void handleBadRequest() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBadRequest(
                new BadRequestException("Invalid data"), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Invalid data", response.getBody().getMessage());
        assertEquals("/api/test", response.getBody().getPath());
    }

    @Test
    @DisplayName("Should return 400 for MethodArgumentNotValidException")
    void handleValidationErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        FieldError fieldError = new FieldError("object", "amount", "Amount must be positive");
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationErrors(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Amount must be positive", response.getBody().getMessage());
        assertEquals(1, response.getBody().getDetails().size());
    }

    @Test
    @DisplayName("Should return 400 for HttpMessageNotReadableException")
    void handleMalformedJson() {
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        when(ex.getMessage()).thenReturn("Cannot deserialize LocalDate");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleMalformedJson(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("Invalid date format"));
    }

    @Test
    @DisplayName("Should return 400 for MethodArgumentTypeMismatchException")
    void handleTypeMismatch() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("month");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleTypeMismatch(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("Parameter 'month' has an invalid value"));
    }

    @Test
    @DisplayName("Should return 400 for MissingServletRequestParameterException")
    void handleMissingParam() {
        MissingServletRequestParameterException ex = new MissingServletRequestParameterException("param", "String");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleMissingParam(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("Required parameter 'param' is missing"));
    }

    @Test
    @DisplayName("Should return 401 for UnauthorizedException and AuthenticationException")
    void handleUnauthorized() {
        ResponseEntity<ErrorResponse> response1 = exceptionHandler.handleUnauthorized(
                new UnauthorizedException("Not logged in"), request);
        assertEquals(HttpStatus.UNAUTHORIZED, response1.getStatusCode());
        assertEquals("Not logged in", response1.getBody().getMessage());

        ResponseEntity<ErrorResponse> response2 = exceptionHandler.handleAuthenticationException(
                new BadCredentialsException("Bad creds"), request);
        assertEquals(HttpStatus.UNAUTHORIZED, response2.getStatusCode());
    }

    @Test
    @DisplayName("Should return 403 for ForbiddenException and AccessDeniedException")
    void handleForbidden() {
        ResponseEntity<ErrorResponse> response1 = exceptionHandler.handleForbidden(
                new ForbiddenException("Default cannot be deleted"), request);
        assertEquals(HttpStatus.FORBIDDEN, response1.getStatusCode());
        assertEquals("Default cannot be deleted", response1.getBody().getMessage());

        ResponseEntity<ErrorResponse> response2 = exceptionHandler.handleAccessDenied(
                new AccessDeniedException("Denied"), request);
        assertEquals(HttpStatus.FORBIDDEN, response2.getStatusCode());
    }

    @Test
    @DisplayName("Should return 404 for ResourceNotFoundException")
    void handleNotFound() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleNotFound(
                new ResourceNotFoundException("Item not found"), request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Item not found", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Should return 409 for ConflictException and DataIntegrityViolationException")
    void handleConflict() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleConflict(
                new ConflictException("Already exists"), request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Already exists", response.getBody().getMessage());

        ResponseEntity<ErrorResponse> response2 = exceptionHandler.handleDataIntegrity(
                new DataIntegrityViolationException("Duplicate key"), request);
        assertEquals(HttpStatus.CONFLICT, response2.getStatusCode());
    }

    @Test
    @DisplayName("Should return 400 for IllegalArgumentException")
    void handleIllegalArgument() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgument(
                new IllegalArgumentException("Invalid argument"), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid argument", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Should return 500 for generic unhandled exceptions")
    void handleGenericException() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(
                new RuntimeException("System failure"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("An unexpected error occurred", response.getBody().getMessage());
    }
}
