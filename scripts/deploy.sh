#!/bin/bash

# Sales CRM Deployment Script
# Supports multiple deployment targets: local, docker, kubernetes

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
APP_NAME="sales-crm"
VERSION="0.0.1-SNAPSHOT"
JAR_FILE="target/${APP_NAME}-${VERSION}.jar"
DOCKER_IMAGE="${APP_NAME}:latest"
MVN_CMD="mvn"  # Will be updated in validate_environment

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
    echo "Usage: $0 [COMMAND] [OPTIONS]"
    echo ""
    echo "Commands:"
    echo "  local       Deploy locally with H2 database"
    echo "  docker      Deploy using Docker Compose"
    echo "  kubernetes  Deploy to Kubernetes cluster"
    echo "  build       Build the application only"
    echo "  clean       Clean build artifacts"
    echo "  validate    Validate deployment environment"
    echo ""
    echo "Options:"
    echo "  --profile   Spring profile (dev, test, prod)"
    echo "  --port      Application port (default: 8080)"
    echo "  --help      Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 local --profile dev"
    echo "  $0 docker --profile prod"
    echo "  $0 kubernetes --profile prod"
    echo "  $0 build"
}

validate_environment() {
    log_info "Validating deployment environment..."
    
    # Check if we're in the project root
    if [ ! -f "pom.xml" ]; then
        log_error "Not in project root directory (pom.xml not found)"
        exit 1
    fi
    
    # Check Java
    if ! command -v java &> /dev/null; then
        log_error "Java is not installed"
        exit 1
    fi
    
    # Check Maven or Maven wrapper
    if command -v mvn &> /dev/null; then
        MVN_CMD="mvn"
    elif [ -f "./mvnw" ]; then
        MVN_CMD="./mvnw"
        log_info "Using Maven wrapper"
    else
        log_error "Maven or Maven wrapper is not available"
        exit 1
    fi
    
    log_success "Environment validation passed"
}

build_application() {
    log_info "Building application..."
    
    if [ ! -f "$JAR_FILE" ] || [ "src/" -nt "$JAR_FILE" ]; then
        log_info "Building with Maven..."
        $MVN_CMD clean package -DskipTests
        
        if [ $? -eq 0 ]; then
            log_success "Build completed successfully"
        else
            log_error "Build failed"
            exit 1
        fi
    else
        log_info "Application is already built and up to date"
    fi
}

deploy_local() {
    local profile=${1:-dev}
    local port=${2:-8080}
    
    log_info "Deploying locally with profile: $profile"
    
    build_application
    
    # Set environment variables for local deployment
    export SERVER_PORT=$port
    export SPRING_PROFILES_ACTIVE=$profile
    
    if [ "$profile" = "prod" ]; then
        # Check required environment variables for production
        if [ -z "$DB_PASSWORD" ] || [ -z "$ENCRYPTION_KEY" ]; then
            log_error "Production deployment requires DB_PASSWORD and ENCRYPTION_KEY environment variables"
            exit 1
        fi
    fi
    
    log_info "Starting application on port $port..."
    java -jar "$JAR_FILE" --spring.profiles.active=$profile --server.port=$port
}

deploy_docker() {
    local profile=${1:-prod}
    
    log_info "Deploying with Docker Compose (profile: $profile)..."
    
    # Check if Docker is available
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed"
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose is not installed"
        exit 1
    fi
    
    # Navigate to Docker directory
    cd docker/postgresql
    
    # Check if .env file exists
    if [ ! -f ".env" ]; then
        log_warning ".env file not found, copying from .env.example"
        if [ -f ".env.example" ]; then
            cp .env.example .env
            log_warning "Please edit .env file with your configuration before running again"
            exit 1
        else
            log_error ".env.example file not found"
            exit 1
        fi
    fi
    
    # Choose compose file based on profile
    if [ "$profile" = "prod" ]; then
        log_info "Using production Docker Compose configuration"
        docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
    else
        log_info "Using development Docker Compose configuration"
        docker-compose up -d
    fi
    
    # Wait for services to be ready
    log_info "Waiting for services to be ready..."
    sleep 10
    
    # Check if services are running
    if docker-compose ps | grep -q "Up"; then
        log_success "Docker deployment completed successfully"
        log_info "Application available at: http://localhost:8080"
        log_info "PgAdmin available at: http://localhost:8081"
    else
        log_error "Some services failed to start"
        docker-compose logs
        exit 1
    fi
    
    cd - > /dev/null
}

