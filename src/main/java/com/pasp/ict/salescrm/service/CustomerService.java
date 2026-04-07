package com.pasp.ict.salescrm.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pasp.ict.salescrm.entity.AuditLog;
import com.pasp.ict.salescrm.entity.Customer;
import com.pasp.ict.salescrm.entity.InteractionLog;
import com.pasp.ict.salescrm.entity.InteractionType;
import com.pasp.ict.salescrm.entity.User;
import com.pasp.ict.salescrm.entity.UserRole;
import com.pasp.ict.salescrm.repository.AuditLogRepository;
import com.pasp.ict.salescrm.repository.CustomerRepository;
import com.pasp.ict.salescrm.repository.InteractionLogRepository;

/**
 * Service class for customer data operations and interaction logging.
 * Handles customer management, duplicate detection, and interaction history.
 */
@Service
@Transactional
public class CustomerService {
    
    private final CustomerRepository customerRepository;
    private final InteractionLogRepository interactionLogRepository;
    private final AuditLogRepository auditLogRepository;
    
    @Autowired
    public CustomerService(CustomerRepository customerRepository,
                          InteractionLogRepository interactionLogRepository,
                          AuditLogRepository auditLogRepository) {
        this.customerRepository = customerRepository;
        this.interactionLogRepository = interactionLogRepository;
        this.auditLogRepository = auditLogRepository;
    }
    
    /**
     * Create a new customer with duplicate detection.
     * @param name the customer name
     * @param email the email address
     * @param phone the phone number
     * @param company the company name
     * @param address the address
     * @param createdBy the user creating the customer
     * @return the created customer
     * @throws IllegalArgumentException if duplicate found or validation fails
     */
    public Customer createCustomer(String name, String email, String phone, 
                                  String company, String address, User createdBy) {
        validateCustomerPermission(createdBy);
        validateCustomerInput(name, email);
        
        // Check for duplicates
        checkForDuplicates(email, company, null);
        
        Customer customer = new Customer(name, email, phone, company, address, createdBy);
        Customer savedCustomer = customerRepository.save(customer);
        
        // Log the creation
        logAuditEvent("CUSTOMER_CREATED", "Customer", savedCustomer.getId(), null, 
                     String.format("Name: %s, Email: %s, Company: %s", name, email, company), 
                     createdBy);
        
        return savedCustomer;
    }
    
