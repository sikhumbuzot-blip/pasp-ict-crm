package com.pasp.ict.salescrm.integration;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.pasp.ict.salescrm.entity.Customer;
import com.pasp.ict.salescrm.entity.Lead;
import com.pasp.ict.salescrm.entity.LeadStatus;
import com.pasp.ict.salescrm.entity.User;
import com.pasp.ict.salescrm.entity.UserRole;
import com.pasp.ict.salescrm.repository.CustomerRepository;
import com.pasp.ict.salescrm.repository.LeadRepository;
import com.pasp.ict.salescrm.repository.UserRepository;
import com.pasp.ict.salescrm.service.SalesService;

/**
 * Web layer integration tests for the Sales CRM Application.
 * Tests controller endpoints, form submissions, and API responses
 * with proper security context and database integration.
 * 
 * **Validates: Requirements 10.2**
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
class WebLayerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private SalesService salesService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User adminUser;
    private User salesUser;
    private Customer testCustomer;
    private Lead testLead;

    @BeforeEach
    void setUp() {
        // Clean up data
        leadRepository.deleteAll();
        customerRepository.deleteAll();
        userRepository.deleteAll();

        // Create test users
        adminUser = createTestUser("admin", "admin@test.com", "Admin", "User", UserRole.ADMIN);
        salesUser = createTestUser("sales", "sales@test.com", "Sales", "User", UserRole.SALES);

        // Create test data
        testCustomer = createTestCustomer("Web Test Customer", "webtest@test.com", 
                                        "555-WEB1", "Web Test Corp", "123 Web St");
        testLead = createTestLead("Web Test Lead", "Web test description", 
                                BigDecimal.valueOf(1000), testCustomer, salesUser);
    }

    /**
     * Test 1: Authentication and Login Flow
     * Tests the complete authentication workflow including login forms and redirects.
     */
    @Test
    @Order(1)
    void testAuthenticationAndLoginFlow() throws Exception {
        // Test login page access
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attributeDoesNotExist("error"));

        // Test login with invalid credentials
        mockMvc.perform(post("/login")
                .param("username", "invalid")
                .param("password", "invalid")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));

        // Test protected resource access without authentication
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        // Test logout functionality
        mockMvc.perform(post("/logout")
                .with(csrf())
                .with(user("sales").roles("SALES")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout=true"));
    }

    /**
     * Test 2: Dashboard Access and Role-Based Content
     * Tests that dashboard displays appropriate content based on user roles.
     */
    @Test
    @Order(2)
    void testDashboardAccessAndRoleBasedContent() throws Exception {
        // Test admin dashboard access
        mockMvc.perform(get("/dashboard")
                .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/admin"))
                .andExpect(model().attributeExists("currentUser"))
                .andExpect(model().attributeExists("systemStats"));

        // Test sales dashboard access
        mockMvc.perform(get("/dashboard")
                .with(user("sales").roles("SALES")))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/sales"))
                .andExpect(model().attributeExists("currentUser"))
                .andExpect(model().attributeExists("salesMetrics"));

        // Test regular user dashboard access
        mockMvc.perform(get("/dashboard")
                .with(user("regular").roles("REGULAR")))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/regular"))
                .andExpect(model().attributeExists("currentUser"));
    }

    /**
     * Test 3: Sales Lead Management Workflow
     * Tests complete lead management through web interface.
     */
    @Test
    @Order(3)
    @WithMockUser(username = "sales", roles = {"SALES"})
    void testSalesLeadManagementWorkflow() throws Exception {
        // Test leads listing page
        mockMvc.perform(get("/sales/leads"))
                .andExpect(status().isOk())
                .andExpect(view().name("sales/leads"))
                .andExpect(model().attributeExists("leads"))
                .andExpect(model().attributeExists("leadStatuses"))
                .andExpect(model().attributeExists("currentUser"));

        // Test lead details page
        mockMvc.perform(get("/sales/leads/" + testLead.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("sales/lead-details"))
                .andExpect(model().attributeExists("lead"))
                .andExpect(model().attribute("lead", hasProperty("title", is("Web Test Lead"))))
                .andExpect(model().attributeExists("validTransitions"));

        // Test create lead form
        mockMvc.perform(get("/sales/leads/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("sales/create-lead"))
                .andExpect(model().attributeExists("customers"))
                .andExpect(model().attributeExists("salesUsers"));

        // Test lead creation via form submission
        mockMvc.perform(post("/sales/leads/create")
                .param("title", "New Web Lead")
                .param("description", "Created via web form")
                .param("estimatedValue", "1500")
                .param("customerId", testCustomer.getId().toString())
                .param("assignedToId", salesUser.getId().toString())
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/sales/leads/*"))
                .andExpect(flash().attributeExists("successMessage"));

        // Test lead status update
        mockMvc.perform(post("/sales/leads/" + testLead.getId() + "/status")
                .param("status", "CONTACTED")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/sales/leads/" + testLead.getId()))
                .andExpect(flash().attributeExists("successMessage"));
    }

    /**
     * Test 4: Lead Conversion to Sale Workflow
     * Tests the complete lead conversion process through web interface.
     */
    @Test
    @Order(4)
    @WithMockUser(username = "sales", roles = {"SALES"})
    void testLeadConversionToSaleWorkflow() throws Exception {
        // Update lead to a convertible status
        salesService.updateLeadStatus(testLead.getId(), LeadStatus.NEGOTIATION, salesUser);

        // Test convert lead form
        mockMvc.perform(get("/sales/leads/" + testLead.getId() + "/convert"))
                .andExpect(status().isOk())
                .andExpect(view().name("sales/convert-lead"))
                .andExpect(model().attributeExists("lead"))
                .andExpect(model().attribute("lead", hasProperty("status", is(LeadStatus.NEGOTIATION))));

        // Test lead conversion via form submission
        mockMvc.perform(post("/sales/leads/" + testLead.getId() + "/convert")
                .param("amount", "1200")
                .param("description", "Converted via web form")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/sales/transactions/*"))
                .andExpect(flash().attributeExists("successMessage"));

        // Verify lead status was updated to CLOSED_WON
        Lead convertedLead = leadRepository.findById(testLead.getId()).orElseThrow();
        assertEquals(LeadStatus.CLOSED_WON, convertedLead.getStatus());
    }

    /**
     * Test 5: Sales Transaction Management
     * Tests sales transaction listing and creation through web interface.
     */
    @Test
    @Order(5)
    @WithMockUser(username = "sales", roles = {"SALES"})
    void testSalesTransactionManagement() throws Exception {
        // Test transactions listing page
        mockMvc.perform(get("/sales/transactions"))
                .andExpect(status().isOk())
                .andExpect(view().name("sales/transactions"))
                .andExpect(model().attributeExists("transactions"))
                .andExpect(model().attributeExists("currentUser"));

        // Test create direct sale form
        mockMvc.perform(get("/sales/transactions/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("sales/create-sale"))
                .andExpect(model().attributeExists("customers"))
                .andExpect(model().attributeExists("saleTransaction"));

        // Test direct sale creation
        mockMvc.perform(post("/sales/transactions/create")
                .param("amount", "2000")
                .param("customerId", testCustomer.getId().toString())
                .param("description", "Direct sale via web")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/sales/transactions/*"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    /**
     * Test 6: Sales Pipeline and Metrics API
     * Tests API endpoints for sales pipeline and metrics data.
     */
    @Test
    @Order(6)
    @WithMockUser(username = "sales", roles = {"SALES"})
    void testSalesPipelineAndMetricsAPI() throws Exception {
        // Test sales pipeline page
        mockMvc.perform(get("/sales/pipeline"))
                .andExpect(status().isOk())
                .andExpect(view().name("sales/pipeline"))
                .andExpect(model().attributeExists("pipelineData"))
                .andExpect(model().attributeExists("leadStatuses"));

        // Test pipeline API endpoint
        mockMvc.perform(get("/sales/api/pipeline")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.NEW").exists())
                .andExpect(jsonPath("$.CONTACTED").exists())
                .andExpect(jsonPath("$.QUALIFIED").exists())
                .andExpect(jsonPath("$.PROPOSAL").exists())
                .andExpect(jsonPath("$.NEGOTIATION").exists())
                .andExpect(jsonPath("$.CLOSED_WON").exists())
                .andExpect(jsonPath("$.CLOSED_LOST").exists());

        // Test metrics page
        mockMvc.perform(get("/sales/metrics"))
                .andExpect(status().isOk())
                .andExpect(view().name("sales/metrics"))
                .andExpect(model().attributeExists("metrics"));

        // Test metrics API endpoint
        mockMvc.perform(get("/sales/api/metrics")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalRevenue").exists())
                .andExpect(jsonPath("$.totalSales").exists())
                .andExpect(jsonPath("$.conversionRate").exists());
    }

    /**
     * Test 7: Customer Management Interface
     * Tests customer management through web interface.
     */
    @Test
    @Order(7)
    @WithMockUser(username = "sales", roles = {"SALES"})
    void testCustomerManagementInterface() throws Exception {
        // Test customers listing page
        mockMvc.perform(get("/customers"))
                .andExpect(status().isOk())
                .andExpect(view().name("customers/list"))
                .andExpect(model().attributeExists("customers"))
                .andExpect(model().attributeExists("currentUser"));

        // Test customer profile page
        mockMvc.perform(get("/customers/" + testCustomer.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("customers/profile"))
                .andExpect(model().attributeExists("customer"))
                .andExpect(model().attribute("customer", hasProperty("name", is("Web Test Customer"))))
                .andExpect(model().attributeExists("leads"))
                .andExpect(model().attributeExists("transactions"));

        // Test create customer form
        mockMvc.perform(get("/customers/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("customers/create"))
                .andExpect(model().attributeExists("customer"));

        // Test customer creation
        mockMvc.perform(post("/customers/create")
                .param("name", "New Web Customer")
                .param("email", "newweb@test.com")
                .param("phone", "555-NEW1")
                .param("company", "New Web Corp")
                .param("address", "456 New Web St")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/customers/*"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    /**
     * Test 8: Admin User Management Interface
     * Tests admin functionality through web interface.
     */
    @Test
    @Order(8)
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testAdminUserManagementInterface() throws Exception {
        // Test admin users page
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attributeExists("currentUser"));

        // Test create user form
        mockMvc.perform(get("/admin/users/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/create-user"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("userRoles"));

        // Test user creation
        mockMvc.perform(post("/admin/users/create")
                .param("username", "newwebuser")
                .param("password", "Password123")
                .param("email", "newwebuser@test.com")
                .param("firstName", "New")
                .param("lastName", "WebUser")
                .param("role", "SALES")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/users/*"))
                .andExpect(flash().attributeExists("successMessage"));

        // Test admin system page
        mockMvc.perform(get("/admin/system"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/system"))
                .andExpect(model().attributeExists("systemInfo"));

        // Test admin reports page
        mockMvc.perform(get("/admin/reports"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reports"))
                .andExpect(model().attributeExists("reportData"));
    }

    /**
     * Test 9: Error Handling and Validation
     * Tests error handling and form validation through web interface.
     */
    @Test
    @Order(9)
    @WithMockUser(username = "sales", roles = {"SALES"})
    void testErrorHandlingAndValidation() throws Exception {
        // Test lead creation with invalid data
        mockMvc.perform(post("/sales/leads/create")
                .param("title", "") // Empty title
                .param("customerId", testCustomer.getId().toString())
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/sales/leads/create"))
                .andExpect(flash().attributeExists("errorMessage"));

        // Test customer creation with duplicate email
        mockMvc.perform(post("/customers/create")
                .param("name", "Duplicate Customer")
                .param("email", testCustomer.getEmail()) // Duplicate email
                .param("phone", "555-DUP1")
                .param("company", "Duplicate Corp")
                .param("address", "789 Duplicate St")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customers/create"))
                .andExpect(flash().attributeExists("errorMessage"));

        // Test accessing non-existent lead
        mockMvc.perform(get("/sales/leads/99999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/sales/leads"));

        // Test invalid lead status transition
        mockMvc.perform(post("/sales/leads/" + testLead.getId() + "/status")
                .param("status", "CLOSED_WON") // Invalid transition from NEW
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/sales/leads/" + testLead.getId()))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    /**
     * Test 10: CSRF Protection and Security Headers
     * Tests that CSRF protection and security headers are properly implemented.
     */
    @Test
    @Order(10)
    @WithMockUser(username = "sales", roles = {"SALES"})
    void testCSRFProtectionAndSecurityHeaders() throws Exception {
        // Test that POST requests without CSRF token are rejected
        mockMvc.perform(post("/sales/leads/create")
                .param("title", "CSRF Test Lead")
                .param("customerId", testCustomer.getId().toString()))
                .andExpect(status().isForbidden());

        // Test that forms include CSRF tokens
        mockMvc.perform(get("/sales/leads/create"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("_csrf")));

        // Test security headers are present
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Content-Type-Options"))
                .andExpect(header().exists("X-Frame-Options"))
                .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"));
    }

    // Helper methods

    private User createTestUser(String username, String email, String firstName, String lastName, UserRole role) {
        User user = new User(username, passwordEncoder.encode("Password123"), email, firstName, lastName, role);
        return userRepository.save(user);
    }

    private Customer createTestCustomer(String name, String email, String phone, String company, String address) {
        Customer customer = new Customer(name, email, phone, company, address, adminUser);
        return customerRepository.save(customer);
    }

    private Lead createTestLead(String title, String description, BigDecimal estimatedValue, Customer customer, User assignedTo) {
        Lead lead = new Lead(title, description, estimatedValue, customer, assignedTo);
        return leadRepository.save(lead);
    }
}