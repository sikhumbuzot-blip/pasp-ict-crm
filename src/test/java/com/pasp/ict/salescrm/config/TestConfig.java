package com.pasp.ict.salescrm.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;

/**
 * Test configuration for mocking external dependencies and setting up test-specific beans.
 */
@TestConfiguration
@Profile("test")
public class TestConfig {
    
    /**
     * Mock mail sender for testing email functionality without sending actual emails.
     */
    @Bean
    @Primary
    public MockJavaMailSender mockMailSender() {
        return new MockJavaMailSender();
    }
    
    /**
     * Password encoder for test environment.
     */
    @Bean
    @Primary
    public PasswordEncoder testPasswordEncoder() {
        return new BCryptPasswordEncoder(4); // Lower strength for faster tests
    }
    
    /**
     * Simplified mock implementation of mail sender for testing.
     */
    public static class MockJavaMailSender {
        
        private final List<MockSimpleMailMessage> sentMessages = new ArrayList<>();
        private int mimeMessageCount = 0;
        
        public void send(MockSimpleMailMessage simpleMessage) {
            sentMessages.add(simpleMessage);
            System.out.println("Mock email sent to: " + String.join(", ", simpleMessage.getTo()) + 
                             " Subject: " + simpleMessage.getSubject());
        }
        
        public void send(MockSimpleMailMessage... simpleMessages) {
            for (MockSimpleMailMessage message : simpleMessages) {
                send(message);
            }
        }
        
        public void sendMimeMessage() {
            mimeMessageCount++;
            System.out.println("Mock MIME email sent");
        }
        
        // Test helper methods
        public List<MockSimpleMailMessage> getSentMessages() {
            return new ArrayList<>(sentMessages);
        }
        
        public void clearSentMessages() {
            sentMessages.clear();
            mimeMessageCount = 0;
        }
        
        public int getSentMessageCount() {
            return sentMessages.size() + mimeMessageCount;
        }
    }
    
    /**
     * Mock implementation of SimpleMailMessage for testing.
     */
    public static class MockSimpleMailMessage {
        private String[] to;
        private String subject;
        private String text;
        private String from;
        
        public String[] getTo() { return to; }
        public void setTo(String... to) { this.to = to; }
        
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        
        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
        
        @Override
        public String toString() {
            return "MockSimpleMailMessage{to=" + String.join(",", to != null ? to : new String[0]) + 
                   ", subject='" + subject + "'}";
        }
    }
}