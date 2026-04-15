# PostgreSQL Setup Guide for Sales CRM Application

This guide will help you set up and run the Sales CRM application with PostgreSQL database.

## Quick Start

### 1. Prerequisites

- Java 17 or higher
- PostgreSQL 12 or higher
- Maven 3.6 or higher

### 2. Install PostgreSQL

#### Ubuntu/Debian
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

#### macOS (using Homebrew)
```bash
brew install postgresql
brew services start postgresql
```

#### CentOS/RHEL
```bash
sudo yum install postgresql-server postgresql-contrib
sudo postgresql-setup initdb
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

### 3. Automated Setup

Run the setup script to automatically configure PostgreSQL:

```bash
./scripts/setup-postgresql.sh
```

This script will:
- Create the database and user
- Set up proper permissions
- Create environment configuration
- Test the connection

### 4. Manual Setup (Alternative)

If you prefer manual setup:

1. **Create database and user:**
```sql
sudo -u postgres psql
CREATE DATABASE crmdb_dev;
CREATE USER crmuser WITH PASSWORD 'crmpass';
GRANT ALL PRIVILEGES ON DATABASE crmdb_dev TO crmuser;
\c crmdb_dev
GRANT ALL ON SCHEMA public TO crmuser;
\q
```

2. **Create environment file:**
```bash
cp .env.example .env
# Edit .env with your database credentials
```

### 5. Start the Application

```bash
./mvnw spring-boot:run
```

Or using your IDE, run the `SalesCrmApplication` class.

### 6. Access the Application

- **URL**: http://localhost:8080
- **Username**: admin
- **Password**: admin123

## Configuration

### Environment Variables

The application uses the following environment variables (defined in `.env`):

```bash
# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=crmdb_dev
DB_USERNAME=crmuser
DB_PASSWORD=crmpass

# Application Security
ENCRYPTION_KEY=defaultEncryptionKey123456789012
ADMIN_USERNAME=admin
ADMIN_PASSWORD=admin123
```

### Profiles

- **Default**: Uses PostgreSQL with development settings
- **dev**: Development profile with debug logging
- **prod**: Production profile with optimized settings
- **test**: Test profile using H2 in-memory database

To run with a specific profile:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## Database Management

### Connect to Database
```bash
psql -h localhost -U crmuser -d crmdb_dev
```

### Backup Database
```bash
pg_dump -h localhost -U crmuser -d crmdb_dev > backup.sql
```

### Restore Database
```bash
psql -h localhost -U crmuser -d crmdb_dev < backup.sql
```

## Troubleshooting

### Common Issues

1. **Connection refused**
   - Ensure PostgreSQL is running: `sudo systemctl status postgresql`
   - Check if port 5432 is open: `netstat -tlnp | grep 5432`

2. **Authentication failed**
   - Verify user credentials in `.env` file
   - Check pg_hba.conf for authentication method

3. **Database does not exist**
   - Run the setup script: `./scripts/setup-postgresql.sh`
   - Or create manually as shown above

4. **Permission denied**
   - Ensure user has proper privileges on database and schema
   - Run: `GRANT ALL ON SCHEMA public TO crmuser;`

### Verification

Run the verification script to check your setup:
```bash
./scripts/verify-postgresql-migration.sh
```

## Development

### Schema Changes

The application uses Hibernate with `ddl-auto=update` in development, so schema changes are automatically applied. For production, use `ddl-auto=validate` and apply schema changes manually.

### Sample Data

The application will create sample data on first startup if no users exist in the database.

### Testing

Tests use H2 in-memory database for fast execution:
```bash
./mvnw test
```

## Production Deployment

For production deployment, see:
- [PostgreSQL Migration Guide](docs/POSTGRESQL_MIGRATION_GUIDE.md)
- [Deployment Guide](docs/DEPLOYMENT_GUIDE.md)
- [Docker Deployment](docker/postgresql/README.md)

## Support

If you encounter issues:
1. Check the troubleshooting section above
2. Review application logs
3. Verify database connection and permissions
4. Consult the full migration guide in `docs/POSTGRESQL_MIGRATION_GUIDE.md`