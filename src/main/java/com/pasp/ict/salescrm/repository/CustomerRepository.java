package com.pasp.ict.salescrm.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.pasp.ict.salescrm.entity.Customer;
import com.pasp.ict.salescrm.entity.User;

/**
 * Repository interface for Customer entity operations.
 * Provides custom query methods for customer search and duplicate detection.
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    
    /**
     * Find customer by email address.
     * @param email the email to search for
     * @return Optional containing the customer if found
     */
    Optional<Customer> findByEmail(String email);
    
    /**
     * Find customers by company name.
     * @param company the company name to search for
     * @return List of customers from the specified company
     */
    List<Customer> findByCompany(String company);
    
    /**
     * Find customer by email and company for duplicate detection.
     * @param email the email address
     * @param company the company name
     * @return Optional containing the customer if found
     */
    Optional<Customer> findByEmailAndCompany(String email, String company);
    
    /**
     * Check if email exists.
     * @param email the email to check
     * @return true if email exists, false otherwise
     */
    boolean existsByEmail(String email);
    
    /**
     * Check if company exists.
     * @param company the company name to check
     * @return true if company exists, false otherwise
     */
    boolean existsByCompany(String company);
    
    /**
     * Find customers created by a specific user.
     * @param createdBy the user who created the customers
     * @return List of customers created by the user
     */
    List<Customer> findByCreatedBy(User createdBy);
    
    /**
     * Search customers by name, email, or company (case-insensitive).
     * @param searchTerm the search term
     * @return List of customers matching the search criteria
     */
    @Query("SELECT c FROM Customer c WHERE " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.company) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Customer> searchCustomers(@Param("searchTerm") String searchTerm);
    
    /**
     * Find customers created within a date range.
     * @param startDate the start date
     * @param endDate the end date
     * @return List of customers created within the date range
     */
    @Query("SELECT c FROM Customer c WHERE c.createdAt BETWEEN :startDate AND :endDate")
    List<Customer> findCustomersCreatedBetween(@Param("startDate") LocalDateTime startDate, 
                                              @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find customers updated within a date range.
     * @param startDate the start date
     * @param endDate the end date
     * @return List of customers updated within the date range
     */
    @Query("SELECT c FROM Customer c WHERE c.updatedAt BETWEEN :startDate AND :endDate")
    List<Customer> findCustomersUpdatedBetween(@Param("startDate") LocalDateTime startDate, 
                                              @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find potential duplicate customers by email or company.
     * @param email the email to check
     * @param company the company to check
     * @param excludeId the customer ID to exclude from results
     * @return List of potential duplicate customers
     */
    @Query("SELECT c FROM Customer c WHERE (c.email = :email OR c.company = :company) AND c.id != :excludeId")
    List<Customer> findPotentialDuplicates(@Param("email") String email, 
                                          @Param("company") String company, 
                                          @Param("excludeId") Long excludeId);
    
    /**
     * Find customers with phone numbers matching a pattern.
     * @param phonePattern the phone pattern to search for
     * @return List of customers with matching phone numbers
     */
    @Query("SELECT c FROM Customer c WHERE c.phone LIKE :phonePattern")
    List<Customer> findByPhonePattern(@Param("phonePattern") String phonePattern);
    
    /**
     * Count customers created by a specific user.
     * @param createdBy the user who created the customers
     * @return count of customers created by the user
     */
    long countByCreatedBy(User createdBy);
    
    /**
     * Find customers with recent activity (updated in the last N days).
     * @param daysAgo the number of days ago
     * @return List of customers with recent activity
     */
    @Query("SELECT c FROM Customer c WHERE c.updatedAt >= :cutoffDate")
    List<Customer> findCustomersWithRecentActivity(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Find customers by partial address match.
     * @param addressTerm the address search term
     * @return List of customers with matching addresses
     */
    @Query("SELECT c FROM Customer c WHERE LOWER(c.address) LIKE LOWER(CONCAT('%', :addressTerm, '%'))")
    List<Customer> findByAddressContaining(@Param("addressTerm") String addressTerm);
    
    /**
     * Find customers ordered by creation date (most recent first).
     * @return List of customers ordered by creation date descending
     */
    @Query("SELECT c FROM Customer c ORDER BY c.createdAt DESC")
    List<Customer> findAllOrderByCreatedAtDesc();
}