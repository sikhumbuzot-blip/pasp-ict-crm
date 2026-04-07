package com.pasp.ict.salescrm.controller;

import java.util.List;
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
import com.pasp.ict.salescrm.entity.InteractionLog;
import com.pasp.ict.salescrm.entity.InteractionType;
import com.pasp.ict.salescrm.entity.Lead;
import com.pasp.ict.salescrm.entity.SaleTransaction;
import com.pasp.ict.salescrm.entity.User;
import com.pasp.ict.salescrm.security.InputValidationService;
import com.pasp.ict.salescrm.service.CustomerService;
import com.pasp.ict.salescrm.service.SalesService;
import com.pasp.ict.salescrm.service.UserService;

/**
 * Controller for handling customer-related requests.
 * Manages customer data, search functionality, and interaction logging.
 */
@Controller
@RequestMapping("/customers")
@PreAuthorize("hasAnyRole('ADMIN', 'SALES')")
public class CustomerController {

    private final CustomerService customerService;
    private final SalesService salesService;
    private final UserService userService;
    private final InputValidationService inputValidationService;

    public CustomerController(CustomerService customerService, SalesService salesService, 
                             UserService userService, InputValidationService inputValidationService) {
        this.customerService = customerService;
        this.salesService = salesService;
        this.userService = userService;
        this.inputValidationService = inputValidationService;
    }

    /**
     * Customer listing and search (within 2 seconds for 10,000 records).
     */
    @GetMapping
    public String customers(@RequestParam(value = "search", required = false) String search,
                           @RequestParam(value = "company", required = false) String company,
                           Model model) {
        User currentUser = getCurrentUser();
        model.addAttribute("currentUser", currentUser);
        
        List<Customer> customers;
        try {
            if (search != null && !search.trim().isEmpty()) {
                String sanitizedSearch = inputValidationService.sanitizeSearchQuery(search);
                customers = customerService.searchCustomers(sanitizedSearch);
                model.addAttribute("searchTerm", sanitizedSearch);
            } else if (company != null && !company.trim().isEmpty()) {
                String sanitizedCompany = inputValidationService.sanitizeSearchQuery(company);
                customers = customerService.findByCompany(sanitizedCompany);
                model.addAttribute("companyFilter", sanitizedCompany);
            } else {
                customers = customerService.searchCustomers(""); // Get all customers
            }
        } catch (SecurityException e) {
            model.addAttribute("errorMessage", "Invalid search criteria: " + e.getMessage());
            customers = customerService.searchCustomers(""); // Get all customers as fallback
        }
        
        model.addAttribute("customers", customers);
        
        return "customers/list";
    }

    /**
     * Customer profile and interaction history.
     */
    @GetMapping("/{id}")
    public String customerProfile(@PathVariable Long id, Model model) {
        User currentUser = getCurrentUser();
        model.addAttribute("currentUser", currentUser);
        
        Optional<Customer> customerOpt = customerService.findById(id);
        if (customerOpt.isEmpty()) {
            model.addAttribute("errorMessage", "Customer not found");
            return "redirect:/customers";
        }
        
        Customer customer = customerOpt.get();
        model.addAttribute("customer", customer);
        
        // Get interaction history
        List<InteractionLog> interactions = customerService.getCustomerInteractionHistory(id);
        model.addAttribute("interactions", interactions);
        
        // Get leads for this customer
        List<Lead> leads = salesService.findLeadsByCustomer(customer);
        model.addAttribute("leads", leads);
        
        // Get sales transactions for this customer
        List<SaleTransaction> transactions = salesService.findSalesByCustomer(customer);
        model.addAttribute("transactions", transactions);
        
        // Get interaction types for dropdown
        model.addAttribute("interactionTypes", InteractionType.values());
        
        return "customers/profile";
    }

    /**
     * Customer creation form.
     */
    @GetMapping("/create")
    public String createCustomerForm(Model model) {
        User currentUser = getCurrentUser();
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("customer", new Customer());
        
        return "customers/create";
    }

