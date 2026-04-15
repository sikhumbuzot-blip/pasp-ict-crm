# Sales CRM Application

A comprehensive web-based Customer Relationship Management system built with Spring Boot and Thymeleaf.

## Quick Reference

### Default Credentials
- **Username:** `admin`
- **Password:** `admin123`
- **Role:** `ADMIN`

⚠️ **Important:** Change default credentials immediately in production!

### Key URLs
- **Application:** http://localhost:8080
- **API Endpoints:** http://localhost:8080/api/*

### Essential Commands
```bash
# Start application
mvn spring-boot:run

# Start with different profile
mvn spring-boot:run -Dspring-boot.run.profiles=prod

# Run tests
mvn test

# Build for production
mvn clean package -DskipTests
```

### Quick Setup (PostgreSQL)
```bash
# Setup PostgreSQL database
./scripts/setup-postgresql.sh

# Start application
mvn spring-boot:run

# Access at http://localhost:8080
# Login: admin / admin123
```

### Alternative Setup Methods
```bash
# Docker deployment (includes PostgreSQL)
cd docker/postgresql && docker-compose up -d

# Manual PostgreSQL setup
sudo apt install postgresql postgresql-contrib
sudo -u postgres createdb crmdb_dev
sudo -u postgres createuser crmuser
# Then run: mvn spring-boot:run
```

### Quick Setup (Docker)
```bash
cd docker/postgresql
cp .env.example .env
# Edit .env with your passwords
docker-compose up -d
```

### Emergency Procedures
```bash
# Check application logs
tail -f logs/sales-crm.log

# Backup database (PostgreSQL)
pg_dump -h localhost -U crmuser crmdb > backup.sql
```

## Features

- **Role-Based Access Control**: Admin, Sales, and Regular user roles with appropriate permissions
- **Sales Pipeline Management**: Lead tracking, opportunity management, and sales transaction processing
- **Customer Data Management**: Complete customer profiles with interaction history
- **Administrative Dashboard**: User management, system statistics, and reporting
- **Security**: Authentication, authorization, data encryption, and audit logging
- **Database Support**: PostgreSQL for all environments with H2 for testing only

## Technology Stack

- **Framework**: Spring Boot 3.x
- **Template Engine**: Thymeleaf
- **Security**: Spring Security
- **Database**: PostgreSQL (production) / H2 (testing only)
- **ORM**: Spring Data JPA with Hibernate
- **Build Tool**: Maven
- **Testing**: JUnit 5, Mockito, jqwik (property-based testing)

## Quick Start

### Prerequisites

- Java 17 or higher
- PostgreSQL 12 or higher
- Maven 3.6 or higher

### Running the Application

1. Clone the repository:
   ```bash
   git clone https://github.com/sikhumbuzot-blip/pasp-ict-crm.git
   cd pasp-ict-crm
   ```

2. Set up PostgreSQL database:
   ```bash
   ./scripts/setup-postgresql.sh
   ```

3. Run with Maven:
   ```bash
   mvn spring-boot:run
   ```

4. Access the application:
   - Application: http://localhost:8080
   - Login: admin / admin123

### Development Mode

Run with development profile for enhanced debugging:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Testing

Run all tests:
```bash
mvn test
```

Run with coverage report:
```bash
mvn test jacoco:report
```

## Deployment

The application supports multiple deployment scenarios with production-ready configurations:

### 🚀 Quick Start Options

#### Docker Deployment (Recommended)
```bash
# Clone and navigate to Docker configuration
git clone https://github.com/sikhumbuzot-blip/pasp-ict-crm.git
cd pasp-ict-crm/docker/postgresql

# Configure environment
cp .env.example .env
nano .env  # Update with your secure passwords

# Deploy the complete stack
docker-compose up -d

# Access at http://localhost:8080
```

#### Kubernetes Deployment
```bash
# Navigate to Kubernetes configuration
cd k8s

# Update configuration
nano secret.yaml     # Update with base64 encoded secrets
nano ingress.yaml    # Update with your domain

# Deploy using the provided script
chmod +x deploy.sh
./deploy.sh deploy
```

#### Traditional Deployment
```bash
# Development with H2
mvn spring-boot:run

# Production with PostgreSQL
mvn spring-boot:run -Dspring.profiles.active=prod
```

### 📚 Documentation

