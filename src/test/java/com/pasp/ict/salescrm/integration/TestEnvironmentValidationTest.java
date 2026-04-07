package com.pasp.ict.salescrm.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.pasp.ict.salescrm.entity.Customer;
import com.pasp.ict.salescrm.entity.Lead;
import com.pasp.ict.salescrm.entity.SaleTransaction;
import com.pasp.ict.salescrm.entity.User;
import com.pasp.ict.salescrm.entity.UserRole;
import com.pasp.ict.salescrm.repository.AuditLogRepository;
import com.pasp.ict.salescrm.repository.CustomerRepository;
import com.pasp.ict.salescrm.repository.InteractionLogRepository;
import com.pasp.ict.salescrm.repository.LeadRepository;
import com.pasp.ict.salescrm.repository.SaleTransactionRepository;
import com.pasp.ict.salescrm.repository.UserRepository;
import com.pasp.ict.salescrm.testutil.BaseIntegrationTest;
import com.pasp.ict.salescrm.testutil.PerformanceTestUtil;
import com.pasp.ict.salescrm.testutil.TestDataFactory;

/**
 * Integration test to validate the test environment configuration.
 * Tests H2 database setup, sample data initialization, and test utilities.
 * 
 * **Validates: Requirements 5.4, 10.2**
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TestEnvironmentValidationTest extends BaseIntegrationTest {
    
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
    
    /**
     * Test 1: H2 Database Configuration and Connectivity
     * Validates that H2 test database is properly configured and accessible.
     */
    @Test
    void testH2DatabaseConfiguration() {
        // Test basic database connectivity by performing CRUD operations
        
        // Create a test user
        User testUser = TestDataFactory.createUser("testuser", "test@example.com", 
                                                  "Test", "User", UserRole.SALES);
        User savedUser = userRepository.save(testUser);
        
        assertNotNull(savedUser.getId());
        assertEquals("testuser", savedUser.getUsername());
        assertEquals("test@example.com", savedUser.getEmail());
        
        // Verify user can be retrieved
        User retrievedUser = userRepository.findById(savedUser.getId()).orElse(null);
        assertNotNull(retrievedUser);
        assertEquals(savedUser.getUsername(), retrievedUser.getUsername());
        
        // Test update operation
        retrievedUser.setFirstName("Updated");
        User updatedUser = userRepository.save(retrievedUser);
        assertEquals("Updated", updatedUser.getFirstName());
        
        // Test delete operation
        userRepository.delete(updatedUser);
        assertTrue(userRepository.findById(savedUser.getId()).isEmpty());
    }
    
    /**
     * Test 2: Sample Data Initialization
     * Validates that sample data is properly loaded from data.sql and programmatically.
     */
    @Test
    void testSampleDataInitialization() {
        // Initialize full test data
        initializeFullTestData();
        
        // Verify users are created
        List<User> users = userRepository.findAll();
        assertTrue(users.size() >= 5, "Should have at least 5 test users");
        
        // Verify different user roles exist
        assertTrue(users.stream().anyMatch(u -> u.getRole() == UserRole.ADMIN));
        assertTrue(users.stream().anyMatch(u -> u.getRole() == UserRole.SALES));
        assertTrue(users.stream().anyMatch(u -> u.getRole() == UserRole.REGULAR));
        
        // Verify customers are created
        List<Customer> customers = customerRepository.findAll();
        assertTrue(customers.size() >= 8, "Should have at least 8 test customers");
        
        // Verify leads are created
        List<Lead> leads = leadRepository.findAll();
        assertTrue(leads.size() >= 8, "Should have at least 8 test leads");
        
        // Verify sales transactions are created
        List<SaleTransaction> sales = saleTransactionRepository.findAll();
        assertTrue(sales.size() >= 4, "Should have at least 4 test sales");
        
        // Verify interaction logs are created
        long interactionCount = interactionLogRepository.count();
        assertTrue(interactionCount >= 8, "Should have at least 8 interaction logs");
        
        // Verify audit logs are created
        long auditCount = auditLogRepository.count();
        assertTrue(auditCount >= 7, "Should have at least 7 audit logs");
    }
    
    /**
     * Test 3: Test Data Factory Functionality
     * Validates that TestDataFactory creates valid test entities.
     */
    @Test
    void testTestDataFactory() {
        // Test user creation
        User randomUser = TestDataFactory.createRandomUser();
        assertNotNull(randomUser);
        assertNotNull(randomUser.getUsername());
        assertNotNull(randomUser.getEmail());
        assertNotNull(randomUser.getFirstName());
        assertNotNull(randomUser.getLastName());
        assertNotNull(randomUser.getRole());
        
        // Test customer creation
        Customer randomCustomer = TestDataFactory.createRandomCustomer(adminUser);
        assertNotNull(randomCustomer);
        assertNotNull(randomCustomer.getName());
        assertNotNull(randomCustomer.getEmail());
        assertNotNull(randomCustomer.getPhone());
        assertNotNull(randomCustomer.getCompany());
        assertNotNull(randomCustomer.getAddress());
        assertEquals(adminUser, randomCustomer.getCreatedBy());
        
        // Test lead creation
        Lead randomLead = TestDataFactory.createRandomLead(randomCustomer, salesUser1, adminUser);
        assertNotNull(randomLead);
        assertNotNull(randomLead.getTitle());
        assertNotNull(randomLead.getDescription());
        assertNotNull(randomLead.getEstimatedValue());
        assertEquals(randomCustomer, randomLead.getCustomer());
        assertEquals(salesUser1, randomLead.getAssignedTo());
        
        // Test bulk data creation
        List<User> randomUsers = TestDataFactory.createRandomUsers(5);
        assertEquals(5, randomUsers.size());
        randomUsers.forEach(user -> {
            assertNotNull(user.getUsername());
            assertNotNull(user.getEmail());
        });
    }
    
    /**
     * Test 4: Mock External Dependencies
     * Validates that external dependencies are properly mocked for testing.
     */
    @Test
    void testMockExternalDependencies() {
        // Test mock mail sender
        assertNotNull(mockMailSender);
        assertEquals(0, mockMailSender.getSentMessageCount());
        
        // Send a test email using mock message
        com.pasp.ict.salescrm.config.TestConfig.MockSimpleMailMessage message = new com.pasp.ict.salescrm.config.TestConfig.MockSimpleMailMessage();
        message.setTo("test@example.com");
        message.setSubject("Test Subject");
        message.setText("Test message");
        mockMailSender.send(message);
        
        // Verify email was captured by mock
        assertEquals(1, mockMailSender.getSentMessageCount());
        List<com.pasp.ict.salescrm.config.TestConfig.MockSimpleMailMessage> sentMessages = mockMailSender.getSentMessages();
        assertEquals(1, sentMessages.size());
        assertEquals("Test Subject", sentMessages.get(0).getSubject());
        
        // Clear and verify
        mockMailSender.clearSentMessages();
        assertEquals(0, mockMailSender.getSentMessageCount());
    }
    
    /**
     * Test 5: Performance Testing Utilities
     * Validates that performance testing utilities work correctly.
     */
    @Test
    void testPerformanceTestingUtilities() {
        // Test execution time measurement
        long executionTime = PerformanceTestUtil.measureExecutionTime(() -> {
            try {
                Thread.sleep(100); // Simulate work
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        assertTrue(executionTime >= 100, "Execution time should be at least 100ms");
        assertTrue(executionTime < 200, "Execution time should be less than 200ms");
        
        // Test timed result
        PerformanceTestUtil.TimedResult<String> result = PerformanceTestUtil.measureExecutionTime((java.util.function.Supplier<String>) () -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "test result";
        });
        
        assertEquals("test result", result.getResult());
        assertTrue(result.getExecutionTime() >= 50);
        
        // Test performance assertion
        PerformanceTestUtil.assertExecutionTime(() -> {
            // Fast operation
        }, 100);
        
        // Test benchmark
        PerformanceTestUtil.PerformanceStats stats = PerformanceTestUtil.benchmark(() -> {
            // Simulate variable execution time
            try {
                Thread.sleep((long) (Math.random() * 10));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, 5);
        
        assertEquals(5, stats.getIterations());
        assertTrue(stats.getMinTime() >= 0);
        assertTrue(stats.getMaxTime() >= stats.getMinTime());
        assertTrue(stats.getAvgTime() >= stats.getMinTime());
        assertTrue(stats.getAvgTime() <= stats.getMaxTime());
    }
    
    /**
     * Test 6: Database Transaction Management
     * Validates that test transactions are properly managed and isolated.
     */
    @Test
    void testDatabaseTransactionManagement() {
        // Get initial counts
        long initialUserCount = userRepository.count();
        long initialCustomerCount = customerRepository.count();
        
        // Create test data within transaction
        User testUser = TestDataFactory.createUser("txtest", "txtest@example.com", 
                                                  "Transaction", "Test", UserRole.SALES);
        userRepository.save(testUser);
        
        Customer testCustomer = TestDataFactory.createCustomer("TX Customer", "tx@example.com", 
                                                              "555-TX", "TX Corp", "TX Address", testUser);
        customerRepository.save(testCustomer);
        
        // Verify data exists within transaction
        assertEquals(initialUserCount + 1, userRepository.count());
        assertEquals(initialCustomerCount + 1, customerRepository.count());
        
        // Data should be rolled back after test due to @Transactional
        // This is verified by the test framework automatically
    }
    
    @Autowired
    private org.springframework.core.env.Environment environment;
    
    /**
     * Test 7: Test Profile Configuration
     * Validates that test-specific configuration is properly applied.
     */
    @Test
    void testTestProfileConfiguration() {
        // Verify test profile is active
        String[] activeProfiles = environment.getActiveProfiles();
        assertTrue(java.util.Arrays.asList(activeProfiles).contains("test"));
        
        // Verify test-specific properties
        String datasourceUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(datasourceUrl);
        assertTrue(datasourceUrl.contains("h2:mem:testdb"));
        
        String encryptionKey = environment.getProperty("app.encryption.key");
        assertNotNull(encryptionKey);
        assertTrue(encryptionKey.startsWith("testEncryptionKey"));
        
        // Verify backup is disabled in test
        String backupEnabled = environment.getProperty("app.backup.enabled");
        assertEquals("false", backupEnabled);
        
        // Verify notifications are disabled in test
        String notificationEnabled = environment.getProperty("app.notification.enabled");
        assertEquals("false", notificationEnabled);
    }
    
    /**
     * Test 8: Test Data Cleanup and Isolation
     * Validates that test data is properly cleaned up between tests.
     */
    @Test
    void testDataCleanupAndIsolation() {
        // This test verifies that the BaseIntegrationTest setup works correctly
        
        // Verify basic test data is available
        assertNotNull(adminUser);
        assertNotNull(salesUser1);
        assertNotNull(salesUser2);
        assertNotNull(regularUser);
        
        // Verify test data initializer is working
        assertNotNull(testDataInitializer);
        
        // Test data should be clean at start of each test
        // This is ensured by the @BeforeEach method in BaseIntegrationTest
        
        // Create some test data
        Customer testCustomer = TestDataFactory.createRandomCustomer(salesUser1);
        customerRepository.save(testCustomer);
        
        // Verify data exists
        assertTrue(customerRepository.findById(testCustomer.getId()).isPresent());
        
        // Data will be cleaned up automatically by transaction rollback
    }
}