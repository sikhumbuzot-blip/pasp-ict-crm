package com.pasp.ict.salescrm.testutil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.pasp.ict.salescrm.entity.AuditLog;
import com.pasp.ict.salescrm.entity.Customer;
import com.pasp.ict.salescrm.entity.InteractionLog;
import com.pasp.ict.salescrm.entity.InteractionType;
import com.pasp.ict.salescrm.entity.Lead;
import com.pasp.ict.salescrm.entity.LeadStatus;
import com.pasp.ict.salescrm.entity.SaleTransaction;
import com.pasp.ict.salescrm.entity.User;
import com.pasp.ict.salescrm.entity.UserRole;

/**
 * Factory class for creating test data objects.
 * Provides methods to create valid test entities with realistic data.
 */
public class TestDataFactory {
    
    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private static final Random random = new Random();
    
    // Sample data arrays for realistic test data generation
    private static final String[] FIRST_NAMES = {
        "John", "Jane", "Michael", "Sarah", "David", "Lisa", "Robert", "Emily",
        "James", "Jessica", "William", "Ashley", "Richard", "Amanda", "Thomas", "Jennifer"
    };
    
    private static final String[] LAST_NAMES = {
        "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
        "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas"
    };
    
    private static final String[] COMPANY_NAMES = {
        "Tech Solutions Inc", "Global Enterprises", "Innovation Corp", "Digital Dynamics",
        "Future Systems", "Smart Solutions", "Advanced Technologies", "Premier Services",
        "Elite Consulting", "Strategic Partners", "Dynamic Solutions", "Innovative Designs"
    };
    
    private static final String[] LEAD_TITLES = {
        "Software License Renewal", "Cloud Migration Project", "Security Audit Services",
        "Training Program", "System Integration", "Mobile App Development", "Data Analytics Platform",
        "Website Redesign", "Infrastructure Upgrade", "Consulting Services", "Support Contract",
        "Custom Development", "Process Automation", "Digital Transformation"
    };
    
    private static final String[] INTERACTION_NOTES = {
        "Initial contact call - discussed requirements",
        "Sent proposal document and pricing information",
        "In-person meeting to review technical specifications",
        "Follow-up call regarding contract terms",
        "Demo session for the proposed solution",
        "Customer requested additional features",
        "Closing call - finalized terms and conditions",
        "Technical discussion about implementation"
    };
    
    /**
     * Creates a test user with random data.
     */
    public static User createRandomUser() {
        return createRandomUser(getRandomUserRole());
    }
    
    /**
     * Creates a test user with specified role.
     */
    public static User createRandomUser(UserRole role) {
        String firstName = getRandomElement(FIRST_NAMES);
        String lastName = getRandomElement(LAST_NAMES);
        String username = (firstName + lastName + random.nextInt(1000)).toLowerCase();
        String email = username + "@test.com";
        
        User user = new User(username, passwordEncoder.encode("Password123"), 
                           email, firstName, lastName, role);
        user.setId((long) (random.nextInt(10000) + 1000));
        user.setActive(random.nextBoolean() || role == UserRole.ADMIN); // Admins are always active
        user.setCreatedAt(getRandomPastDateTime());
        user.setLastLogin(random.nextBoolean() ? getRandomPastDateTime() : null);
        
        return user;
    }
    
    /**
     * Creates a test user with specific parameters.
     */
    public static User createUser(String username, String email, String firstName, 
                                String lastName, UserRole role) {
        User user = new User(username, passwordEncoder.encode("Password123"), 
                           email, firstName, lastName, role);
        user.setId((long) (random.nextInt(10000) + 1000));
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        
        return user;
    }
    
    /**
     * Creates a test customer with random data.
     */
    public static Customer createRandomCustomer(User createdBy) {
        String companyName = getRandomElement(COMPANY_NAMES);
        String firstName = getRandomElement(FIRST_NAMES);
        String lastName = getRandomElement(LAST_NAMES);
        String name = firstName + " " + lastName;
        String email = (firstName + "." + lastName + "@" + 
                       companyName.toLowerCase().replaceAll("[^a-z]", "") + ".com");
        String phone = generateRandomPhone();
        String address = generateRandomAddress();
        
        Customer customer = new Customer(name, email, phone, companyName, address, createdBy);
        customer.setId((long) (random.nextInt(10000) + 1000));
        customer.setCreatedAt(getRandomPastDateTime());
        customer.setUpdatedAt(getRandomPastDateTime());
        
        return customer;
    }
    
