package com.pasp.ict.salescrm.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pasp.ict.salescrm.entity.LeadStatus;
import com.pasp.ict.salescrm.entity.User;
import com.pasp.ict.salescrm.entity.UserRole;
import com.pasp.ict.salescrm.repository.AuditLogRepository;
import com.pasp.ict.salescrm.repository.CustomerRepository;
import com.pasp.ict.salescrm.repository.InteractionLogRepository;
import com.pasp.ict.salescrm.repository.LeadRepository;
import com.pasp.ict.salescrm.repository.SaleTransactionRepository;
import com.pasp.ict.salescrm.repository.UserRepository;
import com.pasp.ict.salescrm.service.AdminService;
import com.pasp.ict.salescrm.service.UserService;

/**
 * Unit tests for AdminService.
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceTest {
    
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
    private UserService userService;
    
    @InjectMocks
    private AdminService adminService;
    
    private User adminUser;
    private User salesUser;
    
    @BeforeEach
    void setUp() {
        adminUser = new User("admin", "encodedPassword", "admin@example.com", 
                           "Admin", "User", UserRole.ADMIN);
        adminUser.setId(1L);
        
        salesUser = new User("salesuser", "encodedPassword", "sales@example.com", 
                           "Sales", "User", UserRole.SALES);
        salesUser.setId(2L);
    }
    
    @Test
    void createUser_ValidAdminUser_CreatesUser() {
        // Arrange
        String username = "newuser";
        String password = "Password123";
        String email = "newuser@example.com";
        String firstName = "New";
        String lastName = "User";
        UserRole role = UserRole.SALES;
        
        User createdUser = new User(username, "encodedPassword", email, firstName, lastName, role);
        createdUser.setId(3L);
        
        when(userService.createUser(username, password, email, firstName, lastName, role, adminUser))
            .thenReturn(createdUser);
        
        // Act
        User result = adminService.createUser(username, password, email, firstName, lastName, role, adminUser);
        
        // Assert
        assertNotNull(result);
        assertEquals(username, result.getUsername());
        assertEquals(email, result.getEmail());
        assertEquals(role, result.getRole());
        
        verify(userService).createUser(username, password, email, firstName, lastName, role, adminUser);
    }
    
    @Test
    void createUser_NonAdminUser_ThrowsException() {
        // Act & Assert
        assertThrows(SecurityException.class, 
                    () -> adminService.createUser("username", "password", "email@example.com", 
                                                 "First", "Last", UserRole.SALES, salesUser));
        
        verify(userService, never()).createUser(anyString(), anyString(), anyString(), 
                                               anyString(), anyString(), any(UserRole.class), any(User.class));
    }
    
    @Test
    void updateUserRole_ValidAdminUser_UpdatesRole() {
        // Arrange
        Long userId = 2L;
        UserRole newRole = UserRole.ADMIN;
        
        User updatedUser = new User("salesuser", "encodedPassword", "sales@example.com", 
                                  "Sales", "User", newRole);
        updatedUser.setId(userId);
        
        when(userService.updateUserRole(userId, newRole, adminUser)).thenReturn(updatedUser);
        
        // Act
        User result = adminService.updateUserRole(userId, newRole, adminUser);
        
        // Assert
        assertEquals(newRole, result.getRole());
        verify(userService).updateUserRole(userId, newRole, adminUser);
    }
    
    @Test
    void updateUserRole_NonAdminUser_ThrowsException() {
        // Act & Assert
        assertThrows(SecurityException.class, 
                    () -> adminService.updateUserRole(2L, UserRole.ADMIN, salesUser));
        
        verify(userService, never()).updateUserRole(anyLong(), any(UserRole.class), any(User.class));
    }
    
    @Test
    void setUserActive_ValidAdminUser_UpdatesActiveStatus() {
        // Arrange
        Long userId = 2L;
        boolean active = false;
        
        User updatedUser = new User("salesuser", "encodedPassword", "sales@example.com", 
                                  "Sales", "User", UserRole.SALES);
        updatedUser.setId(userId);
        updatedUser.setActive(active);
        
        when(userService.setUserActive(userId, active, adminUser)).thenReturn(updatedUser);
        
        // Act
        User result = adminService.setUserActive(userId, active, adminUser);
        
        // Assert
        assertEquals(active, result.isActive());
        verify(userService).setUserActive(userId, active, adminUser);
    }
    
    @Test
    void setUserActive_NonAdminUser_ThrowsException() {
        // Act & Assert
        assertThrows(SecurityException.class, 
                    () -> adminService.setUserActive(2L, false, salesUser));
        
        verify(userService, never()).setUserActive(anyLong(), anyBoolean(), any(User.class));
    }
    
    @Test
    void getSystemStatistics_ReturnsComprehensiveStats() {
        // Arrange
        when(userRepository.count()).thenReturn(10L);
        when(userRepository.countByActive(true)).thenReturn(8L);
        when(userRepository.countByRoleAndActive(UserRole.ADMIN, true)).thenReturn(2L);
        when(userRepository.countByRoleAndActive(UserRole.SALES, true)).thenReturn(4L);
        when(userRepository.countByRoleAndActive(UserRole.REGULAR, true)).thenReturn(2L);
        
        when(customerRepository.count()).thenReturn(50L);
        
        when(leadRepository.count()).thenReturn(100L);
        when(leadRepository.findOpenLeads()).thenReturn(Arrays.asList(/* mock leads */));
        when(leadRepository.findClosedLeads()).thenReturn(Arrays.asList(/* mock leads */));
        when(leadRepository.countByStatus(LeadStatus.CLOSED_WON)).thenReturn(30L);
        when(leadRepository.countByStatus(LeadStatus.CLOSED_LOST)).thenReturn(20L);
        
        when(saleTransactionRepository.count()).thenReturn(75L);
        when(saleTransactionRepository.calculateTotalRevenue()).thenReturn(BigDecimal.valueOf(150000));
        when(saleTransactionRepository.calculateAverageSaleAmount()).thenReturn(BigDecimal.valueOf(2000));
        
        when(interactionLogRepository.count()).thenReturn(500L);
        when(auditLogRepository.count()).thenReturn(1000L);
        
        // Act
        Map<String, Object> result = adminService.getSystemStatistics();
        
        // Assert
        assertNotNull(result);
        assertEquals(10L, result.get("totalUsers"));
        assertEquals(8L, result.get("activeUsers"));
        assertEquals(2L, result.get("adminUsers"));
        assertEquals(4L, result.get("salesUsers"));
        assertEquals(2L, result.get("regularUsers"));
        assertEquals(50L, result.get("totalCustomers"));
        assertEquals(100L, result.get("totalLeads"));
        assertEquals(30L, result.get("wonLeads"));
        assertEquals(20L, result.get("lostLeads"));
        assertEquals(75L, result.get("totalSales"));
        assertEquals(BigDecimal.valueOf(150000), result.get("totalRevenue"));
        assertEquals(BigDecimal.valueOf(2000), result.get("averageSaleAmount"));
        assertEquals(500L, result.get("totalInteractions"));
        assertEquals(1000L, result.get("totalAuditLogs"));
        
        // Verify all repository calls
        verify(userRepository).count();
        verify(userRepository).countByActive(true);
        verify(userRepository, times(3)).countByRoleAndActive(any(UserRole.class), eq(true));
        verify(customerRepository).count();
        verify(leadRepository).count();
        verify(leadRepository).findOpenLeads();
        verify(leadRepository).findClosedLeads();
        verify(leadRepository, times(2)).countByStatus(any(LeadStatus.class));
        verify(saleTransactionRepository).count();
        verify(saleTransactionRepository).calculateTotalRevenue();
        verify(saleTransactionRepository).calculateAverageSaleAmount();
        verify(interactionLogRepository).count();
        verify(auditLogRepository).count();
    }
    
    @Test
    void getPerformanceMetrics_ReturnsTimeBasedMetrics() {
        // Arrange
        BigDecimal monthlyRevenue = BigDecimal.valueOf(50000);
        BigDecimal weeklyRevenue = BigDecimal.valueOf(12000);
        BigDecimal dailyRevenue = BigDecimal.valueOf(2000);
        
        when(saleTransactionRepository.calculateRevenueInDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(monthlyRevenue, weeklyRevenue, dailyRevenue);
        
        when(saleTransactionRepository.countSalesInDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(25L, 6L, 1L);
        
        when(customerRepository.findCustomersCreatedBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(/* mock customers */), Arrays.asList(/* mock customers */), Arrays.asList(/* mock customers */));
        
        when(leadRepository.findLeadsCreatedBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(/* mock leads */), Arrays.asList(/* mock leads */), Arrays.asList(/* mock leads */));
        
        when(leadRepository.count()).thenReturn(100L);
        when(leadRepository.countByStatus(LeadStatus.CLOSED_WON)).thenReturn(30L);
        
        // Act
        Map<String, Object> result = adminService.getPerformanceMetrics();
        
        // Assert
        assertNotNull(result);
        assertEquals(monthlyRevenue, result.get("monthlyRevenue"));
        assertEquals(weeklyRevenue, result.get("weeklyRevenue"));
        assertEquals(dailyRevenue, result.get("dailyRevenue"));
        assertEquals(25L, result.get("monthlySales"));
        assertEquals(6L, result.get("weeklySales"));
        assertEquals(1L, result.get("dailySales"));
        
        // Check conversion rate calculation
        double expectedConversionRate = 30.0; // 30/100 * 100
        assertEquals(expectedConversionRate, (Double) result.get("overallConversionRate"), 0.01);
        
        // Verify repository calls
        verify(saleTransactionRepository, times(3)).calculateRevenueInDateRange(any(LocalDateTime.class), any(LocalDateTime.class));
        verify(saleTransactionRepository, times(3)).countSalesInDateRange(any(LocalDateTime.class), any(LocalDateTime.class));
        verify(customerRepository, times(3)).findCustomersCreatedBetween(any(LocalDateTime.class), any(LocalDateTime.class));
        verify(leadRepository, times(3)).findLeadsCreatedBetween(any(LocalDateTime.class), any(LocalDateTime.class));
    }
    
    @Test
    void getSalesPerformanceByUser_ReturnsUserPerformanceData() {
        // Arrange
        List<User> salesUsers = Arrays.asList(salesUser);
        when(userRepository.findActiveSalesUsers()).thenReturn(salesUsers);
        
        BigDecimal userRevenue = BigDecimal.valueOf(25000);
        when(saleTransactionRepository.calculateRevenueByUser(salesUser)).thenReturn(userRevenue);
        when(saleTransactionRepository.countBySalesUser(salesUser)).thenReturn(10L);
        when(saleTransactionRepository.calculateAverageSaleAmountByUser(salesUser)).thenReturn(BigDecimal.valueOf(2500));
        
        when(leadRepository.countByAssignedTo(salesUser)).thenReturn(20L);
        when(leadRepository.countByStatusAndAssignedTo(LeadStatus.CLOSED_WON, salesUser)).thenReturn(8L);
        
        // Act
        Map<String, Object> result = adminService.getSalesPerformanceByUser();
        
        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey(salesUser.getUsername()));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> userStats = (Map<String, Object>) result.get(salesUser.getUsername());
        assertEquals(userRevenue, userStats.get("totalRevenue"));
        assertEquals(10L, userStats.get("totalSales"));
        assertEquals(BigDecimal.valueOf(2500), userStats.get("averageSaleAmount"));
        assertEquals(20L, userStats.get("assignedLeads"));
        assertEquals(8L, userStats.get("wonLeads"));
        
        // Check conversion rate calculation
        double expectedConversionRate = 40.0; // 8/20 * 100
        assertEquals(expectedConversionRate, (Double) userStats.get("conversionRate"), 0.01);
        
        verify(userRepository).findActiveSalesUsers();
        verify(saleTransactionRepository).calculateRevenueByUser(salesUser);
        verify(saleTransactionRepository).countBySalesUser(salesUser);
        verify(saleTransactionRepository).calculateAverageSaleAmountByUser(salesUser);
        verify(leadRepository).countByAssignedTo(salesUser);
        verify(leadRepository).countByStatusAndAssignedTo(LeadStatus.CLOSED_WON, salesUser);
    }
    
    @Test
    void getSystemHealth_ReturnsHealthIndicators() {
        // Arrange
        when(auditLogRepository.findAuditLogsInDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(/* mock audit logs */));
        when(interactionLogRepository.findInteractionsInDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(/* mock interactions */));
        when(saleTransactionRepository.countSalesInDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(5L);
        
        when(userRepository.findUsersWithOldLastLogin(any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(/* mock users */));
        when(userRepository.countByActive(true)).thenReturn(10L);
        
        when(interactionLogRepository.findCustomersWithNoRecentInteractions(any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(/* mock customers */));
        when(leadRepository.findStaleLeads(any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(/* mock leads */));
        
        // Act
        Map<String, Object> result = adminService.getSystemHealth();
        
        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("recentAuditActivity"));
        assertTrue(result.containsKey("recentUserActivity"));
        assertTrue(result.containsKey("recentSalesActivity"));
        assertTrue(result.containsKey("userActivityRate"));
        assertTrue(result.containsKey("totalActiveUsers"));
        assertTrue(result.containsKey("customersWithoutRecentInteractions"));
        assertTrue(result.containsKey("staleLeads"));
        assertTrue(result.containsKey("overallHealthScore"));
        
        // Verify the health score is within valid range
        Double healthScore = (Double) result.get("overallHealthScore");
        assertTrue(healthScore >= 0.0 && healthScore <= 100.0);
        
        verify(auditLogRepository).findAuditLogsInDateRange(any(LocalDateTime.class), any(LocalDateTime.class));
        verify(interactionLogRepository).findInteractionsInDateRange(any(LocalDateTime.class), any(LocalDateTime.class));
        verify(saleTransactionRepository).countSalesInDateRange(any(LocalDateTime.class), any(LocalDateTime.class));
        verify(userRepository).findUsersWithOldLastLogin(any(LocalDateTime.class));
        verify(userRepository).countByActive(true);
        verify(interactionLogRepository).findCustomersWithNoRecentInteractions(any(LocalDateTime.class));
        verify(leadRepository).findStaleLeads(any(LocalDateTime.class));
    }
    
    @Test
    void getRecentSecurityEvents_ReturnsSecurityAuditLogs() {
        // Arrange
        when(auditLogRepository.findSecurityAuditLogs()).thenReturn(Arrays.asList(/* mock audit logs */));
        
        // Act
        var result = adminService.getRecentSecurityEvents();
        
        // Assert
        assertNotNull(result);
        verify(auditLogRepository).findSecurityAuditLogs();
    }
    
    @Test
    void getRecentSystemActivity_ReturnsRecentAuditLogs() {
        // Arrange
        int hours = 24;
        when(auditLogRepository.findRecentAuditLogs(any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(/* mock audit logs */));
        
        // Act
        var result = adminService.getRecentSystemActivity(hours);
        
        // Assert
        assertNotNull(result);
        verify(auditLogRepository).findRecentAuditLogs(any(LocalDateTime.class));
    }
    
    @Test
    void getUserActivitySummary_ValidUserId_ReturnsActivitySummary() {
        // Arrange
        Long userId = 2L;
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(salesUser));
        when(auditLogRepository.countByUser(salesUser)).thenReturn(50L);
        when(interactionLogRepository.countByUser(salesUser)).thenReturn(100L);
        when(saleTransactionRepository.countBySalesUser(salesUser)).thenReturn(15L);
        when(saleTransactionRepository.calculateRevenueByUser(salesUser)).thenReturn(BigDecimal.valueOf(30000));
        when(leadRepository.countByAssignedTo(salesUser)).thenReturn(25L);
        when(customerRepository.countByCreatedBy(salesUser)).thenReturn(10L);
        
        // Act
        Map<String, Object> result = adminService.getUserActivitySummary(userId);
        
        // Assert
        assertNotNull(result);
        assertEquals(salesUser.getUsername(), result.get("username"));
        assertEquals(salesUser.getRole(), result.get("role"));
        assertEquals(salesUser.isActive(), result.get("active"));
        assertEquals(50L, result.get("auditLogCount"));
        assertEquals(100L, result.get("interactionCount"));
        assertEquals(15L, result.get("salesCount"));
        assertEquals(BigDecimal.valueOf(30000), result.get("totalRevenue"));
        assertEquals(25L, result.get("assignedLeads"));
        assertEquals(10L, result.get("customersCreated"));
        
        verify(userRepository).findById(userId);
        verify(auditLogRepository).countByUser(salesUser);
        verify(interactionLogRepository).countByUser(salesUser);
        verify(saleTransactionRepository).countBySalesUser(salesUser);
        verify(saleTransactionRepository).calculateRevenueByUser(salesUser);
        verify(leadRepository).countByAssignedTo(salesUser);
        verify(customerRepository).countByCreatedBy(salesUser);
    }
    
    @Test
    void getUserActivitySummary_UserNotFound_ThrowsException() {
        // Arrange
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.empty());
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
                    () -> adminService.getUserActivitySummary(userId));
        
        verify(userRepository).findById(userId);
    }
}