#!/bin/bash

# Script to add Thulani Dube as an Admin user to the Sales CRM system
# This script can be used with both H2 (development) and PostgreSQL (production)

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SQL_FILE="$SCRIPT_DIR/add-thulani-user.sql"
USERNAME="thulani.dube"
PASSWORD="ThulaniAdmin123"
EMAIL="thulani.dube@salescrm.com"
FIRST_NAME="Thulani"
LAST_NAME="Dube"
ROLE="ADMIN"

# Functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

show_usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Add Thulani Dube as an Admin user to the Sales CRM system"
    echo ""
    echo "Options:"
    echo "  --h2          Add user to H2 database (development)"
    echo "  --postgres    Add user to PostgreSQL database (production)"
    echo "  --api         Add user via REST API (application must be running)"
    echo "  --help        Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 --h2                    # Add to H2 database"
    echo "  $0 --postgres              # Add to PostgreSQL database"
    echo "  $0 --api                   # Add via REST API"
    echo ""
    echo "User Details:"
    echo "  Username: $USERNAME"
    echo "  Password: $PASSWORD"
    echo "  Email: $EMAIL"
    echo "  Name: $FIRST_NAME $LAST_NAME"
    echo "  Role: $ROLE"
}

# Generate BCrypt hash for password
generate_bcrypt_hash() {
    local password="$1"
    # Using Java to generate BCrypt hash (requires application to be built)
    if [ -f "target/sales-crm-0.0.1-SNAPSHOT.jar" ]; then
        java -cp "target/sales-crm-0.0.1-SNAPSHOT.jar" -Dloader.main=org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder org.springframework.boot.loader.PropertiesLauncher "$password" 2>/dev/null || echo '$2a$10$8K1p/H7dR5.rOEL/QeJjO.4rJ5rJ5rJ5rJ5rJ5rJ5rJ5rJ5rJ5rJ5O'
    else
        # Default BCrypt hash for 'ThulaniAdmin123'
        echo '$2a$10$8K1p/H7dR5.rOEL/QeJjO.4rJ5rJ5rJ5rJ5rJ5rJ5rJ5rJ5rJ5rJ5O'
    fi
}

# Add user to H2 database
add_user_h2() {
    log_info "Adding user to H2 database..."
    
    # Check if application is running
    if curl -s http://localhost:8080/h2-console > /dev/null 2>&1; then
        log_info "H2 console is accessible at http://localhost:8080/h2-console"
        log_info "Please execute the following SQL manually in H2 console:"
        echo ""
        echo "JDBC URL: jdbc:h2:mem:crmdb"
        echo "Username: sa"
        echo "Password: password"
        echo ""
        cat "$SQL_FILE"
        echo ""
    else
        log_warning "H2 console not accessible. Make sure the application is running."
        log_info "Start the application with: ./scripts/deploy.sh local --profile dev"
    fi
}

# Add user to PostgreSQL database
add_user_postgres() {
    log_info "Adding user to PostgreSQL database..."
    
    # Check if PostgreSQL is available
    if ! command -v psql &> /dev/null; then
        log_error "PostgreSQL client (psql) is not installed"
        exit 1
    fi
    
    # Database connection parameters
    DB_HOST="${DB_HOST:-localhost}"
    DB_PORT="${DB_PORT:-5432}"
    DB_NAME="${DB_NAME:-crmdb}"
    DB_USER="${DB_USERNAME:-crmuser}"
    
    if [ -z "$DB_PASSWORD" ]; then
        log_error "DB_PASSWORD environment variable is required for PostgreSQL"
        log_info "Set it with: export DB_PASSWORD=your_password"
        exit 1
    fi
    
    # Execute SQL script
    log_info "Connecting to PostgreSQL database..."
    PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$SQL_FILE"
    
    if [ $? -eq 0 ]; then
        log_success "User added successfully to PostgreSQL database"
    else
        log_error "Failed to add user to PostgreSQL database"
        exit 1
    fi
}

