package com.pasp.ict.salescrm.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pasp.ict.salescrm.entity.Customer;
import com.pasp.ict.salescrm.entity.Lead;
import com.pasp.ict.salescrm.entity.LeadStatus;
import com.pasp.ict.salescrm.entity.SaleTransaction;
import com.pasp.ict.salescrm.entity.User;
import com.pasp.ict.salescrm.entity.UserRole;
import com.pasp.ict.salescrm.repository.AuditLogRepository;
import com.pasp.ict.salescrm.repository.LeadRepository;
import com.pasp.ict.salescrm.repository.SaleTransactionRepository;
import com.pasp.ict.salescrm.service.SalesService;

/**
 * Unit tests for SalesService.
 */
@ExtendWith(MockitoExtension.class)
class SalesServiceTest {
    
    @Mock
    private LeadRepository leadRepository;
    
    @Mock
    private SaleTransactionRepository saleTransactionRepository;
    
    @Mock
    private AuditLogRepository auditLogRepository;
    
    @InjectMocks
    private SalesService salesService;
    
    private User salesUser;
    private Customer testCustomer;
    
    @BeforeEach
    void setUp() {
        salesUser = new User("salesuser", "encodedPassword", "sales@example.com", 
                           "Sales", "User", UserRole.SALES);
        salesUser.setId(1L);
        
        testCustomer = new Customer("Test Customer", "customer@example.com", 
                                  "123-456-7890", "Test Company", "123 Test St", salesUser);
        testCustomer.setId(1L);
    }
    
    @Test
    void createLead_ValidInput_CreatesLead() {
        // Arrange
        String title = "Test Lead";
        String description = "Test Description";
        BigDecimal estimatedValue = BigDecimal.valueOf(1000);
        
        Lead savedLead = new Lead(title, description, estimatedValue, testCustomer, salesUser);
        savedLead.setId(1L);
        
        when(leadRepository.save(any(Lead.class))).thenReturn(savedLead);
        
        // Act
        Lead result = salesService.createLead(title, description, estimatedValue, 
                                            testCustomer, salesUser, salesUser);
        
        // Assert
        assertNotNull(result);
        assertEquals(title, result.getTitle());
        assertEquals(description, result.getDescription());
        assertEquals(estimatedValue, result.getEstimatedValue());
        assertEquals(testCustomer, result.getCustomer());
        assertEquals(salesUser, result.getAssignedTo());
        assertEquals(LeadStatus.NEW, result.getStatus());
        
        verify(leadRepository).save(any(Lead.class));
        verify(auditLogRepository).save(any());
    }
    
    @Test
    void createLead_EmptyTitle_ThrowsException() {
        // Arrange
        String emptyTitle = "";
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
                    () -> salesService.createLead(emptyTitle, "Description", BigDecimal.valueOf(1000), 
                                                testCustomer, salesUser, salesUser));
        
