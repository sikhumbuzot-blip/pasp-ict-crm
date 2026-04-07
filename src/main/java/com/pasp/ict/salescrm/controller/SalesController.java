package com.pasp.ict.salescrm.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.pasp.ict.salescrm.entity.Customer;
import com.pasp.ict.salescrm.entity.Lead;
import com.pasp.ict.salescrm.entity.LeadStatus;
import com.pasp.ict.salescrm.entity.SaleTransaction;
import com.pasp.ict.salescrm.entity.User;
import com.pasp.ict.salescrm.service.CustomerService;
import com.pasp.ict.salescrm.service.SalesService;
import com.pasp.ict.salescrm.service.UserService;

/**
 * Controller for handling sales-related requests.
 * Manages leads, sales transactions, and sales pipeline.
 */
@Controller
@RequestMapping("/sales")
@PreAuthorize("hasAnyRole('ADMIN', 'SALES')")
public class SalesController {

    private final SalesService salesService;
    private final CustomerService customerService;
    private final UserService userService;

    public SalesController(SalesService salesService, CustomerService customerService, UserService userService) {
        this.salesService = salesService;
        this.customerService = customerService;
        this.userService = userService;
    }

    /**
     * Lead management interface - list all leads with filtering options.
     */
    @GetMapping("/leads")
    public String leads(@RequestParam(value = "status", required = false) LeadStatus status,
                       @RequestParam(value = "search", required = false) String search,
                       Model model) {
        User currentUser = getCurrentUser();
        model.addAttribute("currentUser", currentUser);
        
        List<Lead> leads;
        if (search != null && !search.trim().isEmpty()) {
            leads = salesService.searchLeads(search.trim());
        } else if (status != null) {
            leads = salesService.findLeadsByStatus(status);
        } else {
            // Show all open leads by default
            leads = salesService.findOpenLeads();
        }
        
        model.addAttribute("leads", leads);
        model.addAttribute("leadStatuses", LeadStatus.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("searchTerm", search);
        
        return "sales/leads";
    }

    /**
     * Individual lead details and status updates.
     */
    @GetMapping("/leads/{id}")
    public String leadDetails(@PathVariable Long id, Model model) {
        User currentUser = getCurrentUser();
        model.addAttribute("currentUser", currentUser);
        
        Optional<Lead> leadOpt = salesService.findLeadById(id);
        if (leadOpt.isEmpty()) {
            model.addAttribute("errorMessage", "Lead not found");
            return "redirect:/sales/leads";
        }
        
        Lead lead = leadOpt.get();
        model.addAttribute("lead", lead);
        model.addAttribute("leadStatuses", LeadStatus.values());
        
        // Get valid transitions for current status
        model.addAttribute("validTransitions", getValidTransitions(lead.getStatus()));
        
        // Get sales transactions for this lead
        List<SaleTransaction> transactions = salesService.findSalesByCustomer(lead.getCustomer());
        model.addAttribute("transactions", transactions);
        
        return "sales/lead-details";
    }

    /**
     * Create new lead form.
     */
    @GetMapping("/leads/create")
    public String createLeadForm(Model model) {
        User currentUser = getCurrentUser();
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("lead", new Lead());
        
        // Get customers for dropdown
        List<Customer> customers = customerService.searchCustomers("");
        model.addAttribute("customers", customers);
        
        // Get sales users for assignment
        List<User> salesUsers = userService.findActiveSalesUsers();
        model.addAttribute("salesUsers", salesUsers);
        
        return "sales/create-lead";
    }

    /**
     * Create new lead.
     */
    @PostMapping("/leads/create")
    public String createLead(@RequestParam String title,
                            @RequestParam(required = false) String description,
                            @RequestParam(required = false) BigDecimal estimatedValue,
                            @RequestParam Long customerId,
                            @RequestParam(required = false) Long assignedToId,
                            RedirectAttributes redirectAttributes) {
        try {
            User currentUser = getCurrentUser();
            
            Customer customer = customerService.findById(customerId)
                    .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
            
            User assignedTo = null;
            if (assignedToId != null) {
                assignedTo = userService.findById(assignedToId)
                        .orElseThrow(() -> new IllegalArgumentException("Assigned user not found"));
            }
            
            Lead lead = salesService.createLead(title, description, estimatedValue, customer, assignedTo, currentUser);
            
            redirectAttributes.addFlashAttribute("successMessage", "Lead created successfully");
            return "redirect:/sales/leads/" + lead.getId();
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating lead: " + e.getMessage());
            return "redirect:/sales/leads/create";
        }
    }

    /**
     * Update lead status.
     */
    @PostMapping("/leads/{id}/status")
    public String updateLeadStatus(@PathVariable Long id,
                                  @RequestParam LeadStatus status,
                                  RedirectAttributes redirectAttributes) {
        try {
            User currentUser = getCurrentUser();
            Lead updatedLead = salesService.updateLeadStatus(id, status, currentUser);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Lead status updated to " + status.name());
            return "redirect:/sales/leads/" + updatedLead.getId();
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error updating lead status: " + e.getMessage());
            return "redirect:/sales/leads/" + id;
        }
    }

    /**
     * Convert lead to sale form.
     */
    @GetMapping("/leads/{id}/convert")
    public String convertLeadForm(@PathVariable Long id, Model model) {
        User currentUser = getCurrentUser();
        model.addAttribute("currentUser", currentUser);
        
        Optional<Lead> leadOpt = salesService.findLeadById(id);
        if (leadOpt.isEmpty()) {
            model.addAttribute("errorMessage", "Lead not found");
            return "redirect:/sales/leads";
        }
        
        Lead lead = leadOpt.get();
        if (lead.isClosed()) {
            model.addAttribute("errorMessage", "Cannot convert closed lead");
            return "redirect:/sales/leads/" + id;
        }
        
        model.addAttribute("lead", lead);
        model.addAttribute("saleTransaction", new SaleTransaction());
        
        return "sales/convert-lead";
    }

    /**
     * Convert lead to sale.
     */
    @PostMapping("/leads/{id}/convert")
    public String convertLead(@PathVariable Long id,
                             @RequestParam BigDecimal amount,
                             @RequestParam(required = false) String description,
                             RedirectAttributes redirectAttributes) {
        try {
            User currentUser = getCurrentUser();
            
            SaleTransaction transaction = salesService.convertLeadToSale(
                id, amount, LocalDateTime.now(), description, currentUser);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Lead converted to sale successfully");
            return "redirect:/sales/transactions/" + transaction.getId();
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error converting lead: " + e.getMessage());
            return "redirect:/sales/leads/" + id + "/convert";
        }
    }

    /**
     * Sales transaction history and creation.
     */
    @GetMapping("/transactions")
    public String transactions(@RequestParam(value = "customerId", required = false) Long customerId,
                              Model model) {
        User currentUser = getCurrentUser();
        model.addAttribute("currentUser", currentUser);
        
        List<SaleTransaction> transactions;
        if (customerId != null) {
            Customer customer = customerService.findById(customerId)
                    .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
            transactions = salesService.findSalesByCustomer(customer);
            model.addAttribute("selectedCustomer", customer);
        } else {
            transactions = salesService.findSalesByUser(currentUser);
        }
        
        model.addAttribute("transactions", transactions);
        
        return "sales/transactions";
    }

    /**
     * Individual transaction details.
     */
    @GetMapping("/transactions/{id}")
    public String transactionDetails(@PathVariable Long id, Model model) {
        User currentUser = getCurrentUser();
        model.addAttribute("currentUser", currentUser);
        
        Optional<SaleTransaction> transactionOpt = salesService.findSaleById(id);
        if (transactionOpt.isEmpty()) {
            model.addAttribute("errorMessage", "Transaction not found");
            return "redirect:/sales/transactions";
        }
        
        SaleTransaction transaction = transactionOpt.get();
        model.addAttribute("transaction", transaction);
        
        return "sales/transaction-details";
    }

    /**
     * Create direct sale form.
     */
    @GetMapping("/transactions/create")
    public String createSaleForm(Model model) {
        User currentUser = getCurrentUser();
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("saleTransaction", new SaleTransaction());
        
        // Get customers for dropdown
        List<Customer> customers = customerService.searchCustomers("");
        model.addAttribute("customers", customers);
        
        return "sales/create-sale";
    }

    /**
     * Create direct sale.
     */
    @PostMapping("/transactions/create")
    public String createSale(@RequestParam BigDecimal amount,
                            @RequestParam Long customerId,
                            @RequestParam(required = false) String description,
                            RedirectAttributes redirectAttributes) {
        try {
            User currentUser = getCurrentUser();
            
            Customer customer = customerService.findById(customerId)
                    .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
            
            SaleTransaction transaction = salesService.createDirectSale(
                amount, LocalDateTime.now(), description, customer, currentUser);
            
            redirectAttributes.addFlashAttribute("successMessage", "Sale created successfully");
            return "redirect:/sales/transactions/" + transaction.getId();
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating sale: " + e.getMessage());
            return "redirect:/sales/transactions/create";
        }
    }

    /**
     * Sales pipeline view with status visualization.
     */
    @GetMapping("/pipeline")
    public String pipeline(Model model) {
        User currentUser = getCurrentUser();
        model.addAttribute("currentUser", currentUser);
        
        // Get leads grouped by status
        Map<LeadStatus, List<Lead>> pipelineData = Map.of(
            LeadStatus.NEW, salesService.findLeadsByStatus(LeadStatus.NEW),
            LeadStatus.CONTACTED, salesService.findLeadsByStatus(LeadStatus.CONTACTED),
            LeadStatus.QUALIFIED, salesService.findLeadsByStatus(LeadStatus.QUALIFIED),
            LeadStatus.PROPOSAL, salesService.findLeadsByStatus(LeadStatus.PROPOSAL),
            LeadStatus.NEGOTIATION, salesService.findLeadsByStatus(LeadStatus.NEGOTIATION),
            LeadStatus.CLOSED_WON, salesService.findLeadsByStatus(LeadStatus.CLOSED_WON),
            LeadStatus.CLOSED_LOST, salesService.findLeadsByStatus(LeadStatus.CLOSED_LOST)
        );
        
        model.addAttribute("pipelineData", pipelineData);
        model.addAttribute("leadStatuses", LeadStatus.values());
        
        return "sales/pipeline";
    }

    /**
     * Sales performance metrics.
     */
    @GetMapping("/metrics")
    public String metrics(Model model) {
        User currentUser = getCurrentUser();
        model.addAttribute("currentUser", currentUser);
        
        // Get individual performance metrics
        Map<String, Object> metrics = salesService.getIndividualPerformance(currentUser);
        model.addAttribute("metrics", metrics);
        
        return "sales/metrics";
    }

    /**
     * Sales metrics API endpoint.
     */
    @GetMapping("/api/metrics")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getMetricsApi() {
        User currentUser = getCurrentUser();
        Map<String, Object> metrics = salesService.getIndividualPerformance(currentUser);
        return ResponseEntity.ok(metrics);
    }

    /**
     * Pipeline data API endpoint.
     */
    @GetMapping("/api/pipeline")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getPipelineApi() {
        Map<String, Object> pipelineData = Map.of(
            "NEW", salesService.findLeadsByStatus(LeadStatus.NEW).size(),
            "CONTACTED", salesService.findLeadsByStatus(LeadStatus.CONTACTED).size(),
            "QUALIFIED", salesService.findLeadsByStatus(LeadStatus.QUALIFIED).size(),
            "PROPOSAL", salesService.findLeadsByStatus(LeadStatus.PROPOSAL).size(),
            "NEGOTIATION", salesService.findLeadsByStatus(LeadStatus.NEGOTIATION).size(),
            "CLOSED_WON", salesService.findLeadsByStatus(LeadStatus.CLOSED_WON).size(),
            "CLOSED_LOST", salesService.findLeadsByStatus(LeadStatus.CLOSED_LOST).size()
        );
        
        return ResponseEntity.ok(pipelineData);
    }

    /**
     * Get current authenticated user.
     */
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        return userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    /**
     * Get valid status transitions for a lead status.
     */
    private List<LeadStatus> getValidTransitions(LeadStatus currentStatus) {
        switch (currentStatus) {
            case NEW:
                return List.of(LeadStatus.CONTACTED, LeadStatus.CLOSED_LOST);
            case CONTACTED:
                return List.of(LeadStatus.QUALIFIED, LeadStatus.CLOSED_LOST);
            case QUALIFIED:
                return List.of(LeadStatus.PROPOSAL, LeadStatus.CLOSED_LOST);
            case PROPOSAL:
                return List.of(LeadStatus.NEGOTIATION, LeadStatus.CLOSED_LOST);
            case NEGOTIATION:
                return List.of(LeadStatus.CLOSED_WON, LeadStatus.CLOSED_LOST);
            case CLOSED_WON:
            case CLOSED_LOST:
                return List.of(); // No transitions from final states
            default:
                return List.of();
        }
    }
}