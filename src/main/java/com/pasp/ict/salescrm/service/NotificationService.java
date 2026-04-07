package com.pasp.ict.salescrm.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.pasp.ict.salescrm.entity.User;
import com.pasp.ict.salescrm.entity.UserRole;
import com.pasp.ict.salescrm.repository.UserRepository;

/**
 * Service for sending notifications to administrators.
 * Handles security incident alerts, backup notifications, and system alerts.
 */
@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Value("${app.notification.enabled:true}")
    private boolean notificationEnabled;
    
    @Value("${app.notification.from-email:noreply@salescrm.com}")
    private String fromEmail;
    
    @Value("${app.notification.system-name:Sales CRM System}")
    private String systemName;
    
    @Value("${app.notification.admin-emails:}")
    private String adminEmails;
    
    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    public NotificationService(JavaMailSender mailSender, UserRepository userRepository) {
        this.mailSender = mailSender;
        this.userRepository = userRepository;
    }

    /**
     * Notifies administrators of security incidents.
     * 
     * @param incidentType The type of security incident
     * @param details Details about the incident
     * @param username The username involved (if any)
     * @param ipAddress The IP address involved
     */
    public void notifySecurityIncident(String incidentType, String details, String username, String ipAddress) {
        if (!notificationEnabled) {
            logger.debug("Notifications disabled, skipping security incident notification");
            return;
        }
        
        logger.info("Sending security incident notification: {} for user: {} from IP: {}", 
                   incidentType, username, ipAddress);
        
        String subject = String.format("[SECURITY ALERT] %s - %s", systemName, incidentType);
        String body = buildSecurityIncidentBody(incidentType, details, username, ipAddress);
        
        sendNotificationToAdmins(subject, body);
    }

    /**
     * Notifies administrators of failed login attempts.
     * 
     * @param username The username that failed to login
     * @param ipAddress The IP address of the attempt
     * @param attemptCount The number of consecutive failed attempts
     */
    public void notifyFailedLogin(String username, String ipAddress, int attemptCount) {
        if (!notificationEnabled) {
            return;
        }
        
        // Only notify after multiple failed attempts to reduce noise
        if (attemptCount >= 3) {
            String subject = String.format("[SECURITY ALERT] %s - Multiple Failed Login Attempts", systemName);
            String body = String.format(
                "Multiple failed login attempts detected:\n\n" +
                "Username: %s\n" +
                "IP Address: %s\n" +
                "Failed Attempts: %d\n" +
                "Time: %s\n\n" +
                "Please investigate this potential security threat.\n\n" +
                "System: %s",
                username, ipAddress, attemptCount, 
                LocalDateTime.now().format(TIMESTAMP_FORMAT), systemName
            );
            
            sendNotificationToAdmins(subject, body);
        }
    }

    /**
     * Notifies administrators of unauthorized access attempts.
     * 
     * @param username The username attempting unauthorized access
     * @param resource The resource they tried to access
     * @param ipAddress The IP address of the attempt
     */
    public void notifyUnauthorizedAccess(String username, String resource, String ipAddress) {
        if (!notificationEnabled) {
            return;
        }
        
        String subject = String.format("[SECURITY ALERT] %s - Unauthorized Access Attempt", systemName);
        String body = String.format(
            "Unauthorized access attempt detected:\n\n" +
            "Username: %s\n" +
            "Attempted Resource: %s\n" +
            "IP Address: %s\n" +
            "Time: %s\n\n" +
            "The user attempted to access a resource they are not authorized to view.\n\n" +
            "System: %s",
            username, resource, ipAddress, 
            LocalDateTime.now().format(TIMESTAMP_FORMAT), systemName
        );
        
        sendNotificationToAdmins(subject, body);
    }

    /**
     * Notifies administrators of successful backup completion.
     * 
     * @param backupId The backup identifier
     */
    public void notifyBackupSuccess(String backupId) {
        if (!notificationEnabled) {
            return;
        }
        
        logger.info("Sending backup success notification for: {}", backupId);
        
        String subject = String.format("[INFO] %s - Daily Backup Completed", systemName);
        String body = String.format(
            "Daily backup completed successfully:\n\n" +
            "Backup ID: %s\n" +
            "Completion Time: %s\n" +
            "Status: Success\n\n" +
            "All system data has been backed up successfully.\n\n" +
            "System: %s",
            backupId, LocalDateTime.now().format(TIMESTAMP_FORMAT), systemName
        );
        
        sendNotificationToAdmins(subject, body);
    }

    /**
     * Notifies administrators of backup failures.
     * 
     * @param errorMessage The error message from the backup failure
     */
    public void notifyBackupFailure(String errorMessage) {
        if (!notificationEnabled) {
            return;
        }
        
        logger.error("Sending backup failure notification");
        
        String subject = String.format("[CRITICAL] %s - Daily Backup Failed", systemName);
        String body = String.format(
            "Daily backup has failed:\n\n" +
            "Failure Time: %s\n" +
            "Error: %s\n\n" +
            "IMMEDIATE ACTION REQUIRED: Please investigate the backup system and resolve the issue.\n" +
            "Data protection may be compromised until backups are restored.\n\n" +
            "System: %s",
            LocalDateTime.now().format(TIMESTAMP_FORMAT), errorMessage, systemName
        );
        
        sendNotificationToAdmins(subject, body);
    }

    /**
     * Notifies administrators of suspicious activity patterns.
     * 
     * @param activityType The type of suspicious activity
     * @param details Details about the activity
     */
    public void notifySuspiciousActivity(String activityType, String details) {
        if (!notificationEnabled) {
            return;
        }
        
        String subject = String.format("[SECURITY ALERT] %s - Suspicious Activity Detected", systemName);
        String body = String.format(
            "Suspicious activity detected:\n\n" +
            "Activity Type: %s\n" +
            "Details: %s\n" +
            "Detection Time: %s\n\n" +
            "Please review system logs and investigate this activity.\n\n" +
            "System: %s",
            activityType, details, 
            LocalDateTime.now().format(TIMESTAMP_FORMAT), systemName
        );
        
        sendNotificationToAdmins(subject, body);
    }

    /**
     * Notifies administrators of system errors.
     * 
     * @param errorType The type of system error
     * @param errorMessage The error message
     */
    public void notifySystemError(String errorType, String errorMessage) {
        if (!notificationEnabled) {
            return;
        }
        
        String subject = String.format("[ERROR] %s - System Error", systemName);
        String body = String.format(
            "System error occurred:\n\n" +
            "Error Type: %s\n" +
            "Error Message: %s\n" +
            "Time: %s\n\n" +
            "Please check system logs for more details.\n\n" +
            "System: %s",
            errorType, errorMessage, 
            LocalDateTime.now().format(TIMESTAMP_FORMAT), systemName
        );
        
        sendNotificationToAdmins(subject, body);
    }

    /**
     * Sends notification to all administrators.
     * 
     * @param subject The email subject
     * @param body The email body
     */
    private void sendNotificationToAdmins(String subject, String body) {
        try {
            List<String> adminEmailList = getAdminEmails();
            
            if (adminEmailList.isEmpty()) {
                logger.warn("No admin emails configured, cannot send notification");
                return;
            }
            
            for (String adminEmail : adminEmailList) {
                sendEmail(adminEmail, subject, body);
            }
            
        } catch (Exception e) {
            logger.error("Failed to send admin notification", e);
        }
    }

    /**
     * Gets list of administrator email addresses.
     * 
     * @return List of admin email addresses
     */
    private List<String> getAdminEmails() {
        // First try configured admin emails
        if (adminEmails != null && !adminEmails.trim().isEmpty()) {
            return List.of(adminEmails.split(","));
        }
        
        // Fallback to admin users from database
        List<User> adminUsers = userRepository.findByRole(UserRole.ADMIN);
        return adminUsers.stream()
                .filter(User::isActive)
                .map(User::getEmail)
                .filter(email -> email != null && !email.trim().isEmpty())
                .toList();
    }

    /**
     * Sends an email to a specific recipient.
     * 
     * @param to The recipient email address
     * @param subject The email subject
     * @param body The email body
     */
    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to.trim());
            message.setSubject(subject);
            message.setText(body);
            
            mailSender.send(message);
            logger.info("Notification email sent to: {}", to);
            
        } catch (Exception e) {
            logger.error("Failed to send email to: {}", to, e);
        }
    }

    /**
     * Builds the body for security incident notifications.
     */
    private String buildSecurityIncidentBody(String incidentType, String details, String username, String ipAddress) {
        return String.format(
            "Security incident detected:\n\n" +
            "Incident Type: %s\n" +
            "Username: %s\n" +
            "IP Address: %s\n" +
            "Time: %s\n\n" +
            "Details:\n%s\n\n" +
            "Please investigate this security incident immediately.\n\n" +
            "System: %s",
            incidentType, 
            username != null ? username : "N/A", 
            ipAddress != null ? ipAddress : "N/A",
            LocalDateTime.now().format(TIMESTAMP_FORMAT), 
            details, 
            systemName
        );
    }

    /**
     * Tests the notification system by sending a test email.
     * 
     * @return true if test email was sent successfully
     */
    public boolean testNotification() {
        if (!notificationEnabled) {
            logger.info("Notifications are disabled");
            return false;
        }
        
        try {
            String subject = String.format("[TEST] %s - Notification System Test", systemName);
            String body = String.format(
                "This is a test notification from the %s.\n\n" +
                "If you receive this email, the notification system is working correctly.\n\n" +
                "Test Time: %s\n\n" +
                "System: %s",
                systemName, LocalDateTime.now().format(TIMESTAMP_FORMAT), systemName
            );
            
            sendNotificationToAdmins(subject, body);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to send test notification", e);
            return false;
        }
    }
}