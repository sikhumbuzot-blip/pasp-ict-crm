package com.pasp.ict.salescrm.unit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pasp.ict.salescrm.repository.AuditLogRepository;
import com.pasp.ict.salescrm.repository.CustomerRepository;
import com.pasp.ict.salescrm.repository.InteractionLogRepository;
import com.pasp.ict.salescrm.repository.LeadRepository;
import com.pasp.ict.salescrm.repository.SaleTransactionRepository;
import com.pasp.ict.salescrm.repository.UserRepository;
import com.pasp.ict.salescrm.service.BackupService;
import com.pasp.ict.salescrm.service.NotificationService;

/**
 * Unit tests for BackupService.
 */
@ExtendWith(MockitoExtension.class)
class BackupServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private CustomerRepository customerRepository;
    
    @Mock
    private LeadRepository leadRepository;
    
    @Mock
    private SaleTransactionRepository saleTransactionRepository;
    
    @Mock
    private InteractionLogRepository interactionLogRepository;
    
    @Mock
    private AuditLogRepository auditLogRepository;
    
    @Mock
    private NotificationService notificationService;
    
    private BackupService backupService;

    @BeforeEach
    void setUp() {
        backupService = new BackupService(
            userRepository,
            customerRepository,
            leadRepository,
            saleTransactionRepository,
            interactionLogRepository,
            auditLogRepository,
            notificationService
        );
    }

    @Test
    void getBackupStatus_ReturnsStatus() {
        // Act
        String status = backupService.getBackupStatus();
        
        // Assert
        assertNotNull(status);
        assertTrue(status.contains("Backup enabled") || status.contains("No backups found"));
    }

    @Test
    void verifyBackup_WithNonExistentBackup_ReturnsFalse() {
        // Act
        boolean result = backupService.verifyBackup("non-existent-backup");
        
        // Assert
        assertFalse(result); // Should return false for non-existent backup
    }

    @Test
    void createBackup_WithMockedRepositories_CreatesBackup() throws Exception {
        // Arrange
        when(userRepository.findAll()).thenReturn(new ArrayList<>());
        when(customerRepository.findAll()).thenReturn(new ArrayList<>());
        when(leadRepository.findAll()).thenReturn(new ArrayList<>());
        when(saleTransactionRepository.findAll()).thenReturn(new ArrayList<>());
        when(interactionLogRepository.findAll()).thenReturn(new ArrayList<>());
        when(auditLogRepository.findAll()).thenReturn(new ArrayList<>());
        
        when(userRepository.count()).thenReturn(0L);
        when(customerRepository.count()).thenReturn(0L);
        when(leadRepository.count()).thenReturn(0L);
        when(saleTransactionRepository.count()).thenReturn(0L);
        when(interactionLogRepository.count()).thenReturn(0L);
        when(auditLogRepository.count()).thenReturn(0L);
        
        // Act
        String backupId = backupService.createBackup();
        
        // Assert
        assertNotNull(backupId);
        assertTrue(backupId.startsWith("backup_"));
    }
}