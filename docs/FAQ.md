# Frequently Asked Questions (FAQ)

## General Questions

### Q: What is the Sales CRM Application?
**A:** The Sales CRM Application is a comprehensive web-based Customer Relationship Management system built with Spring Boot and Thymeleaf. It provides role-based access control, sales pipeline management, customer data management, and administrative functionality for small to medium-sized businesses.

### Q: What are the system requirements?
**A:** 
- **Java:** 17 or higher
- **Maven:** 3.6 or higher  
- **Memory:** 2GB RAM minimum (4GB recommended)
- **Storage:** 1GB free disk space minimum
- **Database:** H2 (development) or PostgreSQL 12+ (production)
- **Browser:** Modern browsers (Chrome, Firefox, Safari, Edge)

### Q: Is this application free to use?
**A:** Yes, this project is licensed under the MIT License, which allows free use, modification, and distribution, including for commercial purposes.

### Q: Can I use this application for my business?
**A:** Absolutely! The application is designed for business use and includes all necessary features for managing customer relationships, sales processes, and user administration.

### Q: Is the application mobile-friendly?
**A:** Yes, the application uses responsive design and works on tablets and mobile devices. However, it's optimized for desktop use where users typically perform detailed CRM tasks.

### Q: How many users can the system support?
**A:** The application can handle hundreds of concurrent users. Performance testing shows good results with thousands of customer records and dozens of simultaneous users. For larger deployments, consider using PostgreSQL and implementing load balancing.

## Installation and Setup

### Q: How do I install the application?
**A:** 
1. Ensure Java 17+ and Maven 3.6+ are installed
2. Clone the repository: `git clone https://github.com/sikhumbuzot-blip/pasp-ict-crm.git`
3. Navigate to the directory: `cd pasp-ict-crm`
4. Run the application: `mvn spring-boot:run`
5. Access at http://localhost:8080

### Q: What are the default login credentials?
**A:** 
- **Username:** `admin`
- **Password:** `admin123`
- **Role:** `ADMIN`

**Important:** Change these credentials immediately in production environments.

### Q: How do I change the default admin password?
**A:** 
1. Log in as admin
2. Go to Admin → Users
3. Click on the admin user
4. Update the password
5. Save changes

### Q: Can I run the application on a different port?
**A:** Yes, use: `mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081`

Or set `server.port=8081` in `application.properties`.

### Q: How do I enable HTTPS?
**A:** Configure SSL in `application.properties`:
```properties
server.ssl.key-store=keystore.p12
server.ssl.key-store-password=password
server.ssl.key-store-type=PKCS12
server.port=8443
```

Or use a reverse proxy like Nginx for SSL termination.

## Database Questions

### Q: What database does the application use?
**A:** The application uses H2 in-memory database for development and testing. For production, it supports PostgreSQL with a clear migration path.

### Q: How do I access the H2 database console?
**A:** 
1. Ensure the application is running in development mode
2. Navigate to http://localhost:8080/h2-console
3. Use these connection details:
   - **JDBC URL:** `jdbc:h2:mem:crmdb`
   - **Username:** `sa`
   - **Password:** `password`

### Q: How do I migrate from H2 to PostgreSQL?
**A:** Follow the comprehensive [PostgreSQL Migration Guide](POSTGRESQL_MIGRATION_GUIDE.md). The process involves:
1. Installing and configuring PostgreSQL
2. Creating the database and user
3. Running schema creation scripts
4. Updating application configuration
5. Migrating data (if needed)

### Q: Will I lose data when I restart the application?
**A:** 
- **H2 in-memory:** Yes, data is lost on restart (development mode)
- **H2 file-based:** No, data persists (can be configured)
- **PostgreSQL:** No, data persists across restarts

### Q: How do I backup my data?
**A:** 
- **Automatic:** Daily backups are created automatically
- **Manual:** Use Admin → Backups → Create Backup
- **PostgreSQL:** Use standard `pg_dump` commands
- **H2:** Export via H2 console or use backup service

### Q: How do I restore from a backup?
**A:** 
1. Stop the application
2. Restore database from backup files in the `backups/` directory
3. For PostgreSQL: `psql -h localhost -U crmuser -d crmdb < backup.sql`
4. Restart the application

## User Management

### Q: What user roles are available?
**A:** Three roles are supported:
- **ADMIN:** Full system access, user management, system configuration
- **SALES:** Sales operations, customer management, lead tracking
- **REGULAR:** Read-only access to assigned data

