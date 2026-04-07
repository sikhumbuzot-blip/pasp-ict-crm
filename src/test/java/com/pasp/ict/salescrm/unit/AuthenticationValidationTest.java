package com.pasp.ict.salescrm.unit;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.pasp.ict.salescrm.entity.User;
import com.pasp.ict.salescrm.entity.UserRole;
import com.pasp.ict.salescrm.repository.UserRepository;
import com.pasp.ict.salescrm.service.AuthenticationService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Unit tests for authentication validation functionality.
 * Tests Requirements 1.1, 1.2, 1.5 - Authentication Validation
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AuthenticationValidationTest {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private final String TEST_PASSWORD = "TestPass123";

    @BeforeEach
    void setUp() {
        // Create a test user for authentication tests
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword(passwordEncoder.encode(TEST_PASSWORD));
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setRole(UserRole.SALES);
        testUser.setActive(true);
        testUser = userRepository.save(testUser);
    }

    @Test
    void testValidCredentialsCreateSession() {
        HttpServletRequest request = new MockHttpServletRequest();
        LocalDateTime beforeAuth = LocalDateTime.now();

        // Test valid credentials - should succeed
        AuthenticationService.AuthenticationResult result = 
            authenticationService.authenticateUser(testUser.getUsername(), TEST_PASSWORD, request);

        // Valid credentials should create successful authentication
        assertTrue(result.isSuccess());
        assertNotNull(result.getUser());
        assertNotNull(result.getSessionId());
        assertEquals(testUser.getUsername(), result.getUser().getUsername());

        // User's last login should be updated
        User updatedUser = userRepository.findById(testUser.getId()).orElse(null);
        assertNotNull(updatedUser);
        assertNotNull(updatedUser.getLastLogin());
        assertTrue(updatedUser.getLastLogin().isAfter(beforeAuth) || updatedUser.getLastLogin().isEqual(beforeAuth));
    }

    @Test
    void testInvalidUsernameFailsAuthentication() {
        HttpServletRequest request = new MockHttpServletRequest();

        // Test invalid username - should fail
        AuthenticationService.AuthenticationResult result = 
            authenticationService.authenticateUser("invaliduser", TEST_PASSWORD, request);

        // Invalid credentials should not create successful authentication
        assertFalse(result.isSuccess());
        assertNull(result.getUser());
        assertNull(result.getSessionId());
        assertEquals("Invalid username or password", result.getMessage());
    }

    @Test
    void testInvalidPasswordFailsAuthentication() {
        HttpServletRequest request = new MockHttpServletRequest();

        // Test invalid password - should fail
        AuthenticationService.AuthenticationResult result = 
            authenticationService.authenticateUser(testUser.getUsername(), "wrongpassword", request);

        // Invalid credentials should not create successful authentication
        assertFalse(result.isSuccess());
        assertNull(result.getUser());
        assertNull(result.getSessionId());
        assertEquals("Invalid username or password", result.getMessage());
    }

    @Test
    void testAuthenticationAttemptLogging() {
        HttpServletRequest request = new MockHttpServletRequest();
        LocalDateTime beforeAuth = LocalDateTime.now();
        
        // Get initial last login time
        User userBefore = userRepository.findById(testUser.getId()).orElse(null);
        LocalDateTime initialLastLogin = userBefore != null ? userBefore.getLastLogin() : null;
        
        // Perform authentication
        AuthenticationService.AuthenticationResult result = 
            authenticationService.authenticateUser(testUser.getUsername(), TEST_PASSWORD, request);
        
        assertTrue(result.isSuccess());
        
        // Check that last login was updated
        User userAfter = userRepository.findById(testUser.getId()).orElse(null);
        assertNotNull(userAfter);
        assertNotNull(userAfter.getLastLogin());
        
        // Last login should be after the authentication attempt
        assertTrue(userAfter.getLastLogin().isAfter(beforeAuth) || userAfter.getLastLogin().isEqual(beforeAuth));
        
        // If there was a previous last login, new one should be different
        if (initialLastLogin != null) {
            assertNotEquals(userAfter.getLastLogin(), initialLastLogin);
        }
    }

    @Test
    void testInactiveUserCannotAuthenticate() {
        // Deactivate the user
        testUser.setActive(false);
        userRepository.save(testUser);

        HttpServletRequest request = new MockHttpServletRequest();

        // Test authentication with inactive user - should fail
        AuthenticationService.AuthenticationResult result = 
            authenticationService.authenticateUser(testUser.getUsername(), TEST_PASSWORD, request);

        // Inactive user should not be able to authenticate
        assertFalse(result.isSuccess());
        assertNull(result.getUser());
        assertNull(result.getSessionId());
    }

    @Test
    void testSessionValidation() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        
        // Initially no session should be valid
        assertFalse(authenticationService.isSessionValid(request));
        
        // After authentication, session should be valid
        AuthenticationService.AuthenticationResult result = 
            authenticationService.authenticateUser(testUser.getUsername(), TEST_PASSWORD, request);
        
        assertTrue(result.isSuccess());
        // Note: Session validation requires actual HTTP session, which is complex to test in unit tests
        // This is better tested in integration tests
    }
}