**Deployment Documentation:**
- **[DEPLOYMENT_README.md](DEPLOYMENT_README.md)** - Quick start and overview
- **[docs/DEPLOYMENT_GUIDE.md](docs/DEPLOYMENT_GUIDE.md)** - Comprehensive deployment guide
- **[docs/KUBERNETES_DEPLOYMENT_GUIDE.md](docs/KUBERNETES_DEPLOYMENT_GUIDE.md)** - Kubernetes-specific instructions
- **[docs/ENVIRONMENT_CONFIGURATION.md](docs/ENVIRONMENT_CONFIGURATION.md)** - Environment configuration details
- **[docs/DEPLOYMENT_CONFIGURATION_SUMMARY.md](docs/DEPLOYMENT_CONFIGURATION_SUMMARY.md)** - Configuration summary

**User Documentation:**
- **[docs/USER_MANUAL.md](docs/USER_MANUAL.md)** - Complete user guide with step-by-step workflows
- **[docs/API_DOCUMENTATION.md](docs/API_DOCUMENTATION.md)** - Comprehensive API reference and integration guide
- **[docs/FAQ.md](docs/FAQ.md)** - Frequently asked questions and answers
- **[docs/TROUBLESHOOTING_GUIDE.md](docs/TROUBLESHOOTING_GUIDE.md)** - Detailed troubleshooting and diagnostic procedures

**Migration Documentation:**
- **[docs/POSTGRESQL_MIGRATION_GUIDE.md](docs/POSTGRESQL_MIGRATION_GUIDE.md)** - Complete migration process from H2 to PostgreSQL
- **[docs/MIGRATION_QUICK_REFERENCE.md](docs/MIGRATION_QUICK_REFERENCE.md)** - Quick commands and checklist

### 🔧 Configuration Options

| Deployment Type | Best For | Database | Scaling |
|----------------|----------|----------|---------|
| **Docker** | Development, Testing | PostgreSQL Container | Manual |
| **Kubernetes** | Production, Cloud | PostgreSQL Pod | Auto-scaling |
| **Traditional** | On-premises | External PostgreSQL | Manual |

## Configuration

### Environment Profiles

- **default**: Production environment with PostgreSQL database
- **dev**: Development environment with PostgreSQL and debug logging
- **test**: Testing environment with H2 in-memory database
- **prod**: Production environment with optimized PostgreSQL settings

### Database Configuration

#### PostgreSQL (Default)
- URL: `jdbc:postgresql://localhost:5432/crmdb_dev`
- Username: Set via `DB_USERNAME` environment variable (default: crmuser)
- Password: Set via `DB_PASSWORD` environment variable (default: crmpass)

#### H2 (Testing Only)
- URL: `jdbc:h2:mem:testdb`
- Used only for automated testing
- Console: Not available in production builds

### PostgreSQL Migration

For production deployment with PostgreSQL, see the comprehensive migration guides:

- **[PostgreSQL Migration Guide](docs/POSTGRESQL_MIGRATION_GUIDE.md)** - Complete migration process from H2 to PostgreSQL
- **[Deployment Guide](docs/DEPLOYMENT_GUIDE.md)** - Deployment instructions for various environments
- **[Migration Quick Reference](docs/MIGRATION_QUICK_REFERENCE.md)** - Quick commands and checklist

#### Quick PostgreSQL Setup

1. Install PostgreSQL and create database:
   ```bash
   sudo apt install postgresql postgresql-contrib
   sudo -u postgres psql -f scripts/postgresql/create_database.sql
   ```

2. Create schema:
   ```bash
   psql -h localhost -U crmuser -d crmdb -f scripts/postgresql/create_schema.sql
   ```

3. Run application with production profile:
   ```bash
   export DB_USERNAME=crmuser
   export DB_PASSWORD=crmpass
   export ENCRYPTION_KEY=your_32_character_encryption_key
   java -jar target/sales-crm-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
   ```

#### Docker Deployment

For containerized deployment with PostgreSQL:

```bash
cd docker/postgresql
cp .env.example .env
# Edit .env with your configuration
docker-compose up -d
```

Access services:
- Application: http://localhost:8080
- PgAdmin: http://localhost:8081

## Default Credentials

- Username: `admin`
- Password: `admin123`
- Role: `ADMIN`

**Note**: Change default credentials in production environment.

## Project Structure

