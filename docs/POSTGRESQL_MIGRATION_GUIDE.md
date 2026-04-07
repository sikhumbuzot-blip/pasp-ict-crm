# PostgreSQL Migration Guide

## Overview

This guide provides comprehensive instructions for migrating the Sales CRM Application from H2 in-memory database to PostgreSQL for production deployment. The application is designed with database-agnostic JPA entities to ensure smooth migration between database systems.

## Prerequisites

### System Requirements
- PostgreSQL 12 or higher
- Java 17 or higher
- Maven 3.6 or higher
- Minimum 2GB RAM for PostgreSQL
- Minimum 10GB disk space for database and logs

### PostgreSQL Installation

#### Ubuntu/Debian
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

#### CentOS/RHEL
```bash
sudo yum install postgresql-server postgresql-contrib
sudo postgresql-setup initdb
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

#### macOS (using Homebrew)
```bash
brew install postgresql
brew services start postgresql
```

#### Windows
Download and install PostgreSQL from https://www.postgresql.org/download/windows/

## Database Setup

### 1. Create Database and User

Connect to PostgreSQL as superuser:
```bash
sudo -u postgres psql
```

Execute the following SQL commands:
```sql
-- Create database
CREATE DATABASE crmdb;

-- Create user with password
CREATE USER crmuser WITH PASSWORD 'crmpass';

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE crmdb TO crmuser;

-- Connect to the database
\c crmdb

-- Grant schema privileges
GRANT ALL ON SCHEMA public TO crmuser;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO crmuser;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO crmuser;

-- Exit psql
\q
```

### 2. Configure PostgreSQL Settings

Edit PostgreSQL configuration file (`postgresql.conf`):
```bash
# Find the config file location
sudo -u postgres psql -c 'SHOW config_file;'

# Edit the configuration
sudo nano /etc/postgresql/[version]/main/postgresql.conf
```

Recommended settings for production:
```ini
# Connection settings
listen_addresses = 'localhost'
port = 5432
max_connections = 100

# Memory settings
shared_buffers = 256MB
effective_cache_size = 1GB
work_mem = 4MB
maintenance_work_mem = 64MB

# Logging settings
log_destination = 'stderr'
logging_collector = on
log_directory = 'log'
log_filename = 'postgresql-%Y-%m-%d_%H%M%S.log'
log_statement = 'all'
log_min_duration_statement = 1000

# Performance settings
checkpoint_completion_target = 0.9
wal_buffers = 16MB
default_statistics_target = 100
```

Edit client authentication file (`pg_hba.conf`):
```bash
sudo nano /etc/postgresql/[version]/main/pg_hba.conf
```

Add or modify the following line:
```
# TYPE  DATABASE        USER            ADDRESS                 METHOD
local   crmdb           crmuser                                 md5
host    crmdb           crmuser         127.0.0.1/32            md5
host    crmdb           crmuser         ::1/128                 md5
```

Restart PostgreSQL:
```bash
sudo systemctl restart postgresql
```

### 3. Test Database Connection

Test the connection:
```bash
psql -h localhost -U crmuser -d crmdb
```

## Application Configuration

### 1. Update Maven Dependencies

The `pom.xml` already includes the PostgreSQL driver:
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

### 2. Production Configuration

The application includes a production configuration file (`application-prod.properties`) with PostgreSQL settings. Key configurations:

```properties
# Database Configuration (PostgreSQL)
spring.datasource.url=jdbc:postgresql://localhost:5432/crmdb
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.username=${DB_USERNAME:crmuser}
spring.datasource.password=${DB_PASSWORD:crmpass}

# JPA Configuration for Production
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

# Connection Pool Configuration
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.connection-timeout=20000
```

### 3. Environment Variables

Set the following environment variables for production:
```bash
export DB_USERNAME=crmuser
export DB_PASSWORD=your_secure_password
export ENCRYPTION_KEY=your_32_character_encryption_key
export ADMIN_USERNAME=admin
export ADMIN_PASSWORD=your_admin_password
```

## Schema Migration

### 1. Schema Differences Between H2 and PostgreSQL

The application uses JPA annotations that are compatible with both databases. However, there are some differences to be aware of:

#### Data Type Mappings
| Java Type | H2 Type | PostgreSQL Type |
|-----------|---------|-----------------|
| `Long` (ID) | `BIGINT` | `BIGSERIAL` |
| `String` | `VARCHAR` | `VARCHAR` |
| `LocalDateTime` | `TIMESTAMP` | `TIMESTAMP` |
| `BigDecimal` | `DECIMAL` | `NUMERIC` |
| `boolean` | `BOOLEAN` | `BOOLEAN` |
| `Enum` | `VARCHAR` | `VARCHAR` |

#### Sequence Generation
- H2: Uses `IDENTITY` columns
- PostgreSQL: Uses `SERIAL` or `BIGSERIAL` columns with sequences

### 2. Schema Creation Script

Create the PostgreSQL schema using the following SQL script:

```sql
-- Create sequences for primary keys
CREATE SEQUENCE users_seq START 1;
CREATE SEQUENCE customers_seq START 1;
CREATE SEQUENCE leads_seq START 1;
CREATE SEQUENCE sale_transactions_seq START 1;
CREATE SEQUENCE interaction_logs_seq START 1;
CREATE SEQUENCE audit_logs_seq START 1;

