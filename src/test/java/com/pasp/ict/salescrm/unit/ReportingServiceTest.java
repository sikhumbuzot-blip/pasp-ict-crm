package com.pasp.ict.salescrm.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
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

import com.pasp.ict.salescrm.entity.Customer;
import com.pasp.ict.salescrm.entity.LeadStatus;
import com.pasp.ict.salescrm.entity.SaleTransaction;
import com.pasp.ict.salescrm.entity.User;
import com.pasp.ict.salescrm.entity.UserRole;
import com.pasp.ict.salescrm.repository.CustomerRepository;
import com.pasp.ict.salescrm.repository.LeadRepository;
import com.pasp.ict.salescrm.repository.SaleTransactionRepository;
import com.pasp.ict.salescrm.repository.UserRepository;
import com.pasp.ict.salescrm.service.ReportingService;

/**
 * Unit tests for ReportingService.
 */
@ExtendWith(MockitoExtension.class)
class ReportingServiceTest {
    
    @Mock
    private SaleTransactionRepository saleTransactionRepository;
    
    @Mock
    private LeadRepository leadRepository;
    
    @Mock
    private CustomerRepository customerRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private ReportingService reportingService;
    
    private User salesUser;
    private Customer testCustomer;
    private SaleTransaction testSale;
    
    @BeforeEach
    void setUp() {
        salesUser = new User("salesuser", "encodedPassword", "sales@example.com", 
                           "Sales", "User", UserRole.SALES);
        salesUser.setId(1L);
        
        testCustomer = new Customer("Test Customer", "customer@example.com", 
                                  "123-456-7890", "Test Company", "123 Test St", salesUser);
        testCustomer.setId(1L);
        
        testSale = new SaleTransaction(BigDecimal.valueOf(1000), LocalDateTime.now(), 
                                     "Test Sale", testCustomer, salesUser, null);
        testSale.setId(1L);
    }
    
    @Test
    void generateSalesReport_NoFilters_ReturnsComprehensiveReport() {
        // Arrange
        List<SaleTransaction> sales = Arrays.asList(testSale);
        when(saleTransactionRepository.findAllOrderBySaleDateDesc()).thenReturn(sales);
        
        // Act
        Map<String, Object> result = reportingService.generateSalesReport(null, null, null, null);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("generatedAt"));
        assertTrue(result.containsKey("totalRevenue"));
        assertTrue(result.containsKey("totalSales"));
        assertTrue(result.containsKey("averageSaleAmount"));
        assertTrue(result.containsKey("revenueByMonth"));
        assertTrue(result.containsKey("revenueByUser"));
        assertTrue(result.containsKey("revenueByCustomerSegment"));
        assertTrue(result.containsKey("trends"));
        
        assertEquals(0, BigDecimal.valueOf(1000).compareTo((BigDecimal) result.get("totalRevenue")));
        assertEquals(1L, result.get("totalSales"));
        assertEquals(0, BigDecimal.valueOf(1000).compareTo((BigDecimal) result.get("averageSaleAmount")));
        
