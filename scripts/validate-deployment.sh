#!/bin/bash

# Sales CRM Deployment Validation Script
# This script validates the deployment configuration and environment

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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

# Validation functions
validate_java() {
    log_info "Checking Java installation..."
    if command -v java &> /dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
        log_success "Java found: $JAVA_VERSION"
        
        # Check if Java 17 or higher
        JAVA_MAJOR=$(echo $JAVA_VERSION | cut -d'.' -f1)
        if [ "$JAVA_MAJOR" -ge 17 ]; then
            log_success "Java version is compatible (17+)"
        else
            log_error "Java 17 or higher is required. Found: $JAVA_VERSION"
            return 1
        fi
    else
        log_error "Java is not installed"
        return 1
    fi
}

validate_maven() {
    log_info "Checking Maven installation..."
    if command -v mvn &> /dev/null; then
        MAVEN_VERSION=$(mvn -version | head -n 1 | cut -d' ' -f3)
        log_success "Maven found: $MAVEN_VERSION"
    else
        log_warning "Maven is not installed (required for building from source)"
    fi
}

validate_docker() {
    log_info "Checking Docker installation..."
    if command -v docker &> /dev/null; then
        DOCKER_VERSION=$(docker --version | cut -d' ' -f3 | cut -d',' -f1)
        log_success "Docker found: $DOCKER_VERSION"
        
        # Check if Docker is running
        if docker info &> /dev/null; then
            log_success "Docker daemon is running"
        else
            log_warning "Docker daemon is not running"
        fi
    else
        log_warning "Docker is not installed (required for containerized deployment)"
    fi
}

validate_docker_compose() {
    log_info "Checking Docker Compose installation..."
    if command -v docker-compose &> /dev/null; then
        COMPOSE_VERSION=$(docker-compose --version | cut -d' ' -f3 | cut -d',' -f1)
        log_success "Docker Compose found: $COMPOSE_VERSION"
    else
        log_warning "Docker Compose is not installed (required for Docker deployment)"
    fi
}

validate_kubectl() {
    log_info "Checking kubectl installation..."
    if command -v kubectl &> /dev/null; then
        KUBECTL_VERSION=$(kubectl version --client --short 2>/dev/null | cut -d' ' -f3)
        log_success "kubectl found: $KUBECTL_VERSION"
        
        # Check cluster connectivity
        if kubectl cluster-info &> /dev/null; then
            log_success "kubectl can connect to cluster"
        else
            log_warning "kubectl cannot connect to cluster (required for Kubernetes deployment)"
        fi
    else
        log_warning "kubectl is not installed (required for Kubernetes deployment)"
    fi
}

validate_postgresql() {
    log_info "Checking PostgreSQL client installation..."
    if command -v psql &> /dev/null; then
        PSQL_VERSION=$(psql --version | cut -d' ' -f3)
        log_success "PostgreSQL client found: $PSQL_VERSION"
    else
        log_warning "PostgreSQL client is not installed (useful for database management)"
    fi
}

