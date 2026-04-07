package com.pasp.ict.salescrm.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pasp.ict.salescrm.entity.AuditLog;
import com.pasp.ict.salescrm.entity.User;
import com.pasp.ict.salescrm.entity.UserRole;
import com.pasp.ict.salescrm.repository.AuditLogRepository;
import com.pasp.ict.salescrm.repository.UserRepository;

/**
 * Service class for user management operations.
 * Handles user creation, role assignment, activation/deactivation, and search functionality.
 */
@Service
@Transactional
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogRepository auditLogRepository;
    
    @Autowired
    public UserService(UserRepository userRepository, 
                      PasswordEncoder passwordEncoder,
                      AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogRepository = auditLogRepository;
    }
    
    /**
     * Create a new user with validation and password encoding.
     * @param username the username
     * @param password the plain text password
     * @param email the email address
     * @param firstName the first name
     * @param lastName the last name
     * @param role the user role
     * @param createdBy the user creating this account
     * @return the created user
     * @throws IllegalArgumentException if validation fails
     */
    public User createUser(String username, String password, String email, 
                          String firstName, String lastName, UserRole role, User createdBy) {
        // Validate input
        validateUserInput(username, password, email, firstName, lastName, role);
        
        // Check for duplicates
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }
        
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }
        
        // Create user with encoded password
        User user = new User(username, passwordEncoder.encode(password), email, firstName, lastName, role);
        User savedUser = userRepository.save(user);
        
        // Log the creation
        logAuditEvent("USER_CREATED", "User", savedUser.getId(), null, 
                     String.format("Username: %s, Role: %s", username, role), createdBy);
        
        return savedUser;
    }
    
    /**
     * Update user role.
     * @param userId the user ID
     * @param newRole the new role
     * @param updatedBy the user making the change
     * @return the updated user
     * @throws IllegalArgumentException if user not found
     */
    public User updateUserRole(Long userId, UserRole newRole, User updatedBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        UserRole oldRole = user.getRole();
        user.setRole(newRole);
        User savedUser = userRepository.save(user);
        
        // Log the role change
        logAuditEvent("ROLE_UPDATED", "User", userId, 
                     "Role: " + oldRole, "Role: " + newRole, updatedBy);
        
        return savedUser;
    }
    
    /**
     * Activate or deactivate a user.
     * @param userId the user ID
     * @param active the active status
     * @param updatedBy the user making the change
     * @return the updated user
     * @throws IllegalArgumentException if user not found
     */
    public User setUserActive(Long userId, boolean active, User updatedBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        boolean oldActive = user.isActive();
        user.setActive(active);
        User savedUser = userRepository.save(user);
        
        // Log the status change
        String action = active ? "USER_ACTIVATED" : "USER_DEACTIVATED";
        logAuditEvent(action, "User", userId, 
                     "Active: " + oldActive, "Active: " + active, updatedBy);
        
        return savedUser;
    }
    
    /**
     * Update user's last login timestamp.
     * @param userId the user ID
     * @return the updated user
     */
    public User updateLastLogin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        user.setLastLogin(LocalDateTime.now());
        return userRepository.save(user);
    }
    
    /**
     * Change user password.
     * @param userId the user ID
     * @param newPassword the new plain text password
     * @param updatedBy the user making the change
     * @return the updated user
     * @throws IllegalArgumentException if user not found or password invalid
     */
    public User changePassword(Long userId, String newPassword, User updatedBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        // Validate password complexity
        validatePassword(newPassword);
        
        user.setPassword(passwordEncoder.encode(newPassword));
        User savedUser = userRepository.save(user);
        
        // Log the password change
        logAuditEvent("PASSWORD_CHANGED", "User", userId, null, "Password updated", updatedBy);
        
        return savedUser;
    }
    
    /**
     * Find user by username.
     * @param username the username
     * @return Optional containing the user if found
     */
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    /**
     * Find active user by username.
     * @param username the username
     * @return Optional containing the active user if found
     */
    @Transactional(readOnly = true)
    public Optional<User> findActiveUserByUsername(String username) {
        return userRepository.findByUsernameAndActive(username, true);
    }
    
    /**
     * Find user by email.
     * @param email the email address
     * @return Optional containing the user if found
     */
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    /**
     * Find user by ID.
     * @param id the user ID
     * @return Optional containing the user if found
     */
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
    
    /**
     * Find all users by role.
     * @param role the user role
     * @return List of users with the specified role
     */
    @Transactional(readOnly = true)
    public List<User> findByRole(UserRole role) {
        return userRepository.findByRole(role);
    }
    
    /**
     * Find all active users by role.
     * @param role the user role
     * @return List of active users with the specified role
     */
    @Transactional(readOnly = true)
    public List<User> findActiveUsersByRole(UserRole role) {
        return userRepository.findByRoleAndActive(role, true);
    }
    
    /**
     * Find all active users.
     * @return List of active users
     */
    @Transactional(readOnly = true)
    public List<User> findActiveUsers() {
        return userRepository.findByActive(true);
    }
    
    /**
     * Find all active sales users.
     * @return List of active sales users
     */
    @Transactional(readOnly = true)
    public List<User> findActiveSalesUsers() {
        return userRepository.findActiveSalesUsers();
    }
    
    /**
     * Search users by name.
     * @param searchTerm the search term
     * @return List of users matching the search term
     */
    @Transactional(readOnly = true)
    public List<User> searchUsersByName(String searchTerm) {
        return userRepository.findByNameContaining(searchTerm);
    }
    
    /**
     * Find users who haven't logged in recently.
     * @param daysAgo number of days to look back
     * @return List of users with old last login
     */
    @Transactional(readOnly = true)
    public List<User> findUsersWithOldLastLogin(int daysAgo) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysAgo);
        return userRepository.findUsersWithOldLastLogin(cutoffDate);
    }
    
    /**
     * Count active users by role.
     * @param role the user role
     * @return count of active users with the specified role
     */
    @Transactional(readOnly = true)
    public long countActiveUsersByRole(UserRole role) {
        return userRepository.countByRoleAndActive(role, true);
    }
    
    /**
     * Count total active users.
     * @return count of active users
     */
    @Transactional(readOnly = true)
    public long countActiveUsers() {
        return userRepository.countByActive(true);
    }
    
    /**
     * Validate user input data.
     */
    private void validateUserInput(String username, String password, String email, 
                                  String firstName, String lastName, UserRole role) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (username.length() < 3 || username.length() > 50) {
            throw new IllegalArgumentException("Username must be between 3 and 50 characters");
        }
        
        validatePassword(password);
        
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$")) {
            throw new IllegalArgumentException("Email must be valid");
        }
        
        if (firstName == null || firstName.trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required");
        }
        if (firstName.length() > 50) {
            throw new IllegalArgumentException("First name must not exceed 50 characters");
        }
        
        if (lastName == null || lastName.trim().isEmpty()) {
            throw new IllegalArgumentException("Last name is required");
        }
        if (lastName.length() > 50) {
            throw new IllegalArgumentException("Last name must not exceed 50 characters");
        }
        
        if (role == null) {
            throw new IllegalArgumentException("User role is required");
        }
    }
    
    /**
     * Validate password complexity.
     */
    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$")) {
            throw new IllegalArgumentException("Password must contain at least one lowercase letter, one uppercase letter, and one number");
        }
    }
    
    /**
     * Log audit event.
     */
    private void logAuditEvent(String action, String entityType, Long entityId, 
                              String oldValues, String newValues, User user) {
        AuditLog auditLog = new AuditLog(action, entityType, entityId, oldValues, newValues, user);
        auditLogRepository.save(auditLog);
    }
}