# Troubleshooting Guide

## Overview

This guide provides comprehensive troubleshooting information for the Sales CRM Application, covering common issues, diagnostic procedures, and solutions.

## Quick Diagnostic Checklist

Before diving into specific issues, run through this quick checklist:

1. **System Requirements**
   - [ ] Java 17 or higher installed
   - [ ] Maven 3.6+ available
   - [ ] Sufficient memory (2GB+ recommended)
   - [ ] Port 8080 available (or alternative configured)

2. **Basic Connectivity**
   - [ ] Application starts without errors
   - [ ] Can access http://localhost:8080
   - [ ] Login page loads correctly
   - [ ] Database connection established

3. **Authentication**
   - [ ] Can log in with default credentials (admin/admin123)
   - [ ] Session persists during navigation
   - [ ] Role-based access working correctly

## Application Startup Issues

### Issue: Application Fails to Start

**Symptoms:**
- Spring Boot startup errors
- Port binding failures
- ClassNotFoundException errors
- OutOfMemoryError

**Diagnostic Steps:**
```bash
# Check Java version
java -version
# Should show Java 17 or higher

# Check Maven version
mvn -version
# Should show Maven 3.6 or higher

# Check port availability
netstat -tulpn | grep :8080
# Should show no results if port is free

# Check system memory
free -h
# Should show at least 2GB available
```

**Solutions:**

1. **Port Already in Use:**
```bash
# Find process using port 8080
sudo lsof -i :8080

# Kill the process
sudo kill -9 <PID>

# Or use different port
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

2. **Java Version Issues:**
```bash
# Install Java 17 (Ubuntu/Debian)
sudo apt update
sudo apt install openjdk-17-jdk

# Set JAVA_HOME
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
```

3. **Memory Issues:**
```bash
# Increase JVM memory
export MAVEN_OPTS="-Xmx2g -Xms1g"
mvn spring-boot:run

# Or set in application.properties
# server.tomcat.max-threads=50
# spring.jpa.hibernate.ddl-auto=validate
```

4. **Dependency Issues:**
```bash
# Clean and rebuild
mvn clean install

# Force update dependencies
mvn clean install -U

# Skip tests if needed
mvn clean install -DskipTests
```

### Issue: Slow Startup

**Symptoms:**
- Application takes more than 30 seconds to start
- High CPU usage during startup
- Memory warnings

**Solutions:**
```bash
# Enable startup optimization
export JAVA_OPTS="-XX:+UseG1GC -XX:+UseStringDeduplication"

# Reduce Hibernate validation
# Add to application.properties:
# spring.jpa.hibernate.ddl-auto=none
# spring.jpa.show-sql=false

# Profile startup time
mvn spring-boot:run -Dspring-boot.run.arguments=--debug
```

## Database Connection Issues

### Issue: H2 Database Problems

**Symptoms:**
- "Unable to obtain JDBC Connection"
- H2 console not accessible
- Data not persisting between restarts

**Diagnostic Steps:**
```bash
# Check H2 console access
curl -I http://localhost:8080/h2-console
# Should return 200 OK

# Check database URL in logs
grep "jdbc:h2" logs/sales-crm.log

# Verify data directory
ls -la data/
```

**Solutions:**

1. **H2 Console Access:**
```properties
# Ensure these properties in application-dev.properties
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
spring.h2.console.settings.web-allow-others=true
```

2. **Connection Parameters:**
- JDBC URL: `jdbc:h2:mem:crmdb`
- Username: `sa`
- Password: `password`
- Driver Class: `org.h2.Driver`

3. **Data Persistence Issues:**
```properties
# For file-based H2 (persistent)
spring.datasource.url=jdbc:h2:file:./data/crmdb
# For in-memory H2 (temporary)
spring.datasource.url=jdbc:h2:mem:crmdb
```

### Issue: PostgreSQL Connection Problems

**Symptoms:**
- Connection refused errors
- Authentication failures
- SSL connection errors

**Diagnostic Steps:**
```bash
# Check PostgreSQL service
sudo systemctl status postgresql

