# GitHub Deployment Guide

This guide provides step-by-step instructions for deploying the Sales CRM Application to GitHub and setting up CI/CD pipelines.

## 🚀 Quick Deployment to GitHub

### Prerequisites

- Git installed and configured
- Java 17+ installed
- Maven 3.6+ installed
- GitHub account with repository access

### Step 1: Initialize and Push to GitHub

```bash
# Initialize Git repository and push to GitHub
./scripts/github-deploy.sh init
./scripts/github-deploy.sh push --message "Initial deployment of Sales CRM Application"
```

### Step 2: Configure GitHub Secrets (Optional for CI/CD)

If you want to use the GitHub Actions CI/CD pipeline, configure these secrets in your GitHub repository:

1. Go to your repository on GitHub
2. Navigate to Settings → Secrets and variables → Actions
3. Add the following secrets:

| Secret Name | Description | Example |
|-------------|-------------|---------|
| `DOCKER_USERNAME` | Docker Hub username | `your-dockerhub-username` |
| `DOCKER_PASSWORD` | Docker Hub password/token | `your-dockerhub-token` |
| `DB_PASSWORD` | Production database password | `secure_db_password_123` |
| `ENCRYPTION_KEY` | 32-character encryption key | `mySecretEncryptionKey123456789` |
| `ADMIN_PASSWORD` | Default admin password | `secure_admin_password` |

### Step 3: Enable GitHub Actions (Optional)

The repository includes two GitHub Actions workflows:

- **ci.yml**: Basic continuous integration (runs on all branches)
- **ci-cd.yml**: Full CI/CD pipeline with deployment (runs on main/develop)

Both workflows will automatically run when you push code to GitHub.

## 📁 Repository Structure

After deployment, your GitHub repository will contain:

```
├── .github/
│   └── workflows/
│       ├── ci.yml              # Basic CI workflow
│       └── ci-cd.yml           # Full CI/CD workflow
├── scripts/
│   ├── deploy.sh               # Multi-environment deployment script
│   ├── github-deploy.sh        # GitHub-specific deployment script
│   └── validate-deployment.sh  # Environment validation script
├── deployment-config.yml       # Deployment configuration
├── GITHUB_DEPLOYMENT.md        # This file
└── [rest of application files]
```

## 🔧 Deployment Scripts

### Main Deployment Script (`scripts/deploy.sh`)

Supports multiple deployment targets:

```bash
# Local development deployment
./scripts/deploy.sh local --profile dev

# Docker deployment
./scripts/deploy.sh docker --profile prod

# Kubernetes deployment
./scripts/deploy.sh kubernetes --profile prod

# Build only
./scripts/deploy.sh build

# Clean build artifacts
./scripts/deploy.sh clean

# Validate environment
./scripts/deploy.sh validate
```

### GitHub Deployment Script (`scripts/github-deploy.sh`)

Manages Git repository and GitHub deployment:

```bash
# Initialize Git repository
./scripts/github-deploy.sh init

# Push code to GitHub
./scripts/github-deploy.sh push --message "Your commit message"

# Create a release
./scripts/github-deploy.sh release --tag v1.0.0

# Show repository status
./scripts/github-deploy.sh status
```

## 🔄 CI/CD Pipeline

### Basic CI Workflow (ci.yml)

Runs on every push and pull request:

1. **Setup**: Java 17, Maven cache
2. **Validate**: Maven project validation
3. **Compile**: Application compilation
4. **Test**: Run all tests
5. **Build**: Create JAR artifact
6. **Upload**: Store test results and JAR file

### Full CI/CD Workflow (ci-cd.yml)

Runs on main/develop branches:

1. **Test**: Comprehensive testing with PostgreSQL
2. **Build**: Application build and artifact creation
3. **Security Scan**: OWASP dependency check
4. **Docker Build**: Build and push Docker image
5. **Deploy Staging**: Deploy to staging environment (develop branch)
6. **Deploy Production**: Deploy to production (main branch)
7. **Notify**: Send deployment notifications