    /**
     * Update customer information with duplicate detection.
     * @param customerId the customer ID
     * @param name the updated name
     * @param email the updated email
     * @param phone the updated phone
     * @param company the updated company
     * @param address the updated address
     * @param updatedBy the user making the update
     * @return the updated customer
     * @throws IllegalArgumentException if customer not found or duplicate detected
     */
    public Customer updateCustomer(Long customerId, String name, String email, String phone, 
                                  String company, String address, User updatedBy) {
        validateCustomerPermission(updatedBy);
        
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + customerId));
        
        validateCustomerInput(name, email);
        
        // Check for duplicates (excluding current customer)
        checkForDuplicates(email, company, customerId);
        
        // Store old values for audit
        String oldValues = String.format("Name: %s, Email: %s, Company: %s", 
                                        customer.getName(), customer.getEmail(), customer.getCompany());
        
        // Update customer
        customer.setName(name);
        customer.setEmail(email);
        customer.setPhone(phone);
        customer.setCompany(company);
        customer.setAddress(address);
        
        Customer savedCustomer = customerRepository.save(customer);
        
        // Log the update
        String newValues = String.format("Name: %s, Email: %s, Company: %s", name, email, company);
        logAuditEvent("CUSTOMER_UPDATED", "Customer", customerId, oldValues, newValues, updatedBy);
        
        return savedCustomer;
    }
    
    /**
     * Log customer interaction with timestamp and user identification.
     * @param customerId the customer ID
     * @param type the interaction type
     * @param notes the interaction notes
     * @param user the user logging the interaction
     * @return the created interaction log
     */
    public InteractionLog logInteraction(Long customerId, InteractionType type, String notes, User user) {
        validateCustomerPermission(user);
        
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + customerId));
        
        InteractionLog interaction = new InteractionLog(type, notes, customer, user);
        InteractionLog savedInteraction = interactionLogRepository.save(interaction);
        
        // Log the interaction creation
        logAuditEvent("INTERACTION_LOGGED", "InteractionLog", savedInteraction.getId(), null, 
                     String.format("Type: %s, Customer: %s", type, customer.getName()), user);
        
        return savedInteraction;
    }
    
    /**
     * Search customers within 2 seconds for up to 10,000 records.
     * @param searchTerm the search term
     * @return List of customers matching the search criteria
     */
    @Transactional(readOnly = true)
    public List<Customer> searchCustomers(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return customerRepository.findAllOrderByCreatedAtDesc();
        }
        
        return customerRepository.searchCustomers(searchTerm.trim());
    }
    
    /**
     * Find customer by ID.
     * @param customerId the customer ID
     * @return Optional containing the customer if found
     */
    @Transactional(readOnly = true)
    public Optional<Customer> findById(Long customerId) {
        return customerRepository.findById(customerId);
    }
    
    /**
     * Find customer by email.
     * @param email the email address
     * @return Optional containing the customer if found
     */
    @Transactional(readOnly = true)
    public Optional<Customer> findByEmail(String email) {
        return customerRepository.findByEmail(email);
    }
    
    /**
     * Find customers by company.
     * @param company the company name
     * @return List of customers from the company
     */
    @Transactional(readOnly = true)
    public List<Customer> findByCompany(String company) {
        return customerRepository.findByCompany(company);
    }
    
    /**
     * Find customers created by a user.
     * @param createdBy the user who created the customers
     * @return List of customers created by the user
     */
    @Transactional(readOnly = true)
    public List<Customer> findByCreatedBy(User createdBy) {
        return customerRepository.findByCreatedBy(createdBy);
    }
    
    /**
     * Get complete interaction history for a customer.
     * @param customerId the customer ID
     * @return List of interaction logs for the customer
     */
    @Transactional(readOnly = true)
    public List<InteractionLog> getCustomerInteractionHistory(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + customerId));
        
        return interactionLogRepository.findByCustomerOrderByTimestampDesc(customer);
    }
    
    /**
     * Get recent interactions for a customer.
     * @param customerId the customer ID
     * @param days number of days to look back
     * @return List of recent interaction logs
     */
    @Transactional(readOnly = true)
    public List<InteractionLog> getRecentCustomerInteractions(Long customerId, int days) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + customerId));
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        return interactionLogRepository.findRecentCustomerInteractions(customer, cutoffDate);
    }
    
    /**
     * Get interactions by type for a customer.
     * @param customerId the customer ID
     * @param type the interaction type
     * @return List of interaction logs of the specified type
     */
    @Transactional(readOnly = true)
    public List<InteractionLog> getCustomerInteractionsByType(Long customerId, InteractionType type) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + customerId));
        
        return interactionLogRepository.findByCustomerAndType(customer, type);
    }
    
    /**
     * Find customers with recent activity.
     * @param days number of days to look back
     * @return List of customers with recent activity
     */
    @Transactional(readOnly = true)
    public List<Customer> findCustomersWithRecentActivity(int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        return customerRepository.findCustomersWithRecentActivity(cutoffDate);
    }
    
    /**
     * Find customers without recent interactions.
     * @param days number of days to look back
     * @return List of customers without recent interactions
     */
    @Transactional(readOnly = true)
    public List<Customer> findCustomersWithoutRecentInteractions(int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        return interactionLogRepository.findCustomersWithNoRecentInteractions(cutoffDate);
    }
    
    /**
     * Find customers created in date range.
     * @param startDate the start date
     * @param endDate the end date
     * @return List of customers created in the date range
     */
    @Transactional(readOnly = true)
    public List<Customer> findCustomersCreatedBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return customerRepository.findCustomersCreatedBetween(startDate, endDate);
    }
    
    /**
     * Count customers created by a user.
     * @param createdBy the user
     * @return count of customers created by the user
     */
    @Transactional(readOnly = true)
    public long countCustomersCreatedBy(User createdBy) {
        return customerRepository.countByCreatedBy(createdBy);
    }
    
    /**
     * Count interactions for a customer.
     * @param customerId the customer ID
     * @return count of interactions for the customer
     */
    @Transactional(readOnly = true)
    public long countCustomerInteractions(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + customerId));
        
        return interactionLogRepository.countByCustomer(customer);
    }
    
    /**
     * Check for duplicate customers based on email and company validation.
     */
    private void checkForDuplicates(String email, String company, Long excludeId) {
        // Check for exact email match
        Optional<Customer> existingByEmail = customerRepository.findByEmail(email);
        if (existingByEmail.isPresent() && 
            (excludeId == null || !existingByEmail.get().getId().equals(excludeId))) {
            throw new IllegalArgumentException("Customer with email already exists: " + email);
        }
        
        // Check for potential duplicates by email or company
        if (excludeId != null) {
            List<Customer> potentialDuplicates = customerRepository.findPotentialDuplicates(email, company, excludeId);
            if (!potentialDuplicates.isEmpty()) {
                throw new IllegalArgumentException("Potential duplicate customer found with same email or company");
            }
        }
    }
    
    /**
     * Validate customer permission.
     */
    private void validateCustomerPermission(User user) {
        if (user == null || !user.isActive() || 
            (user.getRole() != UserRole.SALES && user.getRole() != UserRole.ADMIN)) {
            throw new SecurityException("Customer management permission required");
        }
    }
    
    /**
     * Validate customer input data.
     */
    private void validateCustomerInput(String name, String email) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name is required");
        }
        if (name.length() > 100) {
            throw new IllegalArgumentException("Customer name must not exceed 100 characters");
        }
        
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$")) {
            throw new IllegalArgumentException("Email must be valid");
        }
    }
    
    /**
     * Log audit event.
     */
    private void logAuditEvent(String action, String entityType, Long entityId, 
                              String oldValues, String newValues, User user) {
        AuditLog auditLog = new AuditLog(action, entityType, entityId, oldValues, newValues, user);
        auditLogRepository.save(auditLog);
    }
}