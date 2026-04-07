package com.pasp.ict.salescrm.entity;

/**
 * Enumeration defining user roles in the Sales CRM system.
 * Each role has different access levels and permissions.
 */
public enum UserRole {
    /**
     * Administrator role with full system access
     */
    ADMIN,
    
    /**
     * Sales user role with access to sales operations and customer management
     */
    SALES,
    
    /**
     * Regular user role with read-only access to assigned data
     */
    REGULAR
}