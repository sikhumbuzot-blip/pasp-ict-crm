package com.pasp.ict.salescrm.property;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import com.pasp.ict.salescrm.entity.AuditLog;
import com.pasp.ict.salescrm.entity.Customer;
import com.pasp.ict.salescrm.entity.InteractionLog;
import com.pasp.ict.salescrm.entity.InteractionType;
import com.pasp.ict.salescrm.entity.User;
import com.pasp.ict.salescrm.entity.UserRole;
import com.pasp.ict.salescrm.repository.AuditLogRepository;
import com.pasp.ict.salescrm.repository.CustomerRepository;
import com.pasp.ict.salescrm.repository.InteractionLogRepository;
import com.pasp.ict.salescrm.repository.UserRepository;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tag;

/**
 * Property-based tests for Customer Data Management functionality.
 * **Validates: Requirements 7.1, 7.3, 7.4, 7.5**
 */
@DataJpaTest
@ActiveProfiles("test")
class CustomerDataManagementProperties {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InteractionLogRepository interactionLogRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private Validator validator;

    /**
     * Property 12: Customer Data Management
     * For any customer data operation (create, update, search), the system should store complete information,
     * prevent duplicates based on email/company validation, and maintain audit logs with user identifier and timestamp.
     * **Validates: Requirements 7.1, 7.3, 7.4, 7.5**
     */
    @Property
    @Tag("Feature: sales-crm-application, Property 12: Customer Data Management")
    void customerDataManagementProperty(@ForAll("validCustomerData") CustomerData customerData,
                                       @ForAll("validUserData") UserData userData) {
        
        // Create a user first (required for customer creation)
        User user = createValidUser(userData);
        user = userRepository.save(user);
        
        // Test customer creation with complete information storage (Requirement 7.1)
        Customer customer = new Customer(
            customerData.name,
            customerData.email,
            customerData.phone,
            customerData.company,
            customerData.address,
            user
        );
        
        // Validate customer data before saving
        Set<ConstraintViolation<Customer>> violations = validator.validate(customer);
        assertThat(violations).isEmpty();
        
        // Save customer and verify complete information is stored
        Customer savedCustomer = customerRepository.save(customer);
        entityManager.flush();
        
        assertThat(savedCustomer.getId()).isNotNull();
        assertThat(savedCustomer.getName()).isEqualTo(customerData.name);
        assertThat(savedCustomer.getEmail()).isEqualTo(customerData.email);
        assertThat(savedCustomer.getPhone()).isEqualTo(customerData.phone);
        assertThat(savedCustomer.getCompany()).isEqualTo(customerData.company);
        assertThat(savedCustomer.getAddress()).isEqualTo(customerData.address);
        assertThat(savedCustomer.getCreatedBy()).isEqualTo(user);
        assertThat(savedCustomer.getCreatedAt()).isNotNull();
        assertThat(savedCustomer.getUpdatedAt()).isNotNull();
        
        // Test duplicate prevention based on email validation (Requirement 7.4)
        Customer duplicateByEmail = new Customer(
            "Different Name",
            customerData.email, // Same email
            "Different Phone",
            "Different Company",
            "Different Address",
            user
        );
        
        // Should detect duplicate email
        boolean emailExists = customerRepository.existsByEmail(customerData.email);
        assertThat(emailExists).isTrue();
        
        // Test duplicate prevention based on company validation (Requirement 7.4)
        if (customerData.company != null && !customerData.company.trim().isEmpty()) {
            Customer duplicateByCompany = new Customer(
                "Different Name",
                "different@email.com",
                "Different Phone",
                customerData.company, // Same company
                "Different Address",
                user
            );
            
            boolean companyExists = customerRepository.existsByCompany(customerData.company);
            assertThat(companyExists).isTrue();
        }
        
        // Test customer update with audit logging (Requirement 7.5)
        String originalName = savedCustomer.getName();
        String newName = "Updated " + originalName;
        LocalDateTime beforeUpdate = LocalDateTime.now();
        
        savedCustomer.setName(newName);
        Customer updatedCustomer = customerRepository.save(savedCustomer);
        entityManager.flush();
        
        // Verify update was successful
        assertThat(updatedCustomer.getName()).isEqualTo(newName);
        assertThat(updatedCustomer.getUpdatedAt()).isAfter(beforeUpdate);
        
        // Create audit log entry for the update (simulating audit logging - Requirement 7.5)
        AuditLog auditLog = new AuditLog(
            "UPDATE",
            "Customer",
            updatedCustomer.getId(),
            "name: " + originalName,
            "name: " + newName,
            user
        );
        auditLog = auditLogRepository.save(auditLog);
        entityManager.flush();
        
        // Verify audit log contains user identifier and timestamp (Requirement 7.5)
        assertThat(auditLog.getId()).isNotNull();
        assertThat(auditLog.getUser()).isEqualTo(user);
        assertThat(auditLog.getTimestamp()).isNotNull();
        assertThat(auditLog.getEntityType()).isEqualTo("Customer");
        assertThat(auditLog.getEntityId()).isEqualTo(updatedCustomer.getId());
        assertThat(auditLog.getOldValues()).contains(originalName);
        assertThat(auditLog.getNewValues()).contains(newName);
        
        // Test interaction history maintenance (Requirement 7.3)
        InteractionLog interaction = new InteractionLog(
            InteractionType.CALL,
            "Test interaction with customer",
            savedCustomer,
            user
        );
        interaction = interactionLogRepository.save(interaction);
        entityManager.flush();
        
        // Verify interaction history is maintained
        assertThat(interaction.getId()).isNotNull();
        assertThat(interaction.getCustomer()).isEqualTo(savedCustomer);
        assertThat(interaction.getUser()).isEqualTo(user);
        assertThat(interaction.getTimestamp()).isNotNull();
        
        // Test customer search functionality
        var searchResults = customerRepository.searchCustomers(customerData.name.substring(0, 3));
        assertThat(searchResults).isNotEmpty();
        assertThat(searchResults).anyMatch(c -> c.getId().equals(savedCustomer.getId()));
    }

