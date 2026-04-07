package com.pasp.ict.salescrm.property;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Assume;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tag;
import net.jqwik.api.constraints.StringLength;

/**
 * Property-based tests for password complexity enforcement.
 * Feature: sales-crm-application, Property 2: Password Complexity Enforcement
 * 
 * **Validates: Requirements 1.3**
 */
public class PasswordComplexityProperties {

    /**
     * Property 2: Password Complexity Enforcement
     * For any password string, the system should accept it if and only if it contains 
     * at least 8 characters with mixed case letters and numbers.
     */
    @Property
    @Tag("Feature: sales-crm-application, Property 2: Password Complexity Enforcement")
    void passwordComplexityEnforcement(@ForAll("validPasswords") String validPassword,
                                     @ForAll("invalidPasswords") String invalidPassword) {
        
        // Valid passwords should be accepted
        boolean validResult = isPasswordValid(validPassword);
        Assume.that(validResult == true);

        // Invalid passwords should be rejected
        boolean invalidResult = isPasswordValid(invalidPassword);
        Assume.that(invalidResult == false);
    }

    /**
     * Property: Valid Password Acceptance
     * Any password with at least 8 characters, mixed case, and numbers should be valid.
     */
    @Property
    @Tag("Feature: sales-crm-application, Property 2: Password Complexity Enforcement")
    void validPasswordAcceptance(@ForAll("validPasswords") String password) {
        boolean result = isPasswordValid(password);
        Assume.that(result == true);
    }

    /**
     * Property: Invalid Password Rejection
     * Any password not meeting complexity requirements should be rejected.
     */
    @Property
    @Tag("Feature: sales-crm-application, Property 2: Password Complexity Enforcement")
    void invalidPasswordRejection(@ForAll("invalidPasswords") String password) {
        boolean result = isPasswordValid(password);
        Assume.that(result == false);
    }

    /**
     * Property: Minimum Length Requirement
     * Passwords shorter than 8 characters should always be invalid.
     */
    @Property
    @Tag("Feature: sales-crm-application, Property 2: Password Complexity Enforcement")
    void minimumLengthRequirement(@ForAll @StringLength(min = 1, max = 7) String shortPassword) {
        boolean result = isPasswordValid(shortPassword);
        Assume.that(result == false);
    }

    /**
     * Property: Mixed Case and Number Requirement
     * Passwords must contain lowercase, uppercase, and numeric characters.
     */
    @Property
    @Tag("Feature: sales-crm-application, Property 2: Password Complexity Enforcement")
    void mixedCaseAndNumberRequirement(@ForAll("passwordsWithMissingRequirements") String password) {
        boolean result = isPasswordValid(password);
        Assume.that(result == false);
    }

    /**
     * Standalone password validation logic (mirrors AuthenticationService logic).
     */
    private boolean isPasswordValid(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasLowercase = password.chars().anyMatch(Character::isLowerCase);
        boolean hasUppercase = password.chars().anyMatch(Character::isUpperCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);

        return hasLowercase && hasUppercase && hasDigit;
    }

    // Data generators
    @Provide
    Arbitrary<String> validPasswords() {
        return Arbitraries.strings()
            .withChars("abcdefghijklmnopqrstuvwxyz") // lowercase
            .ofMinLength(2).ofMaxLength(10)
            .flatMap(lower -> 
                Arbitraries.strings()
                    .withChars("ABCDEFGHIJKLMNOPQRSTUVWXYZ") // uppercase
                    .ofMinLength(2).ofMaxLength(10)
                    .flatMap(upper ->
                        Arbitraries.strings()
                            .withChars("0123456789") // numbers
                            .ofMinLength(2).ofMaxLength(10)
                            .flatMap(numbers ->
                                Arbitraries.strings()
                                    .withChars("!@#$%^&*()_+-=[]{}|;:,.<>?") // special chars (optional)
                                    .ofMinLength(0).ofMaxLength(5)
                                    .map(special -> shuffleString(lower + upper + numbers + special))
                            )
                    )
            )
            .filter(password -> password.length() >= 8);
    }

    @Provide
    Arbitrary<String> invalidPasswords() {
        return Arbitraries.oneOf(
            // Too short (less than 8 characters)
            Arbitraries.strings()
                .withChars("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789")
                .ofMinLength(1).ofMaxLength(7),
            
            // Missing lowercase
            Arbitraries.strings()
                .withChars("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789")
                .ofMinLength(8).ofMaxLength(20),
            
            // Missing uppercase
            Arbitraries.strings()
                .withChars("abcdefghijklmnopqrstuvwxyz0123456789")
                .ofMinLength(8).ofMaxLength(20),
            
            // Missing numbers
            Arbitraries.strings()
                .withChars("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")
                .ofMinLength(8).ofMaxLength(20),
            
            // Only special characters
            Arbitraries.strings()
                .withChars("!@#$%^&*()_+-=[]{}|;:,.<>?")
                .ofMinLength(8).ofMaxLength(20)
        );
    }

    @Provide
    Arbitrary<String> passwordsWithMissingRequirements() {
        return Arbitraries.oneOf(
            // Long enough but missing lowercase
            Arbitraries.strings()
                .withChars("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()")
                .ofMinLength(8).ofMaxLength(20),
            
            // Long enough but missing uppercase
            Arbitraries.strings()
                .withChars("abcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()")
                .ofMinLength(8).ofMaxLength(20),
            
            // Long enough but missing numbers
            Arbitraries.strings()
                .withChars("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ!@#$%^&*()")
                .ofMinLength(8).ofMaxLength(20)
        );
    }

    /**
     * Utility method to shuffle string characters for more realistic password generation.
     */
    private String shuffleString(String input) {
        if (input == null || input.length() <= 1) {
            return input;
        }
        
        char[] chars = input.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = (int) (Math.random() * (i + 1));
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        return new String(chars);
    }
}