## 🐳 Docker Integration

### Automatic Docker Builds

When you push to the main branch, GitHub Actions will:

1. Build a Docker image
2. Tag it with the branch name and commit SHA
3. Push it to Docker Hub (requires secrets configuration)

### Manual Docker Build

```bash
# Build Docker image locally
docker build -t sales-crm:latest .

# Run with Docker Compose
cd docker/postgresql
cp .env.example .env
# Edit .env with your configuration
docker-compose up -d
```

## ☸️ Kubernetes Deployment

### Automatic Kubernetes Deployment

Configure your Kubernetes cluster credentials in GitHub secrets, then the CI/CD pipeline will automatically deploy to Kubernetes.

### Manual Kubernetes Deployment

```bash
# Deploy to Kubernetes
cd k8s
./deploy.sh deploy

# Or use the main deployment script
./scripts/deploy.sh kubernetes --profile prod
```

## 🔒 Security Configuration

### Environment Variables

For production deployment, set these environment variables:

```bash
export DB_HOST=your-database-host
export DB_USERNAME=crmuser
export DB_PASSWORD=your-secure-password
export DB_NAME=crmdb
export ENCRYPTION_KEY=your-32-character-encryption-key
export ADMIN_PASSWORD=your-secure-admin-password
```

### GitHub Secrets

Configure these secrets in your GitHub repository for CI/CD:

- `DOCKER_USERNAME` and `DOCKER_PASSWORD` for Docker Hub
- `DB_PASSWORD` for database access
- `ENCRYPTION_KEY` for data encryption
- `ADMIN_PASSWORD` for default admin account

## 📊 Monitoring and Health Checks

### Health Check Endpoints

- Application health: `http://your-app/actuator/health`
- Database health: `http://your-app/actuator/health/db`
- Application info: `http://your-app/actuator/info`

### GitHub Actions Monitoring

Monitor your deployments in the GitHub Actions tab:

1. Go to your repository on GitHub
2. Click on the "Actions" tab
3. View workflow runs and their status

## 🚨 Troubleshooting

### Common Issues

#### 1. GitHub Actions Failing

**Check logs in GitHub Actions tab:**
- Verify all required secrets are configured
- Check if tests are passing locally
- Ensure Docker Hub credentials are correct

#### 2. Docker Build Failing

```bash
# Test Docker build locally
docker build -t sales-crm:test .

# Check Docker daemon
docker info
```

#### 3. Deployment Script Issues

```bash
# Validate environment
./scripts/validate-deployment.sh

# Check script permissions
chmod +x scripts/*.sh
```

#### 4. Git Issues

```bash
# Check Git configuration
git config --list

# Check remote origin
git remote -v

# Reset if needed
./scripts/github-deploy.sh init
```

### Getting Help

1. Check the troubleshooting section in README.md
2. Review GitHub Actions logs for CI/CD issues
3. Use the validation script: `./scripts/validate-deployment.sh`
4. Check application logs: `tail -f logs/sales-crm.log`

## 📚 Additional Resources

- [Main README](README.md) - Complete application documentation
- [Deployment Guide](docs/DEPLOYMENT_GUIDE.md) - Comprehensive deployment instructions
- [Kubernetes Guide](docs/KUBERNETES_DEPLOYMENT_GUIDE.md) - Kubernetes-specific deployment
- [Environment Configuration](docs/ENVIRONMENT_CONFIGURATION.md) - Environment setup details

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Make your changes and test them
4. Commit your changes: `git commit -m 'Add amazing feature'`
5. Push to the branch: `git push origin feature/amazing-feature`
6. Submit a pull request

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

---

**Repository URL**: https://github.com/sikhumbuzot-blip/pasp-ict-crm

**Need Help?** Create an issue in the GitHub repository or check the documentation in the `docs/` directory.