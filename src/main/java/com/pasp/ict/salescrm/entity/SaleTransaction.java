package com.pasp.ict.salescrm.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Entity representing a completed sales transaction in the CRM system.
 * Sale transactions record the financial details of completed sales.
 */
@Entity
@Table(name = "sale_transactions")
public class SaleTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Sale amount is required")
    @DecimalMin(value = "0.01", message = "Sale amount must be positive")
    @Digits(integer = 8, fraction = 2, message = "Sale amount must have at most 8 integer digits and 2 decimal places")
    private BigDecimal amount;
    
    @Column(name = "sale_date", nullable = false)
    @NotNull(message = "Sale date is required")
    private LocalDateTime saleDate;
    
    @Column(length = 500)
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    // Foreign key relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @NotNull(message = "Customer is required")
    private Customer customer;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_user_id", nullable = false)
    @NotNull(message = "Sales user is required")
    private User salesUser;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private Lead lead;
    
    // Constructors
    public SaleTransaction() {
    }
    
    public SaleTransaction(BigDecimal amount, LocalDateTime saleDate, String description, 
                          Customer customer, User salesUser, Lead lead) {
        this.amount = amount;
        this.saleDate = saleDate;
        this.description = description;
        this.customer = customer;
        this.salesUser = salesUser;
        this.lead = lead;
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (saleDate == null) {
            saleDate = LocalDateTime.now();
        }
    }
    
    // Business methods
    public boolean isFromLead() {
        return lead != null;
    }
    
    public String getFormattedAmount() {
        return String.format(java.util.Locale.US, "$%.2f", amount);
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public LocalDateTime getSaleDate() {
        return saleDate;
    }
    
    public void setSaleDate(LocalDateTime saleDate) {
        this.saleDate = saleDate;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public Customer getCustomer() {
        return customer;
    }
    
    public void setCustomer(Customer customer) {
        this.customer = customer;
    }
    
    public User getSalesUser() {
        return salesUser;
    }
    
    public void setSalesUser(User salesUser) {
        this.salesUser = salesUser;
    }
    
    public Lead getLead() {
        return lead;
    }
    
    public void setLead(Lead lead) {
        this.lead = lead;
    }
    
    @Override
    public String toString() {
        return "SaleTransaction{" +
                "id=" + id +
                ", amount=" + amount +
                ", saleDate=" + saleDate +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}