package com.pasp.ict.salescrm.service;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.opencsv.CSVWriter;
import com.pasp.ict.salescrm.entity.AuditLog;
import com.pasp.ict.salescrm.entity.Customer;
import com.pasp.ict.salescrm.entity.InteractionLog;
import com.pasp.ict.salescrm.entity.Lead;
import com.pasp.ict.salescrm.entity.SaleTransaction;
import com.pasp.ict.salescrm.entity.User;
import com.pasp.ict.salescrm.repository.AuditLogRepository;
import com.pasp.ict.salescrm.repository.CustomerRepository;
import com.pasp.ict.salescrm.repository.InteractionLogRepository;
import com.pasp.ict.salescrm.repository.LeadRepository;
import com.pasp.ict.salescrm.repository.SaleTransactionRepository;
import com.pasp.ict.salescrm.repository.UserRepository;

/**
 * Service for automated backup operations.
 * Provides daily data backups with retention policies and backup verification.
 */
@Service
public class BackupService {

    private static final Logger logger = LoggerFactory.getLogger(BackupService.class);
    private static final DateTimeFormatter BACKUP_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    
    @Value("${app.backup.directory:backups}")
    private String backupDirectory;
    
    @Value("${app.backup.retention.days:30}")
    private int retentionDays;
    
    @Value("${app.backup.enabled:true}")
    private boolean backupEnabled;
    
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final LeadRepository leadRepository;
    private final SaleTransactionRepository saleTransactionRepository;
    private final InteractionLogRepository interactionLogRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationService notificationService;

