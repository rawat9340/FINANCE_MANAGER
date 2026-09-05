package com.syfe.finance.security;

import com.syfe.finance.entity.User;
import com.syfe.finance.exception.UnauthorizedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityUtilsTest {

    private final SecurityUtils securityUtils = new SecurityUtils();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should return authenticated user details when present in context")
    void getCurrentUser_Authenticated_Success() {
        User user = User.builder()
                .id(10L)
                .username("test@example.com")
                .password("password")
                .fullName("Test User")
                .phoneNumber("+1234567890")
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(auth);

        User currentUser = securityUtils.getCurrentUser();
        assertNotNull(currentUser);
        assertEquals(10L, currentUser.getId());
        assertEquals("test@example.com", securityUtils.getCurrentUsername());

        // CustomUserDetails helper assertions
        assertEquals(10L, userDetails.getId());
        assertEquals("test@example.com", userDetails.getUsername());
        assertEquals("password", userDetails.getPassword());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isCredentialsNonExpired());
        assertTrue(userDetails.isEnabled());
        assertEquals(1, userDetails.getAuthorities().size());
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when no authentication in context")
    void getCurrentUser_NoAuth_ThrowsUnauthorized() {
        SecurityContextHolder.clearContext();
        assertThrows(UnauthorizedException.class, securityUtils::getCurrentUser);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when anonymous authentication in context")
    void getCurrentUser_Anonymous_ThrowsUnauthorized() {
        AnonymousAuthenticationToken anonymous = new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
        SecurityContextHolder.getContext().setAuthentication(anonymous);

        assertThrows(UnauthorizedException.class, securityUtils::getCurrentUser);
    }
}