    /**
     * Create new customer.
     */
    @PostMapping("/create")
    public String createCustomer(@RequestParam String name,
                                @RequestParam String email,
                                @RequestParam(required = false) String phone,
                                @RequestParam(required = false) String company,
                                @RequestParam(required = false) String address,
                                RedirectAttributes redirectAttributes) {
        try {
            User currentUser = getCurrentUser();
            
            // Validate and sanitize input
            String validatedName = inputValidationService.validateText(name, "name", 100);
            String validatedEmail = inputValidationService.validateEmail(email);
            String validatedPhone = inputValidationService.validatePhone(phone);
            String validatedCompany = inputValidationService.validateAlphanumeric(company, "company");
            String validatedAddress = inputValidationService.validateNotes(address, 500);
            
            Customer customer = customerService.createCustomer(
                validatedName, validatedEmail, validatedPhone, validatedCompany, validatedAddress, currentUser);
            
            redirectAttributes.addFlashAttribute("successMessage", "Customer created successfully");
            return "redirect:/customers/" + customer.getId();
            
        } catch (SecurityException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Validation error: " + e.getMessage());
            return "redirect:/customers/create";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating customer: " + e.getMessage());
            return "redirect:/customers/create";
        }
    }

    /**
     * Customer editing form.
     */
    @GetMapping("/{id}/edit")
    public String editCustomerForm(@PathVariable Long id, Model model) {
        User currentUser = getCurrentUser();
        model.addAttribute("currentUser", currentUser);
        
        Optional<Customer> customerOpt = customerService.findById(id);
        if (customerOpt.isEmpty()) {
            model.addAttribute("errorMessage", "Customer not found");
            return "redirect:/customers";
        }
        
        Customer customer = customerOpt.get();
        model.addAttribute("customer", customer);
        
        return "customers/edit";
    }