# Test connection
psql -h localhost -U crmuser -d crmdb -c "SELECT version();"

# Check PostgreSQL logs
sudo tail -f /var/log/postgresql/postgresql-*.log

# Verify environment variables
echo $DB_USERNAME
echo $DB_PASSWORD
echo $DB_URL
```

**Solutions:**

1. **Service Not Running:**
```bash
# Start PostgreSQL
sudo systemctl start postgresql
sudo systemctl enable postgresql

# Check status
sudo systemctl status postgresql
```

2. **Database/User Setup:**
```bash
# Create database and user
sudo -u postgres psql << EOF
CREATE DATABASE crmdb;
CREATE USER crmuser WITH PASSWORD 'crmpass';
GRANT ALL PRIVILEGES ON DATABASE crmdb TO crmuser;
\q
EOF
```

3. **Connection Configuration:**
```properties
# application-prod.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/crmdb
spring.datasource.username=${DB_USERNAME:crmuser}
spring.datasource.password=${DB_PASSWORD:crmpass}
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

4. **SSL Issues:**
```properties
# Disable SSL for local development
spring.datasource.url=jdbc:postgresql://localhost:5432/crmdb?sslmode=disable
```

## Authentication and Security Issues

### Issue: Cannot Log In

**Symptoms:**
- "Invalid username or password" with correct credentials
- Login form redirects back to login
- Session immediately expires

**Diagnostic Steps:**
```bash
# Check user in database (H2 Console)
SELECT * FROM users WHERE username = 'admin';

# Check password encoding
SELECT username, password FROM users;

# Check security logs
grep "authentication" logs/sales-crm.log

# Check session configuration
grep "session" logs/sales-crm.log
```

**Solutions:**

1. **Reset Default User (H2 only):**
```sql
-- In H2 Console, run:
DELETE FROM users WHERE username = 'admin';
-- Restart application to recreate default user
```

2. **Password Encoding Issues:**
```java
// Passwords should be BCrypt encoded
// Check if password starts with $2a$ or $2b$
```

3. **Session Configuration:**
```properties
# application.properties
server.servlet.session.timeout=30m
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.secure=false
```

4. **Browser Issues:**
```bash
# Clear browser cache and cookies
# Try incognito/private browsing
# Disable browser extensions
# Check browser developer tools for errors
```

### Issue: Access Denied Errors

**Symptoms:**
- "Access Denied" for authorized users
- Role-based restrictions not working
- CSRF token errors

**Solutions:**

1. **Role Assignment:**
```sql
-- Check user roles in database
SELECT username, role FROM users;

-- Update user role if needed
UPDATE users SET role = 'ADMIN' WHERE username = 'admin';
```

2. **CSRF Token Issues:**
```html
<!-- Ensure forms include CSRF token -->
<input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>
```

3. **Session Timeout:**
```properties
# Increase session timeout
server.servlet.session.timeout=60m
```

## Performance Issues

### Issue: Slow Page Loading

**Symptoms:**
- Pages take more than 5 seconds to load
- Database queries timing out
- High memory usage

**Diagnostic Steps:**
```bash
# Enable SQL logging
# Add to application.properties:
# logging.level.org.hibernate.SQL=DEBUG
# logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE

# Monitor memory usage
jstat -gc <PID>

# Check database performance
# For PostgreSQL:
# SELECT * FROM pg_stat_activity;

# Monitor application metrics
curl http://localhost:8080/actuator/metrics
```

**Solutions:**

1. **Database Optimization:**
```sql
-- Add indexes for frequently queried columns
CREATE INDEX idx_customer_email ON customers(email);
CREATE INDEX idx_lead_status ON leads(status);
CREATE INDEX idx_transaction_date ON sale_transactions(sale_date);
```

2. **JVM Tuning:**
```bash
# Optimize garbage collection
export JAVA_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Increase heap size
export JAVA_OPTS="-Xmx4g -Xms2g"
```

3. **Connection Pool Tuning:**
```properties
# application.properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
```

### Issue: High Memory Usage

**Symptoms:**
- OutOfMemoryError exceptions
- Frequent garbage collection
- Application becomes unresponsive

