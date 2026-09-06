package com.syfe.finance.exception;

import com.syfe.finance.dto.common.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        log.warn("Bad Request: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> details = ex.getBindingResult().getAllErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.toList());

        String message = details.isEmpty() ? "Validation failed" : details.get(0);
        log.warn("Validation error on {}: {}", request.getRequestURI(), message);
        return buildResponse(HttpStatus.BAD_REQUEST, message, request.getRequestURI(), details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Malformed JSON on {}: {}", request.getRequestURI(), ex.getMessage());
        String message = "Malformed or invalid request body";
        if (ex.getMessage() != null && ex.getMessage().contains("LocalDate")) {
            message = "Invalid date format. Required format is YYYY-MM-DD";
        }
        return buildResponse(HttpStatus.BAD_REQUEST, message, request.getRequestURI(), null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = String.format("Parameter '%s' has an invalid value", ex.getName());
        log.warn("Type mismatch on {}: {}", request.getRequestURI(), message);
        return buildResponse(HttpStatus.BAD_REQUEST, message, request.getRequestURI(), null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
        String message = String.format("Required parameter '%s' is missing", ex.getParameterName());
        log.warn("Missing param on {}: {}", request.getRequestURI(), message);
        return buildResponse(HttpStatus.BAD_REQUEST, message, request.getRequestURI(), null);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        log.warn("Unauthorized on {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication failed on {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, "Invalid username or password", request.getRequestURI(), null);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex, HttpServletRequest request) {
        log.warn("Forbidden on {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied on {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, "Access denied", request.getRequestURI(), null);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found on {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "The requested resource was not found", request.getRequestURI(), null);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex, HttpServletRequest request) {
        log.warn("Conflict on {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Data integrity conflict on {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, "Resource conflict or unique constraint violation", request.getRequestURI(), null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Illegal argument on {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled error on {}: ", request.getRequestURI(), ex);
        List<String> details = List.of(ex.getClass().getName() + ": " + ex.getMessage());
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request.getRequestURI(), details);
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message, String path, List<String> details) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .details(details)
                .build();
        return ResponseEntity.status(status).body(errorResponse);
    }
}
