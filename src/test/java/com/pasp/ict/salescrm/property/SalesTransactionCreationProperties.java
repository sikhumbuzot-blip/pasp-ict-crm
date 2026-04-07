package com.pasp.ict.salescrm.property;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
import com.pasp.ict.salescrm.repository.UserRepository;
import com.pasp.ict.salescrm.service.SalesService;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tag;

/**
 * Property-based tests for sales transaction creation.
 * **Validates: Requirements 3.3**
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Tag("Feature: sales-crm-application, Property 6: Sales Transaction Creation")
public class SalesTransactionCreationProperties {
    
    @Autowired
    private SalesService salesService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private LeadRepository leadRepository;
    
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
     * **Property 6: Sales Transaction Creation**
     * For any completed sale by a sales user, the system should create a sale transaction record 
     * containing customer details, amount, and completion date.
     * **Validates: Requirements 3.3**
     */
    @Property(tries = 100)
    void salesTransactionCreation(@ForAll("saleData") SaleData saleData) {
        
        // Create direct sale transaction
        LocalDateTime beforeCreation = LocalDateTime.now().minusSeconds(1);
        SaleTransaction sale = salesService.createDirectSale(saleData.amount, saleData.saleDate, 
                                                            saleData.description, testCustomer, salesUser);
        LocalDateTime afterCreation = LocalDateTime.now().plusSeconds(1);
        
        // Verify sale transaction record is created with all required details
        assertNotNull(sale.getId(), "Sale transaction should have an ID after creation");
        
        // Verify customer details are included
        assertNotNull(sale.getCustomer(), "Sale transaction should have customer details");
        assertEquals(testCustomer.getId(), sale.getCustomer().getId(), 
                    "Sale transaction should be associated with the correct customer");
        assertEquals(testCustomer.getName(), sale.getCustomer().getName(), 
                    "Customer name should be preserved in sale transaction");
        assertEquals(testCustomer.getEmail(), sale.getCustomer().getEmail(), 
                    "Customer email should be preserved in sale transaction");
        assertEquals(testCustomer.getCompany(), sale.getCustomer().getCompany(), 
                    "Customer company should be preserved in sale transaction");
        
        // Verify amount is correctly stored
        assertEquals(saleData.amount, sale.getAmount(), 
                    "Sale transaction should contain the correct amount");
        assertTrue(sale.getAmount().compareTo(BigDecimal.ZERO) > 0, 
                  "Sale amount should be positive");
        
        // Verify completion date is correctly stored
        assertNotNull(sale.getSaleDate(), "Sale transaction should have a completion date");
        if (saleData.saleDate != null) {
            assertEquals(saleData.saleDate, sale.getSaleDate(), 
                        "Sale transaction should have the specified completion date");
        } else {
            // If no date provided, should default to creation time
            assertTrue(sale.getSaleDate().isAfter(beforeCreation) && sale.getSaleDate().isBefore(afterCreation),
                      "Sale date should default to creation time when not specified");
        }
        
        // Verify sales user is recorded
        assertNotNull(sale.getSalesUser(), "Sale transaction should have sales user details");
        assertEquals(salesUser.getId(), sale.getSalesUser().getId(), 
                    "Sale transaction should be associated with the correct sales user");
        
        // Verify creation timestamp
        assertNotNull(sale.getCreatedAt(), "Sale transaction should have creation timestamp");
        assertTrue(sale.getCreatedAt().isAfter(beforeCreation) && sale.getCreatedAt().isBefore(afterCreation),
                  "Sale creation timestamp should be within expected range");
        
        // Verify description is stored if provided
        if (saleData.description != null && !saleData.description.trim().isEmpty()) {
            assertEquals(saleData.description, sale.getDescription(), 
                        "Sale transaction should preserve the description");
        }
    }
    
    /**
     * Property: Sales transactions from lead conversion contain lead reference
     */
    @Property(tries = 50)
    void salesTransactionFromLeadConversion(@ForAll("leadBasedSaleData") LeadBasedSaleData leadSaleData) {
        
        // Create a lead first
        Lead lead = salesService.createLead(leadSaleData.leadTitle, "Test lead description", 
                                          leadSaleData.estimatedValue, testCustomer, salesUser, salesUser);
        
        // Convert lead to sale
        SaleTransaction sale = salesService.convertLeadToSale(lead.getId(), leadSaleData.saleAmount, 
                                                            leadSaleData.saleDate, leadSaleData.description, salesUser);
        
        // Verify sale transaction contains all required details plus lead reference
        assertNotNull(sale.getId(), "Sale transaction should have an ID");
        assertEquals(leadSaleData.saleAmount, sale.getAmount(), "Sale amount should match");
        assertEquals(testCustomer.getId(), sale.getCustomer().getId(), "Customer should match");
        assertEquals(salesUser.getId(), sale.getSalesUser().getId(), "Sales user should match");
        
        // Verify lead reference is included
        assertNotNull(sale.getLead(), "Sale transaction from lead conversion should reference the lead");
        assertEquals(lead.getId(), sale.getLead().getId(), 
                    "Sale transaction should reference the correct lead");
        assertTrue(sale.isFromLead(), "Sale should be identified as originating from a lead");
        
        // Verify lead status is updated to CLOSED_WON
        Lead updatedLead = leadRepository.findById(lead.getId()).orElseThrow();
        assertEquals(LeadStatus.CLOSED_WON, updatedLead.getStatus(), 
                    "Lead should be marked as CLOSED_WON after conversion");
    }
    
    /**
     * Property: Invalid sale data should be rejected
     */
    @Property(tries = 30)
    void invalidSaleDataRejection(@ForAll("invalidSaleAmount") BigDecimal invalidAmount) {
        
        LocalDateTime saleDate = LocalDateTime.now();
        
        // Attempt to create sale with invalid amount should fail
        assertThrows(IllegalArgumentException.class, 
                    () -> salesService.createDirectSale(invalidAmount, saleDate, 
                                                       "Invalid sale", testCustomer, salesUser),
                    "Creating sale with invalid amount should throw exception");
    }
    
    /**
     * Property: Sales transactions preserve data integrity
     */
    @Property(tries = 50)
    void salesTransactionDataIntegrity(@ForAll("saleData") SaleData saleData) {
        
        SaleTransaction sale = salesService.createDirectSale(saleData.amount, saleData.saleDate, 
                                                            saleData.description, testCustomer, salesUser);
        
        // Verify data integrity - amounts should be exact
        assertEquals(0, saleData.amount.compareTo(sale.getAmount()), 
                    "Sale amount should be exactly preserved (no rounding errors)");
        
        // Verify formatted amount is correct
        String expectedFormatted = String.format(java.util.Locale.US, "$%.2f", saleData.amount);
        assertEquals(expectedFormatted, sale.getFormattedAmount(), 
                    "Formatted amount should be correctly calculated");
        
        // Verify relationships are properly established
        assertNotNull(sale.getCustomer(), "Customer relationship should be established");
        assertNotNull(sale.getSalesUser(), "Sales user relationship should be established");
        
        // For direct sales, lead should be null
        assertNull(sale.getLead(), "Direct sales should not have lead reference");
        assertFalse(sale.isFromLead(), "Direct sales should not be identified as from lead");
    }
    
    @Provide
    Arbitrary<SaleData> saleData() {
        return Arbitraries.bigDecimals().between(BigDecimal.valueOf(0.01), BigDecimal.valueOf(1000000))
                .flatMap(amount ->
                    Arbitraries.oneOf(
                        Arbitraries.just((LocalDateTime) null),
                        Arbitraries.of(LocalDateTime.now().minusDays(30), LocalDateTime.now(), LocalDateTime.now().plusDays(1))
                    ).flatMap(saleDate ->
                        Arbitraries.oneOf(
                            Arbitraries.just((String) null),
                            Arbitraries.strings().alpha().ofMaxLength(500)
                        ).map(description -> new SaleData(amount, saleDate, description))
                    )
                );
    }
    
    @Provide
    Arbitrary<LeadBasedSaleData> leadBasedSaleData() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(200)
                .flatMap(leadTitle ->
                    Arbitraries.bigDecimals().between(BigDecimal.valueOf(0.01), BigDecimal.valueOf(100000))
                        .flatMap(estimatedValue ->
                            Arbitraries.bigDecimals().between(BigDecimal.valueOf(0.01), BigDecimal.valueOf(100000))
                                .flatMap(saleAmount ->
                                    Arbitraries.of(LocalDateTime.now().minusDays(1), LocalDateTime.now(), LocalDateTime.now().plusDays(1))
                                        .flatMap(saleDate ->
                                            Arbitraries.strings().alpha().ofMaxLength(500)
                                                .map(description -> new LeadBasedSaleData(leadTitle, estimatedValue, saleAmount, saleDate, description))
                                        )
                                )
                        )
                );
    }
    
    @Provide
    Arbitrary<BigDecimal> invalidSaleAmount() {
        return Arbitraries.oneOf(
            Arbitraries.just((BigDecimal) null),
            Arbitraries.bigDecimals().between(BigDecimal.valueOf(-1000), BigDecimal.ZERO)
        );
    }
    
    static class SaleData {
        final BigDecimal amount;
        final LocalDateTime saleDate;
        final String description;
        
        SaleData(BigDecimal amount, LocalDateTime saleDate, String description) {
            this.amount = amount;
            this.saleDate = saleDate;
            this.description = description;
        }
    }
    
    static class LeadBasedSaleData {
        final String leadTitle;
        final BigDecimal estimatedValue;
        final BigDecimal saleAmount;
        final LocalDateTime saleDate;
        final String description;
        
        LeadBasedSaleData(String leadTitle, BigDecimal estimatedValue, BigDecimal saleAmount, 
                         LocalDateTime saleDate, String description) {
            this.leadTitle = leadTitle;
            this.estimatedValue = estimatedValue;
            this.saleAmount = saleAmount;
            this.saleDate = saleDate;
            this.description = description;
        }
    }
}