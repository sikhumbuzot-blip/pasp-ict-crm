package com.pasp.ict.salescrm.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.pasp.ict.salescrm.entity.Customer;
import com.pasp.ict.salescrm.entity.InteractionLog;
import com.pasp.ict.salescrm.entity.InteractionType;
import com.pasp.ict.salescrm.entity.User;

/**
 * Repository interface for InteractionLog entity operations.
 * Provides custom query methods for customer interaction tracking.
 */
@Repository
public interface InteractionLogRepository extends JpaRepository<InteractionLog, Long> {
    
    /**
     * Find interaction logs by customer.
     * @param customer the customer
     * @return List of interaction logs for the customer
     */
    List<InteractionLog> findByCustomer(Customer customer);
    
    /**
     * Find interaction logs by user.
     * @param user the user who logged the interactions
     * @return List of interaction logs by the user
     */
    List<InteractionLog> findByUser(User user);
    
    /**
     * Find interaction logs by type.
     * @param type the interaction type
     * @return List of interaction logs of the specified type
     */
    List<InteractionLog> findByType(InteractionType type);
    
    /**
     * Find interaction logs by customer and type.
     * @param customer the customer
     * @param type the interaction type
     * @return List of interaction logs matching both criteria
     */
    List<InteractionLog> findByCustomerAndType(Customer customer, InteractionType type);
    
    /**
     * Find interaction logs by customer and user.
     * @param customer the customer
     * @param user the user
     * @return List of interaction logs between the customer and user
     */
    List<InteractionLog> findByCustomerAndUser(Customer customer, User user);
    
    /**
     * Find interaction logs within a date range.
     * @param startDate the start date
     * @param endDate the end date
     * @return List of interaction logs within the date range
     */
    @Query("SELECT il FROM InteractionLog il WHERE il.timestamp BETWEEN :startDate AND :endDate")
    List<InteractionLog> findInteractionsInDateRange(@Param("startDate") LocalDateTime startDate, 
                                                     @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find interaction logs by customer within a date range.
     * @param customer the customer
     * @param startDate the start date
     * @param endDate the end date
     * @return List of interaction logs for the customer within the date range
     */
    @Query("SELECT il FROM InteractionLog il WHERE il.customer = :customer AND il.timestamp BETWEEN :startDate AND :endDate")
    List<InteractionLog> findCustomerInteractionsInDateRange(@Param("customer") Customer customer,
                                                            @Param("startDate") LocalDateTime startDate, 
                                                            @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find interaction logs by user within a date range.
     * @param user the user
     * @param startDate the start date
     * @param endDate the end date
     * @return List of interaction logs by the user within the date range
     */
    @Query("SELECT il FROM InteractionLog il WHERE il.user = :user AND il.timestamp BETWEEN :startDate AND :endDate")
    List<InteractionLog> findUserInteractionsInDateRange(@Param("user") User user,
                                                         @Param("startDate") LocalDateTime startDate, 
                                                         @Param("endDate") LocalDateTime endDate);
    
    /**
     * Search interaction logs by notes content.
     * @param searchTerm the search term
     * @return List of interaction logs with matching notes
     */
    @Query("SELECT il FROM InteractionLog il WHERE LOWER(il.notes) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<InteractionLog> searchByNotes(@Param("searchTerm") String searchTerm);
    
    /**
     * Count interaction logs by customer.
     * @param customer the customer
     * @return count of interaction logs for the customer
     */
    long countByCustomer(Customer customer);
    
    /**
     * Count interaction logs by user.
     * @param user the user
     * @return count of interaction logs by the user
     */
    long countByUser(User user);
    
    /**
     * Count interaction logs by type.
     * @param type the interaction type
     * @return count of interaction logs of the specified type
     */
    long countByType(InteractionType type);
    
    /**
     * Find recent interaction logs for a customer (last N days).
     * @param customer the customer
     * @param cutoffDate the cutoff date
     * @return List of recent interaction logs for the customer
     */
    @Query("SELECT il FROM InteractionLog il WHERE il.customer = :customer AND il.timestamp >= :cutoffDate ORDER BY il.timestamp DESC")
    List<InteractionLog> findRecentCustomerInteractions(@Param("customer") Customer customer, 
                                                        @Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Find interaction logs ordered by timestamp (most recent first).
     * @return List of interaction logs ordered by timestamp descending
     */
    @Query("SELECT il FROM InteractionLog il ORDER BY il.timestamp DESC")
    List<InteractionLog> findAllOrderByTimestampDesc();
    
    /**
     * Find interaction logs by customer ordered by timestamp (most recent first).
     * @param customer the customer
     * @return List of interaction logs for the customer ordered by timestamp descending
     */
    @Query("SELECT il FROM InteractionLog il WHERE il.customer = :customer ORDER BY il.timestamp DESC")
    List<InteractionLog> findByCustomerOrderByTimestampDesc(@Param("customer") Customer customer);
    
    /**
     * Find interaction logs by multiple types.
     * @param types the list of interaction types
     * @return List of interaction logs with any of the specified types
     */
    @Query("SELECT il FROM InteractionLog il WHERE il.type IN :types")
    List<InteractionLog> findByTypeIn(@Param("types") List<InteractionType> types);
    
    /**
     * Find customers with no recent interactions.
     * @param cutoffDate the cutoff date for considering interactions recent
     * @return List of customers with no interactions after the cutoff date
     */
    @Query("SELECT DISTINCT c FROM Customer c WHERE c.id NOT IN " +
           "(SELECT il.customer.id FROM InteractionLog il WHERE il.timestamp >= :cutoffDate)")
    List<Customer> findCustomersWithNoRecentInteractions(@Param("cutoffDate") LocalDateTime cutoffDate);
}