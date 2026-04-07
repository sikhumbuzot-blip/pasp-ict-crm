package com.pasp.ict.salescrm.testutil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
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

/**
 * Utility class for initializing test database with sample data.
 * Can be used programmatically in tests that need pre-populated data.
 */
@Component
public class TestDatabaseInitializer {
    
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
    
    private List<User> testUsers = new ArrayList<>();
    private List<Customer> testCustomers = new ArrayList<>();
    private List<Lead> testLeads = new ArrayList<>();
    private List<SaleTransaction> testSales = new ArrayList<>();
    
    /**
     * Initializes the test database with a complete set of sample data.
     * This method is transactional and will rollback if any error occurs.
     */
    @Transactional
    public void initializeFullTestData() {
        clearAllData();
        createTestUsers();
        createTestCustomers();
        createTestLeads();
        createTestSaleTransactions();
        createTestInteractionLogs();
        createTestAuditLogs();
    }
    
    /**
     * Initializes only basic test data (users and customers).
     */
    @Transactional
    public void initializeBasicTestData() {
        clearAllData();
        createTestUsers();
        createTestCustomers();
    }
    
    /**
     * Initializes test data for sales workflow testing.
     */
    @Transactional
    public void initializeSalesTestData() {
        clearAllData();
        createTestUsers();
        createTestCustomers();
        createTestLeads();
        createTestSaleTransactions();
    }
    
    /**
     * Clears all test data from the database.
     */
    @Transactional
    public void clearAllData() {
        // Clear in reverse dependency order to avoid foreign key constraints
        auditLogRepository.deleteAll();
        interactionLogRepository.deleteAll();
        saleTransactionRepository.deleteAll();
        leadRepository.deleteAll();
        customerRepository.deleteAll();
        userRepository.deleteAll();
        
        // Clear collections
        testUsers.clear();
        testCustomers.clear();
        testLeads.clear();
        testSales.clear();
        
        // Force flush to ensure data is actually deleted
        auditLogRepository.flush();
        interactionLogRepository.flush();
        saleTransactionRepository.flush();
        leadRepository.flush();
        customerRepository.flush();
        userRepository.flush();
    }
    
    /**
     * Creates test users with different roles.
     */
    private void createTestUsers() {
        // Admin user
        User admin = new User("admin", passwordEncoder.encode("Password123"), 
                            "admin@salescrm.com", "System", "Administrator", UserRole.ADMIN);
        admin.setActive(true);
        admin.setCreatedAt(LocalDateTime.now().minusDays(30));
        admin.setLastLogin(LocalDateTime.now().minusHours(1));
        testUsers.add(userRepository.save(admin));
        
        // Sales users
        User sales1 = new User("sales1", passwordEncoder.encode("Password123"), 
                             "sales1@salescrm.com", "John", "Smith", UserRole.SALES);
        sales1.setActive(true);
        sales1.setCreatedAt(LocalDateTime.now().minusDays(25));
        sales1.setLastLogin(LocalDateTime.now().minusHours(2));
        testUsers.add(userRepository.save(sales1));
        
        User sales2 = new User("sales2", passwordEncoder.encode("Password123"), 
                             "sales2@salescrm.com", "Jane", "Johnson", UserRole.SALES);
        sales2.setActive(true);
        sales2.setCreatedAt(LocalDateTime.now().minusDays(20));
        sales2.setLastLogin(LocalDateTime.now().minusHours(3));
        testUsers.add(userRepository.save(sales2));
        
        // Regular user
        User regular = new User("regular1", passwordEncoder.encode("Password123"), 
                              "regular1@salescrm.com", "Bob", "Wilson", UserRole.REGULAR);
        regular.setActive(true);
        regular.setCreatedAt(LocalDateTime.now().minusDays(15));
        regular.setLastLogin(LocalDateTime.now().minusHours(4));
        testUsers.add(userRepository.save(regular));
        
        // Inactive user
        User inactive = new User("inactive", passwordEncoder.encode("Password123"), 
                               "inactive@salescrm.com", "Inactive", "User", UserRole.REGULAR);
        inactive.setActive(false);
        inactive.setCreatedAt(LocalDateTime.now().minusDays(10));
        testUsers.add(userRepository.save(inactive));
    }
    
    /**
     * Creates test customers.
     */
    private void createTestCustomers() {
        User sales1 = getSalesUser1();
        User sales2 = getSalesUser2();
        
        Customer[] customers = {
            new Customer("Acme Corporation", "contact@acme.com", "555-0101", 
                        "Acme Corp", "123 Business Ave, Suite 100", sales1),
            new Customer("TechStart Inc", "info@techstart.com", "555-0102", 
                        "TechStart Inc", "456 Innovation Dr", sales1),
            new Customer("Global Solutions", "sales@globalsolutions.com", "555-0103", 
                        "Global Solutions LLC", "789 Enterprise Blvd", sales2),
            new Customer("Local Business", "owner@localbiz.com", "555-0104", 
                        "Local Business Co", "321 Main Street", sales1),
            new Customer("Enterprise Client", "procurement@enterprise.com", "555-0105", 
                        "Enterprise Client Corp", "654 Corporate Plaza", sales2),
            new Customer("Startup Ventures", "hello@startup.com", "555-0106", 
                        "Startup Ventures", "987 Startup Lane", sales1),
            new Customer("Manufacturing Co", "orders@manufacturing.com", "555-0107", 
                        "Manufacturing Co", "147 Industrial Way", sales2),
            new Customer("Retail Chain", "buyers@retailchain.com", "555-0108", 
                        "Retail Chain Inc", "258 Commerce St", sales1)
        };
        
        for (Customer customer : customers) {
            customer.setCreatedAt(LocalDateTime.now().minusDays(20));
            customer.setUpdatedAt(LocalDateTime.now().minusDays(10));
            testCustomers.add(customerRepository.save(customer));
        }
    }
    