**Solutions:**

1. **Memory Analysis:**
```bash
# Generate heap dump
jcmd <PID> GC.run_finalization
jcmd <PID> VM.gc

# Analyze with tools like Eclipse MAT
```

2. **Configuration Optimization:**
```properties
# Reduce Hibernate cache
spring.jpa.properties.hibernate.cache.use_second_level_cache=false
spring.jpa.properties.hibernate.cache.use_query_cache=false

# Optimize batch processing
spring.jpa.properties.hibernate.jdbc.batch_size=25
spring.jpa.properties.hibernate.order_inserts=true
```

## Data and Backup Issues

### Issue: Backup Creation Fails

**Symptoms:**
- Backup process hangs or fails
- Insufficient disk space errors
- Permission denied errors

**Diagnostic Steps:**
```bash
# Check disk space
df -h

# Check backup directory permissions
ls -la backups/

# Check backup service logs
grep "backup" logs/sales-crm.log

# Manual backup test
pg_dump -h localhost -U crmuser crmdb > test_backup.sql
```

**Solutions:**

1. **Disk Space:**
```bash
# Clean old backups
find backups/ -name "backup_*" -mtime +30 -delete

# Move backups to external storage
rsync -av backups/ /external/storage/backups/
```

2. **Permissions:**
```bash
# Fix backup directory permissions
sudo chown -R $USER:$USER backups/
chmod 755 backups/
```

3. **PostgreSQL Backup Issues:**
```bash
# Ensure pg_dump is available
which pg_dump

# Test connection
pg_dump --version
psql -h localhost -U crmuser -d crmdb -c "SELECT 1;"
```

### Issue: Data Corruption or Loss

**Symptoms:**
- Missing records
- Inconsistent data
- Foreign key constraint errors

**Solutions:**

1. **Data Integrity Check:**
```sql
-- Check for orphaned records
SELECT * FROM leads WHERE customer_id NOT IN (SELECT id FROM customers);
SELECT * FROM sale_transactions WHERE customer_id NOT IN (SELECT id FROM customers);

-- Check for duplicate emails
SELECT email, COUNT(*) FROM customers GROUP BY email HAVING COUNT(*) > 1;
```

2. **Restore from Backup:**
```bash
# List available backups
ls -la backups/

# Restore from backup (PostgreSQL)
psql -h localhost -U crmuser -d crmdb < backups/backup_YYYY-MM-DD_HH-MM-SS/database_backup.sql
```

3. **Data Migration:**
```bash
# Use migration scripts
psql -h localhost -U crmuser -d crmdb -f scripts/postgresql/migrate_data.sql
```

## Security and Audit Issues

### Issue: Security Incidents

**Symptoms:**
- Failed login attempts
- Unauthorized access attempts
- Suspicious user activity

**Diagnostic Steps:**
```bash
# Check security events
# Via web interface: Admin → Security Incidents

# Check audit logs
grep "FAILED_LOGIN\|UNAUTHORIZED" logs/sales-crm.log

# Check system access logs
sudo tail -f /var/log/auth.log
```

**Solutions:**

1. **Review Security Events:**
```sql
-- Check recent security events
SELECT * FROM audit_logs 
WHERE action LIKE '%FAILED%' OR action LIKE '%SECURITY%'
ORDER BY timestamp DESC LIMIT 50;
```

2. **Strengthen Security:**
```properties
# application.properties
# Increase session timeout for security
server.servlet.session.timeout=15m

# Enable additional security headers
security.headers.frame=DENY
security.headers.content-type=nosniff
```

3. **Monitor and Alert:**
```bash
# Set up log monitoring
tail -f logs/sales-crm.log | grep "FAILED_LOGIN"

# Configure email alerts for security events
# (Implementation depends on notification service)
```

## Network and Connectivity Issues

### Issue: Cannot Access Application

**Symptoms:**
- Connection refused errors
- Timeout errors
- DNS resolution issues

**Solutions:**

