package com.pasp.ict.salescrm.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.pasp.ict.salescrm.entity.User;
import com.pasp.ict.salescrm.service.AdminService;
import com.pasp.ict.salescrm.service.SalesService;
import com.pasp.ict.salescrm.service.UserService;

/**
 * Controller for handling dashboard-related requests.
 * Provides role-specific dashboards and real-time metrics API.
 */
@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final UserService userService;
    private final AdminService adminService;
    private final SalesService salesService;

    public DashboardController(UserService userService, AdminService adminService, SalesService salesService) {
        this.userService = userService;
        this.adminService = adminService;
        this.salesService = salesService;
    }

    /**
     * Admin dashboard with system statistics and management tools.
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminDashboard(Model model) {
        User currentUser = getCurrentUser();
        model.addAttribute("currentUser", currentUser);
        
        // Get system statistics
        Map<String, Object> systemStats = adminService.getSystemStatistics();
        model.addAttribute("systemStats", systemStats);
        
        // Get performance metrics
        Map<String, Object> performanceMetrics = adminService.getPerformanceMetrics();
        model.addAttribute("performanceMetrics", performanceMetrics);
        
        // Get system health
        Map<String, Object> systemHealth = adminService.getSystemHealth();
        model.addAttribute("systemHealth", systemHealth);
        
        return "dashboard/admin";
    }

    /**
     * Sales dashboard with sales metrics and lead management.
     */
    @GetMapping("/sales")
    @PreAuthorize("hasRole('SALES')")
    public String salesDashboard(Model model) {
        User currentUser = getCurrentUser();
        model.addAttribute("currentUser", currentUser);
        
        // Get individual performance metrics
        Map<String, Object> individualMetrics = salesService.getIndividualPerformance(currentUser);
        model.addAttribute("individualMetrics", individualMetrics);
        
        // Get assigned leads
        model.addAttribute("assignedLeads", salesService.findLeadsByAssignedUser(currentUser));
        
        // Get recent sales
        model.addAttribute("recentSales", salesService.findSalesByUser(currentUser));
        
        // Get open leads count
        model.addAttribute("openLeadsCount", salesService.findOpenLeads().size());
        
        return "dashboard/sales";
    }

    /**
     * Regular user dashboard with read-only access to assigned data.
     */
    @GetMapping("/regular")
    @PreAuthorize("hasRole('REGULAR')")
    public String regularDashboard(Model model) {
        User currentUser = getCurrentUser();
        model.addAttribute("currentUser", currentUser);
        
        // Regular users have limited access - show basic info only
        model.addAttribute("message", "Welcome to the Sales CRM System. You have read-only access to assigned data.");
        
        return "dashboard/regular";
    }

    /**
     * Real-time metrics API endpoint for AJAX updates.
     */
    @GetMapping("/metrics")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES', 'REGULAR')")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        User currentUser = getCurrentUser();
        
        switch (currentUser.getRole()) {
            case ADMIN:
                // Return comprehensive system metrics for admin
                Map<String, Object> adminMetrics = adminService.getSystemStatistics();
                adminMetrics.putAll(adminService.getPerformanceMetrics());
                return ResponseEntity.ok(adminMetrics);
                
            case SALES:
                // Return individual sales metrics
                Map<String, Object> salesMetrics = salesService.getIndividualPerformance(currentUser);
                return ResponseEntity.ok(salesMetrics);
                
            case REGULAR:
                // Return basic metrics for regular users
                Map<String, Object> basicMetrics = Map.of(
                    "message", "Limited access - contact administrator for more information"
                );
                return ResponseEntity.ok(basicMetrics);
                
            default:
                return ResponseEntity.badRequest().build();
        }
    }

    /**
     * System health API endpoint for admin monitoring.
     */
    @GetMapping("/health")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        Map<String, Object> health = adminService.getSystemHealth();
        return ResponseEntity.ok(health);
    }

    /**
     * Sales performance API endpoint for admin and sales users.
     */
    @GetMapping("/sales-performance")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES')")
    public ResponseEntity<Map<String, Object>> getSalesPerformance() {
        User currentUser = getCurrentUser();
        
        if (currentUser.getRole() == com.pasp.ict.salescrm.entity.UserRole.ADMIN) {
            // Admin can see all users' performance
            Map<String, Object> allPerformance = adminService.getSalesPerformanceByUser();
            return ResponseEntity.ok(allPerformance);
        } else {
            // Sales users can only see their own performance
            Map<String, Object> individualPerformance = salesService.getIndividualPerformance(currentUser);
            return ResponseEntity.ok(Map.of(currentUser.getUsername(), individualPerformance));
        }
    }

    /**
     * Get current authenticated user.
     */
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        return userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }
}