package com.pasp.ict.salescrm.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.pasp.ict.salescrm.entity.AuditLog;
import com.pasp.ict.salescrm.entity.User;
import com.pasp.ict.salescrm.entity.UserRole;
import com.pasp.ict.salescrm.service.AdminService;
import com.pasp.ict.salescrm.service.BackupService;
import com.pasp.ict.salescrm.service.NotificationService;
import com.pasp.ict.salescrm.service.ReportingService;
import com.pasp.ict.salescrm.service.UserService;

/**
 * Controller for handling administrative requests.
 * Manages user accounts, system configuration, and administrative reporting.
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;
    private final ReportingService reportingService;
    private final BackupService backupService;
    private final NotificationService notificationService;

    public AdminController(AdminService adminService, UserService userService, ReportingService reportingService,
                          BackupService backupService, NotificationService notificationService) {
        this.adminService = adminService;
        this.userService = userService;
        this.reportingService = reportingService;
        this.backupService = backupService;
        this.notificationService = notificationService;
    }

    /**
     * User management interface - list all users.
     */
    @GetMapping("/users")
    public String users(@RequestParam(value = "role", required = false) UserRole role,
                       @RequestParam(value = "active", required = false) Boolean active,
                       Model model) {
        User currentUser = getCurrentUser();
        model.addAttribute("currentUser", currentUser);
        
        List<User> users;
        if (role != null && active != null) {
            users = userService.findActiveUsersByRole(role);
        } else if (role != null) {
            users = userService.findByRole(role);
        } else if (active != null) {
            users = userService.findActiveUsers();
        } else {
            users = userService.findActiveUsers(); // Show active users by default
        }
        
        model.addAttribute("users", users);
        model.addAttribute("userRoles", UserRole.values());
        model.addAttribute("selectedRole", role);
        model.addAttribute("selectedActive", active);
        
        return "admin/users";
    }

    /**
     * Individual user management.
     */
    @GetMapping("/users/{id}")
    public String userDetails(@PathVariable Long id, Model model) {
        User currentUser = getCurrentUser();
        model.addAttribute("currentUser", currentUser);
        
        Optional<User> userOpt = userService.findById(id);
        if (userOpt.isEmpty()) {
            model.addAttribute("errorMessage", "User not found");
            return "redirect:/admin/users";
        }
        
        User user = userOpt.get();
        model.addAttribute("user", user);
        model.addAttribute("userRoles", UserRole.values());
        
        // Get user activity summary
        Map<String, Object> activitySummary = adminService.getUserActivitySummary(id);
        model.addAttribute("activitySummary", activitySummary);
        
        return "admin/user-details";
    }

    /**
     * Create new user form.
     */
    @GetMapping("/users/create")
    public String createUserForm(Model model) {
        User currentUser = getCurrentUser();
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("user", new User());
        model.addAttribute("userRoles", UserRole.values());
        
        return "admin/create-user";
    }

    /**
     * Create new user.
     */
    @PostMapping("/users/create")
    public String createUser(@RequestParam String username,
                            @RequestParam String password,
                            @RequestParam String email,
                            @RequestParam String firstName,
                            @RequestParam String lastName,
                            @RequestParam UserRole role,
                            RedirectAttributes redirectAttributes) {
        try {
            User currentUser = getCurrentUser();
            
            User newUser = adminService.createUser(
                username, password, email, firstName, lastName, role, currentUser);
            
            redirectAttributes.addFlashAttribute("successMessage", "User created successfully");
            return "redirect:/admin/users/" + newUser.getId();
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating user: " + e.getMessage());
            return "redirect:/admin/users/create";
        }
    }

    /**
     * Update user role.
     */
    @PostMapping("/users/{id}/role")
    public String updateUserRole(@PathVariable Long id,
                                @RequestParam UserRole role,
                                RedirectAttributes redirectAttributes) {
        try {
            User currentUser = getCurrentUser();
            User updatedUser = adminService.updateUserRole(id, role, currentUser);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "User role updated to " + role.name());
            return "redirect:/admin/users/" + updatedUser.getId();
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error updating user role: " + e.getMessage());
            return "redirect:/admin/users/" + id;
        }
    }

    /**
     * Activate or deactivate user.
     */
    @PostMapping("/users/{id}/status")
    public String updateUserStatus(@PathVariable Long id,
                                  @RequestParam boolean active,
                                  RedirectAttributes redirectAttributes) {
        try {
            User currentUser = getCurrentUser();
            User updatedUser = adminService.setUserActive(id, active, currentUser);
            
            String status = active ? "activated" : "deactivated";
            redirectAttributes.addFlashAttribute("successMessage", 
                "User " + status + " successfully");
            return "redirect:/admin/users/" + updatedUser.getId();
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error updating user status: " + e.getMessage());
            return "redirect:/admin/users/" + id;
        }
    }

    /**
     * System configuration and health monitoring.
     */
    @GetMapping("/system")
    public String systemConfiguration(Model model) {
        User currentUser = getCurrentUser();
        model.addAttribute("currentUser", currentUser);
        
        // Get system statistics
        Map<String, Object> systemStats = adminService.getSystemStatistics();
        model.addAttribute("systemStats", systemStats);
        
        // Get system health
        Map<String, Object> systemHealth = adminService.getSystemHealth();
        model.addAttribute("systemHealth", systemHealth);
        
        // Get performance metrics
        Map<String, Object> performanceMetrics = adminService.getPerformanceMetrics();
        model.addAttribute("performanceMetrics", performanceMetrics);
        
        // Get recent security events
        List<AuditLog> securityEvents = adminService.getRecentSecurityEvents();
        model.addAttribute("securityEvents", securityEvents);
        
        return "admin/system";
    }

    /**
     * Administrative reports and analytics.
     */
    @GetMapping("/reports")
    public String reports(@RequestParam(value = "type", required = false, defaultValue = "sales") String reportType,
                         @RequestParam(value = "period", required = false, defaultValue = "monthly") String period,
                         Model model) {
        User currentUser = getCurrentUser();
        model.addAttribute("currentUser", currentUser);
        
        model.addAttribute("reportType", reportType);
        model.addAttribute("period", period);
        
        try {
            switch (reportType) {
                case "sales":
                    LocalDateTime startDate = getStartDateForPeriod(period);
                    LocalDateTime endDate = LocalDateTime.now();
                    Map<String, Object> salesReport = reportingService.generateSalesReport(startDate, endDate, null, null);
                    model.addAttribute("reportData", salesReport);
                    break;
                case "users":
                    Map<String, Object> userReport = Map.of(
                        "totalUsers", userService.countActiveUsers(),
                        "activeUsers", userService.findActiveUsers().size(),
                        "salesUsers", userService.findActiveSalesUsers().size()
                    );
                    model.addAttribute("reportData", userReport);
                    break;
                case "customers":
                    Map<String, Object> customerReport = Map.of(
                        "totalCustomers", adminService.getSystemStatistics().get("totalCustomers"),
                        "period", period
                    );
                    model.addAttribute("reportData", customerReport);
                    break;
                default:
                    model.addAttribute("errorMessage", "Unknown report type");
            }
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error generating report: " + e.getMessage());
        }
        
        return "admin/reports";
    }

    /**
     * System statistics dashboard.
     */
    @GetMapping("/statistics")
    public String statistics(Model model) {
        User currentUser = getCurrentUser();
        model.addAttribute("currentUser", currentUser);
        
        // Get comprehensive system statistics
        Map<String, Object> systemStats = adminService.getSystemStatistics();
        model.addAttribute("systemStats", systemStats);
        
        // Get sales performance by user
        Map<String, Object> salesPerformance = adminService.getSalesPerformanceByUser();
        model.addAttribute("salesPerformance", salesPerformance);
        
        // Get performance metrics
        Map<String, Object> performanceMetrics = adminService.getPerformanceMetrics();
        model.addAttribute("performanceMetrics", performanceMetrics);
        
        return "admin/statistics";
    }

    /**
     * Recent system activity.
     */
    @GetMapping("/activity")
    public String systemActivity(@RequestParam(value = "hours", defaultValue = "24") int hours,
                                Model model) {
        User currentUser = getCurrentUser();
        model.addAttribute("currentUser", currentUser);
        
        List<AuditLog> recentActivity = adminService.getRecentSystemActivity(hours);
        model.addAttribute("recentActivity", recentActivity);
        model.addAttribute("hours", hours);
        
        return "admin/activity";
    }

    /**
     * System statistics API endpoint.
     */
    @GetMapping("/api/statistics")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSystemStatistics() {
        Map<String, Object> stats = adminService.getSystemStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * System health API endpoint.
     */
    @GetMapping("/api/health")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        Map<String, Object> health = adminService.getSystemHealth();
        return ResponseEntity.ok(health);
    }

    /**
     * Performance metrics API endpoint.
     */
    @GetMapping("/api/performance")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getPerformanceMetrics() {
        Map<String, Object> metrics = adminService.getPerformanceMetrics();
        return ResponseEntity.ok(metrics);
    }

    /**
     * Sales performance API endpoint.
     */
    @GetMapping("/api/sales-performance")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSalesPerformance() {
        Map<String, Object> performance = adminService.getSalesPerformanceByUser();
        return ResponseEntity.ok(performance);
    }

    /**
     * User activity API endpoint.
     */
    @GetMapping("/api/users/{id}/activity")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUserActivity(@PathVariable Long id) {
        try {
            Map<String, Object> activity = adminService.getUserActivitySummary(id);
            return ResponseEntity.ok(activity);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Recent activity API endpoint.
     */
    @GetMapping("/api/activity")
    @ResponseBody
    public ResponseEntity<List<AuditLog>> getRecentActivity(@RequestParam(value = "hours", defaultValue = "24") int hours) {
        List<AuditLog> activity = adminService.getRecentSystemActivity(hours);
        return ResponseEntity.ok(activity);
    }

    /**
     * Security events API endpoint.
     */
    @GetMapping("/api/security-events")
    @ResponseBody
    public ResponseEntity<List<AuditLog>> getSecurityEvents() {
        List<AuditLog> events = adminService.getRecentSecurityEvents();
        return ResponseEntity.ok(events);
    }

    /**
     * Export report API endpoint.
     */
    @GetMapping("/api/reports/export")
    @ResponseBody
    public ResponseEntity<byte[]> exportReport(@RequestParam String type,
                                              @RequestParam String format,
                                              @RequestParam(defaultValue = "monthly") String period) {
        try {
            LocalDateTime startDate = getStartDateForPeriod(period);
            LocalDateTime endDate = LocalDateTime.now();
            
            Map<String, Object> reportData = reportingService.generateSalesReport(startDate, endDate, null, null);
            
            byte[] exportData;
            if (format.equalsIgnoreCase("PDF")) {
                exportData = reportingService.exportReportAsPDF(reportData);
            } else {
                exportData = reportingService.exportReportAsCSV(reportData);
            }
            
            String filename = type + "_report." + format.toLowerCase();
            String contentType = format.equalsIgnoreCase("PDF") ? "application/pdf" : "text/csv";
            
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=" + filename)
                    .header("Content-Type", contentType)
                    .body(exportData);
                    
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get start date for period.
     */
    private LocalDateTime getStartDateForPeriod(String period) {
        LocalDateTime now = LocalDateTime.now();
        switch (period) {
            case "daily":
                return now.minusDays(1);
            case "weekly":
                return now.minusWeeks(1);
            case "monthly":
                return now.minusMonths(1);
            case "quarterly":
                return now.minusMonths(3);
            case "yearly":
                return now.minusYears(1);
            default:
                return now.minusMonths(1); // Default to monthly
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

    // ========== Backup Management Endpoints ==========

    /**
     * Backup management interface.
     */
    @GetMapping("/backups")
    public String backups(Model model) {
        User currentUser = getCurrentUser();
        model.addAttribute("currentUser", currentUser);
        
        // Get backup status
        String backupStatus = backupService.getBackupStatus();
        model.addAttribute("backupStatus", backupStatus);
        
        return "admin/backups";
    }

    /**
     * Create backup manually.
     */
    @PostMapping("/backups/create")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createBackup() {
        try {
            String backupId = backupService.createBackup();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Backup created successfully",
                "backupId", backupId
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Backup failed: " + e.getMessage()
            ));
        }
    }

    /**
     * Get backup status.
     */
    @GetMapping("/api/backup-status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getBackupStatus() {
        String status = backupService.getBackupStatus();
        return ResponseEntity.ok(Map.of("status", status));
    }

    /**
     * Verify backup integrity.
     */
    @PostMapping("/backups/{backupId}/verify")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verifyBackup(@PathVariable String backupId) {
        boolean isValid = backupService.verifyBackup(backupId);
        return ResponseEntity.ok(Map.of(
            "valid", isValid,
            "message", isValid ? "Backup is valid" : "Backup verification failed"
        ));
    }

    /**
     * Test notification system.
     */
    @PostMapping("/notifications/test")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testNotifications() {
        boolean success = notificationService.testNotification();
        return ResponseEntity.ok(Map.of(
            "success", success,
            "message", success ? "Test notification sent successfully" : "Failed to send test notification"
        ));
    }

    /**
     * Audit logs interface.
     */
    @GetMapping("/audit-logs")
    public String auditLogs(@RequestParam(value = "action", required = false) String action,
                           @RequestParam(value = "entityType", required = false) String entityType,
                           @RequestParam(value = "hours", defaultValue = "24") int hours,
                           Model model) {
        User currentUser = getCurrentUser();
        model.addAttribute("currentUser", currentUser);
        
        List<AuditLog> auditLogs = adminService.getRecentSystemActivity(hours);
        
        // Filter by action if specified
        if (action != null && !action.trim().isEmpty()) {
            auditLogs = auditLogs.stream()
                .filter(log -> log.getAction().contains(action.toUpperCase()))
                .toList();
        }
        
        // Filter by entity type if specified
        if (entityType != null && !entityType.trim().isEmpty()) {
            auditLogs = auditLogs.stream()
                .filter(log -> log.getEntityType().contains(entityType.toUpperCase()))
                .toList();
        }
        
        model.addAttribute("auditLogs", auditLogs);
        model.addAttribute("action", action);
        model.addAttribute("entityType", entityType);
        model.addAttribute("hours", hours);
        
        return "admin/audit-logs";
    }

    /**
     * Security incidents interface.
     */
    @GetMapping("/security-incidents")
    public String securityIncidents(Model model) {
        User currentUser = getCurrentUser();
        model.addAttribute("currentUser", currentUser);
        
        List<AuditLog> securityEvents = adminService.getRecentSecurityEvents();
        model.addAttribute("securityEvents", securityEvents);
        
        return "admin/security-incidents";
    }
}