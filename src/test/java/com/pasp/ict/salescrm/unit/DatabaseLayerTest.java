package com.pasp.ict.salescrm.unit;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

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
 * Unit tests for the database layer implementation.
 * Tests core entity classes and repository functionality.
 */
@DataJpaTest
@ActiveProfiles("test")
class DatabaseLayerTest {

    @Autowired
    private TestEntityManager entityManager;

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

    @Test
    void testUserEntityAndRepository() {
        // Create and save a user
        User user = new User("testuser", "Password123", "test@example.com", 
                           "John", "Doe", UserRole.SALES);
        user = userRepository.save(user);
        entityManager.flush();

        // Verify user was saved correctly
        assertThat(user.getId()).isNotNull();
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.isActive()).isTrue();

        // Test repository methods
        User foundUser = userRepository.findByUsername("testuser").orElse(null);
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getEmail()).isEqualTo("test@example.com");
        assertThat(foundUser.getRole()).isEqualTo(UserRole.SALES);

        // Test unique constraints
        boolean usernameExists = userRepository.existsByUsername("testuser");
        assertThat(usernameExists).isTrue();

        boolean emailExists = userRepository.existsByEmail("test@example.com");
        assertThat(emailExists).isTrue();
    }

    @Test
    void testCustomerEntityAndRepository() {
        // Create user first (required for customer)
        User user = new User("salesuser", "Password123", "sales@example.com", 
                           "Jane", "Smith", UserRole.SALES);
        user = userRepository.save(user);

        // Create and save customer
        Customer customer = new Customer("Test Company", "customer@test.com", 
                                       "+1234567890", "Test Corp", "123 Test St", user);
        customer = customerRepository.save(customer);
        entityManager.flush();

        // Verify customer was saved correctly
        assertThat(customer.getId()).isNotNull();
        assertThat(customer.getCreatedAt()).isNotNull();
        assertThat(customer.getUpdatedAt()).isNotNull();
        assertThat(customer.getCreatedBy()).isEqualTo(user);

        // Test repository methods
        Customer foundCustomer = customerRepository.findByEmail("customer@test.com").orElse(null);
        assertThat(foundCustomer).isNotNull();
        assertThat(foundCustomer.getName()).isEqualTo("Test Company");

        // Test search functionality
        var searchResults = customerRepository.searchCustomers("Test");
        assertThat(searchResults).hasSize(1);
        assertThat(searchResults.get(0).getId()).isEqualTo(customer.getId());
    }

    @Test
    void testLeadEntityAndRepository() {
        // Create user and customer first
        User user = new User("leaduser", "Password123", "lead@example.com", 
                           "Bob", "Johnson", UserRole.SALES);
        user = userRepository.save(user);

        Customer customer = new Customer("Lead Customer", "leadcust@test.com", 
                                       "+1987654321", "Lead Corp", "456 Lead Ave", user);
        customer = customerRepository.save(customer);

        // Create and save lead
        Lead lead = new Lead("Test Lead", "This is a test lead", 
                           BigDecimal.valueOf(5000.00), customer, user);
        lead = leadRepository.save(lead);
        entityManager.flush();

        // Verify lead was saved correctly
        assertThat(lead.getId()).isNotNull();
        assertThat(lead.getStatus()).isEqualTo(LeadStatus.NEW);
        assertThat(lead.getCreatedAt()).isNotNull();
        assertThat(lead.getUpdatedAt()).isNotNull();
        assertThat(lead.getCustomer()).isEqualTo(customer);
        assertThat(lead.getAssignedTo()).isEqualTo(user);

        // Test repository methods
        var userLeads = leadRepository.findByAssignedTo(user);
        assertThat(userLeads).hasSize(1);
        assertThat(userLeads.get(0).getId()).isEqualTo(lead.getId());

        var statusLeads = leadRepository.findByStatus(LeadStatus.NEW);
        assertThat(statusLeads).contains(lead);

        // Test business methods
        assertThat(lead.canTransitionTo(LeadStatus.CONTACTED)).isTrue();
        assertThat(lead.canTransitionTo(LeadStatus.PROPOSAL)).isFalse();
        assertThat(lead.isClosed()).isFalse();
    }

    @Test
    void testSaleTransactionEntityAndRepository() {
        // Create user and customer first
        User user = new User("salesrep", "Password123", "salesrep@example.com", 
                           "Alice", "Brown", UserRole.SALES);
        user = userRepository.save(user);

        Customer customer = new Customer("Sale Customer", "salecust@test.com", 
                                       "+1555666777", "Sale Corp", "789 Sale Blvd", user);
        customer = customerRepository.save(customer);

        Lead lead = new Lead("Sale Lead", "Lead that converts to sale", 
                           BigDecimal.valueOf(3000.00), customer, user);
        lead = leadRepository.save(lead);

        // Create and save sale transaction
        SaleTransaction sale = new SaleTransaction(BigDecimal.valueOf(2500.00), 
                                                 LocalDateTime.now(), "Test sale", 
                                                 customer, user, lead);
        sale = saleTransactionRepository.save(sale);
        entityManager.flush();

        // Verify sale was saved correctly
        assertThat(sale.getId()).isNotNull();
        assertThat(sale.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(2500.00));
        assertThat(sale.getSaleDate()).isNotNull();
        assertThat(sale.getCreatedAt()).isNotNull();
        assertThat(sale.getCustomer()).isEqualTo(customer);
        assertThat(sale.getSalesUser()).isEqualTo(user);
        assertThat(sale.getLead()).isEqualTo(lead);

        // Test repository methods
        var customerSales = saleTransactionRepository.findByCustomer(customer);
        assertThat(customerSales).hasSize(1);
        assertThat(customerSales.get(0).getId()).isEqualTo(sale.getId());

        var userSales = saleTransactionRepository.findBySalesUser(user);
        assertThat(userSales).contains(sale);

        // Test aggregate functions
        BigDecimal totalRevenue = saleTransactionRepository.calculateTotalRevenue();
        assertThat(totalRevenue).isEqualByComparingTo(BigDecimal.valueOf(2500.00));

        // Test business methods
        assertThat(sale.isFromLead()).isTrue();
        assertThat(sale.getFormattedAmount()).isEqualTo("$2500.00");
    }

    @Test
    void testInteractionLogEntityAndRepository() {
        // Create user and customer first
        User user = new User("interactionuser", "Password123", "interaction@example.com", 
                           "Charlie", "Davis", UserRole.SALES);
        user = userRepository.save(user);

        Customer customer = new Customer("Interaction Customer", "intcust@test.com", 
                                       "+1444555666", "Int Corp", "321 Int St", user);
        customer = customerRepository.save(customer);

        // Create and save interaction log
        InteractionLog interaction = new InteractionLog(InteractionType.CALL, 
                                                       "Called customer about new product", 
                                                       customer, user);
        interaction = interactionLogRepository.save(interaction);
        entityManager.flush();

        // Verify interaction was saved correctly
        assertThat(interaction.getId()).isNotNull();
        assertThat(interaction.getType()).isEqualTo(InteractionType.CALL);
        assertThat(interaction.getTimestamp()).isNotNull();
        assertThat(interaction.getCustomer()).isEqualTo(customer);
        assertThat(interaction.getUser()).isEqualTo(user);

        // Test repository methods
        var customerInteractions = interactionLogRepository.findByCustomer(customer);
        assertThat(customerInteractions).hasSize(1);
        assertThat(customerInteractions.get(0).getId()).isEqualTo(interaction.getId());

        var userInteractions = interactionLogRepository.findByUser(user);
        assertThat(userInteractions).contains(interaction);

        var callInteractions = interactionLogRepository.findByType(InteractionType.CALL);
        assertThat(callInteractions).contains(interaction);
    }

    @Test
    void testAuditLogEntityAndRepository() {
        // Create user first
        User user = new User("audituser", "Password123", "audit@example.com", 
                           "David", "Wilson", UserRole.ADMIN);
        user = userRepository.save(user);

        // Create and save audit log
        AuditLog auditLog = new AuditLog("CREATE", "Customer", 1L, 
                                       null, "name: New Customer", user);
        auditLog = auditLogRepository.save(auditLog);
        entityManager.flush();

        // Verify audit log was saved correctly
        assertThat(auditLog.getId()).isNotNull();
        assertThat(auditLog.getAction()).isEqualTo("CREATE");
        assertThat(auditLog.getEntityType()).isEqualTo("Customer");
        assertThat(auditLog.getEntityId()).isEqualTo(1L);
        assertThat(auditLog.getTimestamp()).isNotNull();
        assertThat(auditLog.getUser()).isEqualTo(user);

        // Test repository methods
        var userAudits = auditLogRepository.findByUser(user);
        assertThat(userAudits).hasSize(1);
        assertThat(userAudits.get(0).getId()).isEqualTo(auditLog.getId());

        var createAudits = auditLogRepository.findByAction("CREATE");
        assertThat(createAudits).contains(auditLog);

        var customerAudits = auditLogRepository.findByEntityType("Customer");
        assertThat(customerAudits).contains(auditLog);

        // Test business methods
        assertThat(auditLog.hasChanges()).isTrue();
        assertThat(auditLog.getSummary()).contains("audituser");
        assertThat(auditLog.getSummary()).contains("CREATE");
        assertThat(auditLog.getSummary()).contains("Customer");
    }

    @Test
    void testEntityRelationships() {
        // Create a complete entity graph to test relationships
        User user = new User("reluser", "Password123", "rel@example.com", 
                           "Eve", "Miller", UserRole.SALES);
        user = userRepository.save(user);

        Customer customer = new Customer("Rel Customer", "relcust@test.com", 
                                       "+1333444555", "Rel Corp", "654 Rel Ave", user);
        customer = customerRepository.save(customer);

        Lead lead = new Lead("Rel Lead", "Relationship test lead", 
                           BigDecimal.valueOf(4000.00), customer, user);
        lead = leadRepository.save(lead);

        SaleTransaction sale = new SaleTransaction(BigDecimal.valueOf(3500.00), 
                                                 LocalDateTime.now(), "Relationship test sale", 
                                                 customer, user, lead);
        sale = saleTransactionRepository.save(sale);

        InteractionLog interaction = new InteractionLog(InteractionType.EMAIL, 
                                                       "Email about relationship test", 
                                                       customer, user);
        interaction = interactionLogRepository.save(interaction);

        entityManager.flush();
        entityManager.clear(); // Clear persistence context to test lazy loading

        // Test relationships work correctly
        Lead reloadedLead = leadRepository.findById(lead.getId()).orElse(null);
        assertThat(reloadedLead).isNotNull();
        assertThat(reloadedLead.getCustomer().getName()).isEqualTo("Rel Customer");
        assertThat(reloadedLead.getAssignedTo().getUsername()).isEqualTo("reluser");

        SaleTransaction reloadedSale = saleTransactionRepository.findById(sale.getId()).orElse(null);
        assertThat(reloadedSale).isNotNull();
        assertThat(reloadedSale.getCustomer().getName()).isEqualTo("Rel Customer");
        assertThat(reloadedSale.getSalesUser().getUsername()).isEqualTo("reluser");
        assertThat(reloadedSale.getLead().getTitle()).isEqualTo("Rel Lead");

        InteractionLog reloadedInteraction = interactionLogRepository.findById(interaction.getId()).orElse(null);
        assertThat(reloadedInteraction).isNotNull();
        assertThat(reloadedInteraction.getCustomer().getName()).isEqualTo("Rel Customer");
        assertThat(reloadedInteraction.getUser().getUsername()).isEqualTo("reluser");
    }
}