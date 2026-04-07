package com.pasp.ict.salescrm.testutil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.Provide;
import net.jqwik.time.api.DateTimes;

import com.pasp.ict.salescrm.entity.Customer;
import com.pasp.ict.salescrm.entity.InteractionType;
import com.pasp.ict.salescrm.entity.Lead;
import com.pasp.ict.salescrm.entity.LeadStatus;
import com.pasp.ict.salescrm.entity.SaleTransaction;
import com.pasp.ict.salescrm.entity.User;
import com.pasp.ict.salescrm.entity.UserRole;

/**
 * Property-based test data generators for jqwik tests.
 * Provides arbitraries for generating valid and invalid test data.
 */
public class PropertyTestGenerators {
    
    // User-related generators
    
    @Provide
    public static Arbitrary<String> validUsernames() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(3)
                .ofMaxLength(20)
                .filter(s -> s.matches("^[a-z][a-z0-9]*$"));
    }
    
    @Provide
    public static Arbitrary<String> invalidUsernames() {
        return Arbitraries.oneOf(
                Arbitraries.strings().ofMaxLength(2), // Too short
                Arbitraries.strings().ofMinLength(21), // Too long
                Arbitraries.strings().withCharRange('A', 'Z').ofLength(5), // Uppercase only
                Arbitraries.strings().withCharRange('0', '9').ofLength(5), // Numbers only
                Arbitraries.of("", " ", "user name", "user@name", "123user") // Invalid formats
        );
    }
    
    @Provide
    public static Arbitrary<String> validPasswords() {
        return Arbitraries.strings()
                .ofMinLength(8)
                .ofMaxLength(50)
                .filter(s -> s.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$"));
    }
    
    @Provide
    public static Arbitrary<String> invalidPasswords() {
        return Arbitraries.oneOf(
                Arbitraries.strings().ofMaxLength(7), // Too short
                Arbitraries.strings().withCharRange('a', 'z').ofLength(10), // No uppercase or numbers
                Arbitraries.strings().withCharRange('A', 'Z').ofLength(10), // No lowercase or numbers
                Arbitraries.strings().withCharRange('0', '9').ofLength(10), // No letters
                Arbitraries.of("password", "123456", "PASSWORD", "12345678") // Common weak passwords
        );
    }
    
    @Provide
    public static Arbitrary<String> validEmails() {
        return Combinators.combine(
                Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(10),
                Arbitraries.strings().withCharRange('a', 'z').ofMinLength(2).ofMaxLength(10),
                Arbitraries.of("com", "org", "net", "edu", "gov")
        ).as((local, domain, tld) -> local + "@" + domain + "." + tld);
    }
    
    @Provide
    public static Arbitrary<String> invalidEmails() {
        return Arbitraries.oneOf(
                Arbitraries.of("notanemail", "missing@", "@domain.com", "user@", "user@@domain.com"),
                Arbitraries.strings().ofMaxLength(5).filter(s -> !s.contains("@")),
                Arbitraries.strings().ofMinLength(100) // Too long
        );
    }
    
    @Provide
    public static Arbitrary<UserRole> userRoles() {
        return Arbitraries.of(UserRole.class);
    }
    
    @Provide
    public static Arbitrary<User> validUsers() {
        return Combinators.combine(
                validUsernames(),
                validPasswords(),
                validEmails(),
                names(),
                names(),
                userRoles()
        ).as((username, password, email, firstName, lastName, role) -> 
                TestDataFactory.createUser(username, email, firstName, lastName, role));
    }
    
    // Customer-related generators
    
    @Provide
    public static Arbitrary<String> names() {
        return Arbitraries.oneOf(
                Arbitraries.of("John", "Jane", "Michael", "Sarah", "David", "Lisa", "Robert", "Emily"),
                Arbitraries.of("James", "Jessica", "William", "Ashley", "Richard", "Amanda", "Thomas", "Jennifer")
        );
    }
    
    @Provide
    public static Arbitrary<String> companyNames() {
        return Arbitraries.oneOf(
                Arbitraries.of("Tech Solutions Inc", "Global Enterprises", "Innovation Corp", "Digital Dynamics"),
                Arbitraries.of("Future Systems", "Smart Solutions", "Advanced Technologies", "Premier Services")
        );
    }
    
    @Provide
    public static Arbitrary<String> phoneNumbers() {
        return Arbitraries.integers().between(1000, 9999)
                .map(num -> "555-" + String.format("%04d", num));
    }
    
    @Provide
    public static Arbitrary<String> addresses() {
        return Combinators.combine(
                Arbitraries.integers().between(1, 9999),
                Arbitraries.of("Main St", "Oak Ave", "Pine Rd", "Elm Dr", "Maple Ln", "Cedar Blvd")
        ).as((number, street) -> number + " " + street);
    }
    
    @Provide
    public static Arbitrary<Customer> validCustomers() {
        return Combinators.combine(
                names(),
                validEmails(),
                phoneNumbers(),
                companyNames(),
                addresses(),
                validUsers()
        ).as((name, email, phone, company, address, createdBy) -> 
                TestDataFactory.createCustomer(name, email, phone, company, address, createdBy));
    }
    
    // Lead-related generators
    
    @Provide
    public static Arbitrary<String> leadTitles() {
        return Arbitraries.of(
                "Software License Renewal", "Cloud Migration Project", "Security Audit Services",
                "Training Program", "System Integration", "Mobile App Development", "Data Analytics Platform",
                "Website Redesign", "Infrastructure Upgrade", "Consulting Services"
        );
    }
    
    @Provide
    public static Arbitrary<String> leadDescriptions() {
        return leadTitles().map(title -> "Description for " + title.toLowerCase());
    }
    
    @Provide
    public static Arbitrary<BigDecimal> validAmounts() {
        return Arbitraries.doubles()
                .between(100.0, 1000000.0)
                .map(d -> BigDecimal.valueOf(d).setScale(2, java.math.RoundingMode.HALF_UP));
    }
    
    @Provide
    public static Arbitrary<BigDecimal> invalidAmounts() {
        return Arbitraries.oneOf(
                Arbitraries.doubles().between(-1000000.0, -0.01)
                        .map(d -> BigDecimal.valueOf(d).setScale(2, java.math.RoundingMode.HALF_UP)),
                Arbitraries.of(BigDecimal.ZERO, null)
        );
    }
    
    @Provide
    public static Arbitrary<LeadStatus> leadStatuses() {
        return Arbitraries.of(LeadStatus.class);
    }
    
    @Provide
    public static Arbitrary<Lead> validLeads() {
        return Combinators.combine(
                leadTitles(),
                leadDescriptions(),
                validAmounts(),
                validCustomers(),
                salesUsers(),
                validUsers()
        ).as((title, description, amount, customer, assignedTo, createdBy) -> 
                TestDataFactory.createLead(title, description, amount, customer, assignedTo, createdBy));
    }
    
    // Sale Transaction generators
    
    @Provide
    public static Arbitrary<LocalDateTime> pastDateTimes() {
        return DateTimes.dateTimes()
                .between(LocalDateTime.now().minusYears(2), LocalDateTime.now());
    }
    
    @Provide
    public static Arbitrary<LocalDateTime> futureDateTimes() {
        return DateTimes.dateTimes()
                .between(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusYears(1));
    }
    
    @Provide
    public static Arbitrary<String> saleDescriptions() {
        return Arbitraries.of(
                "Software license purchase", "Consulting services", "System upgrade",
                "Training program", "Support contract", "Custom development"
        );
    }
    
    @Provide
    public static Arbitrary<SaleTransaction> validSaleTransactions() {
        return Combinators.combine(
                validAmounts(),
                pastDateTimes(),
                saleDescriptions(),
                validCustomers(),
                salesUsers(),
                validLeads()
        ).as((amount, saleDate, description, customer, salesUser, lead) -> 
                TestDataFactory.createSaleTransaction(amount, saleDate, description, customer, salesUser, lead));
    }
    
    // Interaction-related generators
    
    @Provide
    public static Arbitrary<InteractionType> interactionTypes() {
        return Arbitraries.of(InteractionType.class);
    }
    
    @Provide
    public static Arbitrary<String> interactionNotes() {
        return Arbitraries.of(
                "Initial contact call - discussed requirements",
                "Sent proposal document and pricing information",
                "In-person meeting to review technical specifications",
                "Follow-up call regarding contract terms",
                "Demo session for the proposed solution"
        );
    }
    
    // Role-specific user generators
    
    @Provide
    public static Arbitrary<User> adminUsers() {
        return validUsers().filter(user -> user.getRole() == UserRole.ADMIN);
    }
    
    @Provide
    public static Arbitrary<User> salesUsers() {
        return validUsers().filter(user -> user.getRole() == UserRole.SALES);
    }
    
    @Provide
    public static Arbitrary<User> regularUsers() {
        return validUsers().filter(user -> user.getRole() == UserRole.REGULAR);
    }
    
    // Authentication-related generators
    
    @Provide
    public static Arbitrary<AuthenticationCredentials> validCredentials() {
        return Combinators.combine(
                validUsernames(),
                validPasswords()
        ).as(AuthenticationCredentials::new);
    }
    
    @Provide
    public static Arbitrary<AuthenticationCredentials> invalidCredentials() {
        return Arbitraries.oneOf(
                Combinators.combine(validUsernames(), invalidPasswords()).as(AuthenticationCredentials::new),
                Combinators.combine(invalidUsernames(), validPasswords()).as(AuthenticationCredentials::new),
                Combinators.combine(invalidUsernames(), invalidPasswords()).as(AuthenticationCredentials::new)
        );
    }
    
    // Search criteria generators
    
    @Provide
    public static Arbitrary<String> searchTerms() {
        return Arbitraries.oneOf(
                names(),
                companyNames(),
                Arbitraries.strings().withCharRange('a', 'z').ofMinLength(2).ofMaxLength(10)
        );
    }
    
    @Provide
    public static Arbitrary<List<String>> searchTermLists() {
        return searchTerms().list().ofMinSize(1).ofMaxSize(5);
    }
    
    // Date range generators for reporting
    
    @Provide
    public static Arbitrary<DateRange> validDateRanges() {
        return Combinators.combine(
                pastDateTimes(),
                pastDateTimes()
        ).as((start, end) -> new DateRange(
                start.isBefore(end) ? start : end,
                start.isBefore(end) ? end : start
        ));
    }
    
    @Provide
    public static Arbitrary<DateRange> invalidDateRanges() {
        return Combinators.combine(
                pastDateTimes(),
                pastDateTimes()
        ).as((start, end) -> new DateRange(
                start.isAfter(end) ? start : end,
                start.isAfter(end) ? end : start
        )).filter(range -> range.getStart().isAfter(range.getEnd()));
    }
    
    // Helper classes for test data
    
    public static class AuthenticationCredentials {
        private final String username;
        private final String password;
        
        public AuthenticationCredentials(String username, String password) {
            this.username = username;
            this.password = password;
        }
        
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        
        @Override
        public String toString() {
            return "AuthenticationCredentials{username='" + username + "', password='[HIDDEN]'}";
        }
    }
    
    public static class DateRange {
        private final LocalDateTime start;
        private final LocalDateTime end;
        
        public DateRange(LocalDateTime start, LocalDateTime end) {
            this.start = start;
            this.end = end;
        }
        
        public LocalDateTime getStart() { return start; }
        public LocalDateTime getEnd() { return end; }
        
        @Override
        public String toString() {
            return "DateRange{start=" + start + ", end=" + end + "}";
        }
    }
}