validate_environment_variables() {
    log_info "Checking environment variables..."
    
    # Check for production environment variables
    if [ "$SPRING_PROFILES_ACTIVE" = "prod" ]; then
        log_info "Production profile detected, checking required variables..."
        
        required_vars=("DB_PASSWORD" "ENCRYPTION_KEY" "ADMIN_PASSWORD")
        for var in "${required_vars[@]}"; do
            if [ -z "${!var}" ]; then
                log_error "Required environment variable $var is not set"
            else
                log_success "Environment variable $var is set"
            fi
        done
        
        # Check encryption key length
        if [ -n "$ENCRYPTION_KEY" ] && [ ${#ENCRYPTION_KEY} -lt 32 ]; then
            log_warning "ENCRYPTION_KEY should be at least 32 characters long"
        fi
    else
        log_info "Development/test profile detected, skipping production variable checks"
    fi
}

validate_ports() {
    log_info "Checking port availability..."
    
    ports=(8080 5432 8081)
    port_names=("Application" "PostgreSQL" "PgAdmin")
    
    for i in "${!ports[@]}"; do
        port=${ports[$i]}
        name=${port_names[$i]}
        
        if netstat -tlnp 2>/dev/null | grep -q ":$port "; then
            log_warning "$name port $port is already in use"
        else
            log_success "$name port $port is available"
        fi
    done
}

validate_disk_space() {
    log_info "Checking disk space..."
    
    # Check available disk space (in GB)
    available_space=$(df -BG . | tail -1 | awk '{print $4}' | sed 's/G//')
    
    if [ "$available_space" -gt 10 ]; then
        log_success "Sufficient disk space available: ${available_space}GB"
    elif [ "$available_space" -gt 5 ]; then
        log_warning "Limited disk space available: ${available_space}GB (10GB+ recommended)"
    else
        log_error "Insufficient disk space: ${available_space}GB (minimum 5GB required)"
    fi
}

validate_memory() {
    log_info "Checking available memory..."
    
    # Check available memory (in GB)
    if command -v free &> /dev/null; then
        available_memory=$(free -g | grep '^Mem:' | awk '{print $7}')
        total_memory=$(free -g | grep '^Mem:' | awk '{print $2}')
        
        if [ "$total_memory" -gt 4 ]; then
            log_success "Sufficient memory available: ${total_memory}GB total"
        elif [ "$total_memory" -gt 2 ]; then
            log_warning "Limited memory available: ${total_memory}GB total (4GB+ recommended)"
        else
            log_error "Insufficient memory: ${total_memory}GB total (minimum 2GB required)"
        fi
    else
        log_warning "Cannot check memory (free command not available)"
    fi
}

validate_configuration_files() {
    log_info "Checking configuration files..."
    
    # Check if we're in the project root
    if [ ! -f "pom.xml" ]; then
        log_error "Not in project root directory (pom.xml not found)"
        return 1
    fi
    
    # Check essential configuration files
    config_files=(
        "src/main/resources/application.properties"
        "src/main/resources/application-prod.properties"
        "Dockerfile"
        "docker/postgresql/docker-compose.yml"
    )
    
    for file in "${config_files[@]}"; do
        if [ -f "$file" ]; then
            log_success "Configuration file found: $file"
        else
            log_error "Configuration file missing: $file"
        fi
    done
    
    # Check Docker environment file
    if [ -f "docker/postgresql/.env" ]; then
        log_success "Docker environment file found"
    else
        log_warning "Docker environment file not found (copy from .env.example)"
    fi
}

validate_build() {
    log_info "Validating application build..."
    
    if [ -f "pom.xml" ] && command -v mvn &> /dev/null; then
        log_info "Running Maven validate..."
        if mvn validate -q; then
            log_success "Maven project validation passed"
        else
            log_error "Maven project validation failed"
            return 1
        fi
        
        log_info "Checking dependencies..."
        if mvn dependency:resolve -q; then
            log_success "All dependencies resolved"
        else
            log_error "Dependency resolution failed"
            return 1
        fi
    else
        log_warning "Cannot validate build (Maven or pom.xml not available)"
    fi
}

show_deployment_recommendations() {
    log_info "Deployment Recommendations:"
    echo ""
    
    # Determine best deployment method
    has_docker=$(command -v docker &> /dev/null && echo "yes" || echo "no")
    has_kubectl=$(command -v kubectl &> /dev/null && kubectl cluster-info &> /dev/null && echo "yes" || echo "no")
    has_java=$(command -v java &> /dev/null && echo "yes" || echo "no")
    
    if [ "$has_kubectl" = "yes" ]; then
        echo "✅ Kubernetes deployment recommended (production-ready with auto-scaling)"
        echo "   Command: cd k8s && ./deploy.sh deploy"
    fi
    
    if [ "$has_docker" = "yes" ]; then
        echo "✅ Docker deployment recommended (easy setup and management)"
        echo "   Command: cd docker/postgresql && docker-compose up -d"
    fi
    
    if [ "$has_java" = "yes" ]; then
        echo "✅ Traditional deployment available (direct JAR execution)"
        echo "   Command: mvn spring-boot:run"
    fi
    
    echo ""
    log_info "For detailed instructions, see:"
    echo "  - DEPLOYMENT_README.md"
    echo "  - docs/DEPLOYMENT_GUIDE.md"
    echo "  - docs/KUBERNETES_DEPLOYMENT_GUIDE.md"
}

main() {
    echo "Sales CRM Deployment Validation"
    echo "==============================="
    echo ""
    
    # Run all validations
    validate_java
    validate_maven
    validate_docker
    validate_docker_compose
    validate_kubectl
    validate_postgresql
    validate_environment_variables
    validate_ports
    validate_disk_space
    validate_memory
    validate_configuration_files
    validate_build
    
    echo ""
    echo "==============================="
    show_deployment_recommendations
    
    echo ""
    log_success "Validation completed!"
}

# Check if script is being run from the correct directory
if [ ! -f "pom.xml" ]; then
    log_error "Please run this script from the project root directory"
    exit 1
fi

# Run main function
main