### Q: How do I create new users?
**A:** 
1. Log in as an admin user
2. Go to Admin → Users → Create User
3. Fill in user details and assign appropriate role
4. Save the user

### Q: Can I change a user's role?
**A:** Yes, admin users can change roles:
1. Go to Admin → Users
2. Select the user
3. Update the role
4. Save changes

### Q: How do I deactivate a user?
**A:** 
1. Go to Admin → Users
2. Select the user
3. Change status to inactive
4. Save changes

The user will no longer be able to log in.

### Q: Can users change their own passwords?
**A:** Currently, password changes must be done by admin users. Self-service password changes can be added as a future enhancement.

### Q: What happens when a user's session expires?
**A:** After 30 minutes of inactivity, users are automatically logged out and redirected to the login page with a session timeout message.

## Sales and Customer Management

### Q: How do I create a new customer?
**A:** 
1. Go to Customers → Create Customer
2. Fill in customer details (name, email, phone, company, address)
3. Save the customer

### Q: How do I track leads through the sales pipeline?
**A:** 
1. Create a lead: Sales → Leads → Create Lead
2. Assign to a customer and sales user
3. Update status as the lead progresses: NEW → CONTACTED → QUALIFIED → PROPOSAL → NEGOTIATION → CLOSED_WON/CLOSED_LOST

### Q: How do I convert a lead to a sale?
**A:** 
1. Go to Sales → Leads
2. Select the lead
3. Click "Convert to Sale"
4. Enter sale amount and details
5. The lead is automatically marked as CLOSED_WON

### Q: Can I create sales without leads?
**A:** Yes, you can create direct sales:
1. Go to Sales → Transactions → Create Sale
2. Select customer and enter sale details
3. Save the transaction

### Q: How do I log customer interactions?
**A:** 
1. Go to Customers → [Select Customer]
2. Click "Log Interaction"
3. Choose interaction type (CALL, EMAIL, MEETING, NOTE)
4. Add detailed notes
5. Save the interaction

### Q: How do I search for customers?
**A:** Use the search functionality on the Customers page. You can search by:
- Customer name
- Email address
- Company name
- Phone number

### Q: Can I see sales performance metrics?
**A:** Yes, metrics are available in several places:
- **Sales Dashboard:** Individual performance for sales users
- **Admin Dashboard:** System-wide metrics for admin users
- **Sales → Metrics:** Detailed sales performance page
- **Admin → Statistics:** Comprehensive system statistics

## Security and Data Protection

### Q: How secure is the application?
**A:** The application implements multiple security measures:
- BCrypt password hashing
- Spring Security authentication and authorization
- CSRF protection
- Session management with timeout
- Input validation and sanitization
- SQL injection prevention
- XSS protection
- Comprehensive audit logging

### Q: What data is encrypted?
**A:** Sensitive customer data including contact information and financial details are encrypted at rest using AES encryption.

### Q: How are passwords stored?
**A:** Passwords are hashed using BCrypt with salt, making them extremely difficult to reverse even if the database is compromised.

### Q: Can I see who accessed what data?
**A:** Yes, the application maintains comprehensive audit logs:
- Admin → Audit Logs: View all system activities
- Admin → Security Incidents: View security-related events
- All user actions are logged with timestamps and user identification

### Q: What should I do if I suspect unauthorized access?
**A:** 
1. Check Admin → Security Incidents for failed login attempts
2. Review Admin → Audit Logs for suspicious activities
3. Change passwords for affected accounts
4. Consider deactivating compromised accounts
5. Review system access logs

### Q: How do I comply with data protection regulations?
**A:** The application provides:
- Data encryption for sensitive information
- Comprehensive audit trails
- User access controls
- Data export capabilities
- Secure backup procedures

Consult with legal experts for specific compliance requirements in your jurisdiction.

## Performance and Scalability

### Q: How many customers can the system handle?
**A:** The application is designed to handle thousands of customers efficiently. Performance testing shows sub-2-second search times for databases with 10,000+ customer records.

### Q: What if the application becomes slow?
**A:** Try these optimization steps:
1. Check system resources (CPU, memory, disk)
2. Review database performance and add indexes if needed
3. Increase JVM memory allocation
4. Consider migrating to PostgreSQL for better performance
5. Implement database connection pooling optimization

### Q: Can I scale the application horizontally?
**A:** Yes, for high-traffic deployments:
1. Use PostgreSQL as the shared database
2. Deploy multiple application instances
3. Use a load balancer (Nginx, HAProxy)
4. Consider using Kubernetes for auto-scaling

