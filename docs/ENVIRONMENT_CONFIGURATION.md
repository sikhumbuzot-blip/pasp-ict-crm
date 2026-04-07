# Environment Configuration Guide

## Overview

This guide provides comprehensive information about configuring the Sales CRM Application for different environments (development, testing, production). The application supports multiple deployment scenarios with environment-specific configurations.

## Environment Profiles

The application supports the following Spring profiles:

- **default**: Basic configuration using H2 in-memory database
- **dev**: Development environment with H2 database and debug logging
- **test**: Testing environment with H2 database and test-specific settings
- **prod**: Production environment with PostgreSQL database and optimized settings

## Configuration Files

### Core Configuration Files

| File | Purpose | Environment |
|------|---------|-------------|
| `application.properties` | Base configuration | All |
| `application-dev.properties` | Development overrides | Development |
| `application-test.properties` | Testing overrides | Testing |
| `application-prod.properties` | Production overrides | Production |
| `logback-spring.xml` | Logging configuration | All |

### Docker Configuration Files

| File | Purpose |
|------|---------|
| `Dockerfile` | Application container definition |
| `docker/postgresql/docker-compose.yml` | Complete stack deployment |
| `docker/postgresql/.env.example` | Environment variables template |

## Environment Variables

### Required Environment Variables

#### Database Configuration
```bash
# PostgreSQL Database (Production)
DB_USERNAME=crmuser                    # Database username
DB_PASSWORD=your_secure_password       # Database password (REQUIRED)
DB_HOST=localhost                      # Database host
DB_PORT=5432                          # Database port
DB_NAME=crmdb                         # Database name
```

#### Security Configuration
```bash
# Application Security
ENCRYPTION_KEY=your_32_char_key        # 32-character encryption key (REQUIRED)
ADMIN_USERNAME=admin                   # Default admin username
ADMIN_PASSWORD=secure_password         # Default admin password (REQUIRED)
```

#### Email Configuration (Optional)
```bash
# SMTP Configuration
MAIL_HOST=smtp.gmail.com              # SMTP server host
MAIL_PORT=587                         # SMTP server port
MAIL_USERNAME=your_email@gmail.com    # SMTP username
MAIL_PASSWORD=your_email_password     # SMTP password
```

#### Backup Configuration (Optional)
```bash
# Backup Settings
BACKUP_DIRECTORY=/var/backups/salescrm # Backup storage directory
BACKUP_RETENTION_DAYS=30              # Days to retain backups
BACKUP_ENABLED=true                   # Enable/disable backups
```

#### Notification Configuration (Optional)
```bash
# System Notifications
NOTIFICATION_ENABLED=true             # Enable notifications
NOTIFICATION_FROM_EMAIL=noreply@company.com # From email address
NOTIFICATION_SYSTEM_NAME=Sales CRM    # System name in notifications
NOTIFICATION_ADMIN_EMAILS=admin@company.com # Admin notification emails
```

### Optional Environment Variables

#### Application Configuration
```bash
# Server Configuration
PORT=8080                             # Application port (default: 8080)
JAVA_OPTS=-Xmx512m -Xms256m          # JVM options

# Session Configuration
SESSION_TIMEOUT=30m                   # Session timeout (default: 30m)

# File Upload Configuration
MAX_FILE_SIZE=10MB                    # Maximum file upload size
MAX_REQUEST_SIZE=10MB                 # Maximum request size
```

#### Docker-Specific Variables
```bash
# PgAdmin Configuration (Docker only)
PGADMIN_EMAIL=admin@company.com       # PgAdmin login email
PGADMIN_PASSWORD=admin_password       # PgAdmin login password

# Container Configuration
SPRING_PROFILES_ACTIVE=prod           # Active Spring profile
```

## Environment-Specific Configurations

### Development Environment

**Profile**: `dev`
**Database**: H2 in-memory
**Purpose**: Local development and debugging

#### Key Features:
- H2 console enabled at `/h2-console`
- Debug logging enabled
- Hot reload for templates
- Extended session timeout (60 minutes)
- Detailed error messages

#### Setup:
```bash
# Run with development profile
java -jar sales-crm.jar --spring.profiles.active=dev

# Or set environment variable
export SPRING_PROFILES_ACTIVE=dev
java -jar sales-crm.jar
```

