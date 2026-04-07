package com.pasp.ict.salescrm.unit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Unit tests for role-based access control functionality.
 * Tests Requirements 2.2, 2.3, 2.4, 2.5 - Role-Based Access Control
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
public class RoleBasedAccessControlTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    private MockMvc getMockMvc() {
        if (mockMvc == null) {
            mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        }
        return mockMvc;
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testAdminCanAccessAdminEndpoints() throws Exception {
        getMockMvc().perform(get("/admin/users"))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                // Should not be forbidden (403) or unauthorized (401)
                // May be 404 (not found) since we haven't implemented controllers yet
                assert status != 403 && status != 401;
            });
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testAdminCanAccessSalesEndpoints() throws Exception {
        getMockMvc().perform(get("/sales/leads"))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                // Should not be forbidden (403) or unauthorized (401)
                assert status != 403 && status != 401;
            });
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testAdminCanAccessCustomerEndpoints() throws Exception {
        getMockMvc().perform(get("/customers"))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                // Should not be forbidden (403) or unauthorized (401)
                assert status != 403 && status != 401;
            });
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testAdminCanAccessDashboard() throws Exception {
        getMockMvc().perform(get("/dashboard"))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                // Should not be forbidden (403) or unauthorized (401)
                assert status != 403 && status != 401;
            });
    }

    @Test
    @WithMockUser(roles = "SALES")
    void testSalesCanAccessSalesEndpoints() throws Exception {
        getMockMvc().perform(get("/sales/leads"))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                // Should not be forbidden (403) or unauthorized (401)
                assert status != 403 && status != 401;
            });
    }

    @Test
    @WithMockUser(roles = "SALES")
    void testSalesCanAccessCustomerEndpoints() throws Exception {
        getMockMvc().perform(get("/customers"))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                // Should not be forbidden (403) or unauthorized (401)
                assert status != 403 && status != 401;
            });
    }

    @Test
    @WithMockUser(roles = "SALES")
    void testSalesCanAccessDashboard() throws Exception {
        getMockMvc().perform(get("/dashboard"))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                // Should not be forbidden (403) or unauthorized (401)
                assert status != 403 && status != 401;
            });
    }

    @Test
    @WithMockUser(roles = "SALES")
    void testSalesCannotAccessAdminEndpoints() throws Exception {
        getMockMvc().perform(get("/admin/users"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "REGULAR")
    void testRegularCanAccessDashboard() throws Exception {
        getMockMvc().perform(get("/dashboard"))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                // Should not be forbidden (403) or unauthorized (401)
                assert status != 403 && status != 401;
            });
    }

    @Test
    @WithMockUser(roles = "REGULAR")
    void testRegularCannotAccessAdminEndpoints() throws Exception {
        getMockMvc().perform(get("/admin/users"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "REGULAR")
    void testRegularCannotAccessSalesEndpoints() throws Exception {
        getMockMvc().perform(get("/sales/leads"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "REGULAR")
    void testRegularCannotAccessCustomerEndpoints() throws Exception {
        getMockMvc().perform(get("/customers"))
            .andExpect(status().isForbidden());
    }

    @Test
    void testUnauthenticatedUserCannotAccessProtectedEndpoints() throws Exception {
        getMockMvc().perform(get("/dashboard"))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                // Should be unauthorized (401) or redirect to login (302)
                assert status == 401 || status == 302;
            });
    }

    @Test
    void testUnauthenticatedUserCanAccessLogin() throws Exception {
        getMockMvc().perform(get("/login"))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                // Should be OK (200) or redirect (302), but not forbidden/unauthorized
                assert status != 403 && status != 401;
            });
    }
}