### Q: How do I monitor application performance?
**A:** 
- **Admin Dashboard:** System health and performance metrics
- **Application Logs:** Performance-related log entries
- **Database Monitoring:** Query performance and connection pool status
- **System Monitoring:** CPU, memory, and disk usage

## Deployment and Production

### Q: How do I deploy to production?
**A:** Several deployment options are available:
1. **Traditional:** Build JAR and run on server with PostgreSQL
2. **Docker:** Use provided Docker configurations
3. **Kubernetes:** Use provided K8s manifests
4. **Cloud:** Deploy to AWS, Azure, or Google Cloud

See the [Deployment Guide](DEPLOYMENT_GUIDE.md) for detailed instructions.

### Q: What's the recommended production setup?
**A:** 
- **Database:** PostgreSQL 12+ with regular backups
- **Web Server:** Nginx reverse proxy with SSL/TLS
- **Application:** Spring Boot JAR with sufficient memory allocation
- **Monitoring:** Log aggregation and system monitoring
- **Security:** Firewall, regular updates, strong passwords

### Q: How do I update the application?
**A:** 
1. **Backup:** Create full system backup
2. **Stop:** Stop the current application
3. **Update:** Replace JAR file with new version
4. **Migrate:** Run any database migrations
5. **Start:** Start the new version
6. **Verify:** Test functionality

Always test updates in a staging environment first.

### Q: Can I run multiple environments (dev, staging, prod)?
**A:** Yes, use Spring profiles:
- **Development:** `--spring.profiles.active=dev`
- **Testing:** `--spring.profiles.active=test`
- **Production:** `--spring.profiles.active=prod`

Each profile can have different database configurations and settings.

### Q: How do I configure environment variables?
**A:** Set these environment variables for production:
```bash
export DB_USERNAME=your_db_user
export DB_PASSWORD=your_db_password
export DB_URL=jdbc:postgresql://localhost:5432/crmdb
export ENCRYPTION_KEY=your_32_character_encryption_key
```

## Integration and Customization

### Q: Can I integrate with other systems?
**A:** Yes, the application provides REST API endpoints that return JSON data. These can be consumed by external systems for integration purposes.

### Q: How do I add custom fields?
**A:** Adding custom fields requires code modifications:
1. Update entity classes (Customer, Lead, etc.)
2. Create database migration scripts
3. Update forms and templates
4. Add validation logic
5. Update tests

### Q: Can I customize the user interface?
**A:** Yes, you can modify:
- **Templates:** Thymeleaf templates in `src/main/resources/templates/`
- **Styles:** CSS files in `src/main/resources/static/css/`
- **JavaScript:** JS files in `src/main/resources/static/js/`

### Q: How do I add new user roles?
**A:** Adding new roles requires:
1. Update `UserRole` enum
2. Modify security configuration
3. Update templates and controllers
4. Add appropriate access controls
5. Update tests and documentation

### Q: Can I export data to Excel/CSV?
**A:** Yes, the application supports:
- **Reports:** Admin → Reports with PDF/CSV export
- **Customer Data:** Export via API endpoints
- **Sales Data:** Export transaction and lead data

### Q: How do I set up email notifications?
**A:** Email notifications can be configured by:
1. Adding email service configuration
2. Implementing notification templates
3. Configuring SMTP settings
4. Setting up notification triggers

This requires code modifications and is not available out-of-the-box.

## Troubleshooting

### Q: The application won't start. What should I check?
**A:** 
1. Verify Java 17+ is installed: `java -version`
2. Check if port 8080 is available: `netstat -tulpn | grep :8080`
3. Ensure sufficient memory is available
4. Check for error messages in the console
5. Review the [Troubleshooting Guide](TROUBLESHOOTING_GUIDE.md)

### Q: I can't log in with the default credentials. What's wrong?
**A:** 
1. Ensure you're using `admin` / `admin123`
2. Check if the user exists in the database
3. Clear browser cache and cookies
4. Try incognito/private browsing mode
5. Check application logs for authentication errors

### Q: The application is running slowly. How can I improve performance?
**A:** 
1. Increase JVM memory: `export JAVA_OPTS="-Xmx4g -Xms2g"`
2. Check database performance and add indexes
3. Monitor system resources (CPU, memory, disk)
4. Consider migrating to PostgreSQL
5. Review the performance section in the [Troubleshooting Guide](TROUBLESHOOTING_GUIDE.md)

