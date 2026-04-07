package com.pasp.ict.salescrm.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pasp.ict.salescrm.entity.AuditLog;
import com.pasp.ict.salescrm.entity.Customer;
import com.pasp.ict.salescrm.entity.Lead;
import com.pasp.ict.salescrm.entity.LeadStatus;
import com.pasp.ict.salescrm.entity.SaleTransaction;
import com.pasp.ict.salescrm.entity.User;
import com.pasp.ict.salescrm.entity.UserRole;
import com.pasp.ict.salescrm.repository.AuditLogRepository;
import com.pasp.ict.salescrm.repository.CustomerRepository;
import com.pasp.ict.salescrm.repository.InteractionLogRepository;
import com.pasp.ict.salescrm.repository.LeadRepository;
import com.pasp.ict.salescrm.repository.SaleTransactionRepository;
import com.pasp.ict.salescrm.repository.UserRepository;

/**
 * Service class for administrative functions and system statistics.
 * Provides system management capabilities and performance metrics.
 */
@Service
@Transactional
public class AdminService {
    
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final LeadRepository leadRepository;
    private final SaleTransactionRepository saleTransactionRepository;
    private final InteractionLogRepository interactionLogRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserService userService;
    
    @Autowired
    public AdminService(UserRepository userRepository,
                       CustomerRepository customerRepository,
                       LeadRepository leadRepository,
                       SaleTransactionRepository saleTransactionRepository,
                       InteractionLogRepository interactionLogRepository,
                       AuditLogRepository auditLogRepository,
                       UserService userService) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.leadRepository = leadRepository;
        this.saleTransactionRepository = saleTransactionRepository;
        this.interactionLogRepository = interactionLogRepository;
        this.auditLogRepository = auditLogRepository;
        this.userService = userService;
    }
    
    /**
     * Create a new user account (admin function).
     * @param username the username
     * @param password the password
     * @param email the email
     * @param firstName the first name
     * @param lastName the last name
     * @param role the user role
     * @param adminUser the admin creating the account
     * @return the created user
     */
    public User createUser(String username, String password, String email, 
                          String firstName, String lastName, UserRole role, User adminUser) {
        validateAdminPermission(adminUser);
        return userService.createUser(username, password, email, firstName, lastName, role, adminUser);
    }
    
    /**
     * Update user role (admin function).
     * @param userId the user ID
     * @param newRole the new role
     * @param adminUser the admin making the change
     * @return the updated user
     */
    public User updateUserRole(Long userId, UserRole newRole, User adminUser) {
        validateAdminPermission(adminUser);
        return userService.updateUserRole(userId, newRole, adminUser);
    }
    
    /**
     * Activate or deactivate user (admin function).
     * @param userId the user ID
     * @param active the active status
     * @param adminUser the admin making the change
     * @return the updated user
     */
    public User setUserActive(Long userId, boolean active, User adminUser) {
        validateAdminPermission(adminUser);
        return userService.setUserActive(userId, active, adminUser);
    }
    
    /**
     * Get comprehensive system statistics.
     * @return Map containing system statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getSystemStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // User statistics
        stats.put("totalUsers", userRepository.count());
        stats.put("activeUsers", userRepository.countByActive(true));
        stats.put("adminUsers", userRepository.countByRoleAndActive(UserRole.ADMIN, true));
        stats.put("salesUsers", userRepository.countByRoleAndActive(UserRole.SALES, true));
        stats.put("regularUsers", userRepository.countByRoleAndActive(UserRole.REGULAR, true));
        
        // Customer statistics
        stats.put("totalCustomers", customerRepository.count());
        
        // Lead statistics
        stats.put("totalLeads", leadRepository.count());
        stats.put("openLeads", leadRepository.findOpenLeads().size());
        stats.put("closedLeads", leadRepository.findClosedLeads().size());
        stats.put("wonLeads", leadRepository.countByStatus(LeadStatus.CLOSED_WON));
        stats.put("lostLeads", leadRepository.countByStatus(LeadStatus.CLOSED_LOST));
        
        // Sales statistics
        stats.put("totalSales", saleTransactionRepository.count());
        stats.put("totalRevenue", saleTransactionRepository.calculateTotalRevenue());
        stats.put("averageSaleAmount", saleTransactionRepository.calculateAverageSaleAmount());
        
        // Interaction statistics
        stats.put("totalInteractions", interactionLogRepository.count());
        
        // Audit statistics
        stats.put("totalAuditLogs", auditLogRepository.count());
        
        return stats;
    }
    
    /**
     * Get system performance metrics.
     * @return Map containing performance metrics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastMonth = now.minusMonths(1);
        LocalDateTime lastWeek = now.minusWeeks(1);
        LocalDateTime today = now.toLocalDate().atStartOfDay();
        
        // Monthly metrics
        metrics.put("monthlyRevenue", saleTransactionRepository.calculateRevenueInDateRange(lastMonth, now));
        metrics.put("monthlySales", saleTransactionRepository.countSalesInDateRange(lastMonth, now));
        metrics.put("monthlyNewCustomers", customerRepository.findCustomersCreatedBetween(lastMonth, now).size());
        metrics.put("monthlyNewLeads", leadRepository.findLeadsCreatedBetween(lastMonth, now).size());
        
        // Weekly metrics
        metrics.put("weeklyRevenue", saleTransactionRepository.calculateRevenueInDateRange(lastWeek, now));
        metrics.put("weeklySales", saleTransactionRepository.countSalesInDateRange(lastWeek, now));
        metrics.put("weeklyNewCustomers", customerRepository.findCustomersCreatedBetween(lastWeek, now).size());
        metrics.put("weeklyNewLeads", leadRepository.findLeadsCreatedBetween(lastWeek, now).size());
        
        // Daily metrics
        metrics.put("dailyRevenue", saleTransactionRepository.calculateRevenueInDateRange(today, now));
        metrics.put("dailySales", saleTransactionRepository.countSalesInDateRange(today, now));
        metrics.put("dailyNewCustomers", customerRepository.findCustomersCreatedBetween(today, now).size());
        metrics.put("dailyNewLeads", leadRepository.findLeadsCreatedBetween(today, now).size());
        
        // Conversion metrics
        long totalLeads = leadRepository.count();
        long wonLeads = leadRepository.countByStatus(LeadStatus.CLOSED_WON);
        double conversionRate = totalLeads > 0 ? (double) wonLeads / totalLeads * 100 : 0.0;
        metrics.put("overallConversionRate", conversionRate);
        
        return metrics;
    }
    
    /**
     * Get sales performance by user.
     * @return Map containing user performance data
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getSalesPerformanceByUser() {
        Map<String, Object> performance = new HashMap<>();
        
        List<User> salesUsers = userRepository.findActiveSalesUsers();
        
        for (User user : salesUsers) {
            Map<String, Object> userStats = new HashMap<>();
            
            // Sales metrics
            BigDecimal totalRevenue = saleTransactionRepository.calculateRevenueByUser(user);
            long totalSales = saleTransactionRepository.countBySalesUser(user);
            BigDecimal averageSale = totalSales > 0 ? 
                saleTransactionRepository.calculateAverageSaleAmountByUser(user) : BigDecimal.ZERO;
            
            // Lead metrics
            long assignedLeads = leadRepository.countByAssignedTo(user);
            long wonLeads = leadRepository.countByStatusAndAssignedTo(LeadStatus.CLOSED_WON, user);
            double conversionRate = assignedLeads > 0 ? (double) wonLeads / assignedLeads * 100 : 0.0;
            
            userStats.put("totalRevenue", totalRevenue);
            userStats.put("totalSales", totalSales);
            userStats.put("averageSaleAmount", averageSale);
            userStats.put("assignedLeads", assignedLeads);
            userStats.put("wonLeads", wonLeads);
            userStats.put("conversionRate", conversionRate);
            
            performance.put(user.getUsername(), userStats);
        }
        
        return performance;
    }
    
    /**
     * Get system health status.
     * @return Map containing system health indicators
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastHour = now.minusHours(1);
        LocalDateTime last24Hours = now.minusDays(1);
        
        // Recent activity indicators
        long recentAuditLogs = auditLogRepository.findAuditLogsInDateRange(lastHour, now).size();
        long recentInteractions = interactionLogRepository.findInteractionsInDateRange(lastHour, now).size();
        long recentSales = saleTransactionRepository.countSalesInDateRange(lastHour, now);
        
        health.put("recentAuditActivity", recentAuditLogs);
        health.put("recentUserActivity", recentInteractions);
        health.put("recentSalesActivity", recentSales);
        
        // System usage indicators
        long activeUsersLast24h = userRepository.findUsersWithOldLastLogin(last24Hours).size();
        long totalActiveUsers = userRepository.countByActive(true);
        double userActivityRate = totalActiveUsers > 0 ? 
            (double) (totalActiveUsers - activeUsersLast24h) / totalActiveUsers * 100 : 0.0;
        
        health.put("userActivityRate", userActivityRate);
        health.put("totalActiveUsers", totalActiveUsers);
        
        // Data integrity indicators
        long customersWithoutInteractions = interactionLogRepository
            .findCustomersWithNoRecentInteractions(last24Hours.minusDays(30)).size();
        long staleLeads = leadRepository.findStaleLeads(last24Hours.minusDays(7)).size();
        
        health.put("customersWithoutRecentInteractions", customersWithoutInteractions);
        health.put("staleLeads", staleLeads);
        
        // Overall health score (0-100)
        double healthScore = calculateHealthScore(userActivityRate, recentAuditLogs, 
                                                 customersWithoutInteractions, staleLeads);
        health.put("overallHealthScore", healthScore);
        
        return health;
    }
    
    /**
     * Get recent security events.
     * @return List of recent security-related audit logs
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getRecentSecurityEvents() {
        return auditLogRepository.findSecurityAuditLogs();
    }
    
    /**
     * Get recent system activity.
     * @param hours number of hours to look back
     * @return List of recent audit logs
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getRecentSystemActivity(int hours) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusHours(hours);
        return auditLogRepository.findRecentAuditLogs(cutoffDate);
    }
    
    /**
     * Get user activity summary.
     * @param userId the user ID
     * @return Map containing user activity data
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserActivitySummary(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        Map<String, Object> activity = new HashMap<>();
        
        // Basic user info
        activity.put("username", user.getUsername());
        activity.put("role", user.getRole());
        activity.put("active", user.isActive());
        activity.put("lastLogin", user.getLastLogin());
        activity.put("createdAt", user.getCreatedAt());
        
        // Activity metrics
        activity.put("auditLogCount", auditLogRepository.countByUser(user));
        activity.put("interactionCount", interactionLogRepository.countByUser(user));
        
        if (user.getRole() == UserRole.SALES) {
            activity.put("salesCount", saleTransactionRepository.countBySalesUser(user));
            activity.put("totalRevenue", saleTransactionRepository.calculateRevenueByUser(user));
            activity.put("assignedLeads", leadRepository.countByAssignedTo(user));
            activity.put("customersCreated", customerRepository.countByCreatedBy(user));
        }
        
        return activity;
    }
    
    /**
     * Validate admin permission.
     */
    private void validateAdminPermission(User user) {
        if (user == null || user.getRole() != UserRole.ADMIN || !user.isActive()) {
            throw new SecurityException("Admin permission required");
        }
    }
    
    /**
     * Calculate overall system health score.
     */
    private double calculateHealthScore(double userActivityRate, long recentAuditLogs, 
                                       long customersWithoutInteractions, long staleLeads) {
        double score = 100.0;
        
        // Deduct points for low user activity
        if (userActivityRate < 50) {
            score -= (50 - userActivityRate) * 0.5;
        }
        
        // Deduct points for too many stale customers
        if (customersWithoutInteractions > 10) {
            score -= Math.min(customersWithoutInteractions - 10, 20);
        }
        
        // Deduct points for too many stale leads
        if (staleLeads > 5) {
            score -= Math.min(staleLeads - 5, 15);
        }
        
        // Add points for recent system activity
        if (recentAuditLogs > 10) {
            score += Math.min(recentAuditLogs - 10, 10);
        }
        
        return Math.max(0, Math.min(100, score));
    }
}