    public BackupService(UserRepository userRepository,
                        CustomerRepository customerRepository,
                        LeadRepository leadRepository,
                        SaleTransactionRepository saleTransactionRepository,
                        InteractionLogRepository interactionLogRepository,
                        AuditLogRepository auditLogRepository,
                        NotificationService notificationService) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.leadRepository = leadRepository;
        this.saleTransactionRepository = saleTransactionRepository;
        this.interactionLogRepository = interactionLogRepository;
        this.auditLogRepository = auditLogRepository;
        this.notificationService = notificationService;
    }

    /**
     * Scheduled daily backup at 2:00 AM.
     * Runs automatically every day to create system backups.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void performDailyBackup() {
        if (!backupEnabled) {
            logger.info("Backup is disabled, skipping daily backup");
            return;
        }
        
        logger.info("Starting daily backup process");
        
        try {
            String backupId = createBackup();
            cleanupOldBackups();
            
            logger.info("Daily backup completed successfully: {}", backupId);
            notificationService.notifyBackupSuccess(backupId);
            
        } catch (Exception e) {
            logger.error("Daily backup failed", e);
            notificationService.notifyBackupFailure(e.getMessage());
        }
    }

    /**
     * Creates a complete system backup.
     * 
     * @return The backup identifier
     * @throws IOException if backup creation fails
     */
    @Transactional(readOnly = true)
    public String createBackup() throws IOException {
        String timestamp = LocalDateTime.now().format(BACKUP_DATE_FORMAT);
        String backupId = "backup_" + timestamp;
        
        logger.info("Creating backup: {}", backupId);
        
        // Create backup directory
        Path backupPath = createBackupDirectory(backupId);
        
        // Backup all entities
        backupUsers(backupPath);
        backupCustomers(backupPath);
        backupLeads(backupPath);
        backupSaleTransactions(backupPath);
        backupInteractionLogs(backupPath);
        backupAuditLogs(backupPath);
        
        // Create backup metadata
        createBackupMetadata(backupPath, backupId);
        
        logger.info("Backup created successfully: {}", backupId);
        return backupId;
    }

    /**
     * Creates a backup asynchronously.
     * 
     * @return CompletableFuture with backup identifier
     */
    @Async
    public CompletableFuture<String> createBackupAsync() {
        try {
            String backupId = createBackup();
            return CompletableFuture.completedFuture(backupId);
        } catch (Exception e) {
            logger.error("Async backup failed", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Creates backup directory structure.
     */
    private Path createBackupDirectory(String backupId) throws IOException {
        String backupDir = backupDirectory != null ? backupDirectory : "backups";
        Path backupPath = Paths.get(backupDir, backupId);
        Files.createDirectories(backupPath);
        return backupPath;
    }

    /**
     * Backs up user data to CSV.
     */
    private void backupUsers(Path backupPath) throws IOException {
        List<User> users = userRepository.findAll();
        Path filePath = backupPath.resolve("users.csv");
        
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath.toFile()))) {
            // Write header
            writer.writeNext(new String[]{"id", "username", "email", "firstName", "lastName", "role", "active", "createdAt", "lastLogin"});
            
            // Write data
            for (User user : users) {
                writer.writeNext(new String[]{
                    String.valueOf(user.getId()),
                    user.getUsername(),
                    user.getEmail(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getRole().toString(),
                    String.valueOf(user.isActive()),
                    user.getCreatedAt() != null ? user.getCreatedAt().toString() : "",
                    user.getLastLogin() != null ? user.getLastLogin().toString() : ""
                });
            }
        }
        
        logger.debug("Backed up {} users", users.size());
    }

    /**
     * Backs up customer data to CSV.
     */
    private void backupCustomers(Path backupPath) throws IOException {
        List<Customer> customers = customerRepository.findAll();
        Path filePath = backupPath.resolve("customers.csv");
        
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath.toFile()))) {
            // Write header
            writer.writeNext(new String[]{"id", "name", "email", "phone", "company", "address", "createdAt", "updatedAt", "createdBy"});
            
            // Write data
            for (Customer customer : customers) {
                writer.writeNext(new String[]{
                    String.valueOf(customer.getId()),
                    customer.getName(),
                    customer.getEmail(),
                    customer.getPhone(),
                    customer.getCompany(),
                    customer.getAddress(),
                    customer.getCreatedAt() != null ? customer.getCreatedAt().toString() : "",
                    customer.getUpdatedAt() != null ? customer.getUpdatedAt().toString() : "",
                    customer.getCreatedBy() != null ? String.valueOf(customer.getCreatedBy().getId()) : ""
                });
            }
        }
        
        logger.debug("Backed up {} customers", customers.size());
    }

    /**
     * Backs up lead data to CSV.
     */
    private void backupLeads(Path backupPath) throws IOException {
        List<Lead> leads = leadRepository.findAll();
        Path filePath = backupPath.resolve("leads.csv");
        
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath.toFile()))) {
            // Write header
            writer.writeNext(new String[]{"id", "title", "description", "status", "estimatedValue", "createdAt", "updatedAt", "customerId", "assignedTo"});
            
            // Write data
            for (Lead lead : leads) {
                writer.writeNext(new String[]{
                    String.valueOf(lead.getId()),
                    lead.getTitle(),
                    lead.getDescription(),
                    lead.getStatus().toString(),
                    lead.getEstimatedValue() != null ? lead.getEstimatedValue().toString() : "",
                    lead.getCreatedAt() != null ? lead.getCreatedAt().toString() : "",
                    lead.getUpdatedAt() != null ? lead.getUpdatedAt().toString() : "",
                    lead.getCustomer() != null ? String.valueOf(lead.getCustomer().getId()) : "",
                    lead.getAssignedTo() != null ? String.valueOf(lead.getAssignedTo().getId()) : ""
                });
            }
        }
        
        logger.debug("Backed up {} leads", leads.size());
    }

    /**
     * Backs up sale transaction data to CSV.
     */
    private void backupSaleTransactions(Path backupPath) throws IOException {
        List<SaleTransaction> transactions = saleTransactionRepository.findAll();
        Path filePath = backupPath.resolve("sale_transactions.csv");
        
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath.toFile()))) {
            // Write header
            writer.writeNext(new String[]{"id", "amount", "saleDate", "description", "customerId", "salesUserId", "leadId"});
            
            // Write data
            for (SaleTransaction transaction : transactions) {
                writer.writeNext(new String[]{
                    String.valueOf(transaction.getId()),
                    transaction.getAmount() != null ? transaction.getAmount().toString() : "",
                    transaction.getSaleDate() != null ? transaction.getSaleDate().toString() : "",
                    transaction.getDescription(),
                    transaction.getCustomer() != null ? String.valueOf(transaction.getCustomer().getId()) : "",
                    transaction.getSalesUser() != null ? String.valueOf(transaction.getSalesUser().getId()) : "",
                    transaction.getLead() != null ? String.valueOf(transaction.getLead().getId()) : ""
                });
            }
        }
        
        logger.debug("Backed up {} sale transactions", transactions.size());
    }

    /**
     * Backs up interaction log data to CSV.
     */
    private void backupInteractionLogs(Path backupPath) throws IOException {
        List<InteractionLog> interactions = interactionLogRepository.findAll();
        Path filePath = backupPath.resolve("interaction_logs.csv");
        
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath.toFile()))) {
            // Write header
            writer.writeNext(new String[]{"id", "type", "notes", "timestamp", "customerId", "userId"});
            
            // Write data
            for (InteractionLog interaction : interactions) {
                writer.writeNext(new String[]{
                    String.valueOf(interaction.getId()),
                    interaction.getType().toString(),
                    interaction.getNotes(),
                    interaction.getTimestamp() != null ? interaction.getTimestamp().toString() : "",
                    interaction.getCustomer() != null ? String.valueOf(interaction.getCustomer().getId()) : "",
                    interaction.getUser() != null ? String.valueOf(interaction.getUser().getId()) : ""
                });
            }
        }
        
        logger.debug("Backed up {} interaction logs", interactions.size());
    }

    /**
     * Backs up audit log data to CSV.
     */
    private void backupAuditLogs(Path backupPath) throws IOException {
        List<AuditLog> auditLogs = auditLogRepository.findAll();
        Path filePath = backupPath.resolve("audit_logs.csv");
        
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath.toFile()))) {
            // Write header
            writer.writeNext(new String[]{"id", "action", "entityType", "entityId", "oldValues", "newValues", "timestamp", "ipAddress", "userAgent", "userId"});
            
            // Write data
            for (AuditLog auditLog : auditLogs) {
                writer.writeNext(new String[]{
                    String.valueOf(auditLog.getId()),
                    auditLog.getAction(),
                    auditLog.getEntityType(),
                    auditLog.getEntityId() != null ? String.valueOf(auditLog.getEntityId()) : "",
                    auditLog.getOldValues(),
                    auditLog.getNewValues(),
                    auditLog.getTimestamp() != null ? auditLog.getTimestamp().toString() : "",
                    auditLog.getIpAddress(),
                    auditLog.getUserAgent(),
                    auditLog.getUser() != null ? String.valueOf(auditLog.getUser().getId()) : ""
                });
            }
        }
        
        logger.debug("Backed up {} audit logs", auditLogs.size());
    }

    /**
     * Creates backup metadata file.
     */
    private void createBackupMetadata(Path backupPath, String backupId) throws IOException {
        Path metadataPath = backupPath.resolve("backup_metadata.txt");
        
        try (FileWriter writer = new FileWriter(metadataPath.toFile())) {
            writer.write("Backup ID: " + backupId + "\n");
            writer.write("Created: " + LocalDateTime.now() + "\n");
            writer.write("Type: Full System Backup\n");
            writer.write("Status: Completed\n");
            
            // Add entity counts
            writer.write("\nEntity Counts:\n");
            writer.write("Users: " + userRepository.count() + "\n");
            writer.write("Customers: " + customerRepository.count() + "\n");
            writer.write("Leads: " + leadRepository.count() + "\n");
            writer.write("Sale Transactions: " + saleTransactionRepository.count() + "\n");
            writer.write("Interaction Logs: " + interactionLogRepository.count() + "\n");
            writer.write("Audit Logs: " + auditLogRepository.count() + "\n");
        }
    }

    /**
     * Cleans up old backup files based on retention policy.
     */
    private void cleanupOldBackups() {
        try {
            String backupDir = backupDirectory != null ? backupDirectory : "backups";
            Path backupDirPath = Paths.get(backupDir);
            if (!Files.exists(backupDirPath)) {
                return;
            }
            
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
            
            Files.list(backupDirPath)
                .filter(Files::isDirectory)
                .filter(path -> {
                    String dirName = path.getFileName().toString();
                    if (dirName.startsWith("backup_")) {
                        try {
                            String dateStr = dirName.substring(7); // Remove "backup_" prefix
                            LocalDateTime backupDate = LocalDateTime.parse(dateStr, BACKUP_DATE_FORMAT);
                            return backupDate.isBefore(cutoffDate);
                        } catch (Exception e) {
                            logger.warn("Could not parse backup date from directory: {}", dirName);
                            return false;
                        }
                    }
                    return false;
                })
                .forEach(this::deleteBackupDirectory);
                
        } catch (IOException e) {
            logger.error("Error during backup cleanup", e);
        }
    }

    /**
     * Recursively deletes a backup directory.
     */
    private void deleteBackupDirectory(Path backupPath) {
        try {
            Files.walk(backupPath)
                .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        logger.warn("Could not delete backup file: {}", path, e);
                    }
                });
            logger.info("Deleted old backup: {}", backupPath.getFileName());
        } catch (IOException e) {
            logger.error("Error deleting backup directory: {}", backupPath, e);
        }
    }

    /**
     * Gets backup status information.
     * 
     * @return Backup status summary
     */
    public String getBackupStatus() {
        try {
            String backupDir = backupDirectory != null ? backupDirectory : "backups";
            Path backupDirPath = Paths.get(backupDir);
            if (!Files.exists(backupDirPath)) {
                return "No backups found";
            }
            
            long backupCount = Files.list(backupDirPath)
                .filter(Files::isDirectory)
                .filter(path -> path.getFileName().toString().startsWith("backup_"))
                .count();
                
            return String.format("Backup enabled: %s, Total backups: %d, Retention: %d days", 
                                backupEnabled, backupCount, retentionDays);
        } catch (IOException e) {
            return "Error reading backup status: " + e.getMessage();
        }
    }

    /**
     * Verifies backup integrity.
     * 
     * @param backupId The backup to verify
     * @return true if backup is valid, false otherwise
     */
    public boolean verifyBackup(String backupId) {
        try {
            if (backupId == null || backupId.trim().isEmpty()) {
                return false;
            }
            
            String backupDir = backupDirectory != null ? backupDirectory : "backups";
            Path backupPath = Paths.get(backupDir, backupId);
            if (!Files.exists(backupPath)) {
                return false;
            }
            
            // Check if all required files exist
            String[] requiredFiles = {"users.csv", "customers.csv", "leads.csv", 
                                    "sale_transactions.csv", "interaction_logs.csv", 
                                    "audit_logs.csv", "backup_metadata.txt"};
            
            for (String fileName : requiredFiles) {
                if (!Files.exists(backupPath.resolve(fileName))) {
                    logger.warn("Missing backup file: {}", fileName);
                    return false;
                }
            }
            
            return true;
        } catch (Exception e) {
            logger.error("Error verifying backup: {}", backupId, e);
            return false;
        }
    }
}