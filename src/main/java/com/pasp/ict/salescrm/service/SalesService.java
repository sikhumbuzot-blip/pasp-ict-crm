package com.pasp.ict.salescrm.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pasp.ict.salescrm.entity.AuditLog;
import com.pasp.ict.salescrm.entity.Customer;
import com.pasp.ict.salescrm.entity.Lead;
import com.pasp.ict.salescrm.entity.LeadStatus;
import com.pasp.ict.salescrm.entity.SaleTransaction;
import com.pasp.ict.salescrm.entity.User;
import com.pasp.ict.salescrm.entity.UserRole;
import com.pasp.ict.salescrm.repository.AuditLogRepository;
import com.pasp.ict.salescrm.repository.LeadRepository;
import com.pasp.ict.salescrm.repository.SaleTransactionRepository;

/**
 * Service class for lead management and sales transactions.
 * Handles lead lifecycle management, sales transaction creation, and sales metrics calculation.
 */
@Service
@Transactional
public class SalesService {
    
    private final LeadRepository leadRepository;
    private final SaleTransactionRepository saleTransactionRepository;
    private final AuditLogRepository auditLogRepository;
    
    @Autowired
    public SalesService(LeadRepository leadRepository,
                       SaleTransactionRepository saleTransactionRepository,
                       AuditLogRepository auditLogRepository) {
        this.leadRepository = leadRepository;
        this.saleTransactionRepository = saleTransactionRepository;
        this.auditLogRepository = auditLogRepository;
    }
    
    /**
     * Create a new lead with timestamp and assigned user.
     * @param title the lead title
     * @param description the lead description
     * @param estimatedValue the estimated value
     * @param customer the customer
     * @param assignedTo the user assigned to the lead
     * @param createdBy the user creating the lead
     * @return the created lead
     */
    public Lead createLead(String title, String description, BigDecimal estimatedValue, 
                          Customer customer, User assignedTo, User createdBy) {
        validateSalesPermission(createdBy);
        validateLeadInput(title, customer);
        
        Lead lead = new Lead(title, description, estimatedValue, customer, assignedTo);
        Lead savedLead = leadRepository.save(lead);
        
        // Log the creation
        logAuditEvent("LEAD_CREATED", "Lead", savedLead.getId(), null, 
                     String.format("Title: %s, Customer: %s, Assigned: %s", 
                                  title, customer.getName(), 
                                  assignedTo != null ? assignedTo.getUsername() : "Unassigned"), 
                     createdBy);
        
        return savedLead;
    }
    
    /**
     * Update lead status through valid transitions.
     * @param leadId the lead ID
     * @param newStatus the new status
     * @param updatedBy the user making the change
     * @return the updated lead
     * @throws IllegalArgumentException if transition is invalid
     */
    public Lead updateLeadStatus(Long leadId, LeadStatus newStatus, User updatedBy) {
        validateSalesPermission(updatedBy);
        
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new IllegalArgumentException("Lead not found with ID: " + leadId));
        
        // Check if transition is valid
        if (!lead.canTransitionTo(newStatus)) {
            throw new IllegalArgumentException(
                String.format("Invalid status transition from %s to %s", lead.getStatus(), newStatus));
        }
        
        LeadStatus oldStatus = lead.getStatus();
        lead.setStatus(newStatus);
        Lead savedLead = leadRepository.save(lead);
        
        // Log the status change
        logAuditEvent("LEAD_STATUS_UPDATED", "Lead", leadId, 
                     "Status: " + oldStatus, "Status: " + newStatus, updatedBy);
        
