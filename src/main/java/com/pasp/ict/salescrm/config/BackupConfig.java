package com.pasp.ict.salescrm.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration class for backup and scheduling functionality.
 * Enables asynchronous processing and scheduled tasks.
 */
@Configuration
@EnableScheduling
@EnableAsync
public class BackupConfig {
    // Configuration is handled through annotations
    // Additional backup-specific beans can be added here if needed
}