package com.pasp.ict.salescrm.unit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.pasp.ict.salescrm.service.AuthenticationService;

/**
 * Unit tests for AuthenticationService functionality.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AuthenticationServiceTest {

    @Autowired
    private AuthenticationService authenticationService;

    @Test
    void testPasswordComplexityValidation() {
        // Valid passwords
        assertTrue(authenticationService.isPasswordValid("TestPass123"));
        assertTrue(authenticationService.isPasswordValid("MySecure1"));
        assertTrue(authenticationService.isPasswordValid("Complex9Password"));
        
        // Invalid passwords - too short
        assertFalse(authenticationService.isPasswordValid("Test1"));
        assertFalse(authenticationService.isPasswordValid("Abc123"));
        
        // Invalid passwords - missing uppercase
        assertFalse(authenticationService.isPasswordValid("testpass123"));
        
        // Invalid passwords - missing lowercase
        assertFalse(authenticationService.isPasswordValid("TESTPASS123"));
        
        // Invalid passwords - missing numbers
        assertFalse(authenticationService.isPasswordValid("TestPassword"));
        
        // Invalid passwords - null or empty
        assertFalse(authenticationService.isPasswordValid(null));
        assertFalse(authenticationService.isPasswordValid(""));
    }

    @Test
    void testPasswordEncoding() {
        String rawPassword = "TestPass123";
        String encodedPassword = authenticationService.encodePassword(rawPassword);
        
        assertNotNull(encodedPassword);
        assertNotEquals(rawPassword, encodedPassword);
        assertTrue(encodedPassword.startsWith("$2a$")); // BCrypt prefix
    }
}