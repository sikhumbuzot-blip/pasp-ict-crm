package com.pasp.ict.salescrm.unit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.pasp.ict.salescrm.controller.AuthController;
import com.pasp.ict.salescrm.controller.DashboardController;
import com.pasp.ict.salescrm.controller.SalesController;
import com.pasp.ict.salescrm.entity.Customer;
import com.pasp.ict.salescrm.entity.Lead;
import com.pasp.ict.salescrm.entity.LeadStatus;
import com.pasp.ict.salescrm.entity.User;
import com.pasp.ict.salescrm.entity.UserRole;
import com.pasp.ict.salescrm.service.AdminService;
import com.pasp.ict.salescrm.service.AuthenticationService;
import com.pasp.ict.salescrm.service.CustomerService;
import com.pasp.ict.salescrm.service.SalesService;
import com.pasp.ict.salescrm.service.UserService;

/**
 * Unit tests for controller endpoints focusing on authentication flows,
 * role-based access, form validation, and template rendering.
 * 
 * **Validates: Requirements 6.4, 2.5**
 */
@WebMvcTest({AuthController.class, DashboardController.class, SalesController.class})
public class ControllerEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationService authenticationService;

    @MockBean
    private UserService userService;

    @MockBean
    private AdminService adminService;

    @MockBean
    private SalesService salesService;

    @MockBean
    private CustomerService customerService;

    private User testUser;
    private Customer testCustomer;
    private Lead testLead;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEmail("test@example.com");
        testUser.setRole(UserRole.SALES);
        testUser.setActive(true);
        testUser.setCreatedAt(LocalDateTime.now());

        testCustomer = new Customer();
        testCustomer.setId(1L);
        testCustomer.setName("Test Customer");
        testCustomer.setEmail("customer@example.com");
        testCustomer.setCompany("Test Company");

        testLead = new Lead();
        testLead.setId(1L);
        testLead.setTitle("Test Lead");
        testLead.setDescription("Test lead description");
        testLead.setStatus(LeadStatus.NEW);
        testLead.setEstimatedValue(new BigDecimal("1000.00"));
        testLead.setCustomer(testCustomer);
        testLead.setAssignedTo(testUser);
        testLead.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void testLoginPageRendering() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    void testLoginWithErrorMessage() throws Exception {
        mockMvc.perform(get("/login").param("error", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attribute("errorMessage", "Invalid username or password. Please try again."));
    }

    @Test
    void testLoginWithLogoutMessage() throws Exception {
        mockMvc.perform(get("/login").param("logout", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attribute("successMessage", "You have been successfully logged out."));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "SALES")
    void testDashboardRedirectForSalesUser() throws Exception {
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/sales"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testDashboardRedirectForAdminUser() throws Exception {
        User adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setRole(UserRole.ADMIN);
        
        when(userService.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/admin"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "SALES")
    void testSalesLeadsPageAccess() throws Exception {
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(salesService.findOpenLeads()).thenReturn(Arrays.asList(testLead));

        mockMvc.perform(get("/sales/leads"))
                .andExpect(status().isOk())
                .andExpect(view().name("sales/leads"))
                .andExpect(model().attributeExists("leads"))
                .andExpect(model().attributeExists("leadStatuses"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "REGULAR")
    void testSalesLeadsAccessDeniedForRegularUser() throws Exception {
        mockMvc.perform(get("/sales/leads"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "SALES")
    void testLeadDetailsPageRendering() throws Exception {
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(salesService.findLeadById(1L)).thenReturn(Optional.of(testLead));
        when(salesService.findSalesByCustomer(any())).thenReturn(Arrays.asList());

        mockMvc.perform(get("/sales/leads/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("sales/lead-details"))
                .andExpect(model().attribute("lead", testLead))
                .andExpect(model().attributeExists("validTransitions"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "SALES")
    void testCreateLeadFormRendering() throws Exception {
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(customerService.searchCustomers("")).thenReturn(Arrays.asList(testCustomer));
        when(userService.findActiveSalesUsers()).thenReturn(Arrays.asList(testUser));

        mockMvc.perform(get("/sales/leads/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("sales/create-lead"))
                .andExpect(model().attributeExists("customers"))
                .andExpect(model().attributeExists("salesUsers"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "SALES")
    void testCreateLeadFormSubmission() throws Exception {
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(customerService.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(salesService.createLead(anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn(testLead);

        mockMvc.perform(post("/sales/leads/create")
                .with(csrf())
                .param("title", "New Lead")
                .param("description", "Lead description")
                .param("estimatedValue", "1000.00")
                .param("customerId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/sales/leads/1"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "SALES")
    void testCreateLeadFormValidationError() throws Exception {
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        mockMvc.perform(post("/sales/leads/create")
                .with(csrf())
                .param("title", "") // Empty title should cause validation error
                .param("customerId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/sales/leads/create"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "SALES")
    void testUpdateLeadStatus() throws Exception {
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(salesService.updateLeadStatus(1L, LeadStatus.CONTACTED, testUser))
                .thenReturn(testLead);

        mockMvc.perform(post("/sales/leads/1/status")
                .with(csrf())
                .param("status", "CONTACTED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/sales/leads/1"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "SALES")
    void testConvertLeadFormRendering() throws Exception {
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(salesService.findLeadById(1L)).thenReturn(Optional.of(testLead));

        mockMvc.perform(get("/sales/leads/1/convert"))
                .andExpect(status().isOk())
                .andExpect(view().name("sales/convert-lead"))
                .andExpect(model().attribute("lead", testLead));
    }

    @Test
    void testUnauthenticatedAccessRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/sales/leads"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "SALES")
    void testFormValidationAndErrorHandling() throws Exception {
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(customerService.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/sales/leads/create")
                .with(csrf())
                .param("title", "Valid Title")
                .param("customerId", "999")) // Non-existent customer
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/sales/leads/create"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "SALES")
    void testTemplateDataBinding() throws Exception {
        List<Lead> mockLeads = Arrays.asList(testLead);
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(salesService.findOpenLeads()).thenReturn(mockLeads);

        mockMvc.perform(get("/sales/leads"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("leads", mockLeads))
                .andExpect(model().attribute("currentUser", testUser));
    }
}