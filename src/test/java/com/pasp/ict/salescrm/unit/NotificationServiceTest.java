package com.pasp.ict.salescrm.unit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.lenient;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import com.pasp.ict.salescrm.entity.User;
import com.pasp.ict.salescrm.entity.UserRole;
import com.pasp.ict.salescrm.repository.UserRepository;
import com.pasp.ict.salescrm.service.NotificationService;

/**
 * Unit tests for NotificationService.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;
    
    @Mock
    private UserRepository userRepository;
    
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(mailSender, userRepository);
    }

    @Test
    void notifySecurityIncident_WithValidParameters_DoesNotThrow() {
        // Arrange
        List<User> adminUsers = new ArrayList<>();
        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@example.com");
        admin.setRole(UserRole.ADMIN);
        admin.setActive(true);
        adminUsers.add(admin);
        
        lenient().when(userRepository.findByRole(UserRole.ADMIN)).thenReturn(adminUsers);
        
        // Act & Assert
        assertDoesNotThrow(() -> {
            notificationService.notifySecurityIncident(
                "LOGIN_FAILURE", 
                "Multiple failed login attempts", 
                "testuser", 
                "192.168.1.1"
            );
        });
    }

    @Test
    void notifyBackupSuccess_WithValidBackupId_DoesNotThrow() {
        // Arrange
        List<User> adminUsers = new ArrayList<>();
        lenient().when(userRepository.findByRole(UserRole.ADMIN)).thenReturn(adminUsers);
        
        // Act & Assert
        assertDoesNotThrow(() -> {
            notificationService.notifyBackupSuccess("backup_2024-01-01_02-00-00");
        });
    }

    @Test
    void notifyBackupFailure_WithErrorMessage_DoesNotThrow() {
        // Arrange
        List<User> adminUsers = new ArrayList<>();
        lenient().when(userRepository.findByRole(UserRole.ADMIN)).thenReturn(adminUsers);
        
        // Act & Assert
        assertDoesNotThrow(() -> {
            notificationService.notifyBackupFailure("Database connection failed");
        });
    }
}