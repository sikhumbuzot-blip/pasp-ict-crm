package com.pasp.ict.salescrm.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.pasp.ict.salescrm.entity.User;
import com.pasp.ict.salescrm.entity.UserRole;

/**
 * Repository interface for User entity operations.
 * Provides custom query methods for authentication and user management.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Find user by username for authentication.
     * @param username the username to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Find user by email address.
     * @param email the email to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Find user by username and active status for authentication.
     * @param username the username to search for
     * @param active the active status
     * @return Optional containing the user if found and active
     */
    Optional<User> findByUsernameAndActive(String username, boolean active);
    
    /**
     * Find all users by role.
     * @param role the user role to filter by
     * @return List of users with the specified role
     */
    List<User> findByRole(UserRole role);
    
    /**
     * Find all active users by role.
     * @param role the user role to filter by
     * @param active the active status
     * @return List of active users with the specified role
     */
    List<User> findByRoleAndActive(UserRole role, boolean active);
    
    /**
     * Find all active users.
     * @param active the active status
     * @return List of active users
     */
    List<User> findByActive(boolean active);
    
    /**
     * Check if username exists.
     * @param username the username to check
     * @return true if username exists, false otherwise
     */
    boolean existsByUsername(String username);
    
    /**
     * Check if email exists.
     * @param email the email to check
     * @return true if email exists, false otherwise
     */
    boolean existsByEmail(String email);
    
    /**
     * Find users who haven't logged in since a specific date.
     * @param lastLoginBefore the date threshold
     * @return List of users who haven't logged in recently
     */
    @Query("SELECT u FROM User u WHERE u.lastLogin IS NULL OR u.lastLogin < :lastLoginBefore")
    List<User> findUsersWithOldLastLogin(@Param("lastLoginBefore") LocalDateTime lastLoginBefore);
    
    /**
     * Find users created within a date range.
     * @param startDate the start date
     * @param endDate the end date
     * @return List of users created within the date range
     */
    @Query("SELECT u FROM User u WHERE u.createdAt BETWEEN :startDate AND :endDate")
    List<User> findUsersCreatedBetween(@Param("startDate") LocalDateTime startDate, 
                                      @Param("endDate") LocalDateTime endDate);
    
    /**
     * Count active users by role.
     * @param role the user role
     * @param active the active status
     * @return count of active users with the specified role
     */
    long countByRoleAndActive(UserRole role, boolean active);
    
    /**
     * Count total active users.
     * @param active the active status
     * @return count of active users
     */
    long countByActive(boolean active);
    
    /**
     * Find users by partial name match (first name or last name).
     * @param searchTerm the search term
     * @return List of users matching the search term
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<User> findByNameContaining(@Param("searchTerm") String searchTerm);
    
    /**
     * Find sales users (users with SALES role) who are active.
     * @return List of active sales users
     */
    @Query("SELECT u FROM User u WHERE u.role = 'SALES' AND u.active = true")
    List<User> findActiveSalesUsers();
}