package com.pasp.ict.salescrm.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pasp.ict.salescrm.entity.Customer;
import com.pasp.ict.salescrm.entity.InteractionLog;
import com.pasp.ict.salescrm.entity.InteractionType;
import com.pasp.ict.salescrm.entity.User;
import com.pasp.ict.salescrm.entity.UserRole;
import com.pasp.ict.salescrm.repository.AuditLogRepository;
import com.pasp.ict.salescrm.repository.CustomerRepository;
import com.pasp.ict.salescrm.repository.InteractionLogRepository;
import com.pasp.ict.salescrm.service.CustomerService;

/**
 * Unit tests for CustomerService.
 */
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {
    
    @Mock
    private CustomerRepository customerRepository;
    
    @Mock
    private InteractionLogRepository interactionLogRepository;
    
    @Mock
    private AuditLogRepository auditLogRepository;
    
    @InjectMocks
    private CustomerService customerService;
    
    private User salesUser;
    private User adminUser;
    private Customer testCustomer;
    
    @BeforeEach
    void setUp() {
        salesUser = new User("salesuser", "encodedPassword", "sales@example.com", 
                           "Sales", "User", UserRole.SALES);
        salesUser.setId(1L);
        
        adminUser = new User("admin", "encodedPassword", "admin@example.com", 
                           "Admin", "User", UserRole.ADMIN);
        adminUser.setId(2L);
        
        testCustomer = new Customer("Test Customer", "customer@example.com", 
                                  "123-456-7890", "Test Company", "123 Test St", salesUser);
        testCustomer.setId(1L);
    }
    
    @Test
    void createCustomer_ValidInput_CreatesCustomer() {
        // Arrange
        String name = "New Customer";
        String email = "new@example.com";
        String phone = "987-654-3210";
        String company = "New Company";
        String address = "456 New St";
        
        when(customerRepository.findByEmail(email)).thenReturn(Optional.empty());
        
        Customer savedCustomer = new Customer(name, email, phone, company, address, salesUser);
        savedCustomer.setId(2L);
        when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);
        
        // Act
        Customer result = customerService.createCustomer(name, email, phone, company, address, salesUser);
        
        // Assert
        assertNotNull(result);
        assertEquals(name, result.getName());
        assertEquals(email, result.getEmail());
        assertEquals(phone, result.getPhone());
        assertEquals(company, result.getCompany());
        assertEquals(address, result.getAddress());
        assertEquals(salesUser, result.getCreatedBy());
        
        verify(customerRepository).findByEmail(email);
        verify(customerRepository).save(any(Customer.class));
        verify(auditLogRepository).save(any());
    }
    
    @Test
    void createCustomer_DuplicateEmail_ThrowsException() {
        // Arrange
        String email = "existing@example.com";
        when(customerRepository.findByEmail(email)).thenReturn(Optional.of(testCustomer));
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
                    () -> customerService.createCustomer("Name", email, "123-456-7890", 
                                                        "Company", "Address", salesUser));
        
        verify(customerRepository).findByEmail(email);
        verify(customerRepository, never()).save(any());
    }
    
    @Test
    void createCustomer_EmptyName_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
                    () -> customerService.createCustomer("", "test@example.com", "123-456-7890", 
                                                        "Company", "Address", salesUser));
        
        verify(customerRepository, never()).save(any());
    }
    
    @Test
    void createCustomer_InvalidEmail_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
                    () -> customerService.createCustomer("Name", "invalid-email", "123-456-7890", 
                                                        "Company", "Address", salesUser));
        
        verify(customerRepository, never()).save(any());
    }
    
    @Test
    void createCustomer_RegularUserPermission_ThrowsException() {
        // Arrange
        User regularUser = new User("regular", "password", "regular@example.com", 
                                  "Regular", "User", UserRole.REGULAR);
        
        // Act & Assert
        assertThrows(SecurityException.class, 
                    () -> customerService.createCustomer("Name", "test@example.com", "123-456-7890", 
                                                        "Company", "Address", regularUser));
        
        verify(customerRepository, never()).save(any());
    }
    
    @Test
    void updateCustomer_ValidInput_UpdatesCustomer() {
        // Arrange
        Long customerId = 1L;
        String newName = "Updated Customer";
        String newEmail = "updated@example.com";
        String newPhone = "555-123-4567";
        String newCompany = "Updated Company";
        String newAddress = "789 Updated St";
        
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));
        when(customerRepository.findByEmail(newEmail)).thenReturn(Optional.empty());
        when(customerRepository.findPotentialDuplicates(anyString(), anyString(), eq(customerId))).thenReturn(Arrays.asList());
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);
        
        // Act
        Customer result = customerService.updateCustomer(customerId, newName, newEmail, newPhone, 
                                                        newCompany, newAddress, salesUser);
        
        // Assert
        assertEquals(newName, result.getName());
        assertEquals(newEmail, result.getEmail());
        assertEquals(newPhone, result.getPhone());
        assertEquals(newCompany, result.getCompany());
        assertEquals(newAddress, result.getAddress());
        
        verify(customerRepository).findById(customerId);
        verify(customerRepository).save(testCustomer);
        verify(auditLogRepository).save(any());
    }
    
    @Test
    void updateCustomer_CustomerNotFound_ThrowsException() {
        // Arrange
        Long customerId = 999L;
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
                    () -> customerService.updateCustomer(customerId, "Name", "email@example.com", 
                                                        "123-456-7890", "Company", "Address", salesUser));
        
        verify(customerRepository).findById(customerId);
        verify(customerRepository, never()).save(any());
    }
    
    @Test
    void logInteraction_ValidInput_CreatesInteractionLog() {
        // Arrange
        Long customerId = 1L;
        InteractionType type = InteractionType.CALL;
        String notes = "Customer called about product inquiry";
        
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));
        
        InteractionLog savedInteraction = new InteractionLog(type, notes, testCustomer, salesUser);
        savedInteraction.setId(1L);
        when(interactionLogRepository.save(any(InteractionLog.class))).thenReturn(savedInteraction);
        
        // Act
        InteractionLog result = customerService.logInteraction(customerId, type, notes, salesUser);
        
        // Assert
        assertNotNull(result);
        assertEquals(type, result.getType());
        assertEquals(notes, result.getNotes());
        assertEquals(testCustomer, result.getCustomer());
        assertEquals(salesUser, result.getUser());
        
        verify(customerRepository).findById(customerId);
        verify(interactionLogRepository).save(any(InteractionLog.class));
        verify(auditLogRepository).save(any());
    }
    
    @Test
    void logInteraction_CustomerNotFound_ThrowsException() {
        // Arrange
        Long customerId = 999L;
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
                    () -> customerService.logInteraction(customerId, InteractionType.CALL, 
                                                        "Notes", salesUser));
        
        verify(customerRepository).findById(customerId);
        verify(interactionLogRepository, never()).save(any());
    }
    
    @Test
    void searchCustomers_WithSearchTerm_ReturnsFilteredResults() {
        // Arrange
        String searchTerm = "Test";
        List<Customer> expectedCustomers = Arrays.asList(testCustomer);
        when(customerRepository.searchCustomers(searchTerm)).thenReturn(expectedCustomers);
        
        // Act
        List<Customer> result = customerService.searchCustomers(searchTerm);
        
        // Assert
        assertEquals(expectedCustomers, result);
        verify(customerRepository).searchCustomers(searchTerm);
    }
    
    @Test
    void searchCustomers_EmptySearchTerm_ReturnsAllCustomers() {
        // Arrange
        List<Customer> allCustomers = Arrays.asList(testCustomer);
        when(customerRepository.findAllOrderByCreatedAtDesc()).thenReturn(allCustomers);
        
        // Act
        List<Customer> result = customerService.searchCustomers("");
        
        // Assert
        assertEquals(allCustomers, result);
        verify(customerRepository).findAllOrderByCreatedAtDesc();
        verify(customerRepository, never()).searchCustomers(anyString());
    }
    
    @Test
    void findById_ExistingCustomer_ReturnsCustomer() {
        // Arrange
        Long customerId = 1L;
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));
        
        // Act
        Optional<Customer> result = customerService.findById(customerId);
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals(testCustomer, result.get());
        verify(customerRepository).findById(customerId);
    }
    
    @Test
    void findById_NonExistingCustomer_ReturnsEmpty() {
        // Arrange
        Long customerId = 999L;
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());
        
        // Act
        Optional<Customer> result = customerService.findById(customerId);
        
        // Assert
        assertFalse(result.isPresent());
        verify(customerRepository).findById(customerId);
    }
    
    @Test
    void getCustomerInteractionHistory_ValidCustomer_ReturnsInteractions() {
        // Arrange
        Long customerId = 1L;
        InteractionLog interaction1 = new InteractionLog(InteractionType.CALL, "Call 1", testCustomer, salesUser);
        InteractionLog interaction2 = new InteractionLog(InteractionType.EMAIL, "Email 1", testCustomer, salesUser);
        List<InteractionLog> expectedInteractions = Arrays.asList(interaction1, interaction2);
        
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));
        when(interactionLogRepository.findByCustomerOrderByTimestampDesc(testCustomer)).thenReturn(expectedInteractions);
        
        // Act
        List<InteractionLog> result = customerService.getCustomerInteractionHistory(customerId);
        
        // Assert
        assertEquals(expectedInteractions, result);
        verify(customerRepository).findById(customerId);
        verify(interactionLogRepository).findByCustomerOrderByTimestampDesc(testCustomer);
    }
    
    @Test
    void getRecentCustomerInteractions_ValidInput_ReturnsRecentInteractions() {
        // Arrange
        Long customerId = 1L;
        int days = 7;
        List<InteractionLog> recentInteractions = Arrays.asList(
            new InteractionLog(InteractionType.CALL, "Recent call", testCustomer, salesUser)
        );
        
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));
        when(interactionLogRepository.findRecentCustomerInteractions(eq(testCustomer), any(LocalDateTime.class)))
            .thenReturn(recentInteractions);
        
        // Act
        List<InteractionLog> result = customerService.getRecentCustomerInteractions(customerId, days);
        
        // Assert
        assertEquals(recentInteractions, result);
        verify(customerRepository).findById(customerId);
        verify(interactionLogRepository).findRecentCustomerInteractions(eq(testCustomer), any(LocalDateTime.class));
    }
    
    @Test
    void findByEmail_ExistingEmail_ReturnsCustomer() {
        // Arrange
        String email = "customer@example.com";
        when(customerRepository.findByEmail(email)).thenReturn(Optional.of(testCustomer));
        
        // Act
        Optional<Customer> result = customerService.findByEmail(email);
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals(testCustomer, result.get());
        verify(customerRepository).findByEmail(email);
    }
    
    @Test
    void findByCompany_ValidCompany_ReturnsCustomers() {
        // Arrange
        String company = "Test Company";
        List<Customer> expectedCustomers = Arrays.asList(testCustomer);
        when(customerRepository.findByCompany(company)).thenReturn(expectedCustomers);
        
        // Act
        List<Customer> result = customerService.findByCompany(company);
        
        // Assert
        assertEquals(expectedCustomers, result);
        verify(customerRepository).findByCompany(company);
    }
    
    @Test
    void countCustomersCreatedBy_ValidUser_ReturnsCount() {
        // Arrange
        long expectedCount = 5L;
        when(customerRepository.countByCreatedBy(salesUser)).thenReturn(expectedCount);
        
        // Act
        long result = customerService.countCustomersCreatedBy(salesUser);
        
        // Assert
        assertEquals(expectedCount, result);
        verify(customerRepository).countByCreatedBy(salesUser);
    }
    
    @Test
    void countCustomerInteractions_ValidCustomer_ReturnsCount() {
        // Arrange
        Long customerId = 1L;
        long expectedCount = 10L;
        
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));
        when(interactionLogRepository.countByCustomer(testCustomer)).thenReturn(expectedCount);
        
        // Act
        long result = customerService.countCustomerInteractions(customerId);
        
        // Assert
        assertEquals(expectedCount, result);
        verify(customerRepository).findById(customerId);
        verify(interactionLogRepository).countByCustomer(testCustomer);
    }
    
    @Test
    void findCustomersWithRecentActivity_ValidDays_ReturnsCustomers() {
        // Arrange
        int days = 30;
        List<Customer> expectedCustomers = Arrays.asList(testCustomer);
        when(customerRepository.findCustomersWithRecentActivity(any(LocalDateTime.class)))
            .thenReturn(expectedCustomers);
        
        // Act
        List<Customer> result = customerService.findCustomersWithRecentActivity(days);
        
        // Assert
        assertEquals(expectedCustomers, result);
        verify(customerRepository).findCustomersWithRecentActivity(any(LocalDateTime.class));
    }
    
    @Test
    void findCustomersWithoutRecentInteractions_ValidDays_ReturnsCustomers() {
        // Arrange
        int days = 60;
        List<Customer> expectedCustomers = Arrays.asList(testCustomer);
        when(interactionLogRepository.findCustomersWithNoRecentInteractions(any(LocalDateTime.class)))
            .thenReturn(expectedCustomers);
        
        // Act
        List<Customer> result = customerService.findCustomersWithoutRecentInteractions(days);
        
        // Assert
        assertEquals(expectedCustomers, result);
        verify(interactionLogRepository).findCustomersWithNoRecentInteractions(any(LocalDateTime.class));
    }
}