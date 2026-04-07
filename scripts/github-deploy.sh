#!/bin/bash

# GitHub Deployment Script for Sales CRM Application
# This script prepares and deploys the application to GitHub

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
REPO_URL="https://github.com/sikhumbuzot-blip/pasp-ict-crm.git"
BRANCH="main"

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
    echo "  init        Initialize Git repository and add remote"
    echo "  push        Push code to GitHub repository"
    echo "  release     Create a release tag and push"
    echo "  status      Show Git status and repository information"
    echo ""
    echo "Options:"
    echo "  --branch    Target branch (default: main)"
    echo "  --tag       Release tag version (for release command)"
    echo "  --message   Commit message"
    echo "  --help      Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 init"
    echo "  $0 push --message 'Initial commit'"
    echo "  $0 release --tag v1.0.0"
    echo "  $0 status"
}

check_prerequisites() {
    log_info "Checking prerequisites..."
    
    # Check if Git is installed
    if ! command -v git &> /dev/null; then
        log_error "Git is not installed"
        exit 1
    fi
    
    # Check if we're in the project root
    if [ ! -f "pom.xml" ]; then
        log_error "Not in project root directory (pom.xml not found)"
        exit 1
    fi
    
    log_success "Prerequisites check passed"
}

init_repository() {
    log_info "Initializing Git repository..."
    
    # Initialize Git repository if not already initialized
    if [ ! -d ".git" ]; then
        git init
        log_success "Git repository initialized"
    else
        log_info "Git repository already exists"
    fi
    
    # Check if remote origin exists
    if git remote get-url origin &> /dev/null; then
        local current_remote=$(git remote get-url origin)
        if [ "$current_remote" != "$REPO_URL" ]; then
            log_warning "Remote origin exists but points to different URL: $current_remote"
            log_info "Updating remote origin to: $REPO_URL"
            git remote set-url origin "$REPO_URL"
        else
            log_info "Remote origin already configured correctly"
        fi
    else
        log_info "Adding remote origin: $REPO_URL"
        git remote add origin "$REPO_URL"
    fi
    
    # Configure Git user if not set
    if ! git config user.name &> /dev/null; then
        log_warning "Git user.name not configured"
        echo "Please configure Git user:"
        read -p "Enter your name: " git_name
        git config user.name "$git_name"
    fi
    
    if ! git config user.email &> /dev/null; then
        log_warning "Git user.email not configured"
        read -p "Enter your email: " git_email
        git config user.email "$git_email"
    fi
    
    log_success "Repository initialization completed"
}

prepare_for_deployment() {
    log_info "Preparing application for deployment..."
    
    # Determine Maven command
    if command -v mvn &> /dev/null; then
        MVN_CMD="mvn"
    elif [ -f "./mvnw" ]; then
        MVN_CMD="./mvnw"
        log_info "Using Maven wrapper"
    else
        log_error "Maven or Maven wrapper is not available"
        exit 1
    fi
    
    # Run tests to ensure everything works
    log_info "Running tests..."
    if $MVN_CMD test -q; then
        log_success "All tests passed"
    else
        log_error "Tests failed. Please fix issues before deployment."
        exit 1
    fi
    
    # Build the application
    log_info "Building application..."
    if $MVN_CMD clean package -DskipTests -q; then
        log_success "Application built successfully"
    else
        log_error "Build failed"
        exit 1
    fi
    
    # Validate deployment configuration
    if [ -f "scripts/validate-deployment.sh" ]; then
        log_info "Validating deployment configuration..."
        ./scripts/validate-deployment.sh > /dev/null 2>&1 || log_warning "Deployment validation had warnings"
    fi
    
    log_success "Application prepared for deployment"
}

push_to_github() {
    local commit_message=${1:-"Deploy Sales CRM Application"}
    local target_branch=${2:-$BRANCH}
    
    log_info "Pushing to GitHub repository..."
    
    # Add all files
    git add .
    
    # Check if there are changes to commit
    if git diff --staged --quiet; then
        log_info "No changes to commit"
        return 0
    fi
    
    # Commit changes
    log_info "Committing changes..."
    git commit -m "$commit_message"
    
    # Push to GitHub
    log_info "Pushing to branch: $target_branch"
    git push -u origin "$target_branch"
    
    log_success "Code pushed to GitHub successfully"
    log_info "Repository URL: $REPO_URL"
}

create_release() {
    local tag_version=${1:-"v1.0.0"}
    local release_message=${2:-"Release $tag_version"}
    
    log_info "Creating release: $tag_version"
    
    # Ensure we're on the main branch
    git checkout main 2>/dev/null || git checkout -b main
    
    # Create and push tag
    git tag -a "$tag_version" -m "$release_message"
    git push origin "$tag_version"
    
    log_success "Release $tag_version created and pushed"
    log_info "You can create a GitHub release at: ${REPO_URL}/releases/new?tag=$tag_version"
}

show_status() {
    log_info "Repository Status:"
    echo ""
    
    # Show Git status
    echo "Git Status:"
    git status --short
    echo ""
    
    # Show remote information
    echo "Remote Origin:"
    git remote get-url origin 2>/dev/null || echo "No remote origin configured"
    echo ""
    
    # Show recent commits
    echo "Recent Commits:"
    git log --oneline -5 2>/dev/null || echo "No commits found"
    echo ""
    
    # Show branches
    echo "Branches:"
    git branch -a 2>/dev/null || echo "No branches found"
    echo ""
    
    # Show tags
    echo "Tags:"
    git tag -l 2>/dev/null || echo "No tags found"
}

# Main script logic
COMMAND=${1:-help}
BRANCH="main"
TAG_VERSION=""
COMMIT_MESSAGE="Deploy Sales CRM Application"

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --branch)
            BRANCH="$2"
            shift 2
            ;;
        --tag)
            TAG_VERSION="$2"
            shift 2
            ;;
        --message)
            COMMIT_MESSAGE="$2"
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
    init)
        check_prerequisites
        init_repository
        ;;
    push)
        check_prerequisites
        prepare_for_deployment
        push_to_github "$COMMIT_MESSAGE" "$BRANCH"
        ;;
    release)
        check_prerequisites
        if [ -z "$TAG_VERSION" ]; then
            log_error "Release tag version is required. Use --tag option."
            exit 1
        fi
        prepare_for_deployment
        push_to_github "Release $TAG_VERSION" "$BRANCH"
        create_release "$TAG_VERSION"
        ;;
    status)
        check_prerequisites
        show_status
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