    /**
     * Creates a test customer with specific parameters.
     */
    public static Customer createCustomer(String name, String email, String phone, 
                                        String company, String address, User createdBy) {
        Customer customer = new Customer(name, email, phone, company, address, createdBy);
        customer.setId((long) (random.nextInt(10000) + 1000));
        customer.setCreatedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());
        
        return customer;
    }
    
    /**
     * Creates a test lead with random data.
     */
    public static Lead createRandomLead(Customer customer, User assignedTo, User createdBy) {
        String title = getRandomElement(LEAD_TITLES);
        String description = "Description for " + title.toLowerCase();
        BigDecimal estimatedValue = BigDecimal.valueOf(
            ThreadLocalRandom.current().nextDouble(1000, 100000)
        ).setScale(2, java.math.RoundingMode.HALF_UP);
        LeadStatus status = getRandomLeadStatus();
        
        Lead lead = new Lead(title, description, estimatedValue, customer, assignedTo);
        lead.setId((long) (random.nextInt(10000) + 1000));
        lead.setStatus(status);
        lead.setCreatedAt(getRandomPastDateTime());
        lead.setUpdatedAt(getRandomPastDateTime());
        
        return lead;
    }
    
    /**
     * Creates a test lead with specific parameters.
     */
    public static Lead createLead(String title, String description, BigDecimal estimatedValue,
                                Customer customer, User assignedTo, User createdBy) {
        Lead lead = new Lead(title, description, estimatedValue, customer, assignedTo);
        lead.setId((long) (random.nextInt(10000) + 1000));
        lead.setCreatedAt(LocalDateTime.now());
        lead.setUpdatedAt(LocalDateTime.now());
        
        return lead;
    }
    
    /**
     * Creates a test sale transaction with random data.
     */
    public static SaleTransaction createRandomSaleTransaction(Customer customer, User salesUser, Lead lead) {
        BigDecimal amount = BigDecimal.valueOf(
            ThreadLocalRandom.current().nextDouble(1000, 150000)
        ).setScale(2, java.math.RoundingMode.HALF_UP);
        LocalDateTime saleDate = getRandomPastDateTime();
        String description = "Sale transaction for " + customer.getCompany();
        
        SaleTransaction transaction = new SaleTransaction(amount, saleDate, description, 
                                                        customer, salesUser, lead);
        transaction.setId((long) (random.nextInt(10000) + 1000));
        
        return transaction;
    }
    
    /**
     * Creates a test sale transaction with specific parameters.
     */
    public static SaleTransaction createSaleTransaction(BigDecimal amount, LocalDateTime saleDate,
                                                      String description, Customer customer, 
                                                      User salesUser, Lead lead) {
        SaleTransaction transaction = new SaleTransaction(amount, saleDate, description, 
                                                        customer, salesUser, lead);
        transaction.setId((long) (random.nextInt(10000) + 1000));
        
        return transaction;
    }
    
    /**
     * Creates a test interaction log with random data.
     */
    public static InteractionLog createRandomInteractionLog(Customer customer, User user) {
        InteractionType type = getRandomInteractionType();
        String notes = getRandomElement(INTERACTION_NOTES);
        LocalDateTime timestamp = getRandomPastDateTime();
        
        InteractionLog interaction = new InteractionLog(type, notes, customer, user);
        interaction.setId((long) (random.nextInt(10000) + 1000));
        interaction.setTimestamp(timestamp);
        
        return interaction;
    }
    
    /**
     * Creates a test interaction log with specific parameters.
     */
    public static InteractionLog createInteractionLog(InteractionType type, String notes,
                                                    Customer customer, User user) {
        InteractionLog interaction = new InteractionLog(type, notes, customer, user);
        interaction.setId((long) (random.nextInt(10000) + 1000));
        interaction.setTimestamp(LocalDateTime.now());
        
        return interaction;
    }
    
    /**
     * Creates a test audit log with random data.
     */
    public static AuditLog createRandomAuditLog(User user) {
        String[] actions = {"USER_CREATED", "CUSTOMER_CREATED", "LEAD_CREATED", 
                           "SALE_CREATED", "USER_UPDATED", "CUSTOMER_UPDATED"};
        String[] entityTypes = {"User", "Customer", "Lead", "SaleTransaction"};
        
        String action = getRandomElement(actions);
        String entityType = getRandomElement(entityTypes);
        Long entityId = (long) (random.nextInt(1000) + 1);
        String oldValues = random.nextBoolean() ? "old_value=test" : null;
        String newValues = "new_value=test";
        LocalDateTime timestamp = getRandomPastDateTime();
        
        AuditLog auditLog = new AuditLog(action, entityType, entityId, oldValues, newValues, user);
        auditLog.setId((long) (random.nextInt(10000) + 1000));
        auditLog.setTimestamp(timestamp);
        
        return auditLog;
    }
    
    /**
     * Creates a test audit log with specific parameters.
     */
    public static AuditLog createAuditLog(String action, String entityType, Long entityId,
                                        String oldValues, String newValues, User user) {
        AuditLog auditLog = new AuditLog(action, entityType, entityId, oldValues, newValues, user);
        auditLog.setId((long) (random.nextInt(10000) + 1000));
        auditLog.setTimestamp(LocalDateTime.now());
        
        return auditLog;
    }
    
    /**
     * Creates a list of random users.
     */
    public static List<User> createRandomUsers(int count) {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            users.add(createRandomUser());
        }
        return users;
    }
    
    /**
     * Creates a list of random customers.
     */
    public static List<Customer> createRandomCustomers(int count, User createdBy) {
        List<Customer> customers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            customers.add(createRandomCustomer(createdBy));
        }
        return customers;
    }
    
    /**
     * Creates a list of random leads.
     */
    public static List<Lead> createRandomLeads(int count, List<Customer> customers, 
                                             List<User> salesUsers, User createdBy) {
        List<Lead> leads = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Customer customer = getRandomElement(customers);
            User assignedTo = getRandomElement(salesUsers);
            leads.add(createRandomLead(customer, assignedTo, createdBy));
        }
        return leads;
    }
    
    // Helper methods
    
    private static <T> T getRandomElement(T[] array) {
        return array[random.nextInt(array.length)];
    }
    
    private static <T> T getRandomElement(List<T> list) {
        return list.get(random.nextInt(list.size()));
    }
    
    private static UserRole getRandomUserRole() {
        UserRole[] roles = UserRole.values();
        return roles[random.nextInt(roles.length)];
    }
    
    private static LeadStatus getRandomLeadStatus() {
        LeadStatus[] statuses = LeadStatus.values();
        return statuses[random.nextInt(statuses.length)];
    }
    
    private static InteractionType getRandomInteractionType() {
        InteractionType[] types = InteractionType.values();
        return types[random.nextInt(types.length)];
    }
    
    private static LocalDateTime getRandomPastDateTime() {
        long minDay = LocalDateTime.now().minusDays(365).toEpochSecond(java.time.ZoneOffset.UTC);
        long maxDay = LocalDateTime.now().toEpochSecond(java.time.ZoneOffset.UTC);
        long randomDay = ThreadLocalRandom.current().nextLong(minDay, maxDay);
        return LocalDateTime.ofEpochSecond(randomDay, 0, java.time.ZoneOffset.UTC);
    }
    
    private static String generateRandomPhone() {
        return String.format("555-%04d", random.nextInt(10000));
    }
    
    private static String generateRandomAddress() {
        String[] streets = {"Main St", "Oak Ave", "Pine Rd", "Elm Dr", "Maple Ln", "Cedar Blvd"};
        int number = random.nextInt(9999) + 1;
        String street = getRandomElement(streets);
        return number + " " + street;
    }
    
    /**
     * Generates a random valid password that meets complexity requirements.
     */
    public static String generateValidPassword() {
        String[] passwords = {
            "Password123", "SecurePass1", "TestPass99", "ValidPwd1", 
            "StrongPwd2", "GoodPass3", "SafeWord4", "TrustMe5"
        };
        return getRandomElement(passwords);
    }
    
    /**
     * Generates a random invalid password that fails complexity requirements.
     */
    public static String generateInvalidPassword() {
        String[] passwords = {
            "weak", "123", "password", "abc", "short", "NOLOWER", "noupper", "nonumber"
        };
        return getRandomElement(passwords);
    }
    
    /**
     * Generates a random valid email address.
     */
    public static String generateValidEmail() {
        String[] domains = {"test.com", "example.org", "sample.net", "demo.co"};
        String name = getRandomElement(FIRST_NAMES).toLowerCase() + 
                     getRandomElement(LAST_NAMES).toLowerCase() + 
                     random.nextInt(1000);
        return name + "@" + getRandomElement(domains);
    }
    
    /**
     * Generates a random invalid email address.
     */
    public static String generateInvalidEmail() {
        String[] invalidEmails = {
            "notanemail", "missing@", "@domain.com", "spaces @domain.com", 
            "double@@domain.com", "no-domain@", "toolong" + "x".repeat(100) + "@domain.com"
        };
        return getRandomElement(invalidEmails);
    }
}