    /**
     * Update customer information.
     */
    @PostMapping("/{id}/edit")
    public String updateCustomer(@PathVariable Long id,
                                @RequestParam String name,
                                @RequestParam String email,
                                @RequestParam(required = false) String phone,
                                @RequestParam(required = false) String company,
                                @RequestParam(required = false) String address,
                                RedirectAttributes redirectAttributes) {
        try {
            User currentUser = getCurrentUser();
            
            // Validate and sanitize input
            String validatedName = inputValidationService.validateText(name, "name", 100);
            String validatedEmail = inputValidationService.validateEmail(email);
            String validatedPhone = inputValidationService.validatePhone(phone);
            String validatedCompany = inputValidationService.validateAlphanumeric(company, "company");
            String validatedAddress = inputValidationService.validateNotes(address, 500);
            
            Customer customer = customerService.updateCustomer(
                id, validatedName, validatedEmail, validatedPhone, validatedCompany, validatedAddress, currentUser);
            
            redirectAttributes.addFlashAttribute("successMessage", "Customer updated successfully");
            return "redirect:/customers/" + customer.getId();
            
        } catch (SecurityException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Validation error: " + e.getMessage());
            return "redirect:/customers/" + id + "/edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating customer: " + e.getMessage());
            return "redirect:/customers/" + id + "/edit";
        }
    }

    /**
     * Interaction logging interface.
     */
    @GetMapping("/{id}/interactions")
    public String customerInteractions(@PathVariable Long id,
                                      @RequestParam(value = "type", required = false) InteractionType type,
                                      @RequestParam(value = "days", required = false, defaultValue = "30") int days,
                                      Model model) {
        User currentUser = getCurrentUser();
        model.addAttribute("currentUser", currentUser);
        
        Optional<Customer> customerOpt = customerService.findById(id);
        if (customerOpt.isEmpty()) {
            model.addAttribute("errorMessage", "Customer not found");
            return "redirect:/customers";
        }
        
        Customer customer = customerOpt.get();
        model.addAttribute("customer", customer);
        
        List<InteractionLog> interactions;
        if (type != null) {
            interactions = customerService.getCustomerInteractionsByType(id, type);
            model.addAttribute("selectedType", type);
        } else {
            interactions = customerService.getRecentCustomerInteractions(id, days);
        }
        
        model.addAttribute("interactions", interactions);
        model.addAttribute("interactionTypes", InteractionType.values());
        model.addAttribute("selectedDays", days);
        
        return "customers/interactions";
    }

    /**
     * Log new interaction.
     */
    @PostMapping("/{id}/interactions")
    public String logInteraction(@PathVariable Long id,
                                @RequestParam InteractionType type,
                                @RequestParam String notes,
                                RedirectAttributes redirectAttributes) {
        try {
            User currentUser = getCurrentUser();
            
            // Validate and sanitize notes
            String validatedNotes = inputValidationService.validateNotes(notes, 1000);
            
            customerService.logInteraction(id, type, validatedNotes, currentUser);
            
            redirectAttributes.addFlashAttribute("successMessage", "Interaction logged successfully");
            return "redirect:/customers/" + id;
            
        } catch (SecurityException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Validation error: " + e.getMessage());
            return "redirect:/customers/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error logging interaction: " + e.getMessage());
            return "redirect:/customers/" + id;
        }
    }

    /**
     * Customer search API endpoint.
     */
    @GetMapping("/api/search")
    @ResponseBody
    public ResponseEntity<List<Customer>> searchCustomers(@RequestParam String query) {
        try {
            String sanitizedQuery = inputValidationService.sanitizeSearchQuery(query);
            List<Customer> customers = customerService.searchCustomers(sanitizedQuery);
            return ResponseEntity.ok(customers);
        } catch (SecurityException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Customer details API endpoint.
     */
    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Customer> getCustomer(@PathVariable Long id) {
        Optional<Customer> customer = customerService.findById(id);
        return customer.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Customer interaction history API endpoint.
     */
    @GetMapping("/api/{id}/interactions")
    @ResponseBody
    public ResponseEntity<List<InteractionLog>> getCustomerInteractions(@PathVariable Long id) {
        try {
            List<InteractionLog> interactions = customerService.getCustomerInteractionHistory(id);
            return ResponseEntity.ok(interactions);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Customer statistics API endpoint.
     */
    @GetMapping("/api/{id}/stats")
    @ResponseBody
    public ResponseEntity<Object> getCustomerStats(@PathVariable Long id) {
        try {
            Optional<Customer> customerOpt = customerService.findById(id);
            if (customerOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Customer customer = customerOpt.get();
            
            // Get customer statistics
            long interactionCount = customerService.countCustomerInteractions(id);
            List<Lead> leads = salesService.findLeadsByCustomer(customer);
            List<SaleTransaction> transactions = salesService.findSalesByCustomer(customer);
            
            var stats = java.util.Map.of(
                "interactionCount", interactionCount,
                "leadCount", leads.size(),
                "transactionCount", transactions.size(),
                "totalRevenue", transactions.stream()
                    .map(SaleTransaction::getAmount)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)
            );
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Customers with recent activity.
     */
    @GetMapping("/recent-activity")
    public String customersWithRecentActivity(@RequestParam(value = "days", defaultValue = "7") int days,
                                             Model model) {
        User currentUser = getCurrentUser();
        model.addAttribute("currentUser", currentUser);
        
        List<Customer> customers = customerService.findCustomersWithRecentActivity(days);
        model.addAttribute("customers", customers);
        model.addAttribute("days", days);
        model.addAttribute("title", "Customers with Recent Activity");
        
        return "customers/recent-activity";
    }

    /**
     * Customers without recent interactions.
     */
    @GetMapping("/inactive")
    public String inactiveCustomers(@RequestParam(value = "days", defaultValue = "30") int days,
                                   Model model) {
        User currentUser = getCurrentUser();
        model.addAttribute("currentUser", currentUser);
        
        List<Customer> customers = customerService.findCustomersWithoutRecentInteractions(days);
        model.addAttribute("customers", customers);
        model.addAttribute("days", days);
        model.addAttribute("title", "Customers Without Recent Interactions");
        
        return "customers/inactive";
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
}