```
src/
├── main/
│   ├── java/com/pasp/ict/salescrm/
│   │   ├── config/          # Configuration classes
│   │   ├── controller/      # Web controllers
│   │   ├── entity/          # JPA entities
│   │   ├── repository/      # Data repositories
│   │   ├── service/         # Business logic services
│   │   └── security/        # Security configuration
│   └── resources/
│       ├── templates/       # Thymeleaf templates
│       ├── static/          # Static web resources
│       └── application*.properties
└── test/
    └── java/com/pasp/ict/salescrm/
        ├── integration/     # Integration tests
        ├── unit/           # Unit tests
        └── property/       # Property-based tests
```

## API Endpoints

### Authentication Endpoints

| Method | Endpoint | Description | Access Level |
|--------|----------|-------------|--------------|
| GET | `/login` | Login page with error/success messages | Public |
| POST | `/login` | Process login credentials | Public |
| GET | `/logout` | Logout and redirect to login | Authenticated |
| GET | `/dashboard` | Redirect to role-specific dashboard | Authenticated |

### Dashboard Endpoints

| Method | Endpoint | Description | Access Level |
|--------|----------|-------------|--------------|
| GET | `/dashboard/admin` | Admin dashboard with system statistics | Admin |
| GET | `/dashboard/sales` | Sales dashboard with performance metrics | Sales |
| GET | `/dashboard/regular` | Regular user dashboard (read-only) | Regular |
| GET | `/dashboard/metrics` | Real-time metrics API (JSON) | Authenticated |
| GET | `/dashboard/health` | System health API (JSON) | Admin |
| GET | `/dashboard/sales-performance` | Sales performance API (JSON) | Admin, Sales |

### Sales Management Endpoints

| Method | Endpoint | Description | Access Level |
|--------|----------|-------------|--------------|
| GET | `/sales/leads` | Lead listing with filtering | Admin, Sales |
| GET | `/sales/leads/{id}` | Individual lead details | Admin, Sales |
| GET | `/sales/leads/create` | Create new lead form | Admin, Sales |
| POST | `/sales/leads/create` | Process new lead creation | Admin, Sales |
| POST | `/sales/leads/{id}/status` | Update lead status | Admin, Sales |
| GET | `/sales/leads/{id}/convert` | Convert lead to sale form | Admin, Sales |
| POST | `/sales/leads/{id}/convert` | Process lead conversion | Admin, Sales |
| GET | `/sales/transactions` | Sales transaction history | Admin, Sales |
| GET | `/sales/transactions/{id}` | Individual transaction details | Admin, Sales |
| GET | `/sales/transactions/create` | Create direct sale form | Admin, Sales |
| POST | `/sales/transactions/create` | Process direct sale creation | Admin, Sales |
| GET | `/sales/pipeline` | Sales pipeline visualization | Admin, Sales |
| GET | `/sales/metrics` | Sales performance metrics | Admin, Sales |
| GET | `/sales/api/metrics` | Sales metrics API (JSON) | Admin, Sales |
| GET | `/sales/api/pipeline` | Pipeline data API (JSON) | Admin, Sales |

### Customer Management Endpoints

| Method | Endpoint | Description | Access Level |
|--------|----------|-------------|--------------|
| GET | `/customers` | Customer listing and search | Admin, Sales |
| GET | `/customers/{id}` | Customer profile and history | Admin, Sales |
| GET | `/customers/create` | Create new customer form | Admin, Sales |
| POST | `/customers/create` | Process new customer creation | Admin, Sales |
| GET | `/customers/{id}/edit` | Edit customer form | Admin, Sales |
| POST | `/customers/{id}/edit` | Process customer updates | Admin, Sales |
| GET | `/customers/{id}/interactions` | Customer interaction history | Admin, Sales |
| POST | `/customers/{id}/interactions` | Log new interaction | Admin, Sales |
| GET | `/customers/recent-activity` | Customers with recent activity | Admin, Sales |
| GET | `/customers/inactive` | Inactive customers | Admin, Sales |
| GET | `/customers/api/search` | Customer search API (JSON) | Admin, Sales |
| GET | `/customers/api/{id}` | Customer details API (JSON) | Admin, Sales |
| GET | `/customers/api/{id}/interactions` | Customer interactions API (JSON) | Admin, Sales |
| GET | `/customers/api/{id}/stats` | Customer statistics API (JSON) | Admin, Sales |

