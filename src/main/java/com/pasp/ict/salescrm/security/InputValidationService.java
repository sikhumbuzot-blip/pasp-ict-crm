package com.pasp.ict.salescrm.security;

import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

/**
 * Service for input validation and sanitization to prevent security vulnerabilities.
 * Provides protection against SQL injection, XSS, and other input-based attacks.
 */
@Service
public class InputValidationService {

    // Patterns for detecting potentially malicious input
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i)(union|select|insert|update|delete|drop|create|alter|exec|execute|script|javascript|vbscript|onload|onerror|onclick)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(?i)(<script|</script|javascript:|vbscript:|onload=|onerror=|onclick=|onmouseover=|<iframe|</iframe)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^[+]?[0-9\\s\\-\\(\\)]{10,20}$"
    );

    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9\\s\\-_.,!?()]+$"
    );

    /**
     * Validates and sanitizes user input to prevent SQL injection and XSS attacks.
     * 
     * @param input The input string to validate
     * @param fieldName The name of the field for error reporting
     * @return Sanitized input string
     * @throws SecurityException if malicious content is detected
     */
    public String validateAndSanitizeInput(String input, String fieldName) {
        if (input == null) {
            return null;
        }

        String trimmedInput = input.trim();
        
        if (trimmedInput.isEmpty()) {
            return trimmedInput;
        }

        // Check for SQL injection patterns
        if (SQL_INJECTION_PATTERN.matcher(trimmedInput).find()) {
            throw new SecurityException("Potentially malicious SQL content detected in " + fieldName);
        }

        // Check for XSS patterns
        if (XSS_PATTERN.matcher(trimmedInput).find()) {
            throw new SecurityException("Potentially malicious script content detected in " + fieldName);
        }

        // HTML encode to prevent XSS
        return HtmlUtils.htmlEscape(trimmedInput);
    }

    /**
     * Validates email format and sanitizes the input.
     * 
     * @param email The email to validate
     * @return Sanitized email
     * @throws IllegalArgumentException if email format is invalid
     */
    public String validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }

        String sanitizedEmail = validateAndSanitizeInput(email, "email");
        
        if (!EMAIL_PATTERN.matcher(sanitizedEmail).matches()) {
            throw new IllegalArgumentException("Invalid email format");
        }

        return sanitizedEmail.toLowerCase();
    }

    /**
     * Validates phone number format and sanitizes the input.
     * 
     * @param phone The phone number to validate
     * @return Sanitized phone number
     * @throws IllegalArgumentException if phone format is invalid
     */
    public String validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return null; // Phone is optional
        }

        String sanitizedPhone = validateAndSanitizeInput(phone, "phone");
        
        if (!PHONE_PATTERN.matcher(sanitizedPhone).matches()) {
            throw new IllegalArgumentException("Invalid phone number format");
        }

        return sanitizedPhone;
    }

    /**
     * Validates text input for names, companies, etc.
     * 
     * @param text The text to validate
     * @param fieldName The field name for error reporting
     * @param maxLength Maximum allowed length
     * @return Sanitized text
     * @throws IllegalArgumentException if validation fails
     */
    public String validateText(String text, String fieldName, int maxLength) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty");
        }

        String sanitizedText = validateAndSanitizeInput(text, fieldName);
        
        if (sanitizedText.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must not exceed " + maxLength + " characters");
        }

        return sanitizedText;
    }

    /**
     * Validates alphanumeric input with basic punctuation.
     * 
     * @param input The input to validate
     * @param fieldName The field name for error reporting
     * @return Sanitized input
     * @throws IllegalArgumentException if validation fails
     */
    public String validateAlphanumeric(String input, String fieldName) {
        if (input == null) {
            return null;
        }

        String sanitizedInput = validateAndSanitizeInput(input, fieldName);
        
        if (!ALPHANUMERIC_PATTERN.matcher(sanitizedInput).matches()) {
            throw new IllegalArgumentException(fieldName + " contains invalid characters");
        }

        return sanitizedInput;
    }

    /**
     * Validates and sanitizes notes or description fields.
     * 
     * @param notes The notes to validate
     * @param maxLength Maximum allowed length
     * @return Sanitized notes
     */
    public String validateNotes(String notes, int maxLength) {
        if (notes == null || notes.trim().isEmpty()) {
            return null;
        }

        String sanitizedNotes = validateAndSanitizeInput(notes, "notes");
        
        if (sanitizedNotes.length() > maxLength) {
            throw new IllegalArgumentException("Notes must not exceed " + maxLength + " characters");
        }

        return sanitizedNotes;
    }

    /**
     * Checks if a string contains potentially dangerous content.
     * 
     * @param input The input to check
     * @return true if dangerous content is detected
     */
    public boolean containsMaliciousContent(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }

        return SQL_INJECTION_PATTERN.matcher(input).find() || 
               XSS_PATTERN.matcher(input).find();
    }

    /**
     * Sanitizes search query input to prevent injection attacks.
     * 
     * @param query The search query to sanitize
     * @return Sanitized query
     */
    public String sanitizeSearchQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return "";
        }

        // Remove potentially dangerous characters from search queries
        String sanitized = query.replaceAll("[<>\"'%;()&+]", "");
        
        // Limit length to prevent DoS attacks
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100);
        }

        return sanitized.trim();
    }
}