    /**
     * Creates test leads with various statuses.
     */
    private void createTestLeads() {
        User sales1 = getSalesUser1();
        User sales2 = getSalesUser2();
        
        Lead[] leads = {
            createLead("Software License Renewal", "Annual software license renewal opportunity", 
                      LeadStatus.NEW, BigDecimal.valueOf(15000), testCustomers.get(0), sales1, sales1),
            createLead("Cloud Migration Project", "Migration to cloud infrastructure", 
                      LeadStatus.CONTACTED, BigDecimal.valueOf(50000), testCustomers.get(1), sales1, sales1),
            createLead("Security Audit Services", "Comprehensive security audit and compliance", 
                      LeadStatus.QUALIFIED, BigDecimal.valueOf(25000), testCustomers.get(2), sales2, sales2),
            createLead("Training Program", "Employee training and development program", 
                      LeadStatus.PROPOSAL, BigDecimal.valueOf(8000), testCustomers.get(3), sales1, sales1),
            createLead("System Integration", "Integration of existing systems", 
                      LeadStatus.NEGOTIATION, BigDecimal.valueOf(75000), testCustomers.get(4), sales2, sales2),
            createLead("Mobile App Development", "Custom mobile application development", 
                      LeadStatus.CLOSED_WON, BigDecimal.valueOf(30000), testCustomers.get(5), sales1, sales1),
            createLead("Data Analytics Platform", "Business intelligence and analytics solution", 
                      LeadStatus.CLOSED_LOST, BigDecimal.valueOf(40000), testCustomers.get(6), sales2, sales2),
            createLead("Website Redesign", "Complete website redesign and optimization", 
                      LeadStatus.NEW, BigDecimal.valueOf(12000), testCustomers.get(7), sales1, sales1)
        };
        
        for (Lead lead : leads) {
            testLeads.add(leadRepository.save(lead));
        }
    }
    
    /**
     * Creates test sale transactions.
     */
    private void createTestSaleTransactions() {
        User sales1 = getSalesUser1();
        User sales2 = getSalesUser2();
        
        // Sale from converted lead
        SaleTransaction sale1 = new SaleTransaction(
            BigDecimal.valueOf(30000), LocalDateTime.now().minusDays(5),
            "Mobile App Development - Completed", testCustomers.get(5), sales1, 
            testLeads.stream().filter(l -> l.getStatus() == LeadStatus.CLOSED_WON).findFirst().orElse(null)
        );
        testSales.add(saleTransactionRepository.save(sale1));
        
        // Previous sales without leads
        SaleTransaction sale2 = new SaleTransaction(
            BigDecimal.valueOf(22000), LocalDateTime.now().minusDays(30),
            "Previous Software License", testCustomers.get(0), sales1, null
        );
        testSales.add(saleTransactionRepository.save(sale2));
        
        SaleTransaction sale3 = new SaleTransaction(
            BigDecimal.valueOf(18000), LocalDateTime.now().minusDays(15),
            "Consulting Services", testCustomers.get(2), sales2, null
        );
        testSales.add(saleTransactionRepository.save(sale3));
        
        SaleTransaction sale4 = new SaleTransaction(
            BigDecimal.valueOf(35000), LocalDateTime.now().minusDays(45),
            "System Upgrade", testCustomers.get(4), sales2, null
        );
        testSales.add(saleTransactionRepository.save(sale4));
    }
    
    /**
     * Creates test interaction logs.
     */
    private void createTestInteractionLogs() {
        User sales1 = getSalesUser1();
        User sales2 = getSalesUser2();
        
        InteractionLog[] interactions = {
            createInteraction(InteractionType.CALL, "Initial contact call - discussed requirements", 
                            testCustomers.get(0), sales1, LocalDateTime.now().minusHours(2)),
            createInteraction(InteractionType.EMAIL, "Sent proposal document and pricing information", 
                            testCustomers.get(0), sales1, LocalDateTime.now().minusHours(1)),
            createInteraction(InteractionType.MEETING, "In-person meeting to review technical specifications", 
                            testCustomers.get(1), sales1, LocalDateTime.now().minusDays(1)),
            createInteraction(InteractionType.CALL, "Follow-up call regarding contract terms", 
                            testCustomers.get(2), sales2, LocalDateTime.now().minusHours(4)),
            createInteraction(InteractionType.EMAIL, "Sent updated proposal with revised timeline", 
                            testCustomers.get(3), sales1, LocalDateTime.now().minusDays(2)),
            createInteraction(InteractionType.MEETING, "Demo session for the proposed solution", 
                            testCustomers.get(4), sales2, LocalDateTime.now().minusDays(3)),
            createInteraction(InteractionType.NOTE, "Customer requested additional features", 
                            testCustomers.get(5), sales1, LocalDateTime.now().minusHours(6)),
            createInteraction(InteractionType.CALL, "Closing call - finalized terms and conditions", 
                            testCustomers.get(5), sales1, LocalDateTime.now().minusDays(7))
        };
        
        for (InteractionLog interaction : interactions) {
            interactionLogRepository.save(interaction);
        }
    }
    