1. **Firewall Configuration:**
```bash
# Check firewall status
sudo ufw status

# Allow port 8080
sudo ufw allow 8080

# For production with reverse proxy
sudo ufw allow 80
sudo ufw allow 443
```

2. **Network Configuration:**
```bash
# Check if application is listening
netstat -tulpn | grep :8080

# Test local connectivity
curl -I http://localhost:8080

# Test from another machine
curl -I http://<server-ip>:8080
```

3. **Reverse Proxy Issues (Nginx):**
```nginx
# /etc/nginx/sites-available/sales-crm
server {
    listen 80;
    server_name your-domain.com;
    
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## Docker and Container Issues

### Issue: Docker Container Problems

**Symptoms:**
- Container fails to start
- Database connection issues in container
- Volume mounting problems

**Solutions:**

1. **Container Startup:**
```bash
# Check container logs
docker logs sales-crm-app

# Check container status
docker ps -a

# Restart container
docker restart sales-crm-app
```

2. **Database Connection in Docker:**
```bash
# Check network connectivity
docker exec -it sales-crm-app ping postgres-db

# Check environment variables
docker exec -it sales-crm-app env | grep DB_
```

3. **Volume Issues:**
```bash
# Check volume mounts
docker inspect sales-crm-app | grep -A 10 "Mounts"

# Fix permissions
sudo chown -R 1000:1000 ./data
```

## Log Analysis and Monitoring

### Important Log Locations

- **Application Logs:** `logs/sales-crm.log`
- **Archived Logs:** `logs/sales-crm.YYYY-MM-DD.*.gz`
- **System Logs:** `/var/log/syslog` (Linux)
- **PostgreSQL Logs:** `/var/log/postgresql/`
- **Nginx Logs:** `/var/log/nginx/`

### Log Analysis Commands

```bash
# Search for errors
grep -i error logs/sales-crm.log

# Search for specific user activity
grep "username=admin" logs/sales-crm.log

# Monitor logs in real-time
tail -f logs/sales-crm.log

# Search for database errors
grep -i "sql\|database\|connection" logs/sales-crm.log

# Search for security events
grep -i "authentication\|authorization\|security" logs/sales-crm.log

# Analyze performance issues
grep -i "slow\|timeout\|performance" logs/sales-crm.log
```

### Log Level Configuration

```properties
# application.properties
# Root logging level
logging.level.root=INFO

# Application-specific logging
logging.level.com.pasp.ict.salescrm=DEBUG

# Security logging
logging.level.org.springframework.security=DEBUG

# Database logging
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE

# Performance logging
logging.level.org.springframework.web=DEBUG
```

## Getting Help

### Before Seeking Help

1. **Check this troubleshooting guide**
2. **Review application logs**
3. **Verify system requirements**
4. **Test with minimal configuration**
5. **Document error messages and steps to reproduce**

### Information to Provide

When seeking help, include:

- **System Information:**
  - Operating system and version
  - Java version (`java -version`)
  - Maven version (`mvn -version`)
  - Available memory and disk space

- **Application Information:**
  - Application version/commit hash
  - Configuration files (sanitized)
  - Startup command used
  - Environment variables (sanitized)

- **Error Information:**
  - Complete error messages
  - Stack traces
  - Log excerpts (with timestamps)
  - Steps to reproduce the issue

- **Environment Information:**
  - Database type and version
  - Network configuration
  - Security settings
  - Recent changes made

### Support Channels

1. **GitHub Issues:** Create detailed bug reports
2. **Documentation:** Review README and docs/
3. **Community:** Stack Overflow with relevant tags
4. **Professional Support:** Consider hiring Spring Boot consultants

### Emergency Procedures

For critical production issues:

1. **Immediate Actions:**
   - Check system resources (CPU, memory, disk)
   - Review recent logs for errors
   - Verify database connectivity
   - Check backup status

2. **Rollback Procedures:**
   - Stop application
   - Restore from last known good backup
   - Revert to previous application version
   - Restart with minimal configuration

3. **Communication:**
   - Notify stakeholders of the issue
   - Document timeline and actions taken
   - Prepare incident report
   - Plan post-incident review