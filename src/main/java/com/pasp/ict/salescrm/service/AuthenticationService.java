package com.pasp.ict.salescrm.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pasp.ict.salescrm.entity.User;
import com.pasp.ict.salescrm.repository.UserRepository;
import com.pasp.ict.salescrm.security.CustomUserDetailsService.CustomUserPrincipal;
import com.pasp.ict.salescrm.security.SecurityAuditService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Service for handling user authentication, session management, and security logging.
 * Implements login/logout functionality with 30-minute session timeout.
 */
@Service
@Transactional
public class AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    private static final int SESSION_TIMEOUT_MINUTES = 30;

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionRegistry sessionRegistry;
    private final SecurityAuditService securityAuditService;

    public AuthenticationService(AuthenticationManager authenticationManager,
                               UserRepository userRepository,
                               PasswordEncoder passwordEncoder,
                               SessionRegistry sessionRegistry,
                               SecurityAuditService securityAuditService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionRegistry = sessionRegistry;
        this.securityAuditService = securityAuditService;
    }

    /**
     * Authenticate user with username and password.
     * Logs authentication attempts and updates last login time.
     */
    public AuthenticationResult authenticateUser(String username, String password, HttpServletRequest request) {
        LocalDateTime attemptTime = LocalDateTime.now();
        
        try {
            // Attempt authentication
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
            );

            // Set authentication in security context
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Get user details
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            User user = userPrincipal.getUser();

            // Update last login time
            user.setLastLogin(attemptTime);
            userRepository.save(user);

            // Configure session
            HttpSession session = request.getSession(true);
            session.setMaxInactiveInterval(SESSION_TIMEOUT_MINUTES * 60); // 30 minutes in seconds

            // Log successful authentication
            logger.info("Successful authentication for user: {} at {}", username, attemptTime);
            
            // Log session creation for audit
            securityAuditService.logSessionEvent(user, "CREATED", session.getId(), request);

            return new AuthenticationResult(true, user, "Authentication successful", session.getId());

        } catch (AuthenticationException e) {
            // Log failed authentication attempt
            logger.warn("Failed authentication attempt for user: {} at {} - Reason: {}", 
                       username, attemptTime, e.getMessage());

            return new AuthenticationResult(false, null, "Invalid username or password", null);
        }
    }

    /**
     * Logout user and invalidate session.
     */
    public void logoutUser(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            User user = getCurrentUser().orElse(null);
            
            // Invalidate session
            HttpSession session = request.getSession(false);
            if (session != null) {
                String sessionId = session.getId();
                session.invalidate();
                
                // Log logout
                logger.info("User {} logged out, session {} invalidated at {}", 
                           username, sessionId, LocalDateTime.now());
                
                // Log session termination for audit
                securityAuditService.logSessionEvent(user, "TERMINATED", sessionId, request);
            }

            // Clear security context
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Check if current session is valid and not expired.
     */
    public boolean isSessionValid(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }

        // Check if session is expired
        long lastAccessedTime = session.getLastAccessedTime();
        long currentTime = System.currentTimeMillis();
        long sessionTimeout = session.getMaxInactiveInterval() * 1000L; // Convert to milliseconds

        if (currentTime - lastAccessedTime > sessionTimeout) {
            session.invalidate();
            logger.info("Session {} expired and invalidated", session.getId());
            
            // Log session timeout for audit
            User currentUser = getCurrentUser().orElse(null);
            securityAuditService.logSessionEvent(currentUser, "TIMEOUT", session.getId(), request);
            
            return false;
        }

        return true;
    }

    /**
     * Get currently authenticated user.
     */
    public Optional<User> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() 
            && authentication.getPrincipal() instanceof CustomUserPrincipal) {
            
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            return Optional.of(userPrincipal.getUser());
        }
        
        return Optional.empty();
    }

    /**
     * Validate password complexity requirements.
     */
    public boolean isPasswordValid(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasLowercase = password.chars().anyMatch(Character::isLowerCase);
        boolean hasUppercase = password.chars().anyMatch(Character::isUpperCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);

        return hasLowercase && hasUppercase && hasDigit;
    }

    /**
     * Encode password using BCrypt.
     */
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * Get all active sessions for a user.
     */
    public List<SessionInformation> getUserSessions(String username) {
        return sessionRegistry.getAllSessions(username, false);
    }

    /**
     * Expire all sessions for a user (useful for admin operations).
     */
    public void expireUserSessions(String username) {
        List<SessionInformation> sessions = getUserSessions(username);
        for (SessionInformation session : sessions) {
            session.expireNow();
        }
        logger.info("Expired {} sessions for user: {}", sessions.size(), username);
    }

    /**
     * Result object for authentication operations.
     */
    public static class AuthenticationResult {
        private final boolean success;
        private final User user;
        private final String message;
        private final String sessionId;

        public AuthenticationResult(boolean success, User user, String message, String sessionId) {
            this.success = success;
            this.user = user;
            this.message = message;
            this.sessionId = sessionId;
        }

        public boolean isSuccess() {
            return success;
        }

        public User getUser() {
            return user;
        }

        public String getMessage() {
            return message;
        }

        public String getSessionId() {
            return sessionId;
        }
    }
}