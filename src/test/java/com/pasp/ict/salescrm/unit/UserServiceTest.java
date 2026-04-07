package com.pasp.ict.salescrm.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.pasp.ict.salescrm.entity.User;
import com.pasp.ict.salescrm.entity.UserRole;
import com.pasp.ict.salescrm.repository.AuditLogRepository;
import com.pasp.ict.salescrm.repository.UserRepository;
import com.pasp.ict.salescrm.service.UserService;

/**
 * Unit tests for UserService.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private AuditLogRepository auditLogRepository;
    
    @InjectMocks
    private UserService userService;
    
    private User adminUser;
    
    @BeforeEach
    void setUp() {
        adminUser = new User("admin", "encodedPassword", "admin@example.com", 
                            "Admin", "User", UserRole.ADMIN);
        adminUser.setId(1L);
    }
    
    @Test
    void createUser_ValidInput_CreatesUser() {
        // Arrange
        String username = "testuser";
        String password = "Password123";
        String email = "test@example.com";
        String firstName = "Test";
        String lastName = "User";
        UserRole role = UserRole.SALES;
        
        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("encodedPassword");
        
        User savedUser = new User(username, "encodedPassword", email, firstName, lastName, role);
        savedUser.setId(2L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        
        // Act
        User result = userService.createUser(username, password, email, firstName, lastName, role, adminUser);
        
        // Assert
        assertNotNull(result);
        assertEquals(username, result.getUsername());
        assertEquals(email, result.getEmail());
        assertEquals(firstName, result.getFirstName());
        assertEquals(lastName, result.getLastName());
        assertEquals(role, result.getRole());
        assertTrue(result.isActive());
        
        verify(userRepository).existsByUsername(username);
        verify(userRepository).existsByEmail(email);
        verify(passwordEncoder).encode(password);
        verify(userRepository).save(any(User.class));
        verify(auditLogRepository).save(any());
    }
    
    @Test
    void createUser_DuplicateUsername_ThrowsException() {
        // Arrange
        String username = "existinguser";
        when(userRepository.existsByUsername(username)).thenReturn(true);
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
                    () -> userService.createUser(username, "Password123", "test@example.com", 
                                                "Test", "User", UserRole.SALES, adminUser));
        
        verify(userRepository).existsByUsername(username);
        verify(userRepository, never()).save(any());
    }
    
    @Test
    void createUser_DuplicateEmail_ThrowsException() {
        // Arrange
        String email = "existing@example.com";
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(email)).thenReturn(true);
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
                    () -> userService.createUser("testuser", "Password123", email, 
                                                "Test", "User", UserRole.SALES, adminUser));
        
        verify(userRepository).existsByEmail(email);
        verify(userRepository, never()).save(any());
    }
    
    @Test
    void createUser_InvalidPassword_ThrowsException() {
        // Arrange
        String weakPassword = "weak";
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
                    () -> userService.createUser("testuser", weakPassword, "test@example.com", 
                                                "Test", "User", UserRole.SALES, adminUser));
        
        verify(userRepository, never()).save(any());
    }
    
    @Test
    void updateUserRole_ValidInput_UpdatesRole() {
        // Arrange
        Long userId = 2L;
        UserRole newRole = UserRole.ADMIN;
        
        User existingUser = new User("testuser", "encodedPassword", "test@example.com", 
                                   "Test", "User", UserRole.SALES);
        existingUser.setId(userId);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);
        
        // Act
        User result = userService.updateUserRole(userId, newRole, adminUser);
        
        // Assert
        assertEquals(newRole, result.getRole());
        verify(userRepository).findById(userId);
        verify(userRepository).save(existingUser);
        verify(auditLogRepository).save(any());
    }
    
    @Test
    void updateUserRole_UserNotFound_ThrowsException() {
        // Arrange
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
                    () -> userService.updateUserRole(userId, UserRole.ADMIN, adminUser));
        
        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any());
    }
    
    @Test
    void setUserActive_ValidInput_UpdatesActiveStatus() {
        // Arrange
        Long userId = 2L;
        boolean active = false;
        
        User existingUser = new User("testuser", "encodedPassword", "test@example.com", 
                                   "Test", "User", UserRole.SALES);
        existingUser.setId(userId);
        existingUser.setActive(true);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);
        
        // Act
        User result = userService.setUserActive(userId, active, adminUser);
        
        // Assert
        assertEquals(active, result.isActive());
        verify(userRepository).findById(userId);
        verify(userRepository).save(existingUser);
        verify(auditLogRepository).save(any());
    }
    
    @Test
    void updateLastLogin_ValidInput_UpdatesTimestamp() {
        // Arrange
        Long userId = 2L;
        
        User existingUser = new User("testuser", "encodedPassword", "test@example.com", 
                                   "Test", "User", UserRole.SALES);
        existingUser.setId(userId);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);
        
        LocalDateTime beforeUpdate = LocalDateTime.now().minusSeconds(1);
        
        // Act
        User result = userService.updateLastLogin(userId);
        
        LocalDateTime afterUpdate = LocalDateTime.now().plusSeconds(1);
        
        // Assert
        assertNotNull(result.getLastLogin());
        assertTrue(result.getLastLogin().isAfter(beforeUpdate));
        assertTrue(result.getLastLogin().isBefore(afterUpdate));
        verify(userRepository).findById(userId);
        verify(userRepository).save(existingUser);
    }
    
    @Test
    void changePassword_ValidInput_UpdatesPassword() {
        // Arrange
        Long userId = 2L;
        String newPassword = "NewPassword123";
        
        User existingUser = new User("testuser", "oldEncodedPassword", "test@example.com", 
                                   "Test", "User", UserRole.SALES);
        existingUser.setId(userId);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.encode(newPassword)).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(existingUser);
        
        // Act
        User result = userService.changePassword(userId, newPassword, adminUser);
        
        // Assert
        assertEquals("newEncodedPassword", result.getPassword());
        verify(userRepository).findById(userId);
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(existingUser);
        verify(auditLogRepository).save(any());
    }
    
    @Test
    void changePassword_WeakPassword_ThrowsException() {
        // Arrange
        Long userId = 2L;
        String weakPassword = "weak";
        
        User existingUser = new User("testuser", "oldEncodedPassword", "test@example.com", 
                                   "Test", "User", UserRole.SALES);
        existingUser.setId(userId);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
                    () -> userService.changePassword(userId, weakPassword, adminUser));
        
        verify(userRepository).findById(userId);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }
    
    @Test
    void findByUsername_ExistingUser_ReturnsUser() {
        // Arrange
        String username = "testuser";
        User user = new User(username, "encodedPassword", "test@example.com", 
                           "Test", "User", UserRole.SALES);
        
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        
        // Act
        Optional<User> result = userService.findByUsername(username);
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals(username, result.get().getUsername());
        verify(userRepository).findByUsername(username);
    }
    
    @Test
    void findByUsername_NonExistingUser_ReturnsEmpty() {
        // Arrange
        String username = "nonexistent";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        
        // Act
        Optional<User> result = userService.findByUsername(username);
        
        // Assert
        assertFalse(result.isPresent());
        verify(userRepository).findByUsername(username);
    }
}