    /**
     * Creates test audit logs.
     */
    private void createTestAuditLogs() {
        User admin = getAdminUser();
        User sales1 = getSalesUser1();
        
        AuditLog[] auditLogs = {
            createAuditLog("USER_CREATED", "User", getRegularUser().getId(), 
                          null, "username=regular1,role=REGULAR", admin, LocalDateTime.now().minusDays(15)),
            createAuditLog("CUSTOMER_CREATED", "Customer", testCustomers.get(0).getId(), 
                          null, "name=Acme Corporation,email=contact@acme.com", sales1, LocalDateTime.now().minusDays(20)),
            createAuditLog("LEAD_CREATED", "Lead", testLeads.get(0).getId(), 
                          null, "title=Software License Renewal,status=NEW", sales1, LocalDateTime.now().minusDays(18)),
            createAuditLog("LEAD_STATUS_UPDATED", "Lead", testLeads.get(1).getId(), 
                          "status=NEW", "status=CONTACTED", sales1, LocalDateTime.now().minusDays(17)),
            createAuditLog("SALE_CREATED", "SaleTransaction", testSales.get(0).getId(), 
                          null, "amount=30000.00,customer_id=" + testCustomers.get(5).getId(), sales1, LocalDateTime.now().minusDays(5)),
            createAuditLog("CUSTOMER_UPDATED", "Customer", testCustomers.get(1).getId(), 
                          "phone=555-0102", "phone=555-0199", sales1, LocalDateTime.now().minusDays(10)),
            createAuditLog("USER_ROLE_UPDATED", "User", getInactiveUser().getId(), 
                          "role=REGULAR,active=true", "role=REGULAR,active=false", admin, LocalDateTime.now().minusDays(8))
        };
        
        for (AuditLog auditLog : auditLogs) {
            auditLogRepository.save(auditLog);
        }
    }
    
    // Helper methods
    
    private Lead createLead(String title, String description, LeadStatus status, 
                          BigDecimal estimatedValue, Customer customer, User assignedTo, User createdBy) {
        Lead lead = new Lead(title, description, estimatedValue, customer, assignedTo);
        lead.setStatus(status);
        lead.setCreatedAt(LocalDateTime.now().minusDays(18));
        lead.setUpdatedAt(LocalDateTime.now().minusDays(10));
        return lead;
    }
    
    private InteractionLog createInteraction(InteractionType type, String notes, 
                                           Customer customer, User user, LocalDateTime timestamp) {
        InteractionLog interaction = new InteractionLog(type, notes, customer, user);
        interaction.setTimestamp(timestamp);
        return interaction;
    }
    
    private AuditLog createAuditLog(String action, String entityType, Long entityId, 
                                  String oldValues, String newValues, User user, LocalDateTime timestamp) {
        AuditLog auditLog = new AuditLog(action, entityType, entityId, oldValues, newValues, user);
        auditLog.setTimestamp(timestamp);
        return auditLog;
    }
    
    // Getter methods for accessing test data
    
    public User getAdminUser() {
        return testUsers.stream().filter(u -> u.getRole() == UserRole.ADMIN).findFirst().orElse(null);
    }
    
    public User getSalesUser1() {
        return testUsers.stream().filter(u -> u.getRole() == UserRole.SALES && u.getUsername().equals("sales1")).findFirst().orElse(null);
    }
    
    public User getSalesUser2() {
        return testUsers.stream().filter(u -> u.getRole() == UserRole.SALES && u.getUsername().equals("sales2")).findFirst().orElse(null);
    }
    
    public User getRegularUser() {
        return testUsers.stream().filter(u -> u.getRole() == UserRole.REGULAR && u.isActive()).findFirst().orElse(null);
    }
    
    public User getInactiveUser() {
        return testUsers.stream().filter(u -> !u.isActive()).findFirst().orElse(null);
    }
    
    public List<User> getAllTestUsers() {
        return new ArrayList<>(testUsers);
    }
    
    public List<Customer> getAllTestCustomers() {
        return new ArrayList<>(testCustomers);
    }
    
    public List<Lead> getAllTestLeads() {
        return new ArrayList<>(testLeads);
    }
    
    public List<SaleTransaction> getAllTestSales() {
        return new ArrayList<>(testSales);
    }
    
    public List<User> getSalesUsers() {
        return testUsers.stream().filter(u -> u.getRole() == UserRole.SALES).toList();
    }
}