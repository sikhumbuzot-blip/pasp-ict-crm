package com.pasp.ict.salescrm.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.pasp.ict.salescrm.entity.User;
import com.pasp.ict.salescrm.security.CustomUserDetailsService.CustomUserPrincipal;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Global exception handler for security-related exceptions.
 * Logs security violations and provides appropriate user feedback.
 */
@ControllerAdvice
public class SecurityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(SecurityExceptionHandler.class);
    
    private final SecurityAuditService securityAuditService;

    public SecurityExceptionHandler(SecurityAuditService securityAuditService) {
        this.securityAuditService = securityAuditService;
    }

    /**
     * Handles access denied exceptions (unauthorized access attempts).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(AccessDeniedException ex, HttpServletRequest request, 
                                   RedirectAttributes redirectAttributes) {
        
        User currentUser = getCurrentUser();
        String resource = request.getRequestURI();
        
        // Log unauthorized access attempt
        securityAuditService.logUnauthorizedAccess(currentUser, resource, request);
        
        logger.warn("Access denied for user: {} attempting to access: {}", 
                   currentUser != null ? currentUser.getUsername() : "anonymous", resource);
        
        redirectAttributes.addFlashAttribute("errorMessage", 
            "Access denied. You don't have permission to access this resource.");
        
        // Redirect to appropriate dashboard based on user role
        if (currentUser != null) {
            return "redirect:/dashboard";
        } else {
            return "redirect:/login";
        }
    }

    /**
     * Handles security exceptions (input validation failures, etc.).
     */
    @ExceptionHandler(SecurityException.class)
    public String handleSecurityException(SecurityException ex, HttpServletRequest request, 
                                        RedirectAttributes redirectAttributes) {
        
        User currentUser = getCurrentUser();
        
        // Log security violation
        securityAuditService.logSecurityViolation("INPUT_VALIDATION", ex.getMessage(), currentUser, request);
        
        logger.error("Security exception for user: {} - {}", 
                    currentUser != null ? currentUser.getUsername() : "anonymous", ex.getMessage());
        
        redirectAttributes.addFlashAttribute("errorMessage", 
            "Security violation detected. Please check your input and try again.");
        
        // Redirect to previous page or dashboard
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isEmpty()) {
            return "redirect:" + referer;
        } else {
            return currentUser != null ? "redirect:/dashboard" : "redirect:/login";
        }
    }

    /**
     * Handles illegal argument exceptions (validation failures).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleValidationException(IllegalArgumentException ex, HttpServletRequest request, 
                                          RedirectAttributes redirectAttributes) {
        
        User currentUser = getCurrentUser();
        
        // Log validation failure
        logger.warn("Validation exception for user: {} - {}", 
                   currentUser != null ? currentUser.getUsername() : "anonymous", ex.getMessage());
        
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        
        // Redirect to previous page or dashboard
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isEmpty()) {
            return "redirect:" + referer;
        } else {
            return currentUser != null ? "redirect:/dashboard" : "redirect:/login";
        }
    }

    /**
     * Gets the currently authenticated user.
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() 
            && authentication.getPrincipal() instanceof CustomUserPrincipal) {
            
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            return userPrincipal.getUser();
        }
        
        return null;
    }
}