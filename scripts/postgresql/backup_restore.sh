#!/bin/bash

# PostgreSQL Backup and Restore Script for Sales CRM Application
# This script provides functions for backing up and restoring the PostgreSQL database

# Configuration
DB_NAME="crmdb"
DB_USER="crmuser"
DB_HOST="localhost"
DB_PORT="5432"
BACKUP_DIR="/var/backups/postgresql"
LOG_FILE="/var/log/postgresql_backup.log"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to log messages
log_message() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "$LOG_FILE"
}

# Function to print colored output
print_status() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
    log_message "$message"
}

# Function to check if PostgreSQL is running
check_postgresql() {
    if ! systemctl is-active --quiet postgresql; then
        print_status "$RED" "ERROR: PostgreSQL is not running"
        exit 1
    fi
    print_status "$GREEN" "PostgreSQL is running"
}

# Function to check database connection
check_connection() {
    if ! PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c '\q' 2>/dev/null; then
        print_status "$RED" "ERROR: Cannot connect to database $DB_NAME"
        exit 1
    fi
    print_status "$GREEN" "Database connection successful"
}

# Function to create backup directory
create_backup_dir() {
    if [ ! -d "$BACKUP_DIR" ]; then
        mkdir -p "$BACKUP_DIR"
        chmod 755 "$BACKUP_DIR"
        print_status "$GREEN" "Created backup directory: $BACKUP_DIR"
    fi
}

# Function to perform database backup
backup_database() {
    local backup_type=${1:-"full"}
    local timestamp=$(date +%Y%m%d_%H%M%S)
    local backup_file="$BACKUP_DIR/${DB_NAME}_${backup_type}_backup_${timestamp}.sql"
    
    print_status "$YELLOW" "Starting $backup_type backup of database $DB_NAME..."
    
    case $backup_type in
        "full")
            PGPASSWORD="$DB_PASSWORD" pg_dump \
                -h "$DB_HOST" \
                -p "$DB_PORT" \
                -U "$DB_USER" \
                -d "$DB_NAME" \
                --verbose \
                --no-password \
                --format=custom \
                --compress=9 \
                --file="$backup_file.backup"
            ;;
        "schema")
            PGPASSWORD="$DB_PASSWORD" pg_dump \
                -h "$DB_HOST" \
                -p "$DB_PORT" \
                -U "$DB_USER" \
                -d "$DB_NAME" \
                --verbose \
                --no-password \
                --schema-only \
                --file="$backup_file"
            ;;
        "data")
            PGPASSWORD="$DB_PASSWORD" pg_dump \
                -h "$DB_HOST" \
                -p "$DB_PORT" \
                -U "$DB_USER" \
                -d "$DB_NAME" \
                --verbose \
                --no-password \
                --data-only \
                --file="$backup_file"
            ;;
        *)
            print_status "$RED" "ERROR: Invalid backup type. Use 'full', 'schema', or 'data'"
            exit 1
            ;;
    esac
    
    if [ $? -eq 0 ]; then
        if [ "$backup_type" = "full" ]; then
            backup_file="$backup_file.backup"
        fi
        
        # Compress non-custom format backups
        if [ "$backup_type" != "full" ]; then
            gzip "$backup_file"
            backup_file="$backup_file.gz"
        fi
        
        local file_size=$(du -h "$backup_file" | cut -f1)
        print_status "$GREEN" "Backup completed successfully: $backup_file (Size: $file_size)"
        
        # Create backup metadata
        cat > "$backup_file.meta" << EOF
Backup Type: $backup_type
Database: $DB_NAME
User: $DB_USER
Host: $DB_HOST
Port: $DB_PORT
Timestamp: $(date)
File Size: $file_size
PostgreSQL Version: $(PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT version();" | head -1 | xargs)
EOF
        
        echo "$backup_file"
    else
        print_status "$RED" "ERROR: Backup failed"
        exit 1
    fi
}

