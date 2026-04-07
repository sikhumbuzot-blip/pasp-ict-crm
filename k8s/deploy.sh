#!/bin/bash

# Sales CRM Kubernetes Deployment Script
# This script deploys the Sales CRM application to a Kubernetes cluster

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
NAMESPACE="sales-crm"
APP_NAME="sales-crm-app"
DB_NAME="postgresql"

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

check_prerequisites() {
    log_info "Checking prerequisites..."
    
    # Check if kubectl is installed
    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl is not installed. Please install kubectl first."
        exit 1
    fi
    
    # Check if kubectl can connect to cluster
    if ! kubectl cluster-info &> /dev/null; then
        log_error "Cannot connect to Kubernetes cluster. Please check your kubeconfig."
        exit 1
    fi
    
    # Check if Docker image exists (optional check)
    log_warning "Make sure your Docker image 'sales-crm:latest' is available in your registry"
    
    log_success "Prerequisites check completed"
}

create_namespace() {
    log_info "Creating namespace..."
    kubectl apply -f namespace.yaml
    log_success "Namespace created/updated"
}

deploy_secrets() {
    log_info "Deploying secrets..."
    log_warning "Please ensure you have updated the secrets in secret.yaml with your actual values"
    kubectl apply -f secret.yaml
    log_success "Secrets deployed"
}

deploy_configmaps() {
    log_info "Deploying ConfigMaps..."
    kubectl apply -f configmap.yaml
    kubectl apply -f postgres-init-configmap.yaml
    log_success "ConfigMaps deployed"
}

deploy_storage() {
    log_info "Deploying persistent volumes..."
    kubectl apply -f postgresql-pvc.yaml
    kubectl apply -f sales-crm-pvc.yaml
    log_success "Persistent volumes deployed"
}

deploy_database() {
    log_info "Deploying PostgreSQL database..."
    kubectl apply -f postgresql-deployment.yaml
    
    log_info "Waiting for PostgreSQL to be ready..."
    kubectl wait --for=condition=available --timeout=300s deployment/postgresql -n $NAMESPACE
    
    # Wait for PostgreSQL pod to be ready
    kubectl wait --for=condition=ready --timeout=300s pod -l app=postgresql -n $NAMESPACE
    
    log_success "PostgreSQL deployed and ready"
}

deploy_application() {
    log_info "Deploying Sales CRM application..."
    kubectl apply -f sales-crm-deployment.yaml
    
    log_info "Waiting for application to be ready..."
    kubectl wait --for=condition=available --timeout=600s deployment/sales-crm-app -n $NAMESPACE
    
    log_success "Sales CRM application deployed and ready"
}

deploy_ingress() {
    log_info "Deploying ingress..."
    log_warning "Please update the domain name in ingress.yaml before deploying to production"
    kubectl apply -f ingress.yaml
    log_success "Ingress deployed"
}

check_deployment() {
    log_info "Checking deployment status..."
    
    echo ""
    log_info "Namespace status:"
    kubectl get namespace $NAMESPACE
    
    echo ""
    log_info "Pods status:"
    kubectl get pods -n $NAMESPACE
    
    echo ""
    log_info "Services status:"
    kubectl get services -n $NAMESPACE
    
    echo ""
    log_info "Persistent Volume Claims status:"
    kubectl get pvc -n $NAMESPACE
    
    echo ""
    log_info "Ingress status:"
    kubectl get ingress -n $NAMESPACE
    
    echo ""
    log_info "Deployment status:"
    kubectl get deployments -n $NAMESPACE
}

get_access_info() {
    log_info "Getting access information..."
    
    # Get service information
    SERVICE_IP=$(kubectl get service sales-crm-service -n $NAMESPACE -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || echo "")
    SERVICE_PORT=$(kubectl get service sales-crm-service -n $NAMESPACE -o jsonpath='{.spec.ports[0].port}')
    
    # Get ingress information
    INGRESS_HOST=$(kubectl get ingress sales-crm-ingress -n $NAMESPACE -o jsonpath='{.spec.rules[0].host}' 2>/dev/null || echo "")
    
    echo ""
    log_success "Deployment completed successfully!"
    echo ""
    log_info "Access Information:"
    
    if [ -n "$INGRESS_HOST" ] && [ "$INGRESS_HOST" != "your-domain.com" ]; then
        echo "  External URL: https://$INGRESS_HOST"
    else
        log_warning "  Ingress domain not configured. Update ingress.yaml with your domain."
    fi
    
    if [ -n "$SERVICE_IP" ]; then
        echo "  Service IP: http://$SERVICE_IP:$SERVICE_PORT"
    else
        echo "  Service: sales-crm-service.$NAMESPACE.svc.cluster.local:$SERVICE_PORT"
        log_info "  Use port-forward for local access: kubectl port-forward service/sales-crm-service 8080:80 -n $NAMESPACE"
    fi
    
    echo ""
    log_info "Useful commands:"
    echo "  View logs: kubectl logs -f deployment/sales-crm-app -n $NAMESPACE"
    echo "  Scale app: kubectl scale deployment/sales-crm-app --replicas=3 -n $NAMESPACE"
    echo "  Delete deployment: kubectl delete namespace $NAMESPACE"
    echo "  Port forward: kubectl port-forward service/sales-crm-service 8080:80 -n $NAMESPACE"
}

show_help() {
    echo "Sales CRM Kubernetes Deployment Script"
    echo ""
    echo "Usage: $0 [COMMAND]"
    echo ""
    echo "Commands:"
    echo "  deploy     Deploy the complete application (default)"
    echo "  check      Check deployment status"
    echo "  clean      Remove the deployment"
    echo "  help       Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 deploy    # Deploy the application"
    echo "  $0 check     # Check deployment status"
    echo "  $0 clean     # Remove the deployment"
}

clean_deployment() {
    log_warning "This will remove the entire Sales CRM deployment including data!"
    read -p "Are you sure you want to continue? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        log_info "Removing deployment..."
        kubectl delete namespace $NAMESPACE --ignore-not-found=true
        log_success "Deployment removed"
    else
        log_info "Deployment removal cancelled"
    fi
}

main() {
    case "${1:-deploy}" in
        "deploy")
            log_info "Starting Sales CRM deployment..."
            check_prerequisites
            create_namespace
            deploy_secrets
            deploy_configmaps
            deploy_storage
            deploy_database
            deploy_application
            deploy_ingress
            check_deployment
            get_access_info
            ;;
        "check")
            check_deployment
            get_access_info
            ;;
        "clean")
            clean_deployment
            ;;
        "help"|"-h"|"--help")
            show_help
            ;;
        *)
            log_error "Unknown command: $1"
            show_help
            exit 1
            ;;
    esac
}

# Make sure we're in the right directory
if [ ! -f "namespace.yaml" ]; then
    log_error "Please run this script from the k8s directory"
    exit 1
fi

# Run main function
main "$@"