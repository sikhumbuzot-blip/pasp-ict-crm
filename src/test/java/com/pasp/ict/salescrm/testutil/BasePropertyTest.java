package com.pasp.ict.salescrm.testutil;

import net.jqwik.api.lifecycle.BeforeProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.pasp.ict.salescrm.config.TestConfig;
import com.pasp.ict.salescrm.entity.User;

/**
 * Base class for property-based tests using jqwik.
 * Provides common setup and utilities for property-based testing.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
public abstract class BasePropertyTest extends PropertyTestGenerators {
    
    @Autowired
    protected TestDatabaseInitializer testDataInitializer;
    
    @Autowired
    protected TestConfig.MockJavaMailSender mockMailSender;
    
    protected User adminUser;
    protected User salesUser1;
    protected User salesUser2;
    protected User regularUser;
    
    /**
     * Sets up test environment before each property test.
     * This method is called before each property execution.
     */
    @BeforeProperty
    protected void setUpPropertyTest() {
        // Clear any previous test data
        if (testDataInitializer != null) {
            testDataInitializer.clearAllData();
            testDataInitializer.initializeBasicTestData();
            
            // Cache commonly used test users
            adminUser = testDataInitializer.getAdminUser();
            salesUser1 = testDataInitializer.getSalesUser1();
            salesUser2 = testDataInitializer.getSalesUser2();
            regularUser = testDataInitializer.getRegularUser();
        }
        
        // Clear mock mail sender
        if (mockMailSender != null) {
            mockMailSender.clearSentMessages();
        }
    }
    
    /**
     * Initializes full test data for property tests that need complete data sets.
     */
    protected void initializeFullTestDataForProperty() {
        if (testDataInitializer != null) {
            testDataInitializer.initializeFullTestData();
            
            // Refresh cached users
            adminUser = testDataInitializer.getAdminUser();
            salesUser1 = testDataInitializer.getSalesUser1();
            salesUser2 = testDataInitializer.getSalesUser2();
            regularUser = testDataInitializer.getRegularUser();
        }
    }
    
    /**
     * Validates that a password meets complexity requirements.
     * Used in property tests to verify password validation logic.
     */
    protected boolean isValidPassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 50) {
            return false;
        }
        
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        
        return hasLower && hasUpper && hasDigit;
    }
    
    /**
     * Validates that an email address has a valid format.
     * Used in property tests to verify email validation logic.
     */
    protected boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        // Basic email validation regex
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(emailRegex) && email.length() <= 100;
    }
    
    /**
     * Validates that a username meets requirements.
     * Used in property tests to verify username validation logic.
     */
    protected boolean isValidUsername(String username) {
        if (username == null || username.length() < 3 || username.length() > 20) {
            return false;
        }
        
        // Username should start with letter and contain only letters and numbers
        return username.matches("^[a-zA-Z][a-zA-Z0-9]*$");
    }
    
    /**
     * Validates that a lead status transition is valid.
     * Used in property tests to verify lead lifecycle management.
     */
    protected boolean isValidLeadStatusTransition(com.pasp.ict.salescrm.entity.LeadStatus from, 
                                                 com.pasp.ict.salescrm.entity.LeadStatus to) {
        if (from == null || to == null) {
            return false;
        }
        
        // Define valid transitions based on business rules
        switch (from) {
            case NEW:
                return to == com.pasp.ict.salescrm.entity.LeadStatus.CONTACTED || 
                       to == com.pasp.ict.salescrm.entity.LeadStatus.CLOSED_LOST;
            case CONTACTED:
                return to == com.pasp.ict.salescrm.entity.LeadStatus.QUALIFIED || 
                       to == com.pasp.ict.salescrm.entity.LeadStatus.CLOSED_LOST;
            case QUALIFIED:
                return to == com.pasp.ict.salescrm.entity.LeadStatus.PROPOSAL || 
                       to == com.pasp.ict.salescrm.entity.LeadStatus.CLOSED_LOST;
            case PROPOSAL:
                return to == com.pasp.ict.salescrm.entity.LeadStatus.NEGOTIATION || 
                       to == com.pasp.ict.salescrm.entity.LeadStatus.CLOSED_LOST;
            case NEGOTIATION:
                return to == com.pasp.ict.salescrm.entity.LeadStatus.CLOSED_WON || 
                       to == com.pasp.ict.salescrm.entity.LeadStatus.CLOSED_LOST;
            case CLOSED_WON:
            case CLOSED_LOST:
                return false; // Terminal states
            default:
                return false;
        }
    }
    
    /**
     * Checks if a user role has permission for a specific operation.
     * Used in property tests to verify role-based access control.
     */
    protected boolean hasPermission(com.pasp.ict.salescrm.entity.UserRole role, String operation) {
        if (role == null || operation == null) {
            return false;
        }
        
        switch (role) {
            case ADMIN:
                return true; // Admin has all permissions
            case SALES:
                return operation.startsWith("sales.") || operation.startsWith("customer.") || 
                       operation.equals("dashboard.view");
            case REGULAR:
                return operation.equals("dashboard.view") || operation.startsWith("customer.view");
            default:
                return false;
        }
    }
    
    /**
     * Validates that a monetary amount is valid for business operations.
     * Used in property tests to verify amount validation logic.
     */
    protected boolean isValidAmount(java.math.BigDecimal amount) {
        if (amount == null) {
            return false;
        }
        
        // Amount should be positive and not exceed reasonable business limits
        return amount.compareTo(java.math.BigDecimal.ZERO) > 0 && 
               amount.compareTo(java.math.BigDecimal.valueOf(10_000_000)) <= 0;
    }
    
    /**
     * Validates that a date range is valid for reporting.
     * Used in property tests to verify date range validation logic.
     */
    protected boolean isValidDateRange(java.time.LocalDateTime start, java.time.LocalDateTime end) {
        if (start == null || end == null) {
            return false;
        }
        
        // Start should be before end, and range should not exceed 10 years
        return start.isBefore(end) && 
               start.isAfter(java.time.LocalDateTime.now().minusYears(10)) &&
               end.isBefore(java.time.LocalDateTime.now().plusYears(1));
    }
}