package com.pasp.ict.salescrm.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.pasp.ict.salescrm.entity.AuditLog;
import com.pasp.ict.salescrm.entity.User;

/**
 * Repository interface for AuditLog entity operations.
 * Provides custom query methods for system change tracking and security monitoring.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    /**
     * Find audit logs by user.
     * @param user the user who performed the actions
     * @return List of audit logs for the user
     */
    List<AuditLog> findByUser(User user);
    
    /**
     * Find audit logs by action.
     * @param action the action performed
     * @return List of audit logs for the specified action
     */
    List<AuditLog> findByAction(String action);
    
    /**
     * Find audit logs by entity type.
     * @param entityType the type of entity that was modified
     * @return List of audit logs for the specified entity type
     */
    List<AuditLog> findByEntityType(String entityType);
    
    /**
     * Find audit logs by entity type and entity ID.
     * @param entityType the type of entity
     * @param entityId the ID of the entity
     * @return List of audit logs for the specific entity
     */
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId);
    
    /**
     * Find audit logs within a date range.
     * @param startDate the start date
     * @param endDate the end date
     * @return List of audit logs within the date range
     */
    @Query("SELECT al FROM AuditLog al WHERE al.timestamp BETWEEN :startDate AND :endDate")
    List<AuditLog> findAuditLogsInDateRange(@Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find audit logs by user within a date range.
     * @param user the user
     * @param startDate the start date
     * @param endDate the end date
     * @return List of audit logs by the user within the date range
     */
    @Query("SELECT al FROM AuditLog al WHERE al.user = :user AND al.timestamp BETWEEN :startDate AND :endDate")
    List<AuditLog> findUserAuditLogsInDateRange(@Param("user") User user,
                                               @Param("startDate") LocalDateTime startDate, 
                                               @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find audit logs by action within a date range.
     * @param action the action
     * @param startDate the start date
     * @param endDate the end date
     * @return List of audit logs for the action within the date range
     */
    @Query("SELECT al FROM AuditLog al WHERE al.action = :action AND al.timestamp BETWEEN :startDate AND :endDate")
    List<AuditLog> findActionAuditLogsInDateRange(@Param("action") String action,
                                                 @Param("startDate") LocalDateTime startDate, 
                                                 @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find audit logs by entity type within a date range.
     * @param entityType the entity type
     * @param startDate the start date
     * @param endDate the end date
     * @return List of audit logs for the entity type within the date range
     */
    @Query("SELECT al FROM AuditLog al WHERE al.entityType = :entityType AND al.timestamp BETWEEN :startDate AND :endDate")
    List<AuditLog> findEntityAuditLogsInDateRange(@Param("entityType") String entityType,
                                                 @Param("startDate") LocalDateTime startDate, 
                                                 @Param("endDate") LocalDateTime endDate);
    
    /**
     * Search audit logs by IP address.
     * @param ipAddress the IP address
     * @return List of audit logs from the specified IP address
     */
    List<AuditLog> findByIpAddress(String ipAddress);
    
    /**
     * Search audit logs by user agent pattern.
     * @param userAgentPattern the user agent pattern
     * @return List of audit logs with matching user agents
     */
    @Query("SELECT al FROM AuditLog al WHERE al.userAgent LIKE :userAgentPattern")
    List<AuditLog> findByUserAgentPattern(@Param("userAgentPattern") String userAgentPattern);
    
    /**
     * Count audit logs by user.
     * @param user the user
     * @return count of audit logs for the user
     */
    long countByUser(User user);
    
    /**
     * Count audit logs by action.
     * @param action the action
     * @return count of audit logs for the action
     */
    long countByAction(String action);
    
    /**
     * Count audit logs by entity type.
     * @param entityType the entity type
     * @return count of audit logs for the entity type
     */
    long countByEntityType(String entityType);
    
    /**
     * Find recent audit logs (last N hours).
     * @param cutoffDate the cutoff date
     * @return List of recent audit logs
     */
    @Query("SELECT al FROM AuditLog al WHERE al.timestamp >= :cutoffDate ORDER BY al.timestamp DESC")
    List<AuditLog> findRecentAuditLogs(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Find audit logs ordered by timestamp (most recent first).
     * @return List of audit logs ordered by timestamp descending
     */
    @Query("SELECT al FROM AuditLog al ORDER BY al.timestamp DESC")
    List<AuditLog> findAllOrderByTimestampDesc();
    
    /**
     * Find audit logs by multiple actions.
     * @param actions the list of actions
     * @return List of audit logs with any of the specified actions
     */
    @Query("SELECT al FROM AuditLog al WHERE al.action IN :actions")
    List<AuditLog> findByActionIn(@Param("actions") List<String> actions);
    
    /**
     * Find audit logs by multiple entity types.
     * @param entityTypes the list of entity types
     * @return List of audit logs for any of the specified entity types
     */
    @Query("SELECT al FROM AuditLog al WHERE al.entityType IN :entityTypes")
    List<AuditLog> findByEntityTypeIn(@Param("entityTypes") List<String> entityTypes);
    
    /**
     * Find audit logs with changes (old values or new values present).
     * @return List of audit logs that contain change data
     */
    @Query("SELECT al FROM AuditLog al WHERE al.oldValues IS NOT NULL OR al.newValues IS NOT NULL")
    List<AuditLog> findAuditLogsWithChanges();
    
    /**
     * Find security-related audit logs (failed login attempts, unauthorized access, etc.).
     * @return List of security-related audit logs
     */
    @Query("SELECT al FROM AuditLog al WHERE al.action IN ('LOGIN_FAILED', 'UNAUTHORIZED_ACCESS', 'ACCOUNT_LOCKED', 'PASSWORD_CHANGED')")
    List<AuditLog> findSecurityAuditLogs();
    
    /**
     * Find audit logs for a specific entity across all operations.
     * @param entityType the entity type
     * @param entityId the entity ID
     * @return List of audit logs for the entity ordered by timestamp
     */
    @Query("SELECT al FROM AuditLog al WHERE al.entityType = :entityType AND al.entityId = :entityId ORDER BY al.timestamp ASC")
    List<AuditLog> findEntityHistory(@Param("entityType") String entityType, @Param("entityId") Long entityId);
}