deploy_kubernetes() {
    local profile=${1:-prod}
    
    log_info "Deploying to Kubernetes (profile: $profile)..."
    
    # Check if kubectl is available
    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl is not installed"
        exit 1
    fi
    
    # Check cluster connectivity
    if ! kubectl cluster-info &> /dev/null; then
        log_error "Cannot connect to Kubernetes cluster"
        exit 1
    fi
    
    # Navigate to Kubernetes directory
    cd k8s
    
    # Check if deployment script exists
    if [ -f "deploy.sh" ]; then
        log_info "Using Kubernetes deployment script"
        chmod +x deploy.sh
        ./deploy.sh deploy
    else
        log_info "Deploying manually..."
        
        # Apply Kubernetes manifests
        kubectl apply -f namespace.yaml
        kubectl apply -f configmap.yaml
        kubectl apply -f secret.yaml
        kubectl apply -f postgresql-pvc.yaml
        kubectl apply -f sales-crm-pvc.yaml
        kubectl apply -f postgres-init-configmap.yaml
        kubectl apply -f postgresql-deployment.yaml
        kubectl apply -f sales-crm-deployment.yaml
        kubectl apply -f ingress.yaml
        
        log_info "Waiting for deployment to be ready..."
        kubectl wait --for=condition=available --timeout=300s deployment/sales-crm-app -n sales-crm
        kubectl wait --for=condition=available --timeout=300s deployment/postgresql -n sales-crm
    fi
    
    # Show deployment status
    kubectl get pods -n sales-crm
    kubectl get services -n sales-crm
    
    log_success "Kubernetes deployment completed successfully"
    log_info "Use 'kubectl port-forward service/sales-crm-service 8080:80 -n sales-crm' to access the application"
    
    cd - > /dev/null
}

clean_build() {
    log_info "Cleaning build artifacts..."
    
    if [ -f "pom.xml" ]; then
        $MVN_CMD clean
        log_success "Maven clean completed"
    fi
    
    # Clean Docker images
    if command -v docker &> /dev/null; then
        if docker images | grep -q "$APP_NAME"; then
            log_info "Removing Docker images..."
            docker rmi $(docker images "$APP_NAME" -q) 2>/dev/null || true
        fi
    fi
    
    log_success "Clean completed"
}

# Main script logic
COMMAND=${1:-help}
PROFILE="dev"
PORT="8080"

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --profile)
            PROFILE="$2"
            shift 2
            ;;
        --port)
            PORT="$2"
            shift 2
            ;;
        --help)
            show_usage
            exit 0
            ;;
        *)
            if [ -z "$COMMAND" ] || [ "$COMMAND" = "help" ]; then
                COMMAND="$1"
            fi
            shift
            ;;
    esac
done

# Execute command
case $COMMAND in
    local)
        validate_environment
        deploy_local "$PROFILE" "$PORT"
        ;;
    docker)
        validate_environment
        deploy_docker "$PROFILE"
        ;;
    kubernetes)
        validate_environment
        deploy_kubernetes "$PROFILE"
        ;;
    build)
        validate_environment
        build_application
        ;;
    clean)
        clean_build
        ;;
    validate)
        validate_environment
        if [ -f "scripts/validate-deployment.sh" ]; then
            ./scripts/validate-deployment.sh
        fi
        ;;
    help|--help)
        show_usage
        ;;
    *)
        log_error "Unknown command: $COMMAND"
        show_usage
        exit 1
        ;;
esac