# User Management Scripts

This directory contains scripts and utilities for managing users in the Sales CRM application.

## Files Overview

- `add-thulani-user.sh` - Shell script to add Thulani Dube as Admin user
- `add-thulani-user.sql` - SQL script to add Thulani Dube as Admin user
- `PasswordHashGenerator.java` - Utility to generate BCrypt password hashes
- `USER_MANAGEMENT_README.md` - This documentation file

## Adding Thulani Dube as Admin User

### Method 1: Automatic (Recommended)

The user is automatically added when the application starts because it's included in `src/main/resources/data.sql`.

**User Details:**
- Username: `thulani.dube`
- Password: `admin123` (same as other default users)
- Email: `thulani.dube@salescrm.com`
- Name: Thulani Dube
- Role: ADMIN

**To use:**
1. Start the application: `./scripts/deploy.sh local --profile dev`
2. Login at http://localhost:8080 with `thulani.dube` / `admin123`

### Method 2: Using Shell Script

Use the provided shell script for different scenarios:

```bash
# Add to H2 database (development)
./scripts/add-thulani-user.sh --h2

# Add to PostgreSQL database (production)
export DB_PASSWORD=your_postgres_password
./scripts/add-thulani-user.sh --postgres

# Add via REST API (application must be running)
./scripts/add-thulani-user.sh --api
```

### Method 3: Manual SQL Execution

Execute the SQL script directly:

```bash
# For H2 (via H2 console at http://localhost:8080/h2-console)
# JDBC URL: jdbc:h2:mem:crmdb
# Username: sa
# Password: password
# Then execute: scripts/add-thulani-user.sql

# For PostgreSQL
psql -h localhost -U crmuser -d crmdb -f scripts/add-thulani-user.sql
```

### Method 4: Via Web Interface

1. Login as admin: http://localhost:8080
2. Go to Admin → Users → Create User
3. Fill in the details:
   - Username: `thulani.dube`
   - Password: `admin123` (or custom password)
   - Email: `thulani.dube@salescrm.com`
   - First Name: `Thulani`
   - Last Name: `Dube`
   - Role: `ADMIN`

## Password Management

### Default Passwords

All default users use the password `admin123`:
- `admin` / `admin123` (System Administrator)
- `thulani.dube` / `admin123` (Thulani Dube - Admin)
- `sales` / `admin123` (Sales User)
- `user` / `admin123` (Regular User)

### Generating Custom Password Hashes

To create a user with a custom password:

1. **Using the Java utility:**
   ```bash
   # Build the application first
   mvn clean package -DskipTests
   
   # Compile the utility
   javac -cp "target/sales-crm-0.0.1-SNAPSHOT.jar" scripts/PasswordHashGenerator.java
   
   # Generate hash
   java -cp "scripts:target/sales-crm-0.0.1-SNAPSHOT.jar" PasswordHashGenerator "YourPassword123"
   ```

2. **Using online BCrypt generators:**
   - Visit: https://bcrypt-generator.com/
   - Enter your password
   - Use rounds: 10
   - Copy the generated hash

3. **Using command line (if bcrypt is installed):**
   ```bash
   # Install bcrypt utility
   npm install -g bcrypt-cli
   
   # Generate hash
   bcrypt "YourPassword123"
   ```

### Password Requirements

The application enforces these password requirements:
- Minimum 8 characters
- At least one lowercase letter
- At least one uppercase letter  
- At least one number

## Security Considerations

### Production Deployment

**⚠️ IMPORTANT:** Change all default passwords before production deployment!

1. **Change default admin password:**
   ```bash
   # Login as admin and go to Admin → Users → admin → Edit
   # Or use SQL:
   UPDATE users SET password = 'new_bcrypt_hash' WHERE username = 'admin';
   ```

2. **Change Thulani's password:**
   ```bash
   # Login as thulani.dube and change password via profile
   # Or use SQL:
   UPDATE users SET password = 'new_bcrypt_hash' WHERE username = 'thulani.dube';
   ```

3. **Disable or remove test users:**
   ```bash
   # Disable test users
   UPDATE users SET active = false WHERE username IN ('sales', 'user');
   
   # Or delete them (be careful with foreign key constraints)
   DELETE FROM users WHERE username IN ('sales', 'user');
   ```

### Environment Variables

For production PostgreSQL deployment:
```bash
export DB_USERNAME=crmuser
export DB_PASSWORD=secure_password
export ENCRYPTION_KEY=your_32_character_encryption_key
```

## Troubleshooting

### Common Issues

1. **User already exists:**
   ```sql
   -- Check if user exists
   SELECT * FROM users WHERE username = 'thulani.dube';
   
   -- Delete if needed (be careful with foreign keys)
   DELETE FROM users WHERE username = 'thulani.dube';
   ```

2. **Password not working:**
   - Ensure BCrypt hash is correct
   - Check password requirements are met
   - Verify user is active: `SELECT active FROM users WHERE username = 'thulani.dube';`

3. **Database connection issues:**
   - H2: Ensure application is running on http://localhost:8080
   - PostgreSQL: Check DB_PASSWORD environment variable
   - Verify database credentials and connectivity

4. **Permission denied:**
   ```bash
   # Make script executable
   chmod +x scripts/add-thulani-user.sh
   ```

### Verification

After adding the user, verify it was created successfully:

```sql
-- Check user details
SELECT id, username, email, first_name, last_name, role, active, created_at 
FROM users 
WHERE username = 'thulani.dube';

-- Check audit log
SELECT * FROM audit_logs 
WHERE entity_type = 'User' AND new_values LIKE '%thulani.dube%';
```

## Support

For additional help:
1. Check the main README.md for general application information
2. Review docs/TROUBLESHOOTING_GUIDE.md for common issues
3. Check application logs in `logs/sales-crm.log`
4. Use H2 console for database inspection: http://localhost:8080/h2-console