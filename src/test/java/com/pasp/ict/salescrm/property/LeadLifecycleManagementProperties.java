package com.pasp.ict.salescrm.property;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

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
import com.pasp.ict.salescrm.service.SalesService;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tag;

/**
 * Property-based tests for lead lifecycle management.
 * **Validates: Requirements 3.1, 3.2, 3.5**
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Tag("Feature: sales-crm-application, Property 5: Lead Lifecycle Management")
public class LeadLifecycleManagementProperties {
    
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
    
    private User salesUser;
    private Customer testCustomer;
    
    @BeforeEach
    void setUp() {
        // Create test sales user
        salesUser = new User("testuser", "Password123", "test@example.com", 
                            "Test", "User", UserRole.SALES);
        salesUser = userRepository.save(salesUser);
        
        // Create test customer
        testCustomer = new Customer("Test Customer", "customer@example.com", 
                                   "123-456-7890", "Test Company", "123 Test St", salesUser);
        testCustomer = customerRepository.save(testCustomer);
    }
    
    /**
     * **Property 5: Lead Lifecycle Management**
     * For any lead created by a sales user, the system should store it with timestamp and assigned user,
     * allow status updates through valid transitions, and automatically set status to Closed-Won when converted to a sale.
     * **Validates: Requirements 3.1, 3.2, 3.5**
     */
    @Property(tries = 100)
    void leadLifecycleManagement(@ForAll("leadData") LeadData leadData,
                                @ForAll("validStatusTransition") LeadStatus newStatus,
                                @ForAll("saleAmount") BigDecimal saleAmount) {
        
        // Create lead with timestamp and assigned user (Requirement 3.1)
        LocalDateTime beforeCreation = LocalDateTime.now().minusSeconds(1);
        Lead lead = salesService.createLead(leadData.title, leadData.description, 
                                          leadData.estimatedValue, testCustomer, salesUser, salesUser);
        LocalDateTime afterCreation = LocalDateTime.now().plusSeconds(1);
        
        // Verify lead is stored with timestamp and assigned user
        assertNotNull(lead.getId(), "Lead should have an ID after creation");
        assertNotNull(lead.getCreatedAt(), "Lead should have creation timestamp");
        assertTrue(lead.getCreatedAt().isAfter(beforeCreation) && lead.getCreatedAt().isBefore(afterCreation),
                  "Lead creation timestamp should be within expected range");
        assertEquals(salesUser.getId(), lead.getAssignedTo().getId(), 
                    "Lead should be assigned to the specified user");
        assertEquals(testCustomer.getId(), lead.getCustomer().getId(), 
                    "Lead should be associated with the specified customer");
        assertEquals(LeadStatus.NEW, lead.getStatus(), "New lead should have NEW status");
        
        // Test valid status transitions (Requirement 3.2)
        if (lead.canTransitionTo(newStatus)) {
            Lead updatedLead = salesService.updateLeadStatus(lead.getId(), newStatus, salesUser);
            assertEquals(newStatus, updatedLead.getStatus(), 
                        "Lead status should be updated to the new valid status");
            assertNotNull(updatedLead.getUpdatedAt(), "Lead should have updated timestamp");
        } else {
            // Invalid transitions should throw exception
            assertThrows(IllegalArgumentException.class, 
                        () -> salesService.updateLeadStatus(lead.getId(), newStatus, salesUser),
                        "Invalid status transition should throw exception");
        }
        
        // Test automatic status update when converted to sale (Requirement 3.5)
        if (!lead.isClosed() && saleAmount.compareTo(BigDecimal.ZERO) > 0) {
            LocalDateTime saleDate = LocalDateTime.now();
            SaleTransaction sale = salesService.convertLeadToSale(lead.getId(), saleAmount, 
                                                                saleDate, "Test sale", salesUser);
            
            // Verify sale transaction is created
            assertNotNull(sale.getId(), "Sale transaction should have an ID");
            assertEquals(saleAmount, sale.getAmount(), "Sale amount should match");
            assertEquals(testCustomer.getId(), sale.getCustomer().getId(), 
                        "Sale should be associated with the lead's customer");
            assertEquals(salesUser.getId(), sale.getSalesUser().getId(), 
                        "Sale should be associated with the sales user");
            assertEquals(lead.getId(), sale.getLead().getId(), 
                        "Sale should be linked to the original lead");
            
            // Verify lead status is automatically updated to CLOSED_WON
            Lead convertedLead = leadRepository.findById(lead.getId()).orElseThrow();
            assertEquals(LeadStatus.CLOSED_WON, convertedLead.getStatus(), 
                        "Lead should be automatically updated to CLOSED_WON when converted to sale");
        }
    }
    
    /**
     * Property: Lead status transitions follow valid business rules
     */
    @Property(tries = 50)
    void leadStatusTransitionsAreValid(@ForAll("leadData") LeadData leadData,
                                      @ForAll("statusSequence") List<LeadStatus> statusSequence) {
        
        Lead lead = salesService.createLead(leadData.title, leadData.description, 
                                          leadData.estimatedValue, testCustomer, salesUser, salesUser);
        
        LeadStatus currentStatus = LeadStatus.NEW;
        
        for (LeadStatus nextStatus : statusSequence) {
            if (lead.canTransitionTo(nextStatus)) {
                lead = salesService.updateLeadStatus(lead.getId(), nextStatus, salesUser);
                currentStatus = nextStatus;
                assertEquals(currentStatus, lead.getStatus(), 
                           "Lead status should match the expected status after valid transition");
                
                // If lead is closed, no further transitions should be possible
                if (lead.isClosed()) {
                    break;
                }
            } else {
                // Invalid transition should throw exception
                final LeadStatus invalidStatus = nextStatus;
                final Long leadId = lead.getId();
                assertThrows(IllegalArgumentException.class, 
                           () -> salesService.updateLeadStatus(leadId, invalidStatus, salesUser),
                           "Invalid status transition should throw exception");
            }
        }
    }
    
    /**
     * Property: Closed leads cannot be converted to sales
     */
    @Property(tries = 30)
    void closedLeadsCannotBeConvertedToSales(@ForAll("leadData") LeadData leadData,
                                           @ForAll("closedStatus") LeadStatus closedStatus,
                                           @ForAll("saleAmount") BigDecimal saleAmount) {
        
        Lead lead = salesService.createLead(leadData.title, leadData.description, 
                                          leadData.estimatedValue, testCustomer, salesUser, salesUser);
        
        // Manually set lead to closed status (simulating a closed lead)
        lead.setStatus(closedStatus);
        leadRepository.save(lead);
        
        // Attempt to convert closed lead to sale should fail
        assertThrows(IllegalArgumentException.class, 
                    () -> salesService.convertLeadToSale(lead.getId(), saleAmount, 
                                                       LocalDateTime.now(), "Test sale", salesUser),
                    "Converting closed lead to sale should throw exception");
    }
    
    @Provide
    Arbitrary<LeadData> leadData() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(200)
                .flatMap(title -> 
                    Arbitraries.strings().alpha().ofMaxLength(1000)
                        .flatMap(description ->
                            Arbitraries.bigDecimals().between(BigDecimal.valueOf(0.01), BigDecimal.valueOf(1000000))
                                .map(estimatedValue -> new LeadData(title, description, estimatedValue))
                        )
                );
    }
    
    @Provide
    Arbitrary<LeadStatus> validStatusTransition() {
        return Arbitraries.of(LeadStatus.values());
    }
    
    @Provide
    Arbitrary<LeadStatus> closedStatus() {
        return Arbitraries.of(LeadStatus.CLOSED_WON, LeadStatus.CLOSED_LOST);
    }
    
    @Provide
    Arbitrary<BigDecimal> saleAmount() {
        return Arbitraries.bigDecimals().between(BigDecimal.valueOf(0.01), BigDecimal.valueOf(100000));
    }
    
    @Provide
    Arbitrary<List<LeadStatus>> statusSequence() {
        return Arbitraries.of(LeadStatus.values()).list().ofMinSize(1).ofMaxSize(5);
    }
    
    static class LeadData {
        final String title;
        final String description;
        final BigDecimal estimatedValue;
        
        LeadData(String title, String description, BigDecimal estimatedValue) {
            this.title = title;
            this.description = description;
            this.estimatedValue = estimatedValue;
        }
    }
}