### Q: I'm getting database connection errors. How do I fix this?
**A:** 
1. **H2:** Check H2 console access and connection parameters
2. **PostgreSQL:** Verify service is running and credentials are correct
3. Check database logs for error details
4. Verify network connectivity
5. Review database configuration in application properties

### Q: How do I enable debug logging?
**A:** Add these properties to `application.properties`:
```properties
logging.level.com.pasp.ict.salescrm=DEBUG
logging.level.org.springframework.security=DEBUG
logging.level.org.hibernate.SQL=DEBUG
```

Or run with debug profile: `mvn spring-boot:run -Dspring-boot.run.profiles=dev`

## Development and Contributing

### Q: How do I set up a development environment?
**A:** 
1. Install Java 17+, Maven 3.6+, and your preferred IDE
2. Clone the repository
3. Import as a Maven project
4. Run with development profile: `mvn spring-boot:run -Dspring-boot.run.profiles=dev`
5. Access H2 console for database inspection

### Q: What testing frameworks are used?
**A:** 
- **JUnit 5:** Unit testing framework
- **Mockito:** Mocking framework for unit tests
- **Spring Boot Test:** Integration testing
- **jqwik:** Property-based testing for comprehensive validation

### Q: How do I run the tests?
**A:** 
```bash
# Run all tests
mvn test

# Run with coverage report
mvn test jacoco:report

# Run specific test class
mvn test -Dtest=CustomerServiceTest

# Run property-based tests only
mvn test -Dtest="**/*Properties"
```

### Q: How do I contribute to the project?
**A:** 
1. Fork the repository on GitHub
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Make your changes with appropriate tests
4. Ensure all tests pass: `mvn test`
5. Update documentation as needed
6. Commit changes: `git commit -m 'Add amazing feature'`
7. Push to branch: `git push origin feature/amazing-feature`
8. Submit a pull request

### Q: What coding standards should I follow?
**A:** 
- Follow Spring Boot best practices
- Maintain test coverage above 80%
- Add property-based tests for business logic
- Include appropriate error handling
- Update API documentation for new endpoints
- Follow existing code style and conventions

### Q: How do I add new API endpoints?
**A:** 
1. Add methods to appropriate controller classes
2. Implement business logic in service classes
3. Add appropriate security annotations
4. Write unit and integration tests
5. Update API documentation
6. Test with various user roles

## Support and Resources

### Q: Where can I get help?
**A:** 
1. **Documentation:** Review README.md and docs/ directory
2. **Troubleshooting:** Check the [Troubleshooting Guide](TROUBLESHOOTING_GUIDE.md)
3. **GitHub Issues:** Create issues for bugs or feature requests
4. **Community:** Stack Overflow with relevant tags
5. **Professional Support:** Consider hiring Spring Boot consultants

### Q: How do I report bugs?
**A:** Create a GitHub issue with:
- Detailed description of the problem
- Steps to reproduce the issue
- Expected vs. actual behavior
- System information (OS, Java version, etc.)
- Log excerpts (if applicable)
- Screenshots (if relevant)

### Q: Are there any video tutorials?
**A:** Currently, documentation is text-based. Video tutorials may be added in future releases based on community demand.

### Q: Is professional support available?
**A:** This is an open-source project maintained by the community. Professional support may be available through:
- Third-party consultants familiar with Spring Boot
- Custom development services
- Enterprise support contracts (if available)

### Q: How often is the project updated?
**A:** Updates depend on community contributions and maintenance schedules. Check the GitHub repository for:
- Recent commits and releases
- Open issues and pull requests
- Project roadmap and milestones

### Q: Can I use this project as a starting point for my own CRM?
**A:** Absolutely! The MIT license allows you to:
- Use the code for any purpose
- Modify and customize as needed
- Create derivative works
- Use in commercial projects
- Distribute modified versions

Just maintain the original license notice in your derivative work.

### Q: How do I stay updated with new features?
**A:** 
- **GitHub:** Watch the repository for notifications
- **Releases:** Check the releases page for new versions
- **Documentation:** Review changelog and release notes
- **Community:** Follow discussions and issues

### Q: What's the project roadmap?
**A:** Future enhancements may include:
- Self-service password changes
- Email notification system
- Advanced reporting and analytics
- Mobile application
- API rate limiting
- Webhook support
- Single sign-on (SSO) integration
- Advanced customization options

Check GitHub issues and discussions for the latest roadmap information.