        return savedLead;
    }
    
    /**
     * Convert lead to sale with automatic status update to CLOSED_WON.
     * @param leadId the lead ID
     * @param saleAmount the sale amount
     * @param saleDate the sale date
     * @param description the sale description
     * @param salesUser the sales user
     * @return the created sale transaction
     */
    public SaleTransaction convertLeadToSale(Long leadId, BigDecimal saleAmount, 
                                           LocalDateTime saleDate, String description, User salesUser) {
        validateSalesPermission(salesUser);
        
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new IllegalArgumentException("Lead not found with ID: " + leadId));
        
        if (lead.isClosed()) {
            throw new IllegalArgumentException("Cannot convert closed lead to sale");
        }
        
        // Create the sale transaction
        SaleTransaction saleTransaction = new SaleTransaction(
            saleAmount, saleDate, description, lead.getCustomer(), salesUser, lead);
        SaleTransaction savedTransaction = saleTransactionRepository.save(saleTransaction);
        
        // Automatically update lead status to CLOSED_WON
        LeadStatus oldStatus = lead.getStatus();
        lead.setStatus(LeadStatus.CLOSED_WON);
        leadRepository.save(lead);
        
        // Log both the sale creation and lead status update
        logAuditEvent("SALE_CREATED", "SaleTransaction", savedTransaction.getId(), null, 
                     String.format("Amount: %s, Customer: %s, Lead: %s", 
                                  saleAmount, lead.getCustomer().getName(), lead.getTitle()), 
                     salesUser);
        
        logAuditEvent("LEAD_CONVERTED", "Lead", leadId, 
                     "Status: " + oldStatus, "Status: CLOSED_WON", salesUser);
        
        return savedTransaction;
    }
    
    /**
     * Create a direct sale transaction (not from lead).
     * @param saleAmount the sale amount
     * @param saleDate the sale date
     * @param description the sale description
     * @param customer the customer
     * @param salesUser the sales user
     * @return the created sale transaction
     */
    public SaleTransaction createDirectSale(BigDecimal saleAmount, LocalDateTime saleDate, 
                                          String description, Customer customer, User salesUser) {
        validateSalesPermission(salesUser);
        validateSaleInput(saleAmount, customer);
        
        SaleTransaction saleTransaction = new SaleTransaction(
            saleAmount, saleDate, description, customer, salesUser, null);
        SaleTransaction savedTransaction = saleTransactionRepository.save(saleTransaction);
        
        // Log the creation
        logAuditEvent("DIRECT_SALE_CREATED", "SaleTransaction", savedTransaction.getId(), null, 
                     String.format("Amount: %s, Customer: %s", saleAmount, customer.getName()), 
                     salesUser);
        
        return savedTransaction;
    }
    
    /**
     * Calculate sales metrics including revenue and conversion rates.
     * @param userId optional user ID to filter by (null for all users)
     * @param startDate optional start date (null for all time)
     * @param endDate optional end date (null for all time)
     * @return Map containing sales metrics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> calculateSalesMetrics(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> metrics = new HashMap<>();
        
        // Revenue calculations
        BigDecimal totalRevenue;
        long totalSales;
        
        if (userId != null) {
            User user = new User();
            user.setId(userId);
            
            if (startDate != null && endDate != null) {
                totalRevenue = saleTransactionRepository.calculateRevenueByUserInDateRange(user, startDate, endDate);
                totalSales = saleTransactionRepository.countSalesByUserInDateRange(user, startDate, endDate);
            } else {
                totalRevenue = saleTransactionRepository.calculateRevenueByUser(user);
                totalSales = saleTransactionRepository.countBySalesUser(user);
            }
        } else {
            if (startDate != null && endDate != null) {
                totalRevenue = saleTransactionRepository.calculateRevenueInDateRange(startDate, endDate);
                totalSales = saleTransactionRepository.countSalesInDateRange(startDate, endDate);
            } else {
                totalRevenue = saleTransactionRepository.calculateTotalRevenue();
                totalSales = saleTransactionRepository.count();
            }
        }
        
        metrics.put("totalRevenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
        metrics.put("totalSales", totalSales);
        
        // Average sale amount
        BigDecimal averageSale = totalSales > 0 ? 
            totalRevenue.divide(BigDecimal.valueOf(totalSales), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        metrics.put("averageSaleAmount", averageSale);
        
        // Lead conversion metrics
        long totalLeads;
        long wonLeads;
        
        if (userId != null) {
            User user = new User();
            user.setId(userId);
            totalLeads = leadRepository.countByAssignedTo(user);
            wonLeads = leadRepository.countByStatusAndAssignedTo(LeadStatus.CLOSED_WON, user);
        } else {
            totalLeads = leadRepository.count();
            wonLeads = leadRepository.countByStatus(LeadStatus.CLOSED_WON);
        }
        
        double conversionRate = totalLeads > 0 ? (double) wonLeads / totalLeads * 100 : 0.0;
        
        metrics.put("totalLeads", totalLeads);
        metrics.put("wonLeads", wonLeads);
        metrics.put("conversionRate", Math.round(conversionRate * 100.0) / 100.0);
        
        // Sales from leads vs direct sales
        List<SaleTransaction> salesFromLeads = saleTransactionRepository.findSalesFromLeads();
        List<SaleTransaction> directSales = saleTransactionRepository.findDirectSales();
        
        metrics.put("salesFromLeads", salesFromLeads.size());
        metrics.put("directSales", directSales.size());
        
        return metrics;
    }
    
    /**
     * Get individual performance metrics for a sales user.
     * @param salesUser the sales user
     * @return Map containing individual performance data
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getIndividualPerformance(User salesUser) {
        if (salesUser.getRole() != UserRole.SALES) {
            throw new IllegalArgumentException("User is not a sales user");
        }
        
        return calculateSalesMetrics(salesUser.getId(), null, null);
    }
    
    /**
     * Find leads by status.
     * @param status the lead status
     * @return List of leads with the specified status
     */
    @Transactional(readOnly = true)
    public List<Lead> findLeadsByStatus(LeadStatus status) {
        return leadRepository.findByStatus(status);
    }
    
    /**
     * Find leads assigned to a user.
     * @param assignedTo the assigned user
     * @return List of leads assigned to the user
     */
    @Transactional(readOnly = true)
    public List<Lead> findLeadsByAssignedUser(User assignedTo) {
        return leadRepository.findByAssignedTo(assignedTo);
    }
    
    /**
     * Find leads by customer.
     * @param customer the customer
     * @return List of leads for the customer
     */
    @Transactional(readOnly = true)
    public List<Lead> findLeadsByCustomer(Customer customer) {
        return leadRepository.findByCustomer(customer);
    }
    
    /**
     * Find open leads (not closed).
     * @return List of open leads
     */
    @Transactional(readOnly = true)
    public List<Lead> findOpenLeads() {
        return leadRepository.findOpenLeads();
    }
    
    /**
     * Find sales transactions by user.
     * @param salesUser the sales user
     * @return List of sales transactions by the user
     */
    @Transactional(readOnly = true)
    public List<SaleTransaction> findSalesByUser(User salesUser) {
        return saleTransactionRepository.findBySalesUser(salesUser);
    }
    
    /**
     * Find sales transactions by customer.
     * @param customer the customer
     * @return List of sales transactions for the customer
     */
    @Transactional(readOnly = true)
    public List<SaleTransaction> findSalesByCustomer(Customer customer) {
        return saleTransactionRepository.findByCustomer(customer);
    }
    
    /**
     * Find sales transactions in date range.
     * @param startDate the start date
     * @param endDate the end date
     * @return List of sales transactions in the date range
     */
    @Transactional(readOnly = true)
    public List<SaleTransaction> findSalesInDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return saleTransactionRepository.findSalesInDateRange(startDate, endDate);
    }
    
    /**
     * Search leads by title or description.
     * @param searchTerm the search term
     * @return List of leads matching the search criteria
     */
    @Transactional(readOnly = true)
    public List<Lead> searchLeads(String searchTerm) {
        return leadRepository.searchLeads(searchTerm);
    }
    
    /**
     * Find lead by ID.
     * @param leadId the lead ID
     * @return Optional containing the lead if found
     */
    @Transactional(readOnly = true)
    public Optional<Lead> findLeadById(Long leadId) {
        return leadRepository.findById(leadId);
    }
    
    /**
     * Find sale transaction by ID.
     * @param saleId the sale ID
     * @return Optional containing the sale transaction if found
     */
    @Transactional(readOnly = true)
    public Optional<SaleTransaction> findSaleById(Long saleId) {
        return saleTransactionRepository.findById(saleId);
    }
    
    /**
     * Validate sales permission.
     */
    private void validateSalesPermission(User user) {
        if (user == null || (!user.isActive()) || 
            (user.getRole() != UserRole.SALES && user.getRole() != UserRole.ADMIN)) {
            throw new SecurityException("Sales permission required");
        }
    }
    
    /**
     * Validate lead input data.
     */
    private void validateLeadInput(String title, Customer customer) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Lead title is required");
        }
        if (title.length() > 200) {
            throw new IllegalArgumentException("Lead title must not exceed 200 characters");
        }
        if (customer == null) {
            throw new IllegalArgumentException("Customer is required");
        }
    }
    
    /**
     * Validate sale input data.
     */
    private void validateSaleInput(BigDecimal amount, Customer customer) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Sale amount must be positive");
        }
        if (customer == null) {
            throw new IllegalArgumentException("Customer is required");
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