### Administrative Endpoints

| Method | Endpoint | Description | Access Level |
|--------|----------|-------------|--------------|
| GET | `/admin/users` | User management interface | Admin |
| GET | `/admin/users/{id}` | Individual user management | Admin |
| GET | `/admin/users/create` | Create new user form | Admin |
| POST | `/admin/users/create` | Process new user creation | Admin |
| POST | `/admin/users/{id}/role` | Update user role | Admin |
| POST | `/admin/users/{id}/status` | Activate/deactivate user | Admin |
| GET | `/admin/system` | System configuration and health | Admin |
| GET | `/admin/reports` | Administrative reports | Admin |
| GET | `/admin/statistics` | System statistics dashboard | Admin |
| GET | `/admin/activity` | Recent system activity | Admin |
| GET | `/admin/backups` | Backup management interface | Admin |
| POST | `/admin/backups/create` | Create manual backup | Admin |
| GET | `/admin/audit-logs` | Audit logs interface | Admin |
| GET | `/admin/security-incidents` | Security incidents interface | Admin |

### API Endpoints (JSON Responses)

| Method | Endpoint | Description | Access Level |
|--------|----------|-------------|--------------|
| GET | `/admin/api/statistics` | System statistics (JSON) | Admin |
| GET | `/admin/api/health` | System health (JSON) | Admin |
| GET | `/admin/api/performance` | Performance metrics (JSON) | Admin |
| GET | `/admin/api/sales-performance` | Sales performance (JSON) | Admin |
| GET | `/admin/api/users/{id}/activity` | User activity (JSON) | Admin |
| GET | `/admin/api/activity` | Recent activity (JSON) | Admin |
| GET | `/admin/api/security-events` | Security events (JSON) | Admin |
| GET | `/admin/api/reports/export` | Export reports (PDF/CSV) | Admin |
| GET | `/admin/api/backup-status` | Backup status (JSON) | Admin |
| POST | `/admin/backups/{id}/verify` | Verify backup (JSON) | Admin |
| POST | `/admin/notifications/test` | Test notifications (JSON) | Admin |

## User Workflows

### Getting Started

1. **Initial Login**
   - Navigate to http://localhost:8080
   - Use default credentials: `admin` / `admin123`
   - Change default password immediately in production

2. **First-Time Setup**
   - Create additional user accounts via Admin → Users → Create User
   - Set up customer data via Customers → Create Customer
   - Configure system settings via Admin → System

### Sales User Workflow

1. **Lead Management**
   ```
   Sales → Leads → Create Lead
   ↓
   Fill customer information and estimated value
   ↓
   Track lead through pipeline stages:
   NEW → CONTACTED → QUALIFIED → PROPOSAL → NEGOTIATION → CLOSED_WON/CLOSED_LOST
   ```

2. **Converting Leads to Sales**
   ```
   Sales → Leads → [Select Lead] → Convert to Sale
   ↓
   Enter sale amount and description
   ↓
   Lead automatically marked as CLOSED_WON
   ↓
   Sale transaction created and recorded
   ```

3. **Direct Sales Creation**
   ```
   Sales → Transactions → Create Sale
   ↓
   Select customer and enter sale details
   ↓
   Transaction recorded without lead conversion
   ```

4. **Customer Interaction Tracking**
   ```
   Customers → [Select Customer] → Log Interaction
   ↓
   Choose interaction type (CALL, EMAIL, MEETING, NOTE)
   ↓
   Add detailed notes about the interaction
   ```

### Admin User Workflow

1. **User Management**
   ```
   Admin → Users → Create User
   ↓
   Set username, password, email, and role
   ↓
   Assign appropriate role (ADMIN, SALES, REGULAR)
   ↓
   Monitor user activity and modify roles as needed
   ```

2. **System Monitoring**
   ```
   Admin → System → View Health Metrics
   ↓
   Monitor performance, database status, and security
   ↓
   Review audit logs and security incidents
   ↓
   Create manual backups if needed
   ```

3. **Reporting and Analytics**
   ```
   Admin → Reports → Select Report Type
   ↓
   Choose time period (daily, weekly, monthly, quarterly, yearly)
   ↓
   Export reports in PDF or CSV format
   ↓
   Analyze sales performance by user and time period
   ```

### Regular User Workflow