        verify(saleTransactionRepository).findAllOrderBySaleDateDesc();
    }
    
    @Test
    void generateSalesReport_WithDateRange_FiltersCorrectly() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        LocalDateTime endDate = LocalDateTime.now();
        List<SaleTransaction> sales = Arrays.asList(testSale);
        
        when(saleTransactionRepository.findSalesInDateRange(startDate, endDate)).thenReturn(sales);
        
        // Act
        Map<String, Object> result = reportingService.generateSalesReport(startDate, endDate, null, null);
        
        // Assert
        assertNotNull(result);
        assertEquals(startDate, result.get("startDate"));
        assertEquals(endDate, result.get("endDate"));
        assertEquals(0, BigDecimal.valueOf(1000).compareTo((BigDecimal) result.get("totalRevenue")));
        assertEquals(1L, result.get("totalSales"));
        
        verify(saleTransactionRepository).findSalesInDateRange(startDate, endDate);
    }
    
    @Test
    void generateSalesReport_WithUserId_FiltersCorrectly() {
        // Arrange
        Long userId = 1L;
        List<SaleTransaction> sales = Arrays.asList(testSale);
        
        when(saleTransactionRepository.findBySalesUser(any(User.class))).thenReturn(sales);
        
        // Act
        Map<String, Object> result = reportingService.generateSalesReport(null, null, userId, null);
        
        // Assert
        assertNotNull(result);
        assertEquals(userId, result.get("userId"));
        assertEquals(0, BigDecimal.valueOf(1000).compareTo((BigDecimal) result.get("totalRevenue")));
        assertEquals(1L, result.get("totalSales"));
        
        verify(saleTransactionRepository).findBySalesUser(any(User.class));
    }
    
    @Test
    void calculateSalesMetrics_NoDateRange_ReturnsOverallMetrics() {
        // Arrange
        BigDecimal totalRevenue = BigDecimal.valueOf(50000);
        when(saleTransactionRepository.calculateTotalRevenue()).thenReturn(totalRevenue);
        when(saleTransactionRepository.count()).thenReturn(25L);
        
        when(leadRepository.count()).thenReturn(100L);
        when(leadRepository.countByStatus(LeadStatus.CLOSED_WON)).thenReturn(30L);
        when(leadRepository.countByStatus(LeadStatus.CLOSED_LOST)).thenReturn(20L);
        
        List<User> salesUsers = Arrays.asList(salesUser);
        when(userRepository.findActiveSalesUsers()).thenReturn(salesUsers);
        
        when(saleTransactionRepository.calculateRevenueByUser(salesUser)).thenReturn(BigDecimal.valueOf(25000));
        when(saleTransactionRepository.countBySalesUser(salesUser)).thenReturn(12L);
        when(leadRepository.countByAssignedTo(salesUser)).thenReturn(50L);
        when(leadRepository.countByStatusAndAssignedTo(LeadStatus.CLOSED_WON, salesUser)).thenReturn(15L);
        
        // Act
        Map<String, Object> result = reportingService.calculateSalesMetrics(null, null);
        
        // Assert
        assertNotNull(result);
        assertEquals(totalRevenue, result.get("totalRevenue"));
        assertEquals(25L, result.get("totalSales"));
        assertEquals(0, BigDecimal.valueOf(2000).compareTo((BigDecimal) result.get("averageSaleAmount"))); // 50000/25
        assertEquals(100L, result.get("totalLeads"));
        assertEquals(30L, result.get("wonLeads"));
        assertEquals(20L, result.get("lostLeads"));
        assertEquals(30.0, (Double) result.get("conversionRate"), 0.01); // 30/100 * 100
        assertEquals(20.0, (Double) result.get("lossRate"), 0.01); // 20/100 * 100
        
        assertTrue(result.containsKey("userPerformance"));
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> userPerformance = (Map<String, Map<String, Object>>) result.get("userPerformance");
        assertTrue(userPerformance.containsKey(salesUser.getUsername()));
        
        Map<String, Object> userMetrics = userPerformance.get(salesUser.getUsername());
        assertEquals(BigDecimal.valueOf(25000), userMetrics.get("revenue"));
        assertEquals(12L, userMetrics.get("sales"));
        assertEquals(50L, userMetrics.get("leads"));
        assertEquals(15L, userMetrics.get("wonLeads"));
        assertEquals(30.0, (Double) userMetrics.get("conversionRate"), 0.01); // 15/50 * 100
        
        verify(saleTransactionRepository).calculateTotalRevenue();
        verify(saleTransactionRepository).count();
        verify(leadRepository).count();
        verify(leadRepository, times(2)).countByStatus(any(LeadStatus.class));
        verify(userRepository).findActiveSalesUsers();
        verify(saleTransactionRepository).calculateRevenueByUser(salesUser);
        verify(saleTransactionRepository).countBySalesUser(salesUser);
        verify(leadRepository).countByAssignedTo(salesUser);
        verify(leadRepository).countByStatusAndAssignedTo(LeadStatus.CLOSED_WON, salesUser);
    }
    
    @Test
    void calculateSalesMetrics_WithDateRange_ReturnsFilteredMetrics() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        LocalDateTime endDate = LocalDateTime.now();
        BigDecimal totalRevenue = BigDecimal.valueOf(15000);
        
        when(saleTransactionRepository.calculateRevenueInDateRange(startDate, endDate)).thenReturn(totalRevenue);
        when(saleTransactionRepository.countSalesInDateRange(startDate, endDate)).thenReturn(10L);
        
        when(leadRepository.count()).thenReturn(100L);
        when(leadRepository.countByStatus(LeadStatus.CLOSED_WON)).thenReturn(30L);
        when(leadRepository.countByStatus(LeadStatus.CLOSED_LOST)).thenReturn(20L);
        
        when(userRepository.findActiveSalesUsers()).thenReturn(Arrays.asList(salesUser));
        when(saleTransactionRepository.calculateRevenueByUser(salesUser)).thenReturn(BigDecimal.valueOf(7500));
        when(saleTransactionRepository.countBySalesUser(salesUser)).thenReturn(5L);
        when(leadRepository.countByAssignedTo(salesUser)).thenReturn(25L);
        when(leadRepository.countByStatusAndAssignedTo(LeadStatus.CLOSED_WON, salesUser)).thenReturn(10L);
        
        // Act
        Map<String, Object> result = reportingService.calculateSalesMetrics(startDate, endDate);
        
        // Assert
        assertEquals(totalRevenue, result.get("totalRevenue"));
        assertEquals(10L, result.get("totalSales"));
        assertEquals(0, BigDecimal.valueOf(1500).compareTo((BigDecimal) result.get("averageSaleAmount"))); // 15000/10
        
        verify(saleTransactionRepository).calculateRevenueInDateRange(startDate, endDate);
        verify(saleTransactionRepository).countSalesInDateRange(startDate, endDate);
    }
    
    @Test
    void getDashboardData_ReturnsRealTimeMetrics() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime weekStart = now.minusWeeks(1);
        LocalDateTime monthStart = now.minusMonths(1);
        
        // Today's metrics
        when(saleTransactionRepository.calculateRevenueInDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(BigDecimal.valueOf(2000), BigDecimal.valueOf(12000), BigDecimal.valueOf(50000));
        when(saleTransactionRepository.countSalesInDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(1L, 6L, 25L);
        when(leadRepository.findLeadsCreatedBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(/* mock leads */));
        when(customerRepository.findCustomersCreatedBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(/* mock customers */));
        
        // Pipeline status
        when(leadRepository.countByStatus(LeadStatus.NEW)).thenReturn(10L);
        when(leadRepository.countByStatus(LeadStatus.CONTACTED)).thenReturn(8L);
        when(leadRepository.countByStatus(LeadStatus.QUALIFIED)).thenReturn(6L);
        when(leadRepository.countByStatus(LeadStatus.PROPOSAL)).thenReturn(4L);
        when(leadRepository.countByStatus(LeadStatus.NEGOTIATION)).thenReturn(3L);
        when(leadRepository.countByStatus(LeadStatus.CLOSED_WON)).thenReturn(15L);
        when(leadRepository.countByStatus(LeadStatus.CLOSED_LOST)).thenReturn(5L);
        
        // Top performers
        when(saleTransactionRepository.findSalesInDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(testSale));
        
        // Act
        Map<String, Object> result = reportingService.getDashboardData();
        
        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("today"));
        assertTrue(result.containsKey("thisWeek"));
        assertTrue(result.containsKey("thisMonth"));
        assertTrue(result.containsKey("pipelineStatus"));
        assertTrue(result.containsKey("topPerformers"));
        assertTrue(result.containsKey("lastUpdated"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> todayMetrics = (Map<String, Object>) result.get("today");
        assertEquals(BigDecimal.valueOf(2000), todayMetrics.get("revenue"));
        assertEquals(1L, todayMetrics.get("sales"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> weekMetrics = (Map<String, Object>) result.get("thisWeek");
        assertEquals(BigDecimal.valueOf(12000), weekMetrics.get("revenue"));
        assertEquals(6L, weekMetrics.get("sales"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> monthMetrics = (Map<String, Object>) result.get("thisMonth");
        assertEquals(BigDecimal.valueOf(50000), monthMetrics.get("revenue"));
        assertEquals(25L, monthMetrics.get("sales"));
        
        @SuppressWarnings("unchecked")
        Map<String, Long> pipelineStatus = (Map<String, Long>) result.get("pipelineStatus");
        assertEquals(10L, pipelineStatus.get("NEW"));
        assertEquals(15L, pipelineStatus.get("CLOSED_WON"));
        assertEquals(5L, pipelineStatus.get("CLOSED_LOST"));
        
        // Verify repository calls
        verify(saleTransactionRepository, times(3)).calculateRevenueInDateRange(any(LocalDateTime.class), any(LocalDateTime.class));
        verify(saleTransactionRepository, times(3)).countSalesInDateRange(any(LocalDateTime.class), any(LocalDateTime.class));
        verify(leadRepository, times(3)).findLeadsCreatedBetween(any(LocalDateTime.class), any(LocalDateTime.class));
        verify(customerRepository, times(3)).findCustomersCreatedBetween(any(LocalDateTime.class), any(LocalDateTime.class));
        verify(leadRepository, times(7)).countByStatus(any(LeadStatus.class));
        verify(saleTransactionRepository).findSalesInDateRange(any(LocalDateTime.class), any(LocalDateTime.class));
    }
    
    @Test
    void exportReportAsCSV_ValidReportData_ReturnsCSVBytes() throws IOException {
        // Arrange
        Map<String, Object> reportData = Map.of(
            "generatedAt", LocalDateTime.now(),
            "startDate", LocalDateTime.now().minusDays(30),
            "endDate", LocalDateTime.now(),
            "totalRevenue", BigDecimal.valueOf(50000),
            "totalSales", 25L,
            "averageSaleAmount", BigDecimal.valueOf(2000),
            "revenueByUser", Map.of("salesuser", BigDecimal.valueOf(25000)),
            "revenueByMonth", Map.of("2024-01", BigDecimal.valueOf(50000))
        );
        
        // Act
        byte[] result = reportingService.exportReportAsCSV(reportData);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
        
        String csvContent = new String(result);
        assertTrue(csvContent.contains("Report Generated"));
        assertTrue(csvContent.contains("Total Revenue"));
        assertTrue(csvContent.contains("50000"));
        assertTrue(csvContent.contains("Revenue by User"));
        assertTrue(csvContent.contains("salesuser"));
        assertTrue(csvContent.contains("Revenue by Month"));
    }
    
    @Test
    void exportReportAsPDF_ValidReportData_ReturnsPDFBytes() {
        // Arrange
        Map<String, Object> reportData = Map.of(
            "generatedAt", LocalDateTime.now(),
            "startDate", LocalDateTime.now().minusDays(30),
            "endDate", LocalDateTime.now(),
            "totalRevenue", BigDecimal.valueOf(50000),
            "totalSales", 25L,
            "averageSaleAmount", BigDecimal.valueOf(2000),
            "revenueByUser", Map.of("salesuser", BigDecimal.valueOf(25000))
        );
        
        // Act
        byte[] result = reportingService.exportReportAsPDF(reportData);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
        
        String pdfContent = new String(result);
        assertTrue(pdfContent.contains("SALES REPORT"));
        assertTrue(pdfContent.contains("SUMMARY METRICS"));
        assertTrue(pdfContent.contains("Total Revenue: $50000"));
        assertTrue(pdfContent.contains("Total Sales: 25"));
        assertTrue(pdfContent.contains("REVENUE BY USER"));
        assertTrue(pdfContent.contains("salesuser: $25000"));
    }
    
    @Test
    void calculateSalesMetrics_ZeroSales_HandlesGracefully() {
        // Arrange
        when(saleTransactionRepository.calculateTotalRevenue()).thenReturn(BigDecimal.ZERO);
        when(saleTransactionRepository.count()).thenReturn(0L);
        when(leadRepository.count()).thenReturn(0L);
        when(leadRepository.countByStatus(LeadStatus.CLOSED_WON)).thenReturn(0L);
        when(leadRepository.countByStatus(LeadStatus.CLOSED_LOST)).thenReturn(0L);
        when(userRepository.findActiveSalesUsers()).thenReturn(Arrays.asList());
        
        // Act
        Map<String, Object> result = reportingService.calculateSalesMetrics(null, null);
        
        // Assert
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.get("totalRevenue"));
        assertEquals(0L, result.get("totalSales"));
        assertEquals(BigDecimal.ZERO, result.get("averageSaleAmount"));
        assertEquals(0L, result.get("totalLeads"));
        assertEquals(0L, result.get("wonLeads"));
        assertEquals(0L, result.get("lostLeads"));
        assertEquals(0.0, (Double) result.get("conversionRate"), 0.01);
        assertEquals(0.0, (Double) result.get("lossRate"), 0.01);
    }
}