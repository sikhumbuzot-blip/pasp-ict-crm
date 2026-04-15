#!/bin/bash

# Quick Start Script for Sales CRM Application with PostgreSQL
# This script sets up PostgreSQL and starts the application

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# Function to print header
print_header() {
    echo ""
    print_status "$BLUE" "=================================================="
    print_status "$BLUE" "  Sales CRM Application - Quick Start"
    print_status "$BLUE" "=================================================="
    echo ""
}

# Function to check prerequisites
check_prerequisites() {
    print_status "$YELLOW" "Checking prerequisites..."
    
    # Check Java
    if ! command -v java &> /dev/null; then
        print_status "$RED" "❌ Java not found. Please install Java 17 or higher."
        exit 1
    fi
    
    local java_version=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1)
    if [ "$java_version" -lt 17 ]; then
        print_status "$RED" "❌ Java 17 or higher required. Found Java $java_version"
        exit 1
    fi
    print_status "$GREEN" "✅ Java $java_version found"
    
    # Check Maven
    if ! command -v mvn &> /dev/null && [ ! -f "./mvnw" ]; then
        print_status "$RED" "❌ Maven not found and mvnw wrapper not available"
        exit 1
    fi
    print_status "$GREEN" "✅ Maven found"
    
    # Check PostgreSQL
    if ! command -v psql &> /dev/null; then
        print_status "$YELLOW" "⚠️  PostgreSQL not found. Will attempt to install..."
        install_postgresql
    else
        print_status "$GREEN" "✅ PostgreSQL found"
    fi
}

# Function to install PostgreSQL
install_postgresql() {
    print_status "$YELLOW" "Installing PostgreSQL..."
    
    if command -v apt &> /dev/null; then
        sudo apt update
        sudo apt install -y postgresql postgresql-contrib
        sudo systemctl start postgresql
        sudo systemctl enable postgresql
    elif command -v yum &> /dev/null; then
        sudo yum install -y postgresql-server postgresql-contrib
        sudo postgresql-setup initdb
        sudo systemctl start postgresql
        sudo systemctl enable postgresql
    elif command -v brew &> /dev/null; then
        brew install postgresql
        brew services start postgresql
    else
        print_status "$RED" "❌ Unable to install PostgreSQL automatically"
        print_status "$YELLOW" "Please install PostgreSQL manually and run this script again"
        exit 1
    fi
    
    print_status "$GREEN" "✅ PostgreSQL installed successfully"
}

# Function to setup database
setup_database() {
    print_status "$YELLOW" "Setting up database..."
    
    if [ -f "scripts/setup-postgresql.sh" ]; then
        chmod +x scripts/setup-postgresql.sh
        ./scripts/setup-postgresql.sh
    else
        print_status "$RED" "❌ Database setup script not found"
        exit 1
    fi
}

# Function to start application
start_application() {
    print_status "$YELLOW" "Starting Sales CRM Application..."
    
    # Use Maven wrapper if available, otherwise use system Maven
    if [ -f "./mvnw" ]; then
        MAVEN_CMD="./mvnw"
    else
        MAVEN_CMD="mvn"
    fi
    
    print_status "$BLUE" "Building and starting application..."
    print_status "$YELLOW" "This may take a few minutes on first run..."
    
    $MAVEN_CMD spring-boot:run
}

# Function to show success message
show_success() {
    echo ""
    print_status "$GREEN" "🎉 Sales CRM Application started successfully!"
    echo ""
    print_status "$BLUE" "Access Information:"
    echo "  📱 Application URL: http://localhost:8080"
    echo "  👤 Username: admin"
    echo "  🔑 Password: admin123"
    echo ""
    print_status "$YELLOW" "Important Notes:"
    echo "  • Change the default password immediately in production"
    echo "  • The application will create sample data on first startup"
    echo "  • Database data persists between application restarts"
    echo ""
    print_status "$BLUE" "Useful Commands:"
    echo "  • Stop application: Ctrl+C"
    echo "  • Restart application: ./start-local.sh"
    echo "  • Check database: psql -h localhost -U crmuser -d crmdb_dev"
    echo "  • View logs: tail -f logs/sales-crm.log"
    echo ""
}

# Function to handle errors
handle_error() {
    print_status "$RED" "❌ An error occurred during setup"
    print_status "$YELLOW" "Troubleshooting steps:"
    echo "  1. Check if PostgreSQL is running: sudo systemctl status postgresql"
    echo "  2. Verify Java version: java -version (requires Java 17+)"
    echo "  3. Check Maven: mvn -version"
    echo "  4. Review error messages above"
    echo "  5. Consult POSTGRESQL_SETUP_GUIDE.md for manual setup"
    exit 1
}

# Main execution
main() {
    # Set error handler
    trap handle_error ERR
    
    print_header
    
    print_status "$BLUE" "This script will:"
    echo "  1. Check system prerequisites"
    echo "  2. Install PostgreSQL (if needed)"
    echo "  3. Set up database and user"
    echo "  4. Start the Sales CRM application"
    echo ""
    
    read -p "Continue? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_status "$YELLOW" "Setup cancelled"
        exit 0
    fi
    
    check_prerequisites
    setup_database
    
    print_status "$GREEN" "✅ Setup complete! Starting application..."
    echo ""
    print_status "$YELLOW" "Press Ctrl+C to stop the application"
    echo ""
    
    # Start application and show success message when it's ready
    start_application &
    APP_PID=$!
    
    # Wait for application to start
    print_status "$YELLOW" "Waiting for application to start..."
    sleep 10
    
    # Check if application is running
    if kill -0 $APP_PID 2>/dev/null; then
        show_success
        wait $APP_PID
    else
        print_status "$RED" "❌ Application failed to start"
        handle_error
    fi
}

# Run main function
main "$@"