# Function to restore database from backup
restore_database() {
    local backup_file="$1"
    local restore_type=${2:-"full"}
    
    if [ -z "$backup_file" ]; then
        print_status "$RED" "ERROR: Backup file not specified"
        exit 1
    fi
    
    if [ ! -f "$backup_file" ]; then
        print_status "$RED" "ERROR: Backup file not found: $backup_file"
        exit 1
    fi
    
    print_status "$YELLOW" "Starting restore from backup: $backup_file"
    
    # Confirm restore operation
    read -p "This will overwrite the existing database. Are you sure? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_status "$YELLOW" "Restore cancelled"
        exit 0
    fi
    
    # Stop application if running
    if systemctl is-active --quiet sales-crm; then
        print_status "$YELLOW" "Stopping Sales CRM application..."
        systemctl stop sales-crm
    fi
    
    case $restore_type in
        "full")
            # Drop and recreate database for full restore
            print_status "$YELLOW" "Dropping and recreating database..."
            PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U postgres -c "DROP DATABASE IF EXISTS $DB_NAME;"
            PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U postgres -c "CREATE DATABASE $DB_NAME OWNER $DB_USER;"
            
            # Restore from custom format backup
            if [[ "$backup_file" == *.backup ]]; then
                PGPASSWORD="$DB_PASSWORD" pg_restore \
                    -h "$DB_HOST" \
                    -p "$DB_PORT" \
                    -U "$DB_USER" \
                    -d "$DB_NAME" \
                    --verbose \
                    --no-password \
                    "$backup_file"
            else
                # Handle compressed SQL files
                if [[ "$backup_file" == *.gz ]]; then
                    gunzip -c "$backup_file" | PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME"
                else
                    PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" < "$backup_file"
                fi
            fi
            ;;
        "schema")
            # Restore schema only
            if [[ "$backup_file" == *.gz ]]; then
                gunzip -c "$backup_file" | PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME"
            else
                PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" < "$backup_file"
            fi
            ;;
        "data")
            # Restore data only
            if [[ "$backup_file" == *.gz ]]; then
                gunzip -c "$backup_file" | PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME"
            else
                PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" < "$backup_file"
            fi
            ;;
        *)
            print_status "$RED" "ERROR: Invalid restore type. Use 'full', 'schema', or 'data'"
            exit 1
            ;;
    esac
    
    if [ $? -eq 0 ]; then
        print_status "$GREEN" "Restore completed successfully"
        
        # Restart application if it was running
        if systemctl is-enabled --quiet sales-crm; then
            print_status "$YELLOW" "Starting Sales CRM application..."
            systemctl start sales-crm
            sleep 5
            if systemctl is-active --quiet sales-crm; then
                print_status "$GREEN" "Sales CRM application started successfully"
            else
                print_status "$RED" "WARNING: Sales CRM application failed to start"
            fi
        fi
    else
        print_status "$RED" "ERROR: Restore failed"
        exit 1
    fi
}

