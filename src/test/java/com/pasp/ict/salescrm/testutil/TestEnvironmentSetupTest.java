package com.pasp.ict.salescrm.testutil;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.pasp.ict.salescrm.config.TestConfig;
import com.pasp.ict.salescrm.entity.Customer;
import com.pasp.ict.salescrm.entity.Lead;
import com.pasp.ict.salescrm.entity.LeadStatus;
import com.pasp.ict.salescrm.entity.SaleTransaction;
import com.pasp.ict.salescrm.entity.User;
import com.pasp.ict.salescrm.entity.UserRole;
import com.pasp.ict.salescrm.repository.CustomerRepository;
import com.pasp.ict.salescrm.repository.LeadRepository;
import com.pasp.ict.salescrm.repository.SaleTransactionRepository;
import com.pasp.ict.salescrm.repository.UserRepository;

/**
 * Comprehensive test for validating the test environment setup.
 * This test ensures all test infrastructure components are working correctly.
 * 
 * **Validates: Requirements 5.4, 10.2**
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
class TestEnvironmentSetupTest {
    
    @Autowired
    private Environment environment;
    
    @Autowired
    private TestDatabaseInitializer testDataInitializer;
    
    @Autowired
    private TestConfig.MockJavaMailSender mockMailSender;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private LeadRepository leadRepository;
    
    @Autowired
    private SaleTransactionRepository saleTransactionRepository;
    
    /**
     * Test 1: H2 Database Configuration
     * Validates that H2 test database is properly configured with PostgreSQL compatibility mode.
     */
    @Test
    void testH2DatabaseConfiguration() {
        // Verify H2 database URL configuration
        String datasourceUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(datasourceUrl, "Datasource URL should be configured");
        assertTrue(datasourceUrl.contains("h2:mem:testdb"), "Should use H2 in-memory database");
        assertTrue(datasourceUrl.contains("MODE=PostgreSQL"), "Should use PostgreSQL compatibility mode");
        assertTrue(datasourceUrl.contains("DB_CLOSE_DELAY=-1"), "Should keep database open");
        
        // Verify database driver
        String driverClass = environment.getProperty("spring.datasource.driver-class-name");
        assertEquals("org.h2.Driver", driverClass, "Should use H2 driver");
        
        // Verify JPA configuration
        String ddlAuto = environment.getProperty("spring.jpa.hibernate.ddl-auto");
        assertEquals("create-drop", ddlAuto, "Should use create-drop for tests");
        
        String dialect = environment.getProperty("spring.jpa.database-platform");
        assertEquals("org.hibernate.dialect.H2Dialect", dialect, "Should use H2 dialect");
    }
    
    /**
     * Test 2: Test Profile Configuration
     * Validates that test-specific properties are correctly applied.
     */
    @Test
    void testTestProfileConfiguration() {
        // Verify active profiles
        String[] activeProfiles = environment.getActiveProfiles();
        assertTrue(java.util.Arrays.asList(activeProfiles).contains("test"), 
                  "Test profile should be active");
        
        // Verify test-specific configurations
        assertEquals("false", environment.getProperty("app.backup.enabled"), 
                    "Backup should be disabled in tests");
        assertEquals("false", environment.getProperty("app.notification.enabled"), 
                    "Notifications should be disabled in tests");
        assertEquals("off", environment.getProperty("spring.main.banner-mode"), 
                    "Banner should be disabled in tests");
        
        // Verify encryption key for tests
        String encryptionKey = environment.getProperty("app.encryption.key");
        assertNotNull(encryptionKey, "Encryption key should be configured for tests");
        assertTrue(encryptionKey.startsWith("testEncryptionKey"), 
                  "Should use test encryption key");
        
        // Verify jqwik configuration
        assertEquals("100", environment.getProperty("jqwik.tries.default"), 
                    "jqwik should use 100 default tries");
        assertEquals("true", environment.getProperty("jqwik.reporting.only-failures"), 
                    "jqwik should only report failures");
    }
    
    /**
     * Test 3: Test Data Initialization
     * Validates that TestDatabaseInitializer creates proper test data.
     */
    @Test
    void testTestDataInitialization() {
        // Clear any existing data
        testDataInitializer.clearAllData();
        assertEquals(0, userRepository.count(), "Should start with no users");
        
        // Initialize basic test data
        testDataInitializer.initializeBasicTestData();
        
        // Verify users are created
        List<User> users = userRepository.findAll();
        assertTrue(users.size() >= 5, "Should create at least 5 users");
        
        // Verify admin user
        User adminUser = testDataInitializer.getAdminUser();
        assertNotNull(adminUser, "Admin user should be created");
        assertEquals(UserRole.ADMIN, adminUser.getRole(), "Admin should have ADMIN role");
        assertTrue(adminUser.isActive(), "Admin should be active");
        assertEquals("admin", adminUser.getUsername(), "Admin username should be 'admin'");
        
        // Verify sales users
        User salesUser1 = testDataInitializer.getSalesUser1();
        User salesUser2 = testDataInitializer.getSalesUser2();
        assertNotNull(salesUser1, "Sales user 1 should be created");
        assertNotNull(salesUser2, "Sales user 2 should be created");
        assertEquals(UserRole.SALES, salesUser1.getRole(), "Sales user should have SALES role");
        assertEquals(UserRole.SALES, salesUser2.getRole(), "Sales user should have SALES role");
        
        // Verify regular user
        User regularUser = testDataInitializer.getRegularUser();
        assertNotNull(regularUser, "Regular user should be created");
        assertEquals(UserRole.REGULAR, regularUser.getRole(), "Regular user should have REGULAR role");
        
        // Verify customers are created
        List<Customer> customers = testDataInitializer.getAllTestCustomers();
        assertTrue(customers.size() >= 5, "Should create at least 5 customers");
        
        // Initialize full test data
        testDataInitializer.initializeFullTestData();
        
        // Verify leads are created
        List<Lead> leads = testDataInitializer.getAllTestLeads();
        assertTrue(leads.size() >= 5, "Should create at least 5 leads");
        
        // Verify different lead statuses exist
        assertTrue(leads.stream().anyMatch(l -> l.getStatus() == LeadStatus.NEW), 
                  "Should have NEW leads");
        assertTrue(leads.stream().anyMatch(l -> l.getStatus() == LeadStatus.CONTACTED), 
                  "Should have CONTACTED leads");
        assertTrue(leads.stream().anyMatch(l -> l.getStatus() == LeadStatus.CLOSED_WON), 
                  "Should have CLOSED_WON leads");
        
        // Verify sales transactions are created
        List<SaleTransaction> sales = testDataInitializer.getAllTestSales();
        assertTrue(sales.size() >= 3, "Should create at least 3 sales");
        
        // Verify sales have proper amounts
        assertTrue(sales.stream().allMatch(s -> s.getAmount().compareTo(BigDecimal.ZERO) > 0), 
                  "All sales should have positive amounts");
    }
    
    /**
     * Test 4: Property Test Generators
     * Validates that PropertyTestGenerators create valid test data.
     */
    @Test
    void testPropertyTestGenerators() {
        // Test valid username generation
        String validUsername = PropertyTestGenerators.validUsernames().sample();
        assertNotNull(validUsername, "Valid username should be generated");
        assertTrue(validUsername.length() >= 3, "Username should be at least 3 characters");
        assertTrue(validUsername.length() <= 20, "Username should be at most 20 characters");
        assertTrue(validUsername.matches("^[a-z][a-z0-9]*$"), "Username should match pattern");
        
        // Test valid password generation
        String validPassword = PropertyTestGenerators.validPasswords().sample();
        assertNotNull(validPassword, "Valid password should be generated");
        assertTrue(validPassword.length() >= 8, "Password should be at least 8 characters");
        assertTrue(validPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$"), 
                  "Password should contain lowercase, uppercase, and digit");
        
        // Test valid email generation
        String validEmail = PropertyTestGenerators.validEmails().sample();
        assertNotNull(validEmail, "Valid email should be generated");
        assertTrue(validEmail.contains("@"), "Email should contain @");
        assertTrue(validEmail.contains("."), "Email should contain domain");
        
        // Test valid amount generation
        BigDecimal validAmount = PropertyTestGenerators.validAmounts().sample();
        assertNotNull(validAmount, "Valid amount should be generated");
        assertTrue(validAmount.compareTo(BigDecimal.valueOf(100)) >= 0, 
                  "Amount should be at least 100");
        assertTrue(validAmount.compareTo(BigDecimal.valueOf(1000000)) <= 0, 
                  "Amount should be at most 1,000,000");
        
        // Test user role generation
        UserRole role = PropertyTestGenerators.userRoles().sample();
        assertNotNull(role, "User role should be generated");
        assertTrue(role == UserRole.ADMIN || role == UserRole.SALES || role == UserRole.REGULAR, 
                  "Role should be valid");
        
        // Test lead status generation
        LeadStatus status = PropertyTestGenerators.leadStatuses().sample();
        assertNotNull(status, "Lead status should be generated");
        
        // Test date time generation
        LocalDateTime pastDateTime = PropertyTestGenerators.pastDateTimes().sample();
        assertNotNull(pastDateTime, "Past date time should be generated");
        assertTrue(pastDateTime.isBefore(LocalDateTime.now()), "Should be in the past");
    }
    
    /**
     * Test 5: Test Data Factory
     * Validates that TestDataFactory creates realistic test entities.
     */
    @Test
    void testTestDataFactory() {
        // Test random user creation
        User randomUser = TestDataFactory.createRandomUser();
        assertNotNull(randomUser, "Random user should be created");
        assertNotNull(randomUser.getUsername(), "Username should not be null");
        assertNotNull(randomUser.getEmail(), "Email should not be null");
        assertNotNull(randomUser.getFirstName(), "First name should not be null");
        assertNotNull(randomUser.getLastName(), "Last name should not be null");
        assertNotNull(randomUser.getRole(), "Role should not be null");
        assertTrue(randomUser.getPassword().startsWith("$2a$"), "Password should be encoded");
        
        // Test specific user creation
        User specificUser = TestDataFactory.createUser("testuser", "test@example.com", 
                                                       "Test", "User", UserRole.SALES);
        assertEquals("testuser", specificUser.getUsername(), "Username should match");
        assertEquals("test@example.com", specificUser.getEmail(), "Email should match");
        assertEquals("Test", specificUser.getFirstName(), "First name should match");
        assertEquals("User", specificUser.getLastName(), "Last name should match");
        assertEquals(UserRole.SALES, specificUser.getRole(), "Role should match");
        
        // Test random customer creation
        Customer randomCustomer = TestDataFactory.createRandomCustomer(randomUser);
        assertNotNull(randomCustomer, "Random customer should be created");
        assertNotNull(randomCustomer.getName(), "Customer name should not be null");
        assertNotNull(randomCustomer.getEmail(), "Customer email should not be null");
        assertNotNull(randomCustomer.getPhone(), "Customer phone should not be null");
        assertNotNull(randomCustomer.getCompany(), "Customer company should not be null");
        assertNotNull(randomCustomer.getAddress(), "Customer address should not be null");
        assertEquals(randomUser, randomCustomer.getCreatedBy(), "Created by should match");
        
        // Test random lead creation
        Lead randomLead = TestDataFactory.createRandomLead(randomCustomer, randomUser, randomUser);
        assertNotNull(randomLead, "Random lead should be created");
        assertNotNull(randomLead.getTitle(), "Lead title should not be null");
        assertNotNull(randomLead.getDescription(), "Lead description should not be null");
        assertNotNull(randomLead.getEstimatedValue(), "Lead estimated value should not be null");
        assertTrue(randomLead.getEstimatedValue().compareTo(BigDecimal.ZERO) > 0, 
                  "Estimated value should be positive");
        assertEquals(randomCustomer, randomLead.getCustomer(), "Customer should match");
        assertEquals(randomUser, randomLead.getAssignedTo(), "Assigned to should match");
        
        // Test bulk creation
        List<User> randomUsers = TestDataFactory.createRandomUsers(5);
        assertEquals(5, randomUsers.size(), "Should create 5 users");
        randomUsers.forEach(user -> {
            assertNotNull(user.getUsername(), "Each user should have username");
            assertNotNull(user.getEmail(), "Each user should have email");
        });
    }
    
    /**
     * Test 6: Mock External Dependencies
     * Validates that external dependencies are properly mocked.
     */
    @Test
    void testMockExternalDependencies() {
        // Test mock mail sender
        assertNotNull(mockMailSender, "Mock mail sender should be injected");
        
        // Verify initial state
        assertEquals(0, mockMailSender.getSentMessageCount(), "Should start with no sent messages");
        assertTrue(mockMailSender.getSentMessages().isEmpty(), "Sent messages list should be empty");
        
        // Test sending mock email
        TestConfig.MockSimpleMailMessage message = new TestConfig.MockSimpleMailMessage();
        message.setTo("test@example.com", "test2@example.com");
        message.setSubject("Test Subject");
        message.setText("Test message body");
        message.setFrom("sender@example.com");
        
        mockMailSender.send(message);
        
        // Verify email was captured
        assertEquals(1, mockMailSender.getSentMessageCount(), "Should have 1 sent message");
        
        List<TestConfig.MockSimpleMailMessage> sentMessages = mockMailSender.getSentMessages();
        assertEquals(1, sentMessages.size(), "Sent messages list should have 1 message");
        
        TestConfig.MockSimpleMailMessage sentMessage = sentMessages.get(0);
        assertEquals("Test Subject", sentMessage.getSubject(), "Subject should match");
        assertEquals("test@example.com", sentMessage.getTo()[0], "First recipient should match");
        assertEquals("test2@example.com", sentMessage.getTo()[1], "Second recipient should match");
        assertEquals("Test message body", sentMessage.getText(), "Text should match");
        assertEquals("sender@example.com", sentMessage.getFrom(), "From should match");
        
        // Test multiple messages
        TestConfig.MockSimpleMailMessage message2 = new TestConfig.MockSimpleMailMessage();
        message2.setTo("another@example.com");
        message2.setSubject("Another Subject");
        mockMailSender.send(message2);
        
        assertEquals(2, mockMailSender.getSentMessageCount(), "Should have 2 sent messages");
        
        // Test clearing messages
        mockMailSender.clearSentMessages();
        assertEquals(0, mockMailSender.getSentMessageCount(), "Should have no messages after clear");
        assertTrue(mockMailSender.getSentMessages().isEmpty(), "Sent messages list should be empty after clear");
    }
    
    /**
     * Test 7: Database Schema Compatibility
     * Validates that the database schema works correctly with H2 in PostgreSQL mode.
     */
    @Test
    void testDatabaseSchemaCompatibility() {
        // Test entity persistence and retrieval
        testDataInitializer.initializeBasicTestData();
        
        // Test user entity
        User adminUser = testDataInitializer.getAdminUser();
        User retrievedUser = userRepository.findById(adminUser.getId()).orElse(null);
        assertNotNull(retrievedUser, "User should be retrievable");
        assertEquals(adminUser.getUsername(), retrievedUser.getUsername(), "Username should match");
        assertEquals(adminUser.getRole(), retrievedUser.getRole(), "Role should match");
        
        // Test customer entity with relationships
        List<Customer> customers = testDataInitializer.getAllTestCustomers();
        if (!customers.isEmpty()) {
            Customer customer = customers.get(0);
            Customer retrievedCustomer = customerRepository.findById(customer.getId()).orElse(null);
            assertNotNull(retrievedCustomer, "Customer should be retrievable");
            assertEquals(customer.getName(), retrievedCustomer.getName(), "Customer name should match");
            assertNotNull(retrievedCustomer.getCreatedBy(), "Created by relationship should work");
        }
        
        // Test complex queries
        List<User> salesUsers = userRepository.findByRole(UserRole.SALES);
        assertTrue(salesUsers.size() >= 2, "Should find sales users");
        
        List<Customer> customersByCreator = customerRepository.findByCreatedBy(adminUser);
        assertNotNull(customersByCreator, "Should find customers by creator");
        
        // Test date/time handling
        LocalDateTime now = LocalDateTime.now();
        User testUser = TestDataFactory.createUser("datetest", "date@test.com", 
                                                   "Date", "Test", UserRole.REGULAR);
        testUser.setCreatedAt(now);
        testUser.setLastLogin(now.minusHours(1));
        
        User savedUser = userRepository.save(testUser);
        User retrievedDateUser = userRepository.findById(savedUser.getId()).orElse(null);
        
        assertNotNull(retrievedDateUser, "User with dates should be retrievable");
        assertNotNull(retrievedDateUser.getCreatedAt(), "Created at should be preserved");
        assertNotNull(retrievedDateUser.getLastLogin(), "Last login should be preserved");
    }
    
    /**
     * Test 8: Transaction Management
     * Validates that test transactions are properly managed.
     */
    @Test
    void testTransactionManagement() {
        // Get initial count
        long initialCount = userRepository.count();
        
        // Create test data within transaction
        User testUser = TestDataFactory.createUser("txtest", "tx@test.com", 
                                                   "Transaction", "Test", UserRole.REGULAR);
        userRepository.save(testUser);
        
        // Verify data exists within transaction
        assertEquals(initialCount + 1, userRepository.count(), "User should be added within transaction");
        
        User retrievedUser = userRepository.findById(testUser.getId()).orElse(null);
        assertNotNull(retrievedUser, "User should be retrievable within transaction");
        assertEquals("txtest", retrievedUser.getUsername(), "Username should match");
        
        // Transaction will be rolled back after test method due to @Transactional annotation
        // This ensures test isolation
    }
}