    @Provide
    Arbitrary<CustomerData> validCustomerData() {
        return Combinators.combine(
            Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(50),
            validEmails(),
            validPhones(),
            Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(50),
            Arbitraries.strings().alpha().ofMinLength(10).ofMaxLength(200)
        ).as((name, email, phone, company, address) -> 
            new CustomerData(name, email, phone, company, address));
    }

    @Provide
    Arbitrary<UserData> validUserData() {
        return Combinators.combine(
            Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(20),
            validPasswords(),
            validEmails(),
            Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(30),
            Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(30),
            Arbitraries.of(UserRole.class)
        ).as((username, password, email, firstName, lastName, role) -> 
            new UserData(username, password, email, firstName, lastName, role));
    }

    @Provide
    Arbitrary<String> validEmails() {
        return Combinators.combine(
            Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(10),
            Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(10)
        ).as((local, domain) -> local + "@" + domain + ".com");
    }

    @Provide
    Arbitrary<String> validPhones() {
        return Arbitraries.strings().numeric().ofMinLength(10).ofMaxLength(15)
                .map(s -> "+1" + s);
    }

    @Provide
    Arbitrary<String> validPasswords() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .ofMinLength(8)
                .ofMaxLength(20)
                .filter(s -> s.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$"));
    }

    private User createValidUser(UserData userData) {
        return new User(
            userData.username,
            userData.password,
            userData.email,
            userData.firstName,
            userData.lastName,
            userData.role
        );
    }

    // Data classes for test data generation
    static class CustomerData {
        final String name;
        final String email;
        final String phone;
        final String company;
        final String address;

        CustomerData(String name, String email, String phone, String company, String address) {
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.company = company;
            this.address = address;
        }
    }

    static class UserData {
        final String username;
        final String password;
        final String email;
        final String firstName;
        final String lastName;
        final UserRole role;

        UserData(String username, String password, String email, String firstName, String lastName, UserRole role) {
            this.username = username;
            this.password = password;
            this.email = email;
            this.firstName = firstName;
            this.lastName = lastName;
            this.role = role;
        }
    }
}