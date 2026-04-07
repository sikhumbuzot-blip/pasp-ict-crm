package com.pasp.ict.salescrm.entity;

/**
 * Enumeration defining the possible statuses for leads in the sales pipeline.
 */
public enum LeadStatus {
    /**
     * Initial lead creation
     */
    NEW,
    
    /**
     * First contact made with the lead
     */
    CONTACTED,
    
    /**
     * Lead meets qualification criteria
     */
    QUALIFIED,
    
    /**
     * Proposal has been sent to the lead
     */
    PROPOSAL,
    
    /**
     * Currently in negotiation phase
     */
    NEGOTIATION,
    
    /**
     * Successfully converted to a sale
     */
    CLOSED_WON,
    
    /**
     * Lost opportunity
     */
    CLOSED_LOST
}