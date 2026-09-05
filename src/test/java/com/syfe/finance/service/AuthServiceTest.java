package com.syfe.finance.service;

import com.syfe.finance.dto.auth.AuthResponse;
import com.syfe.finance.dto.auth.LoginRequest;
import com.syfe.finance.dto.auth.RegisterRequest;
import com.syfe.finance.dto.auth.RegisterResponse;
import com.syfe.finance.entity.User;
import com.syfe.finance.exception.ConflictException;
import com.syfe.finance.exception.UnauthorizedException;
import com.syfe.finance.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private SecurityContextRepository securityContextRepository;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should successfully register a new user with hashed password")
    void register_Success() {
        RegisterRequest request = RegisterRequest.builder()
                .username("john@example.com")
                .password("Password123")
                .fullName("John Doe")
                .phoneNumber("+1234567890")
                .build();

        when(userRepository.existsByUsername("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("hashedPassword123");

        User savedUser = User.builder()
                .id(1L)
                .username("john@example.com")
                .password("hashedPassword123")
                .fullName("John Doe")
                .phoneNumber("+1234567890")
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        RegisterResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("User registered successfully", response.getMessage());
        assertEquals(1L, response.getUserId());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw ConflictException when registering duplicate email")
    void register_DuplicateUsername_ThrowsConflict() {
        RegisterRequest request = RegisterRequest.builder()
                .username("existing@example.com")
                .password("Password123")
                .fullName("Existing User")
                .phoneNumber("+1234567890")
                .build();

        when(userRepository.existsByUsername("existing@example.com")).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> authService.register(request));
        assertEquals("Username/email already registered: existing@example.com", ex.getMessage());
    }

    @Test
    @DisplayName("Should successfully login and persist security context in session")
    void login_Success() {
        LoginRequest request = LoginRequest.builder()
                .username("john@example.com")
                .password("Password123")
                .build();

        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        Authentication auth = mock(Authentication.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);

        AuthResponse response = authService.login(request, httpRequest, httpResponse);

        assertNotNull(response);
        assertEquals("Login successful", response.getMessage());
        verify(securityContextRepository).saveContext(any(SecurityContext.class), eq(httpRequest), eq(httpResponse));
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when login fails")
    void login_InvalidCredentials_ThrowsException() {
        LoginRequest request = LoginRequest.builder()
                .username("john@example.com")
                .password("WrongPassword")
                .build();

        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request, httpRequest, httpResponse));
    }

    @Test
    @DisplayName("Should successfully logout and invalidate HTTP session")
    void logout_Success() {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);

        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(httpRequest.getSession(false)).thenReturn(session);

        AuthResponse response = authService.logout(httpRequest, httpResponse);

        assertNotNull(response);
        assertEquals("Logout successful", response.getMessage());
        verify(session).invalidate();
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when logging out without session")
    void logout_Unauthenticated_ThrowsUnauthorized() {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);

        SecurityContextHolder.clearContext();

        assertThrows(UnauthorizedException.class, () -> authService.logout(httpRequest, httpResponse));
    }
}
