package com.pasp.ict.salescrm.security;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.pasp.ict.salescrm.entity.AuditLog;
import com.pasp.ict.salescrm.entity.User;
import com.pasp.ict.salescrm.repository.AuditLogRepository;
import com.pasp.ict.salescrm.service.NotificationService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Service for logging security-related events and audit trails.
 * Provides comprehensive security monitoring and incident tracking.
 */
@Service
public class SecurityAuditService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityAuditService.class);
    
    private final AuditLogRepository auditLogRepository;
    private final NotificationService notificationService;

    public SecurityAuditService(AuditLogRepository auditLogRepository, NotificationService notificationService) {
        this.auditLogRepository = auditLogRepository;
        this.notificationService = notificationService;
    }

    /**
     * Logs authentication attempts (successful and failed).
     * 
     * @param username The username attempting authentication
     * @param success Whether the authentication was successful
     * @param ipAddress The IP address of the client
     * @param userAgent The user agent string
     */
    public void logAuthenticationAttempt(String username, boolean success, String ipAddress, String userAgent) {
        String action = success ? "LOGIN_SUCCESS" : "LOGIN_FAILURE";
        String details = String.format("IP: %s, UserAgent: %s", ipAddress, userAgent);
        
        if (success) {
            logger.info("Successful login for user: {} from IP: {}", username, ipAddress);
        } else {
            logger.warn("Failed login attempt for user: {} from IP: {}", username, ipAddress);
            
            // Count recent failed attempts for this username
            int failedAttempts = countRecentFailedAttempts(username);
            if (failedAttempts >= 3) {
                notificationService.notifyFailedLogin(username, ipAddress, failedAttempts);
            }
        }
        
        // Create audit log entry
        AuditLog auditLog = new AuditLog();
        auditLog.setAction(action);
        auditLog.setEntityType("USER_AUTHENTICATION");
        auditLog.setOldValues(username);
        auditLog.setNewValues(details);
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setIpAddress(ipAddress);
        auditLog.setUserAgent(userAgent);
        
        auditLogRepository.save(auditLog);
    }

    /**
     * Logs security violations (SQL injection attempts, XSS attempts, etc.).
     * 
     * @param violationType The type of security violation
     * @param details Details about the violation
     * @param user The user associated with the violation (if any)
     * @param request The HTTP request that triggered the violation
     */
    public void logSecurityViolation(String violationType, String details, User user, HttpServletRequest request) {
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        String requestUri = request.getRequestURI();
        
        logger.error("Security violation detected - Type: {}, User: {}, IP: {}, URI: {}, Details: {}", 
                    violationType, user != null ? user.getUsername() : "anonymous", 
                    ipAddress, requestUri, details);
        
        // Notify administrators of security incident
        notificationService.notifySecurityIncident(violationType, details, 
                                                  user != null ? user.getUsername() : "anonymous", 
                                                  ipAddress);
        
        // Create audit log entry
        AuditLog auditLog = new AuditLog();
        auditLog.setAction("SECURITY_VIOLATION");
        auditLog.setEntityType(violationType);
        auditLog.setOldValues(String.format("IP: %s, URI: %s, UserAgent: %s", ipAddress, requestUri, userAgent));
        auditLog.setNewValues(details);
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setIpAddress(ipAddress);
        auditLog.setUserAgent(userAgent);
        auditLog.setUser(user);
        
        auditLogRepository.save(auditLog);
    }

    /**
     * Logs unauthorized access attempts.
     * 
     * @param user The user attempting unauthorized access
     * @param resource The resource they tried to access
     * @param request The HTTP request
     */
    public void logUnauthorizedAccess(User user, String resource, HttpServletRequest request) {
        String ipAddress = getClientIpAddress(request);
        String details = String.format("Attempted access to: %s from IP: %s", resource, ipAddress);
        
        logger.warn("Unauthorized access attempt by user: {} to resource: {} from IP: {}", 
                   user.getUsername(), resource, ipAddress);
        
        // Notify administrators of unauthorized access
        notificationService.notifyUnauthorizedAccess(user.getUsername(), resource, ipAddress);
        
        // Create audit log entry
        AuditLog auditLog = new AuditLog();
        auditLog.setAction("UNAUTHORIZED_ACCESS");
        auditLog.setEntityType("ACCESS_CONTROL");
        auditLog.setOldValues(resource);
        auditLog.setNewValues(details);
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setIpAddress(ipAddress);
        auditLog.setUserAgent(request.getHeader("User-Agent"));
        auditLog.setUser(user);
        
        auditLogRepository.save(auditLog);
    }

    /**
     * Logs data access events for sensitive operations.
     * 
     * @param user The user accessing the data
     * @param action The action performed (READ, CREATE, UPDATE, DELETE)
     * @param entityType The type of entity accessed
     * @param entityId The ID of the entity
     * @param details Additional details about the operation
     */
    public void logDataAccess(User user, String action, String entityType, Long entityId, String details) {
        logger.info("Data access - User: {}, Action: {}, Entity: {} (ID: {})", 
                   user.getUsername(), action, entityType, entityId);
        
        // Create audit log entry
        AuditLog auditLog = new AuditLog();
        auditLog.setAction("DATA_ACCESS_" + action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setNewValues(details);
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setUser(user);
        
        auditLogRepository.save(auditLog);
    }

    /**
     * Logs session events (creation, timeout, invalidation).
     * 
     * @param user The user associated with the session
     * @param event The session event type
     * @param sessionId The session ID
     * @param request The HTTP request (optional)
     */
    public void logSessionEvent(User user, String event, String sessionId, HttpServletRequest request) {
        String ipAddress = request != null ? getClientIpAddress(request) : "unknown";
        String userAgent = request != null ? request.getHeader("User-Agent") : "unknown";
        String details = String.format("Session: %s, IP: %s", sessionId, ipAddress);
        
        logger.info("Session event - User: {}, Event: {}, Session: {}, IP: {}", 
                   user != null ? user.getUsername() : "anonymous", event, sessionId, ipAddress);
        
        // Create audit log entry
        AuditLog auditLog = new AuditLog();
        auditLog.setAction("SESSION_" + event);
        auditLog.setEntityType("USER_SESSION");
        auditLog.setOldValues(sessionId);
        auditLog.setNewValues(details);
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setIpAddress(ipAddress);
        auditLog.setUserAgent(userAgent);
        auditLog.setUser(user);
        
        auditLogRepository.save(auditLog);
    }

    /**
     * Logs administrative actions for compliance.
     * 
     * @param admin The administrator performing the action
     * @param action The administrative action
     * @param targetUser The user being affected (if applicable)
     * @param details Additional details about the action
     */
    public void logAdminAction(User admin, String action, User targetUser, String details) {
        logger.info("Admin action - Admin: {}, Action: {}, Target: {}, Details: {}", 
                   admin.getUsername(), action, 
                   targetUser != null ? targetUser.getUsername() : "N/A", details);
        
        // Create audit log entry
        AuditLog auditLog = new AuditLog();
        auditLog.setAction("ADMIN_" + action);
        auditLog.setEntityType("USER_MANAGEMENT");
        auditLog.setEntityId(targetUser != null ? targetUser.getId() : null);
        auditLog.setOldValues(targetUser != null ? targetUser.getUsername() : null);
        auditLog.setNewValues(details);
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setUser(admin);
        
        auditLogRepository.save(auditLog);
    }

    /**
     * Extracts the client IP address from the HTTP request.
     * Handles cases where the request comes through proxies or load balancers.
     * 
     * @param request The HTTP request
     * @return The client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * Counts recent failed login attempts for a username.
     * 
     * @param username The username to check
     * @return Number of failed attempts in the last hour
     */
    private int countRecentFailedAttempts(String username) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        List<AuditLog> recentFailures = auditLogRepository.findActionAuditLogsInDateRange(
            "LOGIN_FAILURE", oneHourAgo, LocalDateTime.now());
        
        return (int) recentFailures.stream()
            .filter(log -> username.equals(log.getOldValues()))
            .count();
    }
}