#### Configuration Highlights:
```properties
# H2 Database
spring.datasource.url=jdbc:h2:mem:crmdb
spring.h2.console.enabled=true

# Debug Logging
logging.level.com.pasp.ict.salescrm=DEBUG
logging.level.org.hibernate.SQL=DEBUG

# Development Security
server.servlet.session.cookie.secure=false
```

### Testing Environment

**Profile**: `test`
**Database**: H2 in-memory (isolated)
**Purpose**: Automated testing and CI/CD

#### Key Features:
- Isolated test database
- Minimal logging
- Fast startup
- Test data initialization
- Mock external services

#### Setup:
```bash
# Run tests
mvn test

# Run with test profile
java -jar sales-crm.jar --spring.profiles.active=test
```

#### Configuration Highlights:
```properties
# Test Database
spring.datasource.url=jdbc:h2:mem:testdb

# Minimal Logging
logging.level.root=WARN
logging.level.com.pasp.ict.salescrm=INFO

# Test Security
spring.security.user.password=test123
```

### Production Environment

**Profile**: `prod`
**Database**: PostgreSQL
**Purpose**: Live production deployment

#### Key Features:
- PostgreSQL database
- Optimized performance settings
- Security hardening
- Comprehensive monitoring
- Automated backups

#### Setup:
```bash
# Set required environment variables
export DB_PASSWORD=your_secure_password
export ENCRYPTION_KEY=your_32_character_key
export ADMIN_PASSWORD=your_admin_password

# Run with production profile
java -jar sales-crm.jar --spring.profiles.active=prod
```

#### Configuration Highlights:
```properties
# PostgreSQL Database
spring.datasource.url=jdbc:postgresql://localhost:5432/crmdb
spring.jpa.hibernate.ddl-auto=validate

# Production Security
server.servlet.session.cookie.secure=true
server.error.include-message=never

# Connection Pooling
spring.datasource.hikari.maximum-pool-size=20
```

## Docker Deployment Configuration

### Docker Compose Setup

The application includes a complete Docker Compose configuration for production deployment:

```yaml
# docker/postgresql/docker-compose.yml
services:
  postgresql:     # PostgreSQL database
  pgadmin:        # Database administration
  sales-crm-app:  # Application container
```

#### Environment File Setup:
```bash
# Copy environment template
cp docker/postgresql/.env.example docker/postgresql/.env

# Edit environment variables
nano docker/postgresql/.env
```

#### Required Environment Variables for Docker:
```bash
# Database
DB_PASSWORD=your_secure_password

# Application Security
ENCRYPTION_KEY=your_32_character_key
ADMIN_USERNAME=admin
ADMIN_PASSWORD=your_admin_password

# PgAdmin
PGADMIN_EMAIL=admin@company.com
PGADMIN_PASSWORD=admin_password
```

### Docker Deployment Commands

```bash
# Navigate to Docker configuration
cd docker/postgresql

# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down

# Rebuild and restart
docker-compose up -d --build
```

### Container Health Checks

The Docker configuration includes health checks for all services:

- **PostgreSQL**: Database connectivity check
- **Application**: HTTP health endpoint check
- **PgAdmin**: Web interface availability check

## Security Configuration

### Production Security Settings

#### Database Security:
- Dedicated database user with minimal privileges
- Strong password requirements
- SSL/TLS connections (configurable)
- Connection pooling with limits

#### Application Security:
- Secure session cookies
- CSRF protection enabled
- Input validation and sanitization
- Encrypted sensitive data storage

#### Environment Security:
- Sensitive data in environment variables
- No hardcoded passwords
- Secure default configurations
- Regular security updates

### Security Checklist

- [ ] Strong database password set
- [ ] Unique encryption key generated
- [ ] Admin password changed from default
- [ ] SSL/TLS configured for database connections
- [ ] Firewall rules configured
- [ ] Regular backup verification
- [ ] Security monitoring enabled

## Performance Configuration

### Database Performance

#### Connection Pool Settings:
```properties
# Optimal settings for production
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.connection-timeout=20000
```

#### PostgreSQL Tuning:
```ini
# postgresql.conf recommendations
shared_buffers = 256MB
effective_cache_size = 1GB
work_mem = 4MB
maintenance_work_mem = 64MB
```

### Application Performance

#### JVM Settings:
```bash
# Recommended JVM options
JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:+UseContainerSupport"
```

#### Caching Configuration:
```properties
# Template caching (production)
spring.thymeleaf.cache=true

# Static resource caching
spring.web.resources.cache.cachecontrol.max-age=31536000
```

