package com.pasp.ict.salescrm.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.pasp.ict.salescrm.entity.AuditLog;
import com.pasp.ict.salescrm.entity.Customer;
import com.pasp.ict.salescrm.entity.InteractionLog;
import com.pasp.ict.salescrm.entity.InteractionType;
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
import com.pasp.ict.salescrm.service.CustomerService;
import com.pasp.ict.salescrm.service.SalesService;
import com.pasp.ict.salescrm.service.UserService;

/**
 * Core integration tests for the Sales CRM Application.
 * Tests complete user workflows, database operations, transaction management,
 * and security integration.
 * 
 * **Validates: Requirements 10.2**
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
class CoreIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private SalesService salesService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private SaleTransactionRepository saleTransactionRepository;

    @Autowired
    private InteractionLogRepository interactionLogRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User adminUser;
    private User salesUser;
    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        // Clean up data before each test
        auditLogRepository.deleteAll();
        saleTransactionRepository.deleteAll();
        leadRepository.deleteAll();
        interactionLogRepository.deleteAll();
        customerRepository.deleteAll();
        userRepository.deleteAll();

        // Create test users
        adminUser = createTestUser("admin", "admin@test.com", "Admin", "User", UserRole.ADMIN);
        salesUser = createTestUser("sales", "sales@test.com", "Sales", "User", UserRole.SALES);

        // Create test customer
        testCustomer = createTestCustomer("Test Customer", "customer@test.com", "123-456-7890", 
                                        "Test Company", "123 Test St");
    }

    /**
     * Test 1: Complete Sales Workflow - Lead Creation to Sale Conversion
     * Tests the entire sales process from lead creation through conversion to sale.
     */
    @Test
    @Order(1)
    void testCompleteSalesWorkflow() {
        // Step 1: Create a lead
        Lead lead = salesService.createLead("Integration Test Lead", "Test Description", 
                                          BigDecimal.valueOf(1000), testCustomer, salesUser, salesUser);
        
        assertNotNull(lead);
        assertEquals(LeadStatus.NEW, lead.getStatus());
        assertEquals("Integration Test Lead", lead.getTitle());
        assertEquals(testCustomer, lead.getCustomer());

        // Step 2: Progress lead through status transitions
        lead = salesService.updateLeadStatus(lead.getId(), LeadStatus.CONTACTED, salesUser);
        assertEquals(LeadStatus.CONTACTED, lead.getStatus());

        lead = salesService.updateLeadStatus(lead.getId(), LeadStatus.QUALIFIED, salesUser);
        assertEquals(LeadStatus.QUALIFIED, lead.getStatus());

        lead = salesService.updateLeadStatus(lead.getId(), LeadStatus.PROPOSAL, salesUser);
        assertEquals(LeadStatus.PROPOSAL, lead.getStatus());

        lead = salesService.updateLeadStatus(lead.getId(), LeadStatus.NEGOTIATION, salesUser);
        assertEquals(LeadStatus.NEGOTIATION, lead.getStatus());

        // Step 3: Convert lead to sale
        SaleTransaction sale = salesService.convertLeadToSale(lead.getId(), BigDecimal.valueOf(1200), 
                                                            LocalDateTime.now(), "Converted sale", salesUser);
        
        assertNotNull(sale);
        assertEquals(BigDecimal.valueOf(1200), sale.getAmount());
        assertEquals(testCustomer, sale.getCustomer());
        assertEquals(salesUser, sale.getSalesUser());
        assertEquals(lead, sale.getLead());

        // Step 4: Verify lead status was automatically updated to CLOSED_WON
        Lead updatedLead = leadRepository.findById(lead.getId()).orElseThrow();
        assertEquals(LeadStatus.CLOSED_WON, updatedLead.getStatus());

        // Step 5: Verify audit logs were created for all operations
        List<AuditLog> auditLogs = auditLogRepository.findAll();
        assertTrue(auditLogs.size() >= 6); // Lead creation + 4 status updates + sale creation + lead conversion

        // Step 6: Verify sales metrics are calculated correctly
        var metrics = salesService.getIndividualPerformance(salesUser);
        assertEquals(BigDecimal.valueOf(1200), metrics.get("totalRevenue"));
        assertEquals(1L, metrics.get("totalSales"));
        assertEquals(1L, metrics.get("wonLeads"));
    }

    /**
     * Test 2: Database Transaction Management
     * Tests that database transactions are properly managed and rolled back on errors.
     */
    @Test
    @Order(2)
    void testDatabaseTransactionManagement() {
        // Create initial data
        Lead lead = salesService.createLead("Transaction Test Lead", "Description", 
                                          BigDecimal.valueOf(500), testCustomer, salesUser, salesUser);
        
        long initialLeadCount = leadRepository.count();
        long initialSaleCount = saleTransactionRepository.count();

        // Attempt to convert lead with invalid data (should trigger rollback)
        try {
            salesService.convertLeadToSale(lead.getId(), BigDecimal.valueOf(-100), // Invalid negative amount
                                         LocalDateTime.now(), "Invalid sale", salesUser);
            fail("Expected exception for invalid sale amount");
        } catch (Exception e) {
            // Expected exception
        }

        // Verify that no partial data was committed (transaction rolled back)
        assertEquals(initialLeadCount, leadRepository.count());
        assertEquals(initialSaleCount, saleTransactionRepository.count());
        
        // Lead status should remain unchanged
        Lead unchangedLead = leadRepository.findById(lead.getId()).orElseThrow();
        assertEquals(LeadStatus.NEW, unchangedLead.getStatus());

        // Verify successful transaction commits properly
        SaleTransaction validSale = salesService.convertLeadToSale(lead.getId(), BigDecimal.valueOf(600), 
                                                                 LocalDateTime.now(), "Valid sale", salesUser);
        
        assertNotNull(validSale);
        assertEquals(initialSaleCount + 1, saleTransactionRepository.count());
        
        Lead convertedLead = leadRepository.findById(lead.getId()).orElseThrow();
        assertEquals(LeadStatus.CLOSED_WON, convertedLead.getStatus());
    }

    /**
     * Test 3: Role-Based Access Control Integration
     * Tests that security integration properly enforces role-based access control.
     */
    @Test
    @Order(3)
    void testRoleBasedAccessControlIntegration() {
        // Test that sales service validates permissions correctly
        User regularUser = createTestUser("regular", "regular@test.com", "Regular", "User", UserRole.REGULAR);
        
        // Regular user should not be able to create leads
        assertThrows(SecurityException.class, () -> {
            salesService.createLead("Unauthorized Lead", "Description", 
                                  BigDecimal.valueOf(100), testCustomer, regularUser, regularUser);
        });

        // Regular user should not be able to create customers
        assertThrows(SecurityException.class, () -> {
            customerService.createCustomer("Unauthorized Customer", "unauthorized@test.com", 
                                         "555-UNAUTH", "Unauthorized Corp", "Unauthorized St", regularUser);
        });

        // Sales user should be able to create leads and customers
        Lead validLead = salesService.createLead("Valid Lead", "Description", 
                                               BigDecimal.valueOf(200), testCustomer, salesUser, salesUser);
        assertNotNull(validLead);

        Customer validCustomer = customerService.createCustomer("Valid Customer", "valid@test.com", 
                                                              "555-VALID", "Valid Corp", "Valid St", salesUser);
        assertNotNull(validCustomer);

        // Admin user should be able to perform all operations
        Lead adminLead = salesService.createLead("Admin Lead", "Description", 
                                               BigDecimal.valueOf(300), testCustomer, adminUser, adminUser);
        assertNotNull(adminLead);

        Customer adminCustomer = customerService.createCustomer("Admin Customer", "admin@test.com", 
                                                              "555-ADMIN", "Admin Corp", "Admin St", adminUser);
        assertNotNull(adminCustomer);
    }

    /**
     * Test 4: Customer Data Management Workflow
     * Tests complete customer data management including creation, updates, and interaction logging.
     */
    @Test
    @Order(4)
    void testCustomerDataManagementWorkflow() {
        // Step 1: Create customer
        Customer customer = customerService.createCustomer("Integration Customer", "integration@test.com", 
                                                          "555-0123", "Integration Corp", "456 Integration Ave", salesUser);
        
        assertNotNull(customer);
        assertEquals("Integration Customer", customer.getName());
        assertEquals("integration@test.com", customer.getEmail());

        // Step 2: Update customer information
        customer = customerService.updateCustomer(customer.getId(), "Updated Customer", 
                                                customer.getEmail(), "555-0124", "Updated Corp", 
                                                "789 Updated St", salesUser);
        
        assertEquals("Updated Customer", customer.getName());
        assertEquals("555-0124", customer.getPhone());

        // Step 3: Log customer interaction
        customerService.logInteraction(customer.getId(), InteractionType.CALL, "Follow-up call completed", salesUser);

        // Step 4: Verify interaction was logged
        var interactions = customerService.getCustomerInteractionHistory(customer.getId());
        assertEquals(1, interactions.size());
        assertEquals(InteractionType.CALL, interactions.get(0).getType());
        assertEquals("Follow-up call completed", interactions.get(0).getNotes());

        // Step 5: Search for customer
        List<Customer> searchResults = customerService.searchCustomers("Updated");
        final Long customerId = customer.getId();
        assertTrue(searchResults.stream().anyMatch(c -> c.getId().equals(customerId)));

        // Step 6: Verify audit logs for customer operations
        List<AuditLog> customerAudits = auditLogRepository.findByEntityTypeAndEntityId("Customer", customer.getId());
        assertTrue(customerAudits.size() >= 2); // Creation + update
    }

    /**
     * Test 5: Data Integrity and Validation
     * Tests that data integrity constraints and validation rules are properly enforced.
     */
    @Test
    @Order(5)
    void testDataIntegrityAndValidation() {
        // Test duplicate customer email prevention
        Customer customer1 = customerService.createCustomer("Customer 1", "duplicate@test.com", 
                                                           "555-0001", "Company 1", "Address 1", salesUser);
        
        assertThrows(IllegalArgumentException.class, () -> {
            customerService.createCustomer("Customer 2", "duplicate@test.com", // Same email
                                         "555-0002", "Company 2", "Address 2", salesUser);
        });

        // Test lead title validation
        assertThrows(IllegalArgumentException.class, () -> {
            salesService.createLead("", "Description", BigDecimal.valueOf(100), // Empty title
                                   testCustomer, salesUser, salesUser);
        });

        // Test invalid lead status transitions
        Lead lead = salesService.createLead("Valid Lead", "Description", 
                                          BigDecimal.valueOf(100), testCustomer, salesUser, salesUser);
        
        assertThrows(IllegalArgumentException.class, () -> {
            salesService.updateLeadStatus(lead.getId(), LeadStatus.CLOSED_WON, salesUser); // Invalid transition from NEW
        });

        // Test sale amount validation
        Lead validLead = salesService.createLead("Another Lead", "Description", 
                                               BigDecimal.valueOf(100), testCustomer, salesUser, salesUser);
        
        assertThrows(IllegalArgumentException.class, () -> {
            salesService.convertLeadToSale(validLead.getId(), BigDecimal.valueOf(-100), // Negative amount
                                         LocalDateTime.now(), "Invalid sale", salesUser);
        });
    }

    /**
     * Test 6: Security Integration and Audit Logging
     * Tests that security events are properly logged and audit trails are maintained.
     */
    @Test
    @Order(6)
    void testSecurityIntegrationAndAuditLogging() {
        long initialAuditCount = auditLogRepository.count();

        // Perform various operations that should generate audit logs
        User newUser = userService.createUser("testuser", "Password123", "testuser@test.com", 
                                            "Test", "User", UserRole.SALES, adminUser);
        
        userService.updateUserRole(newUser.getId(), UserRole.REGULAR, adminUser);
        userService.setUserActive(newUser.getId(), false, adminUser);

        Customer customer = customerService.createCustomer("Audit Customer", "audit@test.com", 
                                                          "555-AUDIT", "Audit Corp", "Audit St", adminUser);
        
        Lead lead = salesService.createLead("Audit Lead", "Description", 
                                          BigDecimal.valueOf(500), customer, newUser, adminUser);

        // Verify audit logs were created
        List<AuditLog> auditLogs = auditLogRepository.findAll();
        long finalAuditCount = auditLogs.size();
        
        assertTrue(finalAuditCount > initialAuditCount);

        // Verify audit log details
        boolean userCreationLogged = auditLogs.stream()
                .anyMatch(log -> "USER_CREATED".equals(log.getAction()) && 
                               "User".equals(log.getEntityType()) && 
                               newUser.getId().equals(log.getEntityId()));
        assertTrue(userCreationLogged);

        boolean roleUpdateLogged = auditLogs.stream()
                .anyMatch(log -> "USER_ROLE_UPDATED".equals(log.getAction()) && 
                               log.getOldValues().contains("SALES") && 
                               log.getNewValues().contains("REGULAR"));
        assertTrue(roleUpdateLogged);

        // Verify audit logs contain proper user attribution
        auditLogs.forEach(log -> {
            assertNotNull(log.getUser());
            assertNotNull(log.getTimestamp());
        });
    }

    /**
     * Test 7: Performance and Scalability
     * Tests system performance with larger datasets.
     */
    @Test
    @Order(7)
    void testPerformanceAndScalability() {
        // Create multiple customers
        for (int i = 1; i <= 10; i++) {
            customerService.createCustomer("Customer " + i, "customer" + i + "@test.com", 
                                         "555-000" + i, "Company " + i, "Address " + i, adminUser);
        }

        // Create multiple leads for each customer
        List<Customer> customers = customerRepository.findAll();
        for (Customer customer : customers) {
            for (int j = 1; j <= 3; j++) {
                salesService.createLead("Lead " + j + " for " + customer.getName(), 
                                      "Description", BigDecimal.valueOf(100 * j), 
                                      customer, salesUser, adminUser);
            }
        }

        // Verify data was created
        assertEquals(11, customerRepository.count()); // 10 + testCustomer
        assertEquals(30, leadRepository.count());

        // Test search performance
        long startTime = System.currentTimeMillis();
        List<Customer> searchResults = customerService.searchCustomers("Customer");
        long searchTime = System.currentTimeMillis() - startTime;
        
        assertEquals(10, searchResults.size());
        assertTrue(searchTime < 2000); // Should complete within 2 seconds as per requirements

        // Test metrics calculation performance
        startTime = System.currentTimeMillis();
        var metrics = salesService.calculateSalesMetrics(null, null, null);
        long metricsTime = System.currentTimeMillis() - startTime;
        
        assertNotNull(metrics);
        assertTrue(metricsTime < 5000); // Should complete within 5 seconds as per requirements
    }

    /**
     * Test 8: Database Operations and Entity Relationships
     * Tests that all entity relationships are properly configured and functional.
     */
    @Test
    @Order(8)
    void testDatabaseOperationsAndEntityRelationships() {
        // Create customer
        Customer customer = new Customer("DB Test Customer", "db@test.com", 
                                       "555-DB", "DB Corp", "DB St", adminUser);
        customer = customerRepository.save(customer);
        assertNotNull(customer.getId());

        // Create lead with customer relationship
        Lead lead = new Lead("DB Test Lead", "Description", BigDecimal.valueOf(1000), 
                           customer, salesUser);
        lead = leadRepository.save(lead);
        assertNotNull(lead.getId());
        assertEquals(customer.getId(), lead.getCustomer().getId());
        assertEquals(salesUser.getId(), lead.getAssignedTo().getId());

        // Create sale transaction with lead and customer relationships
        SaleTransaction sale = new SaleTransaction(BigDecimal.valueOf(1200), LocalDateTime.now(), 
                                                 "DB test sale", customer, salesUser, lead);
        sale = saleTransactionRepository.save(sale);
        assertNotNull(sale.getId());
        assertEquals(customer.getId(), sale.getCustomer().getId());
        assertEquals(salesUser.getId(), sale.getSalesUser().getId());
        assertEquals(lead.getId(), sale.getLead().getId());

        // Create interaction log with customer relationship
        InteractionLog interaction = new InteractionLog(InteractionType.CALL, 
                                                       "DB test interaction", customer, salesUser);
        interaction = interactionLogRepository.save(interaction);
        assertNotNull(interaction.getId());
        assertEquals(customer.getId(), interaction.getCustomer().getId());
        assertEquals(salesUser.getId(), interaction.getUser().getId());

        // Create audit log with user relationship
        AuditLog audit = new AuditLog("DB_TEST", "Customer", customer.getId(), 
                                    null, "Test audit", adminUser);
        audit = auditLogRepository.save(audit);
        assertNotNull(audit.getId());
        assertEquals(adminUser.getId(), audit.getUser().getId());

        // Verify all relationships are maintained
        Customer retrievedCustomer = customerRepository.findById(customer.getId()).orElseThrow();
        assertEquals("DB Test Customer", retrievedCustomer.getName());
        assertEquals(adminUser.getId(), retrievedCustomer.getCreatedBy().getId());

        Lead retrievedLead = leadRepository.findById(lead.getId()).orElseThrow();
        assertEquals(customer.getId(), retrievedLead.getCustomer().getId());
        assertEquals(salesUser.getId(), retrievedLead.getAssignedTo().getId());
    }

    // Helper methods

    private User createTestUser(String username, String email, String firstName, String lastName, UserRole role) {
        User user = new User(username, passwordEncoder.encode("Password123"), email, firstName, lastName, role);
        return userRepository.save(user);
    }

    private Customer createTestCustomer(String name, String email, String phone, String company, String address) {
        Customer customer = new Customer(name, email, phone, company, address, adminUser);
        return customerRepository.save(customer);
    }
}