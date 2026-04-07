package com.pasp.ict.salescrm.security;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

/**
 * Event listener for Spring Security authentication events.
 * Automatically logs authentication attempts for security monitoring.
 */
@Component
public class SecurityEventListener {

    private final SecurityAuditService securityAuditService;

    public SecurityEventListener(SecurityAuditService securityAuditService) {
        this.securityAuditService = securityAuditService;
    }

    /**
     * Handles successful authentication events.
     * 
     * @param event The authentication success event
     */
    @EventListener
    public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Authentication authentication = event.getAuthentication();
        String username = authentication.getName();
        String ipAddress = "unknown";
        String userAgent = "unknown";

        // Extract IP address and user agent if available
        if (authentication.getDetails() instanceof WebAuthenticationDetails) {
            WebAuthenticationDetails details = (WebAuthenticationDetails) authentication.getDetails();
            ipAddress = details.getRemoteAddress();
        }

        securityAuditService.logAuthenticationAttempt(username, true, ipAddress, userAgent);
    }

    /**
     * Handles failed authentication events.
     * 
     * @param event The authentication failure event
     */
    @EventListener
    public void handleAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        Authentication authentication = event.getAuthentication();
        String username = authentication.getName();
        String ipAddress = "unknown";
        String userAgent = "unknown";

        // Extract IP address and user agent if available
        if (authentication.getDetails() instanceof WebAuthenticationDetails) {
            WebAuthenticationDetails details = (WebAuthenticationDetails) authentication.getDetails();
            ipAddress = details.getRemoteAddress();
        }

        securityAuditService.logAuthenticationAttempt(username, false, ipAddress, userAgent);
    }
}