## Monitoring and Logging

### Application Monitoring

#### Health Endpoints:
- `/actuator/health` - Overall application health
- `/actuator/health/db` - Database connectivity
- `/actuator/info` - Application information

#### Logging Configuration:
```xml
<!-- logback-spring.xml -->
<springProfile name="prod">
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/sales-crm.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/sales-crm.%d{yyyy-MM-dd}.%i.gz</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
    </appender>
</springProfile>
```

### Database Monitoring

#### PostgreSQL Monitoring:
```sql
-- Monitor active connections
SELECT count(*) FROM pg_stat_activity WHERE state = 'active';

-- Monitor database size
SELECT pg_size_pretty(pg_database_size('crmdb'));

-- Monitor slow queries
SELECT query, mean_time, calls 
FROM pg_stat_statements 
ORDER BY mean_time DESC LIMIT 10;
```

## Backup and Recovery

### Automated Backup Configuration

#### Application Backup Settings:
```properties
# Backup configuration
app.backup.directory=/var/backups/salescrm
app.backup.retention.days=30
app.backup.enabled=true
```

#### Database Backup Script:
```bash
# Automated PostgreSQL backup
pg_dump -h localhost -U crmuser -d crmdb > backup_$(date +%Y%m%d_%H%M%S).sql
```

### Backup Verification

#### Backup Testing:
```bash
# Test backup restoration
./scripts/postgresql/backup_restore.sh verify /path/to/backup.sql

# Restore to test environment
./scripts/postgresql/backup_restore.sh restore /path/to/backup.sql test
```

## Troubleshooting

### Common Configuration Issues

#### Database Connection Issues:
```bash
# Test database connectivity
psql -h localhost -U crmuser -d crmdb

# Check application logs
tail -f logs/sales-crm.log

# Verify environment variables
env | grep DB_
```

#### Application Startup Issues:
```bash
# Check Java version
java -version

# Verify configuration
java -jar sales-crm.jar --spring.profiles.active=prod --debug

# Check port availability
netstat -tlnp | grep 8080
```

#### Docker Issues:
```bash
# Check container status
docker-compose ps

# View container logs
docker-compose logs sales-crm-app

# Restart services
docker-compose restart
```

### Configuration Validation

#### Environment Validation Script:
```bash
#!/bin/bash
# validate-environment.sh

echo "Validating environment configuration..."

# Check required environment variables
required_vars=("DB_PASSWORD" "ENCRYPTION_KEY" "ADMIN_PASSWORD")
for var in "${required_vars[@]}"; do
    if [ -z "${!var}" ]; then
        echo "ERROR: $var is not set"
        exit 1
    fi
done

# Check database connectivity
if ! pg_isready -h ${DB_HOST:-localhost} -p ${DB_PORT:-5432} -U ${DB_USERNAME:-crmuser}; then
    echo "ERROR: Cannot connect to database"
    exit 1
fi

echo "Environment validation successful"
```

## Best Practices

### Configuration Management

1. **Environment Variables**: Use environment variables for sensitive data
2. **Profile Separation**: Keep environment-specific settings in separate files
3. **Default Values**: Provide sensible defaults with environment variable overrides
4. **Documentation**: Document all configuration options and their purposes
5. **Validation**: Validate configuration at startup

### Security Best Practices

1. **Secrets Management**: Never commit secrets to version control
2. **Least Privilege**: Use minimal database privileges
3. **Regular Updates**: Keep dependencies and base images updated
4. **Monitoring**: Monitor for security events and anomalies
5. **Backup Security**: Encrypt and secure backup files

### Deployment Best Practices

1. **Blue-Green Deployment**: Use blue-green deployment for zero downtime
2. **Health Checks**: Implement comprehensive health checks
3. **Rollback Plan**: Have a tested rollback procedure
4. **Monitoring**: Monitor application and infrastructure metrics
5. **Documentation**: Maintain up-to-date deployment documentation

## Conclusion

This configuration guide provides comprehensive information for deploying the Sales CRM Application across different environments. Follow the environment-specific guidelines and security best practices to ensure a successful deployment.

For additional support:
- Review the [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) for step-by-step deployment instructions
- Check the [POSTGRESQL_MIGRATION_GUIDE.md](POSTGRESQL_MIGRATION_GUIDE.md) for database-specific configuration
- Refer to the application logs for troubleshooting information