1. **Read-Only Access**
   - View assigned customer data
   - Access basic dashboard information
   - Limited to viewing permissions only

## Troubleshooting Guide

### Common Issues and Solutions

#### 1. Application Won't Start

**Symptoms:**
- Application fails to start
- Port 8080 already in use
- Database connection errors

**Solutions:**
```bash
# Check if port 8080 is in use
netstat -tulpn | grep :8080

# Kill process using port 8080
sudo kill -9 $(lsof -t -i:8080)

# Start with different port
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081

# Check Java version (requires Java 17+)
java -version

# Verify Maven installation
mvn -version
```

#### 2. Database Connection Issues

**Symptoms:**
- "Unable to obtain JDBC Connection" errors
- H2 console not accessible
- Data not persisting

**Solutions:**
```bash
# For H2 Database Issues:
# 1. Check H2 console at http://localhost:8080/h2-console
# 2. Use these connection details:
#    JDBC URL: jdbc:h2:mem:crmdb
#    Username: sa
#    Password: password

# For PostgreSQL Issues:
# 1. Verify PostgreSQL is running
sudo systemctl status postgresql

# 2. Check database exists
sudo -u postgres psql -l | grep crmdb

# 3. Test connection
psql -h localhost -U crmuser -d crmdb -c "SELECT version();"

# 4. Check environment variables
echo $DB_USERNAME
echo $DB_PASSWORD
```

#### 3. Authentication Problems

**Symptoms:**
- Cannot log in with correct credentials
- Session expires immediately
- Access denied errors

**Solutions:**
```bash
# Reset to default admin user (H2 only)
# Delete data.sql and restart application

# Check user status in database
# H2 Console: SELECT * FROM users WHERE username = 'admin';

# Verify password encoding
# Passwords should be BCrypt encoded in database

# Clear browser cache and cookies
# Try incognito/private browsing mode
```

#### 4. Performance Issues

**Symptoms:**
- Slow page loading
- Database queries taking too long
- High memory usage

**Solutions:**
```bash
# Increase JVM memory
export JAVA_OPTS="-Xmx2g -Xms1g"
mvn spring-boot:run

# Enable SQL logging to identify slow queries
# Add to application.properties:
# logging.level.org.hibernate.SQL=DEBUG
# logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE

# Monitor application performance
# Access actuator endpoints (if enabled):
# http://localhost:8080/actuator/health
# http://localhost:8080/actuator/metrics
```

#### 5. Security and Access Issues

**Symptoms:**
- CSRF token errors
- Cross-origin request blocked
- Unauthorized access attempts

**Solutions:**
```bash
# For CSRF issues:
# Ensure forms include CSRF token
# Check browser developer tools for token presence

# For session issues:
# Clear browser cookies
# Check session timeout configuration

# For security incidents:
# Review Admin → Security Incidents
# Check Admin → Audit Logs for suspicious activity
```

#### 6. Backup and Data Issues

**Symptoms:**
- Backup creation fails
- Data export errors
- Missing or corrupted data

**Solutions:**
```bash
# Manual backup creation
# Navigate to Admin → Backups → Create Backup

# Verify backup integrity
# Use Admin → Backups → Verify function

# Check backup directory permissions
ls -la backups/

# For PostgreSQL backup issues:
# Ensure pg_dump is available
which pg_dump

# Manual database backup
pg_dump -h localhost -U crmuser crmdb > manual_backup.sql
```

### Log File Locations

- **Application Logs:** `logs/sales-crm.log`
- **Archived Logs:** `logs/sales-crm.YYYY-MM-DD.*.gz`
- **Backup Logs:** Check Admin → System → Backup Status
- **Audit Logs:** Available via Admin → Audit Logs interface

### Debug Mode

Enable debug logging for troubleshooting:

```bash
# Run with debug profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Or set logging level in application.properties
logging.level.com.pasp.ict.salescrm=DEBUG
logging.level.org.springframework.security=DEBUG
```

## Frequently Asked Questions (FAQ)

### General Questions

**Q: What are the system requirements?**
A: Java 17 or higher, Maven 3.6+, and 2GB RAM minimum. PostgreSQL 12+ for production deployment.

**Q: Can I use this application commercially?**
A: Yes, this project is licensed under the MIT License, allowing commercial use.

**Q: How do I change the default admin password?**
A: Log in as admin, go to Admin → Users → [admin user] → Edit, and update the password. Always change default credentials in production.