-- Create users table
CREATE TABLE users (
    id BIGINT PRIMARY KEY DEFAULT nextval('users_seq'),
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(500) UNIQUE NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'SALES', 'REGULAR')),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP
);

-- Create customers table
CREATE TABLE customers (
    id BIGINT PRIMARY KEY DEFAULT nextval('customers_seq'),
    name VARCHAR(100) NOT NULL,
    email VARCHAR(500) UNIQUE NOT NULL,
    phone VARCHAR(255),
    company VARCHAR(100),
    address VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by_id BIGINT NOT NULL,
    FOREIGN KEY (created_by_id) REFERENCES users(id)
);

-- Create leads table
CREATE TABLE leads (
    id BIGINT PRIMARY KEY DEFAULT nextval('leads_seq'),
    title VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(20) NOT NULL DEFAULT 'NEW' 
        CHECK (status IN ('NEW', 'CONTACTED', 'QUALIFIED', 'PROPOSAL', 'NEGOTIATION', 'CLOSED_WON', 'CLOSED_LOST')),
    estimated_value NUMERIC(10,2) CHECK (estimated_value > 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    customer_id BIGINT NOT NULL,
    assigned_to_id BIGINT,
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    FOREIGN KEY (assigned_to_id) REFERENCES users(id)
);

-- Create sale_transactions table
CREATE TABLE sale_transactions (
    id BIGINT PRIMARY KEY DEFAULT nextval('sale_transactions_seq'),
    amount NUMERIC(10,2) NOT NULL CHECK (amount > 0),
    sale_date TIMESTAMP NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    customer_id BIGINT NOT NULL,
    sales_user_id BIGINT NOT NULL,
    lead_id BIGINT,
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    FOREIGN KEY (sales_user_id) REFERENCES users(id),
    FOREIGN KEY (lead_id) REFERENCES leads(id)
);

-- Create interaction_logs table
CREATE TABLE interaction_logs (
    id BIGINT PRIMARY KEY DEFAULT nextval('interaction_logs_seq'),
    type VARCHAR(20) NOT NULL CHECK (type IN ('CALL', 'EMAIL', 'MEETING', 'NOTE')),
    notes VARCHAR(2000),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    customer_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Create audit_logs table
CREATE TABLE audit_logs (
    id BIGINT PRIMARY KEY DEFAULT nextval('audit_logs_seq'),
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT,
    old_values VARCHAR(5000),
    new_values VARCHAR(5000),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    user_id BIGINT,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Create indexes for better performance
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_active ON users(active);

CREATE INDEX idx_customers_email ON customers(email);
CREATE INDEX idx_customers_company ON customers(company);
CREATE INDEX idx_customers_created_by ON customers(created_by_id);

CREATE INDEX idx_leads_status ON leads(status);
CREATE INDEX idx_leads_customer ON leads(customer_id);
CREATE INDEX idx_leads_assigned_to ON leads(assigned_to_id);
CREATE INDEX idx_leads_created_at ON leads(created_at);

CREATE INDEX idx_sale_transactions_customer ON sale_transactions(customer_id);
CREATE INDEX idx_sale_transactions_sales_user ON sale_transactions(sales_user_id);
CREATE INDEX idx_sale_transactions_sale_date ON sale_transactions(sale_date);
CREATE INDEX idx_sale_transactions_lead ON sale_transactions(lead_id);

CREATE INDEX idx_interaction_logs_customer ON interaction_logs(customer_id);
CREATE INDEX idx_interaction_logs_user ON interaction_logs(user_id);
CREATE INDEX idx_interaction_logs_timestamp ON interaction_logs(timestamp);
CREATE INDEX idx_interaction_logs_type ON interaction_logs(type);

CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
```

### 3. Data Migration Script

If you have existing data in H2 that needs to be migrated, use the following approach:

1. **Export data from H2:**
```sql
-- Connect to H2 database
-- Export each table to CSV
CALL CSVWRITE('/path/to/users.csv', 'SELECT * FROM users');
CALL CSVWRITE('/path/to/customers.csv', 'SELECT * FROM customers');
CALL CSVWRITE('/path/to/leads.csv', 'SELECT * FROM leads');
CALL CSVWRITE('/path/to/sale_transactions.csv', 'SELECT * FROM sale_transactions');
CALL CSVWRITE('/path/to/interaction_logs.csv', 'SELECT * FROM interaction_logs');
CALL CSVWRITE('/path/to/audit_logs.csv', 'SELECT * FROM audit_logs');
```

2. **Import data to PostgreSQL:**
```sql
-- Connect to PostgreSQL database
-- Import each table from CSV
\copy users FROM '/path/to/users.csv' WITH CSV HEADER;
\copy customers FROM '/path/to/customers.csv' WITH CSV HEADER;
\copy leads FROM '/path/to/leads.csv' WITH CSV HEADER;
\copy sale_transactions FROM '/path/to/sale_transactions.csv' WITH CSV HEADER;
\copy interaction_logs FROM '/path/to/interaction_logs.csv' WITH CSV HEADER;
\copy audit_logs FROM '/path/to/audit_logs.csv' WITH CSV HEADER;

-- Update sequences to current maximum values
SELECT setval('users_seq', (SELECT MAX(id) FROM users));
SELECT setval('customers_seq', (SELECT MAX(id) FROM customers));
SELECT setval('leads_seq', (SELECT MAX(id) FROM leads));
SELECT setval('sale_transactions_seq', (SELECT MAX(id) FROM sale_transactions));
SELECT setval('interaction_logs_seq', (SELECT MAX(id) FROM interaction_logs));
SELECT setval('audit_logs_seq', (SELECT MAX(id) FROM audit_logs));
```

## Deployment Process

### 1. Pre-Migration Checklist

- [ ] PostgreSQL server installed and running
- [ ] Database and user created with proper permissions
- [ ] Application configuration updated for PostgreSQL
- [ ] Environment variables set
- [ ] Database schema created
- [ ] Existing data migrated (if applicable)
- [ ] Connection tested successfully

### 2. Application Deployment

1. **Build the application:**
```bash
mvn clean package -Pprod
```

2. **Run with production profile:**
```bash
java -jar target/sales-crm-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

3. **Verify deployment:**
- Check application logs for successful startup
- Verify database connections in logs
- Test login functionality
- Verify data integrity

### 3. Post-Migration Verification

1. **Database Connection Test:**
```bash
psql -h localhost -U crmuser -d crmdb -c "SELECT COUNT(*) FROM users;"
```

2. **Application Health Check:**
```bash
curl http://localhost:8080/actuator/health
```

3. **Functional Testing:**
- Test user authentication
- Test CRUD operations for all entities
- Test reporting functionality
- Test backup operations

## Performance Optimization

### 1. Database Tuning

#### Connection Pool Settings
```properties
# Optimal connection pool settings
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.max-lifetime=1800000
```

#### PostgreSQL Configuration
```ini
# Memory settings
shared_buffers = 256MB
effective_cache_size = 1GB
work_mem = 4MB
maintenance_work_mem = 64MB

# Checkpoint settings
checkpoint_completion_target = 0.9
wal_buffers = 16MB

# Query planner settings
default_statistics_target = 100
random_page_cost = 1.1
```

### 2. Monitoring and Maintenance

#### Database Monitoring
```sql
-- Monitor active connections
SELECT count(*) FROM pg_stat_activity WHERE state = 'active';

-- Monitor database size
SELECT pg_size_pretty(pg_database_size('crmdb'));

-- Monitor table sizes
SELECT schemaname,tablename,attname,n_distinct,correlation 
FROM pg_stats WHERE schemaname = 'public';
```

#### Regular Maintenance
```sql
-- Analyze tables for query optimization
ANALYZE;

-- Vacuum tables to reclaim space
VACUUM;

-- Reindex for performance
REINDEX DATABASE crmdb;
```

## Troubleshooting

### Common Issues and Solutions

#### 1. Connection Issues
**Problem:** Application cannot connect to PostgreSQL
**Solutions:**
- Verify PostgreSQL is running: `sudo systemctl status postgresql`
- Check connection parameters in application-prod.properties
- Verify pg_hba.conf allows connections from application host
- Test connection manually: `psql -h localhost -U crmuser -d crmdb`

#### 2. Authentication Issues
**Problem:** Authentication failed for user
**Solutions:**
- Verify user exists: `SELECT * FROM pg_user WHERE usename = 'crmuser';`
- Check password: `ALTER USER crmuser PASSWORD 'newpassword';`
- Verify pg_hba.conf authentication method

#### 3. Schema Issues
**Problem:** Tables or sequences not found
**Solutions:**
- Verify schema creation script was executed successfully
- Check table ownership: `SELECT * FROM information_schema.tables WHERE table_schema = 'public';`
- Verify sequence values: `SELECT * FROM information_schema.sequences;`

#### 4. Performance Issues
**Problem:** Slow query performance
**Solutions:**
- Check query execution plans: `EXPLAIN ANALYZE SELECT ...`
- Verify indexes are being used
- Update table statistics: `ANALYZE table_name;`
- Consider adding additional indexes

#### 5. Data Migration Issues
**Problem:** Data import fails or data corruption
**Solutions:**
- Verify CSV format matches table structure
- Check for encoding issues (use UTF-8)
- Validate foreign key constraints
- Use transactions for data consistency

### Log Analysis

#### Application Logs
Monitor application logs for:
- Database connection errors
- SQL exceptions
- Performance warnings
- Authentication failures

#### PostgreSQL Logs
Monitor PostgreSQL logs for:
- Connection attempts
- Slow queries
- Lock waits
- Error messages

Location of PostgreSQL logs:
```bash
# Find log directory
sudo -u postgres psql -c 'SHOW log_directory;'

# View recent logs
sudo tail -f /var/log/postgresql/postgresql-*.log
```

## Security Considerations

### 1. Database Security

#### User Privileges
- Use dedicated database user with minimal required privileges
- Avoid using superuser accounts for application connections
- Regularly review and audit user permissions

#### Network Security
- Configure PostgreSQL to listen only on required interfaces
- Use SSL/TLS for database connections in production
- Implement firewall rules to restrict database access

#### Data Encryption
- Enable encryption at rest for sensitive data
- Use encrypted connections (SSL/TLS)
- Implement application-level encryption for PII data

### 2. Application Security

#### Configuration Security
- Store sensitive configuration in environment variables
- Use strong passwords for database and admin accounts
- Regularly rotate encryption keys and passwords

#### Backup Security
- Encrypt database backups
- Store backups in secure locations
- Test backup restoration procedures regularly

## Backup and Recovery

### 1. Database Backup

#### Automated Backup Script
```bash
#!/bin/bash
# PostgreSQL backup script

DB_NAME="crmdb"
DB_USER="crmuser"
BACKUP_DIR="/var/backups/postgresql"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/crmdb_backup_$DATE.sql"

# Create backup directory if it doesn't exist
mkdir -p $BACKUP_DIR

# Create database backup
pg_dump -h localhost -U $DB_USER -d $DB_NAME > $BACKUP_FILE

# Compress backup
gzip $BACKUP_FILE

# Remove backups older than 30 days
find $BACKUP_DIR -name "*.sql.gz" -mtime +30 -delete

echo "Backup completed: $BACKUP_FILE.gz"
```

#### Schedule Automated Backups
```bash
# Add to crontab for daily backups at 2 AM
crontab -e

# Add this line:
0 2 * * * /path/to/backup_script.sh
```

### 2. Database Recovery

#### Full Database Restore
```bash
# Stop application
sudo systemctl stop sales-crm

# Drop and recreate database
sudo -u postgres psql -c "DROP DATABASE crmdb;"
sudo -u postgres psql -c "CREATE DATABASE crmdb;"
sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE crmdb TO crmuser;"

# Restore from backup
gunzip -c /var/backups/postgresql/crmdb_backup_YYYYMMDD_HHMMSS.sql.gz | psql -h localhost -U crmuser -d crmdb

# Start application
sudo systemctl start sales-crm
```

#### Point-in-Time Recovery
For point-in-time recovery, enable WAL archiving in PostgreSQL:
```ini
# In postgresql.conf
wal_level = replica
archive_mode = on
archive_command = 'cp %p /var/lib/postgresql/wal_archive/%f'
```

## Conclusion

This migration guide provides comprehensive instructions for transitioning from H2 to PostgreSQL. The application's database-agnostic design ensures smooth migration with minimal code changes. Follow the checklist and verification steps to ensure a successful migration.

For additional support or questions, refer to:
- PostgreSQL Documentation: https://www.postgresql.org/docs/
- Spring Boot Database Documentation: https://docs.spring.io/spring-boot/docs/current/reference/html/data.html
- Application README.md for specific configuration details