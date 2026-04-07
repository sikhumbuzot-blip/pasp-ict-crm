package com.pasp.ict.salescrm.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity representing a lead in the Sales CRM system.
 * Leads represent potential sales opportunities and track the sales pipeline.
 */
@Entity
@Table(name = "leads")
public class Lead {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 200)
    @NotBlank(message = "Lead title is required")
    @Size(max = 200, message = "Lead title must not exceed 200 characters")
    private String title;
    
    @Column(length = 1000)
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Lead status is required")
    private LeadStatus status = LeadStatus.NEW;
    
    @Column(name = "estimated_value", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", inclusive = false, message = "Estimated value must be positive")
    @Digits(integer = 8, fraction = 2, message = "Estimated value must have at most 8 integer digits and 2 decimal places")
    private BigDecimal estimatedValue;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Foreign key relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @NotNull(message = "Customer is required")
    private Customer customer;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;
    
    // Relationships
    @OneToMany(mappedBy = "lead", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SaleTransaction> saleTransactions;
    
    // Constructors
    public Lead() {
    }
    
    public Lead(String title, String description, BigDecimal estimatedValue, Customer customer, User assignedTo) {
        this.title = title;
        this.description = description;
        this.estimatedValue = estimatedValue;
        this.customer = customer;
        this.assignedTo = assignedTo;
        this.status = LeadStatus.NEW;
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Business methods
    public boolean canTransitionTo(LeadStatus newStatus) {
        if (this.status == newStatus) {
            return false;
        }
        
        // Define valid status transitions
        switch (this.status) {
            case NEW:
                return newStatus == LeadStatus.CONTACTED || newStatus == LeadStatus.CLOSED_LOST;
            case CONTACTED:
                return newStatus == LeadStatus.QUALIFIED || newStatus == LeadStatus.CLOSED_LOST;
            case QUALIFIED:
                return newStatus == LeadStatus.PROPOSAL || newStatus == LeadStatus.CLOSED_LOST;
            case PROPOSAL:
                return newStatus == LeadStatus.NEGOTIATION || newStatus == LeadStatus.CLOSED_LOST;
            case NEGOTIATION:
                return newStatus == LeadStatus.CLOSED_WON || newStatus == LeadStatus.CLOSED_LOST;
            case CLOSED_WON:
            case CLOSED_LOST:
                return false; // Final states
            default:
                return false;
        }
    }
    
    public boolean isClosed() {
        return status == LeadStatus.CLOSED_WON || status == LeadStatus.CLOSED_LOST;
    }
    
    public boolean isWon() {
        return status == LeadStatus.CLOSED_WON;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public LeadStatus getStatus() {
        return status;
    }
    
    public void setStatus(LeadStatus status) {
        this.status = status;
    }
    
    public BigDecimal getEstimatedValue() {
        return estimatedValue;
    }
    
    public void setEstimatedValue(BigDecimal estimatedValue) {
        this.estimatedValue = estimatedValue;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Customer getCustomer() {
        return customer;
    }
    
    public void setCustomer(Customer customer) {
        this.customer = customer;
    }
    
    public User getAssignedTo() {
        return assignedTo;
    }
    
    public void setAssignedTo(User assignedTo) {
        this.assignedTo = assignedTo;
    }
    
    public List<SaleTransaction> getSaleTransactions() {
        return saleTransactions;
    }
    
    public void setSaleTransactions(List<SaleTransaction> saleTransactions) {
        this.saleTransactions = saleTransactions;
    }
    
    @Override
    public String toString() {
        return "Lead{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", status=" + status +
                ", estimatedValue=" + estimatedValue +
                ", createdAt=" + createdAt +
                '}';
    }
}