**Q: Is the application mobile-friendly?**
A: Yes, the application uses responsive design and works on tablets and mobile devices, though it's optimized for desktop use.

### Technical Questions

**Q: How do I migrate from H2 to PostgreSQL?**
A: Follow the comprehensive [PostgreSQL Migration Guide](docs/POSTGRESQL_MIGRATION_GUIDE.md) for step-by-step instructions.

**Q: Can I customize the user roles?**
A: The application supports three predefined roles (ADMIN, SALES, REGULAR). Custom roles require code modifications to the UserRole enum and security configuration.

**Q: How do I backup my data?**
A: Use Admin → Backups → Create Backup for manual backups. Automated daily backups are created automatically. For PostgreSQL, use standard pg_dump commands.

**Q: What's the maximum number of users/customers supported?**
A: The application is designed to handle thousands of users and customers. Performance testing shows good results up to 10,000 customer records with sub-2-second search times.

**Q: How do I integrate with external systems?**
A: The application provides REST API endpoints (JSON responses) that can be consumed by external systems. See the API Endpoints section above.

### Security Questions

**Q: How secure is the application?**
A: The application implements Spring Security with BCrypt password hashing, CSRF protection, session management, input validation, SQL injection prevention, and comprehensive audit logging.

**Q: How do I enable HTTPS?**
A: Configure SSL in application.properties or use a reverse proxy like Nginx. See [Deployment Guide](docs/DEPLOYMENT_GUIDE.md) for HTTPS setup instructions.

**Q: What data is encrypted?**
A: Sensitive customer data including contact information and financial details are encrypted at rest using AES encryption.

**Q: How do I monitor security incidents?**
A: Use Admin → Security Incidents to view security events and Admin → Audit Logs to track all system activities.

### Deployment Questions

**Q: Can I deploy this to the cloud?**
A: Yes, the application supports Docker deployment and includes Kubernetes configurations. See [Kubernetes Deployment Guide](docs/KUBERNETES_DEPLOYMENT_GUIDE.md).

**Q: How do I scale the application?**
A: Use the Kubernetes deployment for auto-scaling, or deploy multiple instances behind a load balancer with a shared PostgreSQL database.

**Q: What's the recommended production setup?**
A: PostgreSQL database, reverse proxy (Nginx), SSL/TLS encryption, regular backups, and monitoring. See [Deployment Guide](docs/DEPLOYMENT_GUIDE.md) for details.

**Q: How do I update the application?**
A: Stop the application, backup your data, update the JAR file, run database migrations if needed, and restart. Always test updates in a staging environment first.

### Development Questions

**Q: How do I add new features?**
A: Follow Spring Boot conventions, add tests for new functionality, update documentation, and ensure all existing tests pass.

**Q: What testing frameworks are used?**
A: JUnit 5 for unit tests, Spring Boot Test for integration tests, and jqwik for property-based testing.

**Q: How do I contribute to the project?**
A: Fork the repository, create a feature branch, implement your changes with tests, and submit a pull request.

**Q: Where can I find the API documentation?**
A: API endpoints are documented in this README. For detailed request/response formats, examine the controller classes in `src/main/java/com/pasp/ict/salescrm/controller/`.

### Support and Resources

**Q: Where can I get help?**
A: Check this documentation, review the troubleshooting guide above, examine log files, and create GitHub issues for bugs or feature requests.

**Q: Are there any video tutorials?**
A: Currently, documentation is text-based. Video tutorials may be added in future releases.

**Q: How do I report bugs?**
A: Create an issue on the GitHub repository with detailed steps to reproduce, expected behavior, actual behavior, and system information.

**Q: Is professional support available?**
A: This is an open-source project. Professional support may be available through third-party consultants familiar with Spring Boot applications.

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes with appropriate tests
4. Ensure all tests pass (`mvn test`)
5. Update documentation as needed
6. Commit your changes (`git commit -m 'Add amazing feature'`)
7. Push to the branch (`git push origin feature/amazing-feature`)
8. Submit a pull request

### Development Guidelines

- Follow Spring Boot best practices
- Maintain test coverage above 80%
- Add property-based tests for new business logic
- Update API documentation for new endpoints
- Include appropriate error handling and validation
- Follow the existing code style and conventions

## License

This project is licensed under the MIT License - see the LICENSE file for details.