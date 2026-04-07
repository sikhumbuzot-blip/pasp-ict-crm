# PostgreSQL Migration - Quick Reference

## Pre-Migration Checklist

- [ ] PostgreSQL 12+ installed and running
- [ ] Database `crmdb` and user `crmuser` created
- [ ] Application built with `mvn clean package`
- [ ] Environment variables configured
- [ ] Backup of existing H2 data (if applicable)

## Quick Migration Commands

### 1. Database Setup
```bash
# Install PostgreSQL (Ubuntu)
sudo apt update && sudo apt install postgresql postgresql-contrib

# Create database and user
sudo -u postgres psql << EOF
CREATE DATABASE crmdb;
CREATE USER crmuser WITH PASSWORD 'crmpass';
GRANT ALL PRIVILEGES ON DATABASE crmdb TO crmuser;
\q
EOF
```

### 2. Schema Creation
```bash
# Execute schema creation script
psql -h localhost -U crmuser -d crmdb -f scripts/postgresql/create_schema.sql
```

### 3. Data Migration (if needed)
```bash
# Export from H2 (if you have existing data)
# Connect to H2 and export tables to CSV

# Import to PostgreSQL
psql -h localhost -U crmuser -d crmdb -f scripts/postgresql/migrate_data.sql
```

### 4. Application Configuration
```bash
# Set environment variables
export DB_USERNAME=crmuser
export DB_PASSWORD=crmpass
export ENCRYPTION_KEY=your_32_character_encryption_key
export ADMIN_USERNAME=admin
export ADMIN_PASSWORD=your_admin_password

# Run with production profile
java -jar target/sales-crm-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## Docker Quick Start

```bash
# Clone repository
git clone https://github.com/sikhumbuzot-blip/pasp-ict-crm.git
cd pasp-ict-crm/docker/postgresql

# Configure environment
cp .env.example .env
# Edit .env with your passwords

# Start services
docker-compose up -d

# Check status
docker-compose ps
```

## Verification Commands

```bash
# Test database connection
psql -h localhost -U crmuser -d crmdb -c "SELECT version();"

# Check application health
curl http://localhost:8080/actuator/health

# Verify tables created
psql -h localhost -U crmuser -d crmdb -c "\dt"

# Check sample data
psql -h localhost -U crmuser -d crmdb -c "SELECT COUNT(*) FROM users;"
```

## Backup Commands

```bash
# Create backup
./scripts/postgresql/backup_restore.sh backup full

# List backups
./scripts/postgresql/backup_restore.sh list

# Verify backup
./scripts/postgresql/backup_restore.sh verify /path/to/backup.backup

# Restore backup
./scripts/postgresql/backup_restore.sh restore /path/to/backup.backup full
```

## Troubleshooting Quick Fixes

### Connection Issues
```bash
# Check PostgreSQL status
sudo systemctl status postgresql

# Restart PostgreSQL
sudo systemctl restart postgresql

# Check pg_hba.conf
sudo nano /etc/postgresql/*/main/pg_hba.conf
# Add: host crmdb crmuser 127.0.0.1/32 md5
```

### Application Issues
```bash
# Check application logs
sudo journalctl -u sales-crm -f

# Verify Java version
java -version

# Check port availability
netstat -tlnp | grep 8080
```

### Performance Issues
```bash
# Check database stats
./scripts/postgresql/backup_restore.sh stats

# Monitor connections
psql -h localhost -U crmuser -d crmdb -c "SELECT count(*) FROM pg_stat_activity;"

# Analyze tables
psql -h localhost -U crmuser -d crmdb -c "ANALYZE;"
```

## Key Configuration Files

| File | Purpose |
|------|---------|
| `src/main/resources/application-prod.properties` | Production configuration |
| `scripts/postgresql/create_database.sql` | Database creation |
| `scripts/postgresql/create_schema.sql` | Schema creation |
| `scripts/postgresql/migrate_data.sql` | Data migration |
| `docker/postgresql/docker-compose.yml` | Docker deployment |
| `Dockerfile` | Application container |

## Default Credentials

| Service | Username | Password | Notes |
|---------|----------|----------|-------|
| Database | crmuser | crmpass | Change in production |
| Application | admin | admin123 | Change via env vars |
| PgAdmin | admin@salescrm.com | admin123 | Docker only |

## Important URLs

| Service | URL | Description |
|---------|-----|-------------|
| Application | http://localhost:8080 | Main application |
| Health Check | http://localhost:8080/actuator/health | Application health |
| PgAdmin | http://localhost:8081 | Database admin (Docker) |
| H2 Console | http://localhost:8080/h2-console | Development only |

## Environment Variables

```bash
# Required for production
DB_USERNAME=crmuser
DB_PASSWORD=your_secure_password
ENCRYPTION_KEY=your_32_character_key
ADMIN_USERNAME=admin
ADMIN_PASSWORD=your_admin_password

# Optional
MAIL_HOST=smtp.gmail.com
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_email_password
BACKUP_ENABLED=true
NOTIFICATION_ENABLED=true
```

## Migration Timeline

1. **Preparation** (30 minutes)
   - Install PostgreSQL
   - Create database and user
   - Configure environment

2. **Schema Migration** (10 minutes)
   - Run schema creation script
   - Verify tables and indexes

3. **Data Migration** (varies)
   - Export from H2 (if applicable)
   - Import to PostgreSQL
   - Verify data integrity

4. **Application Deployment** (15 minutes)
   - Update configuration
   - Deploy application
   - Test functionality

5. **Verification** (15 minutes)
   - Test all features
   - Verify backups
   - Monitor performance

**Total Estimated Time: 1-2 hours**

## Support Resources

- **Full Migration Guide**: [POSTGRESQL_MIGRATION_GUIDE.md](POSTGRESQL_MIGRATION_GUIDE.md)
- **Deployment Guide**: [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)
- **Application README**: [README.md](../README.md)
- **PostgreSQL Documentation**: https://www.postgresql.org/docs/
- **Spring Boot Documentation**: https://docs.spring.io/spring-boot/docs/current/reference/html/