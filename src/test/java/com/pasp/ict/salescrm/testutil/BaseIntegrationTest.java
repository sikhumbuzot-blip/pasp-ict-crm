package com.pasp.ict.salescrm.testutil;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.pasp.ict.salescrm.config.TestConfig;
import com.pasp.ict.salescrm.entity.User;

/**
 * Base class for integration tests with common setup and utilities.
 * Provides access to test data and common test infrastructure.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
public abstract class BaseIntegrationTest {
    
    @Autowired
    protected TestDatabaseInitializer testDataInitializer;
    
    @Autowired
    protected TestConfig.MockJavaMailSender mockMailSender;
    
    protected User adminUser;
    protected User salesUser1;
    protected User salesUser2;
    protected User regularUser;
    
    /**
     * Sets up basic test data before each test.
     * Subclasses can override this method to customize test data setup.
     */
    @BeforeEach
    protected void setUpBaseTest() {
        // Clear any previous test data
        testDataInitializer.clearAllData();
        
        // Clear mock mail sender
        if (mockMailSender != null) {
            mockMailSender.clearSentMessages();
        }
        
        // Initialize basic test data
        testDataInitializer.initializeBasicTestData();
        
        // Cache commonly used test users
        adminUser = testDataInitializer.getAdminUser();
        salesUser1 = testDataInitializer.getSalesUser1();
        salesUser2 = testDataInitializer.getSalesUser2();
        regularUser = testDataInitializer.getRegularUser();
    }
    
    /**
     * Initializes full test data including leads, sales, and interactions.
     * Call this method in tests that need complete test data.
     */
    protected void initializeFullTestData() {
        testDataInitializer.initializeFullTestData();
        
        // Refresh cached users
        adminUser = testDataInitializer.getAdminUser();
        salesUser1 = testDataInitializer.getSalesUser1();
        salesUser2 = testDataInitializer.getSalesUser2();
        regularUser = testDataInitializer.getRegularUser();
    }
    
    /**
     * Initializes sales-specific test data.
     * Call this method in tests that focus on sales functionality.
     */
    protected void initializeSalesTestData() {
        testDataInitializer.initializeSalesTestData();
        
        // Refresh cached users
        adminUser = testDataInitializer.getAdminUser();
        salesUser1 = testDataInitializer.getSalesUser1();
        salesUser2 = testDataInitializer.getSalesUser2();
        regularUser = testDataInitializer.getRegularUser();
    }
    
    /**
     * Asserts that an email was sent with the specified subject.
     */
    protected void assertEmailSent(String expectedSubject) {
        var sentMessages = mockMailSender.getSentMessages();
        boolean found = sentMessages.stream()
                .anyMatch(msg -> expectedSubject.equals(msg.getSubject()));
        
        if (!found) {
            throw new AssertionError("Expected email with subject '" + expectedSubject + 
                                   "' was not sent. Sent messages: " + 
                                   sentMessages.stream().map(msg -> msg.getSubject()).toList());
        }
    }
    
    /**
     * Asserts that an email was sent to the specified recipient.
     */
    protected void assertEmailSentTo(String expectedRecipient) {
        var sentMessages = mockMailSender.getSentMessages();
        boolean found = sentMessages.stream()
                .anyMatch(msg -> msg.getTo() != null && 
                               java.util.Arrays.asList(msg.getTo()).contains(expectedRecipient));
        
        if (!found) {
            throw new AssertionError("Expected email to '" + expectedRecipient + 
                                   "' was not sent. Sent messages: " + 
                                   sentMessages.stream()
                                           .flatMap(msg -> msg.getTo() != null ? 
                                                   java.util.Arrays.stream(msg.getTo()) : 
                                                   java.util.stream.Stream.empty())
                                           .toList());
        }
    }
    
    /**
     * Asserts that no emails were sent.
     */
    protected void assertNoEmailsSent() {
        int sentCount = mockMailSender.getSentMessageCount();
        if (sentCount > 0) {
            throw new AssertionError("Expected no emails to be sent, but " + sentCount + " were sent");
        }
    }
    
    /**
     * Gets the number of emails sent during the test.
     */
    protected int getSentEmailCount() {
        return mockMailSender.getSentMessageCount();
    }
    
    /**
     * Clears all sent emails from the mock mail sender.
     */
    protected void clearSentEmails() {
        mockMailSender.clearSentMessages();
    }
}