        verify(leadRepository, never()).save(any());
    }
    
    @Test
    void createLead_NullCustomer_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
                    () -> salesService.createLead("Title", "Description", BigDecimal.valueOf(1000), 
                                                null, salesUser, salesUser));
        
        verify(leadRepository, never()).save(any());
    }
    
    @Test
    void updateLeadStatus_ValidTransition_UpdatesStatus() {
        // Arrange
        Long leadId = 1L;
        LeadStatus newStatus = LeadStatus.CONTACTED;
        
        Lead existingLead = new Lead("Test Lead", "Description", BigDecimal.valueOf(1000), 
                                   testCustomer, salesUser);
        existingLead.setId(leadId);
        existingLead.setStatus(LeadStatus.NEW);
        
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(existingLead));
        when(leadRepository.save(any(Lead.class))).thenReturn(existingLead);
        
        // Act
        Lead result = salesService.updateLeadStatus(leadId, newStatus, salesUser);
        
        // Assert
        assertEquals(newStatus, result.getStatus());
        verify(leadRepository).findById(leadId);
        verify(leadRepository).save(existingLead);
        verify(auditLogRepository).save(any());
    }
    
    @Test
    void updateLeadStatus_InvalidTransition_ThrowsException() {
        // Arrange
        Long leadId = 1L;
        LeadStatus invalidStatus = LeadStatus.CLOSED_WON; // Can't go directly from NEW to CLOSED_WON
        
        Lead existingLead = new Lead("Test Lead", "Description", BigDecimal.valueOf(1000), 
                                   testCustomer, salesUser);
        existingLead.setId(leadId);
        existingLead.setStatus(LeadStatus.NEW);
        
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(existingLead));
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
                    () -> salesService.updateLeadStatus(leadId, invalidStatus, salesUser));
        
        verify(leadRepository).findById(leadId);
        verify(leadRepository, never()).save(any());
    }
    
    @Test
    void updateLeadStatus_LeadNotFound_ThrowsException() {
        // Arrange
        Long leadId = 999L;
        when(leadRepository.findById(leadId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
                    () -> salesService.updateLeadStatus(leadId, LeadStatus.CONTACTED, salesUser));
        
        verify(leadRepository).findById(leadId);
        verify(leadRepository, never()).save(any());
    }
    
    @Test
    void convertLeadToSale_ValidLead_CreatesTransactionAndUpdatesLead() {
        // Arrange
        Long leadId = 1L;
        BigDecimal saleAmount = BigDecimal.valueOf(1500);
        LocalDateTime saleDate = LocalDateTime.now();
        String description = "Converted sale";
        
        Lead existingLead = new Lead("Test Lead", "Description", BigDecimal.valueOf(1000), 
                                   testCustomer, salesUser);
        existingLead.setId(leadId);
        existingLead.setStatus(LeadStatus.NEGOTIATION);
        
        SaleTransaction savedTransaction = new SaleTransaction(saleAmount, saleDate, description, 
                                                             testCustomer, salesUser, existingLead);
        savedTransaction.setId(1L);
        
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(existingLead));
        when(saleTransactionRepository.save(any(SaleTransaction.class))).thenReturn(savedTransaction);
        when(leadRepository.save(any(Lead.class))).thenReturn(existingLead);
        
        // Act
        SaleTransaction result = salesService.convertLeadToSale(leadId, saleAmount, saleDate, 
                                                              description, salesUser);
        
        // Assert
        assertNotNull(result);
        assertEquals(saleAmount, result.getAmount());
        assertEquals(saleDate, result.getSaleDate());
        assertEquals(description, result.getDescription());
        assertEquals(testCustomer, result.getCustomer());
        assertEquals(salesUser, result.getSalesUser());
        assertEquals(existingLead, result.getLead());
        
        // Verify lead status was updated to CLOSED_WON
        assertEquals(LeadStatus.CLOSED_WON, existingLead.getStatus());
        
        verify(leadRepository).findById(leadId);
        verify(saleTransactionRepository).save(any(SaleTransaction.class));
        verify(leadRepository).save(existingLead);
        verify(auditLogRepository, times(2)).save(any()); // One for sale, one for lead update
    }
    
    @Test
    void convertLeadToSale_ClosedLead_ThrowsException() {
        // Arrange
        Long leadId = 1L;
        
        Lead closedLead = new Lead("Test Lead", "Description", BigDecimal.valueOf(1000), 
                                 testCustomer, salesUser);
        closedLead.setId(leadId);
        closedLead.setStatus(LeadStatus.CLOSED_LOST);
        
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(closedLead));
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
                    () -> salesService.convertLeadToSale(leadId, BigDecimal.valueOf(1500), 
                                                       LocalDateTime.now(), "Description", salesUser));
        
        verify(leadRepository).findById(leadId);
        verify(saleTransactionRepository, never()).save(any());
    }
    
    @Test
    void createDirectSale_ValidInput_CreatesSaleTransaction() {
        // Arrange
        BigDecimal saleAmount = BigDecimal.valueOf(2000);
        LocalDateTime saleDate = LocalDateTime.now();
        String description = "Direct sale";
        
        SaleTransaction savedTransaction = new SaleTransaction(saleAmount, saleDate, description, 
                                                             testCustomer, salesUser, null);
        savedTransaction.setId(1L);
        
        when(saleTransactionRepository.save(any(SaleTransaction.class))).thenReturn(savedTransaction);
        
        // Act
        SaleTransaction result = salesService.createDirectSale(saleAmount, saleDate, description, 
                                                             testCustomer, salesUser);
        
        // Assert
        assertNotNull(result);
        assertEquals(saleAmount, result.getAmount());
        assertEquals(saleDate, result.getSaleDate());
        assertEquals(description, result.getDescription());
        assertEquals(testCustomer, result.getCustomer());
        assertEquals(salesUser, result.getSalesUser());
        assertNull(result.getLead()); // Direct sale should not have lead reference
        
        verify(saleTransactionRepository).save(any(SaleTransaction.class));
        verify(auditLogRepository).save(any());
    }
    
    @Test
    void createDirectSale_InvalidAmount_ThrowsException() {
        // Arrange
        BigDecimal invalidAmount = BigDecimal.valueOf(-100);
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
                    () -> salesService.createDirectSale(invalidAmount, LocalDateTime.now(), 
                                                       "Description", testCustomer, salesUser));
        
        verify(saleTransactionRepository, never()).save(any());
    }
    
    @Test
    void createDirectSale_NullCustomer_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
                    () -> salesService.createDirectSale(BigDecimal.valueOf(1000), LocalDateTime.now(), 
                                                       "Description", null, salesUser));
        
        verify(saleTransactionRepository, never()).save(any());
    }
    
    @Test
    void calculateSalesMetrics_NoFilters_ReturnsCorrectMetrics() {
        // Arrange
        BigDecimal totalRevenue = BigDecimal.valueOf(5000);
        long totalSales = 3L;
        long totalLeads = 10L;
        long wonLeads = 2L;
        
        when(saleTransactionRepository.calculateTotalRevenue()).thenReturn(totalRevenue);
        when(saleTransactionRepository.count()).thenReturn(totalSales);
        when(leadRepository.count()).thenReturn(totalLeads);
        when(leadRepository.countByStatus(LeadStatus.CLOSED_WON)).thenReturn(wonLeads);
        
        // Act
        Map<String, Object> result = salesService.calculateSalesMetrics(null, null, null);
        
        // Assert
        assertEquals(totalRevenue, result.get("totalRevenue"));
        assertEquals(totalSales, result.get("totalSales"));
        assertEquals(totalLeads, result.get("totalLeads"));
        assertEquals(wonLeads, result.get("wonLeads"));
        
        // Check conversion rate calculation
        double expectedConversionRate = (double) wonLeads / totalLeads * 100;
        assertEquals(expectedConversionRate, (Double) result.get("conversionRate"), 0.01);
        
        verify(saleTransactionRepository).calculateTotalRevenue();
        verify(saleTransactionRepository).count();
        verify(leadRepository).count();
        verify(leadRepository).countByStatus(LeadStatus.CLOSED_WON);
    }
    
    @Test
    void calculateSalesMetrics_WithUserId_ReturnsUserSpecificMetrics() {
        // Arrange
        Long userId = 1L;
        BigDecimal userRevenue = BigDecimal.valueOf(2000);
        long userSales = 2L;
        long userLeads = 5L;
        long userWonLeads = 1L;
        
        when(saleTransactionRepository.calculateRevenueByUser(any(User.class))).thenReturn(userRevenue);
        when(saleTransactionRepository.countBySalesUser(any(User.class))).thenReturn(userSales);
        when(leadRepository.countByAssignedTo(any(User.class))).thenReturn(userLeads);
        when(leadRepository.countByStatusAndAssignedTo(eq(LeadStatus.CLOSED_WON), any(User.class))).thenReturn(userWonLeads);
        
        // Act
        Map<String, Object> result = salesService.calculateSalesMetrics(userId, null, null);
        
        // Assert
        assertEquals(userRevenue, result.get("totalRevenue"));
        assertEquals(userSales, result.get("totalSales"));
        assertEquals(userLeads, result.get("totalLeads"));
        assertEquals(userWonLeads, result.get("wonLeads"));
        
        // Check conversion rate calculation
        double expectedConversionRate = (double) userWonLeads / userLeads * 100;
        assertEquals(expectedConversionRate, (Double) result.get("conversionRate"), 0.01);
        
        verify(saleTransactionRepository).calculateRevenueByUser(any(User.class));
        verify(saleTransactionRepository).countBySalesUser(any(User.class));
        verify(leadRepository).countByAssignedTo(any(User.class));
        verify(leadRepository).countByStatusAndAssignedTo(eq(LeadStatus.CLOSED_WON), any(User.class));
    }
}