# Function to list available backups
list_backups() {
    print_status "$YELLOW" "Available backups in $BACKUP_DIR:"
    if [ -d "$BACKUP_DIR" ] && [ "$(ls -A $BACKUP_DIR 2>/dev/null)" ]; then
        ls -lah "$BACKUP_DIR"/*.{sql,sql.gz,backup} 2>/dev/null | while read line; do
            echo "  $line"
        done
    else
        print_status "$YELLOW" "No backups found"
    fi
}

# Function to cleanup old backups
cleanup_backups() {
    local retention_days=${1:-30}
    
    print_status "$YELLOW" "Cleaning up backups older than $retention_days days..."
    
    if [ -d "$BACKUP_DIR" ]; then
        local deleted_count=$(find "$BACKUP_DIR" -name "*.sql" -o -name "*.sql.gz" -o -name "*.backup" -o -name "*.meta" | \
                             xargs ls -la | \
                             awk -v days="$retention_days" '$6 " " $7 " " $8 < systime() - days*24*3600 {print $9}' | \
                             wc -l)
        
        find "$BACKUP_DIR" -name "*.sql" -o -name "*.sql.gz" -o -name "*.backup" -o -name "*.meta" | \
        xargs ls -la | \
        awk -v days="$retention_days" '$6 " " $7 " " $8 < systime() - days*24*3600 {print $9}' | \
        xargs rm -f
        
        print_status "$GREEN" "Cleanup completed. Removed $deleted_count old backup files"
    else
        print_status "$YELLOW" "Backup directory does not exist"
    fi
}

# Function to verify backup integrity
verify_backup() {
    local backup_file="$1"
    
    if [ -z "$backup_file" ]; then
        print_status "$RED" "ERROR: Backup file not specified"
        exit 1
    fi
    
    if [ ! -f "$backup_file" ]; then
        print_status "$RED" "ERROR: Backup file not found: $backup_file"
        exit 1
    fi
    
    print_status "$YELLOW" "Verifying backup integrity: $backup_file"
    
    if [[ "$backup_file" == *.backup ]]; then
        # Verify custom format backup
        PGPASSWORD="$DB_PASSWORD" pg_restore --list "$backup_file" > /dev/null 2>&1
        if [ $? -eq 0 ]; then
            print_status "$GREEN" "Backup file is valid (custom format)"
        else
            print_status "$RED" "ERROR: Backup file is corrupted or invalid"
            exit 1
        fi
    elif [[ "$backup_file" == *.gz ]]; then
        # Verify compressed SQL file
        if gunzip -t "$backup_file" 2>/dev/null; then
            print_status "$GREEN" "Backup file is valid (compressed SQL)"
        else
            print_status "$RED" "ERROR: Backup file is corrupted"
            exit 1
        fi
    else
        # Verify plain SQL file
        if [ -r "$backup_file" ] && [ -s "$backup_file" ]; then
            print_status "$GREEN" "Backup file is valid (plain SQL)"
        else
            print_status "$RED" "ERROR: Backup file is empty or unreadable"
            exit 1
        fi
    fi
}

# Function to show database statistics
show_stats() {
    print_status "$YELLOW" "Database Statistics for $DB_NAME:"
    
    PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" << EOF
\echo 'Table Row Counts:'
SELECT 
    schemaname,
    tablename,
    n_tup_ins as inserts,
    n_tup_upd as updates,
    n_tup_del as deletes,
    n_live_tup as live_rows,
    n_dead_tup as dead_rows
FROM pg_stat_user_tables 
ORDER BY n_live_tup DESC;

\echo ''
\echo 'Database Size:'
SELECT pg_size_pretty(pg_database_size('$DB_NAME')) as database_size;

\echo ''
\echo 'Table Sizes:'
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
FROM pg_tables 
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

\echo ''
\echo 'Active Connections:'
SELECT count(*) as active_connections 
FROM pg_stat_activity 
WHERE state = 'active' AND datname = '$DB_NAME';
EOF
}

# Main script logic
case "$1" in
    "backup")
        check_postgresql
        check_connection
        create_backup_dir
        backup_database "$2"
        ;;
    "restore")
        check_postgresql
        restore_database "$2" "$3"
        ;;
    "list")
        list_backups
        ;;
    "cleanup")
        cleanup_backups "$2"
        ;;
    "verify")
        verify_backup "$2"
        ;;
    "stats")
        check_postgresql
        check_connection
        show_stats
        ;;
    *)
        echo "Usage: $0 {backup|restore|list|cleanup|verify|stats} [options]"
        echo ""
        echo "Commands:"
        echo "  backup [type]           - Create database backup (type: full, schema, data)"
        echo "  restore <file> [type]   - Restore database from backup file"
        echo "  list                    - List available backup files"
        echo "  cleanup [days]          - Remove backups older than specified days (default: 30)"
        echo "  verify <file>           - Verify backup file integrity"
        echo "  stats                   - Show database statistics"
        echo ""
        echo "Examples:"
        echo "  $0 backup full          - Create full database backup"
        echo "  $0 backup schema        - Create schema-only backup"
        echo "  $0 restore /path/to/backup.sql full"
        echo "  $0 cleanup 7            - Remove backups older than 7 days"
        echo "  $0 verify /path/to/backup.backup"
        echo ""
        echo "Environment Variables:"
        echo "  DB_PASSWORD             - Database password (required)"
        echo "  BACKUP_DIR              - Backup directory (default: /var/backups/postgresql)"
        exit 1
        ;;
esac