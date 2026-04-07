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
import com.pasp.ict.salescrm.entity.SaleTransaction;
import com.pasp.ict.salescrm.entity.User;

/**
 * Repository interface for SaleTransaction entity operations.
 * Provides custom query methods for sales reporting and metrics calculations.
 */
@Repository
public interface SaleTransactionRepository extends JpaRepository<SaleTransaction, Long> {
    
    /**
     * Find sales transactions by customer.
     * @param customer the customer
     * @return List of sales transactions for the customer
     */
    List<SaleTransaction> findByCustomer(Customer customer);
    
    /**
     * Find sales transactions by sales user.
     * @param salesUser the sales user
     * @return List of sales transactions by the user
     */
    List<SaleTransaction> findBySalesUser(User salesUser);
    
    /**
     * Find sales transactions by lead.
     * @param lead the lead that was converted
     * @return List of sales transactions from the lead
     */
    List<SaleTransaction> findByLead(Lead lead);
    
    /**
     * Find sales transactions within a date range.
     * @param startDate the start date
     * @param endDate the end date
     * @return List of sales transactions within the date range
     */
    @Query("SELECT st FROM SaleTransaction st WHERE st.saleDate BETWEEN :startDate AND :endDate")
    List<SaleTransaction> findSalesInDateRange(@Param("startDate") LocalDateTime startDate, 
                                              @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find sales transactions by user within a date range.
     * @param salesUser the sales user
     * @param startDate the start date
     * @param endDate the end date
     * @return List of sales transactions by user within the date range
     */
    @Query("SELECT st FROM SaleTransaction st WHERE st.salesUser = :salesUser AND st.saleDate BETWEEN :startDate AND :endDate")
    List<SaleTransaction> findSalesByUserInDateRange(@Param("salesUser") User salesUser,
                                                    @Param("startDate") LocalDateTime startDate, 
                                                    @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find sales transactions with amount greater than specified value.
     * @param minAmount the minimum amount
     * @return List of sales transactions above the threshold
     */
    @Query("SELECT st FROM SaleTransaction st WHERE st.amount > :minAmount")
    List<SaleTransaction> findSalesAboveAmount(@Param("minAmount") BigDecimal minAmount);
    
    /**
     * Calculate total sales revenue.
     * @return total revenue from all sales transactions
     */
    @Query("SELECT COALESCE(SUM(st.amount), 0) FROM SaleTransaction st")
    BigDecimal calculateTotalRevenue();
    
    /**
     * Calculate total sales revenue within a date range.
     * @param startDate the start date
     * @param endDate the end date
     * @return total revenue within the date range
     */
    @Query("SELECT COALESCE(SUM(st.amount), 0) FROM SaleTransaction st WHERE st.saleDate BETWEEN :startDate AND :endDate")
    BigDecimal calculateRevenueInDateRange(@Param("startDate") LocalDateTime startDate, 
                                          @Param("endDate") LocalDateTime endDate);
    
    /**
     * Calculate total sales revenue by user.
     * @param salesUser the sales user
     * @return total revenue by the user
     */
    @Query("SELECT COALESCE(SUM(st.amount), 0) FROM SaleTransaction st WHERE st.salesUser = :salesUser")
    BigDecimal calculateRevenueByUser(@Param("salesUser") User salesUser);
    
    /**
     * Calculate total sales revenue by user within a date range.
     * @param salesUser the sales user
     * @param startDate the start date
     * @param endDate the end date
     * @return total revenue by user within the date range
     */
    @Query("SELECT COALESCE(SUM(st.amount), 0) FROM SaleTransaction st WHERE st.salesUser = :salesUser AND st.saleDate BETWEEN :startDate AND :endDate")
    BigDecimal calculateRevenueByUserInDateRange(@Param("salesUser") User salesUser,
                                                @Param("startDate") LocalDateTime startDate, 
                                                @Param("endDate") LocalDateTime endDate);
    
    /**
     * Count sales transactions by user.
     * @param salesUser the sales user
     * @return count of sales transactions by the user
     */
    long countBySalesUser(User salesUser);
    
    /**
     * Count sales transactions within a date range.
     * @param startDate the start date
     * @param endDate the end date
     * @return count of sales transactions within the date range
     */
    @Query("SELECT COUNT(st) FROM SaleTransaction st WHERE st.saleDate BETWEEN :startDate AND :endDate")
    long countSalesInDateRange(@Param("startDate") LocalDateTime startDate, 
                              @Param("endDate") LocalDateTime endDate);
    
    /**
     * Count sales transactions by user within a date range.
     * @param salesUser the sales user
     * @param startDate the start date
     * @param endDate the end date
     * @return count of sales transactions by user within the date range
     */
    @Query("SELECT COUNT(st) FROM SaleTransaction st WHERE st.salesUser = :salesUser AND st.saleDate BETWEEN :startDate AND :endDate")
    long countSalesByUserInDateRange(@Param("salesUser") User salesUser,
                                    @Param("startDate") LocalDateTime startDate, 
                                    @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find sales transactions from converted leads.
     * @return List of sales transactions that originated from leads
     */
    @Query("SELECT st FROM SaleTransaction st WHERE st.lead IS NOT NULL")
    List<SaleTransaction> findSalesFromLeads();
    
    /**
     * Find sales transactions not from leads (direct sales).
     * @return List of direct sales transactions
     */
    @Query("SELECT st FROM SaleTransaction st WHERE st.lead IS NULL")
    List<SaleTransaction> findDirectSales();
    
    /**
     * Calculate average sale amount.
     * @return average sale amount
     */
    @Query("SELECT AVG(st.amount) FROM SaleTransaction st")
    BigDecimal calculateAverageSaleAmount();
    
    /**
     * Calculate average sale amount by user.
     * @param salesUser the sales user
     * @return average sale amount by the user
     */
    @Query("SELECT AVG(st.amount) FROM SaleTransaction st WHERE st.salesUser = :salesUser")
    BigDecimal calculateAverageSaleAmountByUser(@Param("salesUser") User salesUser);
    
    /**
     * Find top performing sales users by revenue.
     * @param limit the maximum number of results
     * @return List of sales users ordered by total revenue descending
     */
    @Query("SELECT st.salesUser FROM SaleTransaction st GROUP BY st.salesUser ORDER BY SUM(st.amount) DESC")
    List<User> findTopPerformingSalesUsers();
    
    /**
     * Find sales transactions ordered by amount (highest first).
     * @return List of sales transactions ordered by amount descending
     */
    @Query("SELECT st FROM SaleTransaction st ORDER BY st.amount DESC")
    List<SaleTransaction> findAllOrderByAmountDesc();
    
    /**
     * Find sales transactions ordered by sale date (most recent first).
     * @return List of sales transactions ordered by sale date descending
     */
    @Query("SELECT st FROM SaleTransaction st ORDER BY st.saleDate DESC")
    List<SaleTransaction> findAllOrderBySaleDateDesc();
    
    /**
     * Search sales transactions by description.
     * @param searchTerm the search term
     * @return List of sales transactions with matching descriptions
     */
    @Query("SELECT st FROM SaleTransaction st WHERE LOWER(st.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<SaleTransaction> searchByDescription(@Param("searchTerm") String searchTerm);
}