package com.pasp.ict.salescrm.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.pasp.ict.salescrm.entity.User;
import com.pasp.ict.salescrm.entity.UserRole;
import com.pasp.ict.salescrm.service.AuthenticationService;
import com.pasp.ict.salescrm.service.UserService;

/**
 * Controller for handling authentication-related requests.
 * Manages login and logout pages with appropriate error/success messages.
 */
@Controller
public class AuthController {

    private final AuthenticationService authenticationService;
    private final UserService userService;

    public AuthController(AuthenticationService authenticationService, UserService userService) {
        this.authenticationService = authenticationService;
        this.userService = userService;
    }

    /**
     * Display login page with optional error/success messages.
     */
    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                       @RequestParam(value = "logout", required = false) String logout,
                       @RequestParam(value = "expired", required = false) String expired,
                       Model model) {
        
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid username or password. Please try again.");
        }
        
        if (logout != null) {
            model.addAttribute("successMessage", "You have been successfully logged out.");
        }
        
        if (expired != null) {
            model.addAttribute("warningMessage", "Your session has expired. Please log in again.");
        }
        
        return "auth/login";
    }

    /**
     * Redirect to role-specific dashboard after successful login.
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES', 'REGULAR')")
    public String dashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        User currentUser = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        
        model.addAttribute("currentUser", currentUser);
        
        // Redirect to role-specific dashboard
        switch (currentUser.getRole()) {
            case ADMIN:
                return "redirect:/dashboard/admin";
            case SALES:
                return "redirect:/dashboard/sales";
            case REGULAR:
                return "redirect:/dashboard/regular";
            default:
                return "redirect:/login?error";
        }
    }

    /**
     * Handle logout success.
     */
    @GetMapping("/logout-success")
    public String logoutSuccess() {
        return "redirect:/login?logout";
    }
}