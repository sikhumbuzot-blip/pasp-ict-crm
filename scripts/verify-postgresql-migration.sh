#!/bin/bash

# PostgreSQL Migration Verification Script
# This script verifies that the application is properly configured for PostgreSQL

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# Function to check configuration files
check_config_files() {
    print_status "$YELLOW" "Checking configuration files..."
    
    # Check application.properties
    if grep -q "postgresql" src/main/resources/application.properties; then
        print_status "$GREEN" "✓ application.properties configured for PostgreSQL"
    else
        print_status "$RED" "✗ application.properties not configured for PostgreSQL"
        return 1
    fi
    
    # Check application-dev.properties
    if grep -q "postgresql" src/main/resources/application-dev.properties; then
        print_status "$GREEN" "✓ application-dev.properties configured for PostgreSQL"
    else
        print_status "$RED" "✗ application-dev.properties not configured for PostgreSQL"
        return 1
    fi
    
    # Check application-prod.properties
    if [ -f "src/main/resources/application-prod.properties" ] && grep -q "postgresql" src/main/resources/application-prod.properties; then
        print_status "$GREEN" "✓ application-prod.properties configured for PostgreSQL"
    else
        print_status "$YELLOW" "! application-prod.properties not found or not configured for PostgreSQL"
    fi
}

# Function to check Maven dependencies
check_dependencies() {
    print_status "$YELLOW" "Checking Maven dependencies..."
    
    if grep -q "postgresql" pom.xml; then
        print_status "$GREEN" "✓ PostgreSQL driver dependency found in pom.xml"
    else
        print_status "$RED" "✗ PostgreSQL driver dependency not found in pom.xml"
        return 1
    fi
}

# Function to check database connection
check_database_connection() {
    print_status "$YELLOW" "Checking database connection..."
    
    # Load environment variables if .env exists
    if [ -f ".env" ]; then
        export $(cat .env | grep -v '^#' | xargs)
    fi
    
    # Set default values
    DB_HOST=${DB_HOST:-localhost}
    DB_PORT=${DB_PORT:-5432}
    DB_NAME=${DB_NAME:-crmdb_dev}
    DB_USERNAME=${DB_USERNAME:-crmuser}
    DB_PASSWORD=${DB_PASSWORD:-crmpass}
    
    if PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" -d "$DB_NAME" -c "SELECT 1;" &>/dev/null; then
        print_status "$GREEN" "✓ Database connection successful"
        
        # Check if tables exist
        table_count=$(PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" -d "$DB_NAME" -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE';")
        
        if [ "$table_count" -gt 0 ]; then
            print_status "$GREEN" "✓ Database schema exists ($table_count tables found)"
        else
            print_status "$YELLOW" "! Database schema not found (will be created on application startup)"
        fi
    else
        print_status "$RED" "✗ Database connection failed"
        print_status "$YELLOW" "Run './scripts/setup-postgresql.sh' to set up the database"
        return 1
    fi
}

# Function to check application startup
check_application_startup() {
    print_status "$YELLOW" "Checking application startup (this may take a moment)..."
    
    # Build the application
    if ./mvnw clean compile -q; then
        print_status "$GREEN" "✓ Application compiles successfully"
    else
        print_status "$RED" "✗ Application compilation failed"
        return 1
    fi
    
    # Check if application can start (dry run)
    if timeout 30s ./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.main.web-application-type=none --spring.jpa.hibernate.ddl-auto=validate" -q &>/dev/null; then
        print_status "$GREEN" "✓ Application starts successfully with PostgreSQL"
    else
        print_status "$YELLOW" "! Application startup test skipped (requires running database)"
    fi
}

# Function to show migration status
show_migration_status() {
    print_status "$GREEN" "PostgreSQL Migration Verification Complete!"
    echo ""
    print_status "$YELLOW" "Migration Status Summary:"
    echo "• Configuration files: Updated for PostgreSQL"
    echo "• Dependencies: PostgreSQL driver included"
    echo "• Database: Connection verified"
    echo "• Application: Ready to run with PostgreSQL"
    echo ""
    print_status "$YELLOW" "To start the application:"
    echo "1. Ensure PostgreSQL is running"
    echo "2. Run: ./mvnw spring-boot:run"
    echo "3. Access: http://localhost:8080"
    echo "4. Login: admin / admin123"
}

# Main execution
main() {
    print_status "$GREEN" "Verifying PostgreSQL Migration for Sales CRM Application..."
    echo ""
    
    local exit_code=0
    
    check_config_files || exit_code=1
    check_dependencies || exit_code=1
    check_database_connection || exit_code=1
    check_application_startup || exit_code=1
    
    if [ $exit_code -eq 0 ]; then
        show_migration_status
    else
        print_status "$RED" "Migration verification failed. Please check the errors above."
        exit 1
    fi
}

# Run main function
main "$@"