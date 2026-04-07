package com.pasp.ict.salescrm.unit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for base template functionality and layout rendering
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class BaseTemplateTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testLoginPageRendersCorrectly() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sales CRM")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sign in to your account")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Bootstrap")));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testAdminDashboardRendersWithBaseTemplate() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sales CRM")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Admin Dashboard")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("navbar")));
    }

    @Test
    @WithMockUser(username = "sales", roles = {"SALES"})
    public void testSalesDashboardRendersWithBaseTemplate() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sales CRM")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sales Dashboard")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("navbar")));
    }

    @Test
    @WithMockUser(username = "user", roles = {"REGULAR"})
    public void testRegularDashboardRendersWithBaseTemplate() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sales CRM")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Dashboard")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Regular User")));
    }

    @Test
    public void testStaticResourcesAreAccessible() throws Exception {
        // Test that custom CSS is accessible
        mockMvc.perform(get("/css/custom.css"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/css"));

        // Test that common JS is accessible
        mockMvc.perform(get("/js/common.js"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/javascript"));
    }

    @Test
    public void testResponsiveDesignElements() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("viewport")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Bootstrap")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("responsive")));
    }
}