#!/bin/bash

# PostgreSQL Setup Script for Sales CRM Application
# This script sets up PostgreSQL database for local development

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
DB_NAME="crmdb_dev"
DB_USER="crmuser"
DB_PASSWORD="crmpass"
DB_HOST="localhost"
DB_PORT="5432"

# Function to print colored output
print_status() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# Function to check if PostgreSQL is installed
check_postgresql_installed() {
    if ! command -v psql &> /dev/null; then
        print_status "$RED" "PostgreSQL is not installed. Please install PostgreSQL first."
        print_status "$YELLOW" "Ubuntu/Debian: sudo apt install postgresql postgresql-contrib"
        print_status "$YELLOW" "macOS: brew install postgresql"
        print_status "$YELLOW" "CentOS/RHEL: sudo yum install postgresql-server postgresql-contrib"
        exit 1
    fi
    print_status "$GREEN" "PostgreSQL is installed"
}

# Function to check if PostgreSQL is running
check_postgresql_running() {
    if ! systemctl is-active --quiet postgresql 2>/dev/null && ! brew services list | grep postgresql | grep started &>/dev/null; then
        print_status "$YELLOW" "Starting PostgreSQL service..."
        if command -v systemctl &> /dev/null; then
            sudo systemctl start postgresql
        elif command -v brew &> /dev/null; then
            brew services start postgresql
        else
            print_status "$RED" "Unable to start PostgreSQL. Please start it manually."
            exit 1
        fi
    fi
    print_status "$GREEN" "PostgreSQL is running"
}

# Function to create database and user
setup_database() {
    print_status "$YELLOW" "Setting up database and user..."
    
    # Create user and database
    sudo -u postgres psql << EOF
-- Create user if not exists
DO \$\$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = '$DB_USER') THEN
        CREATE USER $DB_USER WITH PASSWORD '$DB_PASSWORD';
    END IF;
END
\$\$;

-- Create database if not exists
SELECT 'CREATE DATABASE $DB_NAME OWNER $DB_USER'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$DB_NAME')\gexec

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE $DB_NAME TO $DB_USER;

-- Connect to the database and grant schema privileges
\c $DB_NAME

GRANT ALL ON SCHEMA public TO $DB_USER;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO $DB_USER;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO $DB_USER;

-- Set default privileges for future objects
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO $DB_USER;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO $DB_USER;

\q
EOF

    print_status "$GREEN" "Database and user created successfully"
}

# Function to test database connection
test_connection() {
    print_status "$YELLOW" "Testing database connection..."
    
    if PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "SELECT 1;" &>/dev/null; then
        print_status "$GREEN" "Database connection successful"
    else
        print_status "$RED" "Database connection failed"
        exit 1
    fi
}

# Function to create schema
create_schema() {
    print_status "$YELLOW" "Creating database schema..."
    
    if [ -f "scripts/postgresql/create_schema.sql" ]; then
        PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f scripts/postgresql/create_schema.sql
        print_status "$GREEN" "Database schema created successfully"
    else
        print_status "$YELLOW" "Schema file not found, will be created by application on startup"
    fi
}

# Function to create environment file
create_env_file() {
    print_status "$YELLOW" "Creating environment configuration..."
    
    cat > .env << EOF
# Database Configuration
DB_HOST=$DB_HOST
DB_PORT=$DB_PORT
DB_NAME=$DB_NAME
DB_USERNAME=$DB_USER
DB_PASSWORD=$DB_PASSWORD

# Application Security
ENCRYPTION_KEY=defaultEncryptionKey123456789012
ADMIN_USERNAME=admin
ADMIN_PASSWORD=admin123

# Email Configuration (Optional)
MAIL_HOST=localhost
MAIL_PORT=1025
MAIL_USERNAME=
MAIL_PASSWORD=

# Backup Configuration
BACKUP_DIRECTORY=backups
BACKUP_RETENTION_DAYS=30
BACKUP_ENABLED=true

# Notification Configuration
NOTIFICATION_ENABLED=true
NOTIFICATION_FROM_EMAIL=noreply@salescrm.com
NOTIFICATION_SYSTEM_NAME=Sales CRM System
NOTIFICATION_ADMIN_EMAILS=admin@example.com
EOF

    print_status "$GREEN" "Environment file created: .env"
}

# Function to show next steps
show_next_steps() {
    print_status "$GREEN" "PostgreSQL setup completed successfully!"
    echo ""
    print_status "$YELLOW" "Next steps:"
    echo "1. Start the application with: ./mvnw spring-boot:run"
    echo "2. Or use your IDE to run the SalesCrmApplication class"
    echo "3. Access the application at: http://localhost:8080"
    echo "4. Login with: admin / admin123"
    echo ""
    print_status "$YELLOW" "Database connection details:"
    echo "Host: $DB_HOST"
    echo "Port: $DB_PORT"
    echo "Database: $DB_NAME"
    echo "Username: $DB_USER"
    echo "Password: $DB_PASSWORD"
    echo ""
    print_status "$YELLOW" "To connect manually:"
    echo "psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME"
}

# Main execution
main() {
    print_status "$GREEN" "Setting up PostgreSQL for Sales CRM Application..."
    echo ""
    
    check_postgresql_installed
    check_postgresql_running
    setup_database
    test_connection
    create_schema
    create_env_file
    show_next_steps
}

# Run main function
main "$@"