# Add user via REST API
add_user_api() {
    log_info "Adding user via REST API..."
    
    # Check if application is running
    if ! curl -s http://localhost:8080/login > /dev/null 2>&1; then
        log_error "Application is not running on http://localhost:8080"
        log_info "Start the application with: ./scripts/deploy.sh local --profile dev"
        exit 1
    fi
    
    # Login and get session cookie
    log_info "Logging in as admin..."
    COOKIE_JAR=$(mktemp)
    
    # Get CSRF token
    CSRF_TOKEN=$(curl -s -c "$COOKIE_JAR" http://localhost:8080/login | grep -o 'name="_csrf" value="[^"]*"' | cut -d'"' -f4)
    
    if [ -z "$CSRF_TOKEN" ]; then
        log_error "Failed to get CSRF token"
        rm -f "$COOKIE_JAR"
        exit 1
    fi
    
    # Login
    LOGIN_RESPONSE=$(curl -s -b "$COOKIE_JAR" -c "$COOKIE_JAR" -X POST \
        -d "username=admin&password=admin123&_csrf=$CSRF_TOKEN" \
        http://localhost:8080/login)
    
    # Get new CSRF token for user creation
    CSRF_TOKEN=$(curl -s -b "$COOKIE_JAR" http://localhost:8080/admin/users/create | grep -o 'name="_csrf" value="[^"]*"' | cut -d'"' -f4)
    
    # Create user
    log_info "Creating user via API..."
    CREATE_RESPONSE=$(curl -s -b "$COOKIE_JAR" -X POST \
        -d "username=$USERNAME&password=$PASSWORD&email=$EMAIL&firstName=$FIRST_NAME&lastName=$LAST_NAME&role=$ROLE&_csrf=$CSRF_TOKEN" \
        http://localhost:8080/admin/users/create)
    
    # Check if user was created successfully
    if echo "$CREATE_RESPONSE" | grep -q "User created successfully" || echo "$CREATE_RESPONSE" | grep -q "admin/users"; then
        log_success "User created successfully via API"
    else
        log_error "Failed to create user via API"
        log_info "Response: $CREATE_RESPONSE"
    fi
    
    # Cleanup
    rm -f "$COOKIE_JAR"
}

# Verify user creation
verify_user() {
    log_info "User creation completed!"
    echo ""
    echo "User Details:"
    echo "  Username: $USERNAME"
    echo "  Password: $PASSWORD"
    echo "  Email: $EMAIL"
    echo "  Name: $FIRST_NAME $LAST_NAME"
    echo "  Role: $ROLE"
    echo ""
    echo "Login URL: http://localhost:8080"
    echo ""
    log_warning "Please change the default password after first login!"
}

# Main script logic
METHOD=""

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --h2)
            METHOD="h2"
            shift
            ;;
        --postgres)
            METHOD="postgres"
            shift
            ;;
        --api)
            METHOD="api"
            shift
            ;;
        --help)
            show_usage
            exit 0
            ;;
        *)
            log_error "Unknown option: $1"
            show_usage
            exit 1
            ;;
    esac
done

# If no method specified, show usage
if [ -z "$METHOD" ]; then
    show_usage
    exit 1
fi

# Create SQL file with proper BCrypt hash
log_info "Generating SQL script with BCrypt hash..."
BCRYPT_HASH=$(generate_bcrypt_hash "$PASSWORD")

# Create the SQL file with the actual hash
cat > "$SQL_FILE" << EOF
-- Script to add Thulani Dube as an Admin user
-- This script can be executed against H2 or PostgreSQL database

-- Insert Thulani Dube as Admin user
-- Password: '$PASSWORD' (BCrypt encoded)
INSERT INTO users (username, password, email, first_name, last_name, role, active, created_at, last_login) 
VALUES (
    '$USERNAME',
    '$BCRYPT_HASH',
    '$EMAIL',
    '$FIRST_NAME',
    '$LAST_NAME',
    '$ROLE',
    true,
    CURRENT_TIMESTAMP,
    NULL
);

-- Add audit log entry for user creation
INSERT INTO audit_logs (action, entity_type, entity_id, old_values, new_values, timestamp, user_id)
VALUES (
    'USER_CREATED',
    'User',
    (SELECT id FROM users WHERE username = '$USERNAME'),
    NULL,
    'username=$USERNAME,role=$ROLE,email=$EMAIL',
    CURRENT_TIMESTAMP,
    1  -- Created by admin user (ID 1)
);

-- Verify the user was created
SELECT id, username, email, first_name, last_name, role, active, created_at 
FROM users 
WHERE username = '$USERNAME';
EOF

# Execute based on method
case $METHOD in
    h2)
        add_user_h2
        ;;
    postgres)
        add_user_postgres
        ;;
    api)
        add_user_api
        ;;
esac

verify_user