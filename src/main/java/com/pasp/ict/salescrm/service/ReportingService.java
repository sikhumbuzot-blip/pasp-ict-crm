package com.pasp.ict.salescrm.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.opencsv.CSVWriter;
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

/**
 * Service class for sales metrics, analytics, and report generation.
 * Handles report generation with PDF and CSV export capabilities.
 */
@Service
@Transactional(readOnly = true)
public class ReportingService {
    
    private final SaleTransactionRepository saleTransactionRepository;
    private final LeadRepository leadRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Autowired
    public ReportingService(SaleTransactionRepository saleTransactionRepository,
                           LeadRepository leadRepository,
                           CustomerRepository customerRepository,
                           UserRepository userRepository) {
        this.saleTransactionRepository = saleTransactionRepository;
        this.leadRepository = leadRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
    }
    
    /**
     * Generate sales report showing revenue by time period, user, and customer segment.
     * @param startDate the start date (optional)
     * @param endDate the end date (optional)
     * @param userId the user ID to filter by (optional)
     * @param customerSegment the customer segment to filter by (optional)
     * @return Map containing sales report data
     */
    public Map<String, Object> generateSalesReport(LocalDateTime startDate, LocalDateTime endDate, 
                                                   Long userId, String customerSegment) {
        Map<String, Object> report = new HashMap<>();
        
        // Report metadata
        report.put("generatedAt", LocalDateTime.now());
        report.put("startDate", startDate);
        report.put("endDate", endDate);
        report.put("userId", userId);
        report.put("customerSegment", customerSegment);
        
        // Get filtered sales data
        List<SaleTransaction> sales = getFilteredSales(startDate, endDate, userId, customerSegment);
        
        // Calculate basic metrics
        BigDecimal totalRevenue = sales.stream()
                .map(SaleTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        long totalSales = sales.size();
        BigDecimal averageSale = totalSales > 0 ? 
                totalRevenue.divide(BigDecimal.valueOf(totalSales), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        
        report.put("totalRevenue", totalRevenue);
        report.put("totalSales", totalSales);
        report.put("averageSaleAmount", averageSale);
        
        // Revenue by time period (monthly breakdown)
        Map<String, BigDecimal> revenueByMonth = calculateRevenueByMonth(sales);
        report.put("revenueByMonth", revenueByMonth);
        
        // Revenue by user
        Map<String, BigDecimal> revenueByUser = calculateRevenueByUser(sales);
        report.put("revenueByUser", revenueByUser);
        
        // Revenue by customer segment (company-based)
        Map<String, BigDecimal> revenueBySegment = calculateRevenueByCustomerSegment(sales);
        report.put("revenueByCustomerSegment", revenueBySegment);
        
        // Sales trends and performance metrics
        Map<String, Object> trends = calculateSalesTrends(sales, startDate, endDate);
        report.put("trends", trends);
        
        return report;
    }
    
    /**
     * Calculate sales metrics including revenue and conversion rates.
     * @param startDate the start date (optional)
     * @param endDate the end date (optional)
     * @return Map containing sales metrics
     */
    public Map<String, Object> calculateSalesMetrics(LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> metrics = new HashMap<>();
        
        // Revenue metrics
        BigDecimal totalRevenue;
        long totalSales;
        
        if (startDate != null && endDate != null) {
            totalRevenue = saleTransactionRepository.calculateRevenueInDateRange(startDate, endDate);
            totalSales = saleTransactionRepository.countSalesInDateRange(startDate, endDate);
        } else {
            totalRevenue = saleTransactionRepository.calculateTotalRevenue();
            totalSales = saleTransactionRepository.count();
        }
        
        metrics.put("totalRevenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
        metrics.put("totalSales", totalSales);
        
        // Average sale amount
        BigDecimal averageSale = totalSales > 0 ? 
                totalRevenue.divide(BigDecimal.valueOf(totalSales), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        metrics.put("averageSaleAmount", averageSale);
        
        // Lead conversion metrics
        long totalLeads = leadRepository.count();
        long wonLeads = leadRepository.countByStatus(LeadStatus.CLOSED_WON);
        long lostLeads = leadRepository.countByStatus(LeadStatus.CLOSED_LOST);
        
        double conversionRate = totalLeads > 0 ? (double) wonLeads / totalLeads * 100 : 0.0;
        double lossRate = totalLeads > 0 ? (double) lostLeads / totalLeads * 100 : 0.0;
        
        metrics.put("totalLeads", totalLeads);
        metrics.put("wonLeads", wonLeads);
        metrics.put("lostLeads", lostLeads);
        metrics.put("conversionRate", Math.round(conversionRate * 100.0) / 100.0);
        metrics.put("lossRate", Math.round(lossRate * 100.0) / 100.0);
        
        // Performance by sales users
        List<User> salesUsers = userRepository.findActiveSalesUsers();
        Map<String, Map<String, Object>> userPerformance = new HashMap<>();
        
        for (User user : salesUsers) {
            Map<String, Object> userMetrics = new HashMap<>();
            
            BigDecimal userRevenue = saleTransactionRepository.calculateRevenueByUser(user);
            long userSales = saleTransactionRepository.countBySalesUser(user);
            long userLeads = leadRepository.countByAssignedTo(user);
            long userWonLeads = leadRepository.countByStatusAndAssignedTo(LeadStatus.CLOSED_WON, user);
            
            double userConversionRate = userLeads > 0 ? (double) userWonLeads / userLeads * 100 : 0.0;
            
            userMetrics.put("revenue", userRevenue != null ? userRevenue : BigDecimal.ZERO);
            userMetrics.put("sales", userSales);
            userMetrics.put("leads", userLeads);
            userMetrics.put("wonLeads", userWonLeads);
            userMetrics.put("conversionRate", Math.round(userConversionRate * 100.0) / 100.0);
            
            userPerformance.put(user.getUsername(), userMetrics);
        }
        
        metrics.put("userPerformance", userPerformance);
        
        return metrics;
    }
    
    /**
     * Generate real-time dashboard data for KPIs.
     * @return Map containing dashboard data
     */
    public Map<String, Object> getDashboardData() {
        Map<String, Object> dashboard = new HashMap<>();
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime weekStart = now.minusWeeks(1);
        LocalDateTime monthStart = now.minusMonths(1);
        
        // Today's metrics
        Map<String, Object> todayMetrics = new HashMap<>();
        todayMetrics.put("revenue", saleTransactionRepository.calculateRevenueInDateRange(todayStart, now));
        todayMetrics.put("sales", saleTransactionRepository.countSalesInDateRange(todayStart, now));
        todayMetrics.put("newLeads", leadRepository.findLeadsCreatedBetween(todayStart, now).size());
        todayMetrics.put("newCustomers", customerRepository.findCustomersCreatedBetween(todayStart, now).size());
        dashboard.put("today", todayMetrics);
        
        // This week's metrics
        Map<String, Object> weekMetrics = new HashMap<>();
        weekMetrics.put("revenue", saleTransactionRepository.calculateRevenueInDateRange(weekStart, now));
        weekMetrics.put("sales", saleTransactionRepository.countSalesInDateRange(weekStart, now));
        weekMetrics.put("newLeads", leadRepository.findLeadsCreatedBetween(weekStart, now).size());
        weekMetrics.put("newCustomers", customerRepository.findCustomersCreatedBetween(weekStart, now).size());
        dashboard.put("thisWeek", weekMetrics);
        
        // This month's metrics
        Map<String, Object> monthMetrics = new HashMap<>();
        monthMetrics.put("revenue", saleTransactionRepository.calculateRevenueInDateRange(monthStart, now));
        monthMetrics.put("sales", saleTransactionRepository.countSalesInDateRange(monthStart, now));
        monthMetrics.put("newLeads", leadRepository.findLeadsCreatedBetween(monthStart, now).size());
        monthMetrics.put("newCustomers", customerRepository.findCustomersCreatedBetween(monthStart, now).size());
        dashboard.put("thisMonth", monthMetrics);
        
        // Pipeline status
        Map<String, Long> pipelineStatus = new HashMap<>();
        for (LeadStatus status : LeadStatus.values()) {
            pipelineStatus.put(status.name(), leadRepository.countByStatus(status));
        }
        dashboard.put("pipelineStatus", pipelineStatus);
        
        // Top performers (last 30 days)
        List<SaleTransaction> recentSales = saleTransactionRepository.findSalesInDateRange(monthStart, now);
        Map<String, BigDecimal> topPerformers = calculateRevenueByUser(recentSales);
        dashboard.put("topPerformers", topPerformers);
        
        dashboard.put("lastUpdated", now);
        
        return dashboard;
    }
    
    /**
     * Export report in CSV format.
     * @param reportData the report data to export
     * @return byte array containing CSV data
     * @throws IOException if export fails
     */
    public byte[] exportReportAsCSV(Map<String, Object> reportData) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(outputStream))) {
            // Write header
            writer.writeNext(new String[]{"Report Generated", reportData.get("generatedAt").toString()});
            writer.writeNext(new String[]{"Start Date", 
                reportData.get("startDate") != null ? reportData.get("startDate").toString() : "All Time"});
            writer.writeNext(new String[]{"End Date", 
                reportData.get("endDate") != null ? reportData.get("endDate").toString() : "All Time"});
            writer.writeNext(new String[]{}); // Empty row
            
            // Write summary metrics
            writer.writeNext(new String[]{"Summary Metrics"});
            writer.writeNext(new String[]{"Total Revenue", reportData.get("totalRevenue").toString()});
            writer.writeNext(new String[]{"Total Sales", reportData.get("totalSales").toString()});
            writer.writeNext(new String[]{"Average Sale Amount", reportData.get("averageSaleAmount").toString()});
            writer.writeNext(new String[]{}); // Empty row
            
            // Write revenue by user
            @SuppressWarnings("unchecked")
            Map<String, BigDecimal> revenueByUser = (Map<String, BigDecimal>) reportData.get("revenueByUser");
            if (revenueByUser != null && !revenueByUser.isEmpty()) {
                writer.writeNext(new String[]{"Revenue by User"});
                writer.writeNext(new String[]{"User", "Revenue"});
                for (Map.Entry<String, BigDecimal> entry : revenueByUser.entrySet()) {
                    writer.writeNext(new String[]{entry.getKey(), entry.getValue().toString()});
                }
                writer.writeNext(new String[]{}); // Empty row
            }
            
            // Write revenue by month
            @SuppressWarnings("unchecked")
            Map<String, BigDecimal> revenueByMonth = (Map<String, BigDecimal>) reportData.get("revenueByMonth");
            if (revenueByMonth != null && !revenueByMonth.isEmpty()) {
                writer.writeNext(new String[]{"Revenue by Month"});
                writer.writeNext(new String[]{"Month", "Revenue"});
                for (Map.Entry<String, BigDecimal> entry : revenueByMonth.entrySet()) {
                    writer.writeNext(new String[]{entry.getKey(), entry.getValue().toString()});
                }
            }
        }
        
        return outputStream.toByteArray();
    }
    
    /**
     * Export report in PDF format (simplified implementation).
     * Note: In a real implementation, you would use a PDF library like iText or Apache PDFBox
     * @param reportData the report data to export
     * @return byte array containing PDF data
     */
    public byte[] exportReportAsPDF(Map<String, Object> reportData) {
        // This is a simplified implementation that returns a text-based "PDF"
        // In a real application, you would use a proper PDF library
        StringBuilder pdfContent = new StringBuilder();
        
        pdfContent.append("SALES REPORT\n");
        pdfContent.append("=============\n\n");
        pdfContent.append("Generated: ").append(reportData.get("generatedAt")).append("\n");
        pdfContent.append("Start Date: ").append(
            reportData.get("startDate") != null ? reportData.get("startDate") : "All Time").append("\n");
        pdfContent.append("End Date: ").append(
            reportData.get("endDate") != null ? reportData.get("endDate") : "All Time").append("\n\n");
        
        pdfContent.append("SUMMARY METRICS\n");
        pdfContent.append("---------------\n");
        pdfContent.append("Total Revenue: $").append(reportData.get("totalRevenue")).append("\n");
        pdfContent.append("Total Sales: ").append(reportData.get("totalSales")).append("\n");
        pdfContent.append("Average Sale: $").append(reportData.get("averageSaleAmount")).append("\n\n");
        
        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> revenueByUser = (Map<String, BigDecimal>) reportData.get("revenueByUser");
        if (revenueByUser != null && !revenueByUser.isEmpty()) {
            pdfContent.append("REVENUE BY USER\n");
            pdfContent.append("---------------\n");
            for (Map.Entry<String, BigDecimal> entry : revenueByUser.entrySet()) {
                pdfContent.append(entry.getKey()).append(": $").append(entry.getValue()).append("\n");
            }
        }
        
        return pdfContent.toString().getBytes();
    }
    
    /**
     * Get filtered sales based on criteria.
     */
    private List<SaleTransaction> getFilteredSales(LocalDateTime startDate, LocalDateTime endDate, 
                                                   Long userId, String customerSegment) {
        List<SaleTransaction> sales;
        
        if (startDate != null && endDate != null) {
            if (userId != null) {
                User user = new User();
                user.setId(userId);
                sales = saleTransactionRepository.findSalesByUserInDateRange(user, startDate, endDate);
            } else {
                sales = saleTransactionRepository.findSalesInDateRange(startDate, endDate);
            }
        } else {
            if (userId != null) {
                User user = new User();
                user.setId(userId);
                sales = saleTransactionRepository.findBySalesUser(user);
            } else {
                sales = saleTransactionRepository.findAllOrderBySaleDateDesc();
            }
        }
        
        // Filter by customer segment if specified
        if (customerSegment != null && !customerSegment.trim().isEmpty()) {
            sales = sales.stream()
                    .filter(sale -> sale.getCustomer().getCompany() != null && 
                                   sale.getCustomer().getCompany().toLowerCase().contains(customerSegment.toLowerCase()))
                    .toList();
        }
        
        return sales;
    }
    
    /**
     * Calculate revenue by month.
     */
    private Map<String, BigDecimal> calculateRevenueByMonth(List<SaleTransaction> sales) {
        Map<String, BigDecimal> revenueByMonth = new HashMap<>();
        
        for (SaleTransaction sale : sales) {
            String monthKey = sale.getSaleDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            revenueByMonth.merge(monthKey, sale.getAmount(), BigDecimal::add);
        }
        
        return revenueByMonth;
    }
    
    /**
     * Calculate revenue by user.
     */
    private Map<String, BigDecimal> calculateRevenueByUser(List<SaleTransaction> sales) {
        Map<String, BigDecimal> revenueByUser = new HashMap<>();
        
        for (SaleTransaction sale : sales) {
            String userKey = sale.getSalesUser().getUsername();
            revenueByUser.merge(userKey, sale.getAmount(), BigDecimal::add);
        }
        
        return revenueByUser;
    }
    
    /**
     * Calculate revenue by customer segment.
     */
    private Map<String, BigDecimal> calculateRevenueByCustomerSegment(List<SaleTransaction> sales) {
        Map<String, BigDecimal> revenueBySegment = new HashMap<>();
        
        for (SaleTransaction sale : sales) {
            String segment = sale.getCustomer().getCompany() != null ? 
                    sale.getCustomer().getCompany() : "Individual";
            revenueBySegment.merge(segment, sale.getAmount(), BigDecimal::add);
        }
        
        return revenueBySegment;
    }
    
    /**
     * Calculate sales trends.
     */
    private Map<String, Object> calculateSalesTrends(List<SaleTransaction> sales, 
                                                     LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> trends = new HashMap<>();
        
        if (sales.isEmpty()) {
            trends.put("growth", 0.0);
            trends.put("trend", "stable");
            return trends;
        }
        
        // Calculate growth rate if we have date range
        if (startDate != null && endDate != null) {
            LocalDateTime midPoint = startDate.plusDays(
                java.time.Duration.between(startDate, endDate).toDays() / 2);
            
            BigDecimal firstHalfRevenue = sales.stream()
                    .filter(sale -> sale.getSaleDate().isBefore(midPoint))
                    .map(SaleTransaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal secondHalfRevenue = sales.stream()
                    .filter(sale -> sale.getSaleDate().isAfter(midPoint))
                    .map(SaleTransaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            if (firstHalfRevenue.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal growth = secondHalfRevenue.subtract(firstHalfRevenue)
                        .divide(firstHalfRevenue, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                
                trends.put("growth", growth.doubleValue());
                trends.put("trend", growth.compareTo(BigDecimal.ZERO) > 0 ? "growing" : "declining");
            } else {
                trends.put("growth", 0.0);
                trends.put("trend", "stable");
            }
        }
        
        // Calculate average days between sales
        if (sales.size() > 1) {
            List<SaleTransaction> sortedSales = new ArrayList<>(sales);
            sortedSales.sort((a, b) -> a.getSaleDate().compareTo(b.getSaleDate()));
            
            long totalDays = 0;
            for (int i = 1; i < sortedSales.size(); i++) {
                totalDays += java.time.Duration.between(
                        sortedSales.get(i-1).getSaleDate(), 
                        sortedSales.get(i).getSaleDate()).toDays();
            }
            
            double averageDaysBetweenSales = (double) totalDays / (sortedSales.size() - 1);
            trends.put("averageDaysBetweenSales", averageDaysBetweenSales);
        }
        
        return trends;
    }
}