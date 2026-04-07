package com.pasp.ict.salescrm.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.pasp.ict.salescrm.entity.Customer;
import com.pasp.ict.salescrm.entity.Lead;
import com.pasp.ict.salescrm.entity.LeadStatus;
import com.pasp.ict.salescrm.entity.User;

/**
 * Repository interface for Lead entity operations.
 * Provides custom query methods for lead management and sales pipeline tracking.
 */
@Repository
public interface LeadRepository extends JpaRepository<Lead, Long> {
    
    /**
     * Find leads by status.
     * @param status the lead status to filter by
     * @return List of leads with the specified status
     */
    List<Lead> findByStatus(LeadStatus status);
    
    /**
     * Find leads assigned to a specific user.
     * @param assignedTo the user assigned to the leads
     * @return List of leads assigned to the user
     */
    List<Lead> findByAssignedTo(User assignedTo);
    
    /**
     * Find leads by customer.
     * @param customer the customer associated with the leads
     * @return List of leads for the specified customer
     */
    List<Lead> findByCustomer(Customer customer);
    
    /**
     * Find leads by status and assigned user.
     * @param status the lead status
     * @param assignedTo the assigned user
     * @return List of leads matching both criteria
     */
    List<Lead> findByStatusAndAssignedTo(LeadStatus status, User assignedTo);
    
    /**
     * Find leads by customer and status.
     * @param customer the customer
     * @param status the lead status
     * @return List of leads matching both criteria
     */
    List<Lead> findByCustomerAndStatus(Customer customer, LeadStatus status);
    
    /**
     * Find open leads (not closed).
     * @return List of open leads
     */
    @Query("SELECT l FROM Lead l WHERE l.status NOT IN ('CLOSED_WON', 'CLOSED_LOST')")
    List<Lead> findOpenLeads();
    
    /**
     * Find closed leads (won or lost).
     * @return List of closed leads
     */
    @Query("SELECT l FROM Lead l WHERE l.status IN ('CLOSED_WON', 'CLOSED_LOST')")
    List<Lead> findClosedLeads();
    
    /**
     * Find leads with estimated value greater than specified amount.
     * @param minValue the minimum estimated value
     * @return List of leads with estimated value above the threshold
     */
    @Query("SELECT l FROM Lead l WHERE l.estimatedValue > :minValue")
    List<Lead> findLeadsWithValueAbove(@Param("minValue") BigDecimal minValue);
    
    /**
     * Find leads created within a date range.
     * @param startDate the start date
     * @param endDate the end date
     * @return List of leads created within the date range
     */
    @Query("SELECT l FROM Lead l WHERE l.createdAt BETWEEN :startDate AND :endDate")
    List<Lead> findLeadsCreatedBetween(@Param("startDate") LocalDateTime startDate, 
                                      @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find leads updated within a date range.
     * @param startDate the start date
     * @param endDate the end date
     * @return List of leads updated within the date range
     */
    @Query("SELECT l FROM Lead l WHERE l.updatedAt BETWEEN :startDate AND :endDate")
    List<Lead> findLeadsUpdatedBetween(@Param("startDate") LocalDateTime startDate, 
                                      @Param("endDate") LocalDateTime endDate);
    
    /**
     * Search leads by title or description (case-insensitive).
     * @param searchTerm the search term
     * @return List of leads matching the search criteria
     */
    @Query("SELECT l FROM Lead l WHERE " +
           "LOWER(l.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(l.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Lead> searchLeads(@Param("searchTerm") String searchTerm);
    
    /**
     * Count leads by status.
     * @param status the lead status
     * @return count of leads with the specified status
     */
    long countByStatus(LeadStatus status);
    
    /**
     * Count leads assigned to a specific user.
     * @param assignedTo the assigned user
     * @return count of leads assigned to the user
     */
    long countByAssignedTo(User assignedTo);
    
    /**
     * Count leads by status and assigned user.
     * @param status the lead status
     * @param assignedTo the assigned user
     * @return count of leads matching both criteria
     */
    long countByStatusAndAssignedTo(LeadStatus status, User assignedTo);
    
    /**
     * Calculate total estimated value for leads by status.
     * @param status the lead status
     * @return total estimated value for leads with the specified status
     */
    @Query("SELECT COALESCE(SUM(l.estimatedValue), 0) FROM Lead l WHERE l.status = :status")
    BigDecimal sumEstimatedValueByStatus(@Param("status") LeadStatus status);
    
    /**
     * Calculate total estimated value for leads assigned to a user.
     * @param assignedTo the assigned user
     * @return total estimated value for leads assigned to the user
     */
    @Query("SELECT COALESCE(SUM(l.estimatedValue), 0) FROM Lead l WHERE l.assignedTo = :assignedTo")
    BigDecimal sumEstimatedValueByAssignedTo(@Param("assignedTo") User assignedTo);
    
    /**
     * Find leads that haven't been updated recently (stale leads).
     * @param cutoffDate the cutoff date for considering leads stale
     * @return List of stale leads
     */
    @Query("SELECT l FROM Lead l WHERE l.updatedAt < :cutoffDate AND l.status NOT IN ('CLOSED_WON', 'CLOSED_LOST')")
    List<Lead> findStaleLeads(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Find leads ordered by estimated value (highest first).
     * @return List of leads ordered by estimated value descending
     */
    @Query("SELECT l FROM Lead l ORDER BY l.estimatedValue DESC NULLS LAST")
    List<Lead> findAllOrderByEstimatedValueDesc();
    
    /**
     * Find leads by multiple statuses.
     * @param statuses the list of lead statuses
     * @return List of leads with any of the specified statuses
     */
    @Query("SELECT l FROM Lead l WHERE l.status IN :statuses")
    List<Lead> findByStatusIn(@Param("statuses") List<LeadStatus> statuses);
}