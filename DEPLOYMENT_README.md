# Sales CRM Application - Deployment Configuration

## Overview

This document provides a comprehensive guide to the deployment configurations available for the Sales CRM Application. The application supports multiple deployment scenarios with production-ready configurations for PostgreSQL database integration and containerized deployment.

## 🚀 Quick Start

### Docker Deployment (Recommended for Development/Testing)
```bash
# Clone repository
git clone https://github.com/sikhumbuzot-blip/pasp-ict-crm.git
cd pasp-ict-crm

# Navigate to Docker configuration
cd docker/postgresql

# Copy and configure environment
cp .env.example .env
nano .env  # Update with your secure passwords

# Deploy the stack
docker-compose up -d

# Access the application
open http://localhost:8080
```

### Kubernetes Deployment (Recommended for Production)
```bash
# Navigate to Kubernetes configuration
cd k8s

# Update configuration
nano secret.yaml     # Update with base64 encoded secrets
nano ingress.yaml    # Update with your domain

# Deploy using the provided script
chmod +x deploy.sh
./deploy.sh deploy

# Access via port-forward (if ingress not configured)
kubectl port-forward service/sales-crm-service 8080:80 -n sales-crm
```

## 📁 Configuration Structure

```
├── src/main/resources/
│   ├── application.properties              # Base configuration
│   ├── application-prod.properties         # Production overrides
│   └── application-test.properties         # Testing overrides
├── docker/postgresql/
│   ├── docker-compose.yml                  # Main Docker stack
│   ├── docker-compose.prod.yml            # Production overrides
│   ├── docker-compose.override.yml        # Development overrides
│   ├── .env.example                       # Environment template
│   └── nginx/nginx.conf                   # Nginx configuration
├── k8s/
│   ├── namespace.yaml                     # Kubernetes namespace
│   ├── configmap.yaml                     # Non-sensitive config
│   ├── secret.yaml                        # Sensitive configuration
│   ├── postgresql-deployment.yaml         # Database deployment
│   ├── sales-crm-deployment.yaml         # Application deployment
│   ├── ingress.yaml                       # External access
│   └── deploy.sh                          # Deployment script
├── docs/
│   ├── DEPLOYMENT_GUIDE.md                # Comprehensive deployment guide
│   ├── KUBERNETES_DEPLOYMENT_GUIDE.md     # Kubernetes-specific guide
│   ├── ENVIRONMENT_CONFIGURATION.md       # Environment configuration
│   └── DEPLOYMENT_CONFIGURATION_SUMMARY.md # Configuration summary
└── Dockerfile                             # Application container
```

## 🔧 Environment Configuration

### Required Environment Variables

| Variable | Description | Example | Required |
|----------|-------------|---------|----------|
| `DB_PASSWORD` | PostgreSQL password | `secure_password123` | ✅ |
| `ENCRYPTION_KEY` | 32-character encryption key | `mySecretEncryptionKey123456789` | ✅ |
| `ADMIN_PASSWORD` | Default admin password | `admin_secure_pass` | ✅ |
| `DB_HOST` | Database host | `localhost` | ✅ |
| `DB_USERNAME` | Database username | `crmuser` | ✅ |
| `DB_NAME` | Database name | `crmdb` | ✅ |

### Optional Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `MAIL_HOST` | SMTP server host | `smtp.gmail.com` |
| `MAIL_PORT` | SMTP server port | `587` |
| `BACKUP_RETENTION_DAYS` | Backup retention period | `30` |
| `NOTIFICATION_ENABLED` | Enable notifications | `true` |

## 🐳 Docker Deployment Options

### Development Environment
```bash
# Use development override
docker-compose -f docker-compose.yml -f docker-compose.override.yml up -d

# Features:
# - Debug port exposed (5005)
# - Development database
# - Hot reload support
# - Reduced resource limits
```

### Production Environment
```bash
# Use production configuration
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# Features:
# - Nginx reverse proxy
# - SSL termination
# - Resource limits
# - Health checks
# - Log rotation
```

### Standard Environment
```bash
# Use standard configuration
docker-compose up -d

# Features:
# - PostgreSQL database
# - PgAdmin interface
# - Application container
# - Basic monitoring
```

## ☸️ Kubernetes Deployment

### Prerequisites
- Kubernetes cluster (v1.20+)
- kubectl configured
- Storage class available
- Ingress controller (optional)

### Deployment Steps

1. **Update Configuration**
   ```bash
   # Generate base64 encoded secrets
   echo -n "your_password" | base64
   
   # Update secret.yaml with encoded values
   nano k8s/secret.yaml
   ```

2. **Deploy Application**
   ```bash
   # Automated deployment
   cd k8s
   ./deploy.sh deploy
   
   # Manual deployment
   kubectl apply -f namespace.yaml
   kubectl apply -f configmap.yaml
   kubectl apply -f secret.yaml
   kubectl apply -f postgresql-pvc.yaml
   kubectl apply -f sales-crm-pvc.yaml
   kubectl apply -f postgres-init-configmap.yaml
   kubectl apply -f postgresql-deployment.yaml
   kubectl apply -f sales-crm-deployment.yaml
   kubectl apply -f ingress.yaml
   ```

3. **Verify Deployment**
   ```bash
   # Check status
   kubectl get pods -n sales-crm
   kubectl get services -n sales-crm
   
   # View logs
   kubectl logs -f deployment/sales-crm-app -n sales-crm
   ```

### Scaling
```bash
# Scale application
kubectl scale deployment/sales-crm-app --replicas=3 -n sales-crm

# Auto-scaling (create HPA)
kubectl autoscale deployment sales-crm-app --cpu-percent=70 --min=2 --max=10 -n sales-crm
```

## 🔒 Security Configuration

### Database Security
- Dedicated database user with minimal privileges
- Strong password requirements
- SSL/TLS connections (configurable)
- Connection pooling with limits

### Application Security
- Secure session cookies
- CSRF protection enabled
- Input validation and sanitization
- Encrypted sensitive data storage
- Security headers (via Nginx)

### Infrastructure Security
- Network isolation (Kubernetes NetworkPolicies)
- Secret management (Kubernetes Secrets)
- Regular security updates
- Firewall rules and access control

## 📊 Monitoring and Health Checks

### Health Endpoints
- `/actuator/health` - Overall application health
- `/actuator/health/db` - Database connectivity
- `/actuator/info` - Application information

### Monitoring Commands
```bash
# Docker
docker-compose logs -f
docker stats

# Kubernetes
kubectl get pods -n sales-crm
kubectl logs -f deployment/sales-crm-app -n sales-crm
kubectl top pods -n sales-crm
```

### Log Locations
- **Docker**: Container logs via `docker-compose logs`
- **Kubernetes**: Pod logs via `kubectl logs`
- **Traditional**: `/var/log/sales-crm/` or systemd journal

## 💾 Backup and Recovery

### Automated Backups
- Daily PostgreSQL backups
- 30-day retention policy (configurable)
- Backup integrity verification
- Application data backups

### Manual Backup Commands
```bash
# Docker
docker-compose exec postgresql pg_dump -U crmuser -d crmdb > backup.sql

# Kubernetes
kubectl exec -it deployment/postgresql -n sales-crm -- pg_dump -U crmuser -d crmdb > backup.sql

# Traditional
pg_dump -h localhost -U crmuser -d crmdb > backup.sql
```

## 🚨 Troubleshooting

### Common Issues

#### Application Won't Start
```bash
# Check logs
docker-compose logs sales-crm-app
kubectl logs deployment/sales-crm-app -n sales-crm
journalctl -u sales-crm -f

# Check configuration
env | grep DB_
kubectl get configmap sales-crm-config -n sales-crm -o yaml
```

#### Database Connection Issues
```bash
# Test connectivity
docker-compose exec sales-crm-app nc -zv postgresql 5432
kubectl exec -it deployment/sales-crm-app -n sales-crm -- nc -zv postgresql-service 5432
telnet localhost 5432

# Check database logs
docker-compose logs postgresql
kubectl logs deployment/postgresql -n sales-crm
```

#### Performance Issues
```bash
# Check resource usage
docker stats
kubectl top pods -n sales-crm
htop

# Check database performance
docker-compose exec postgresql psql -U crmuser -d crmdb -c "SELECT count(*) FROM pg_stat_activity;"
```

## 📈 Performance Tuning

### Application Performance
```bash
# JVM tuning
JAVA_OPTS="-Xmx1g -Xms512m -XX:+UseG1GC -XX:+UseContainerSupport"

# Connection pool tuning
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
```

### Database Performance
```sql
-- PostgreSQL tuning
shared_buffers = 256MB
effective_cache_size = 1GB
work_mem = 4MB
maintenance_work_mem = 64MB
```

### Container Resource Limits
```yaml
resources:
  requests:
    memory: "512Mi"
    cpu: "250m"
  limits:
    memory: "1Gi"
    cpu: "500m"
```

## 🔄 Migration Between Deployments

### From H2 to PostgreSQL
1. Export H2 data (if any)
2. Set up PostgreSQL environment
3. Update configuration to use PostgreSQL
4. Import data (if applicable)

### From Docker to Kubernetes
1. Export database: `docker-compose exec postgresql pg_dump -U crmuser -d crmdb > migration.sql`
2. Build and push container image
3. Deploy to Kubernetes
4. Import database: `kubectl exec -it deployment/postgresql -n sales-crm -- psql -U crmuser -d crmdb < migration.sql`

## 📚 Additional Resources

- [DEPLOYMENT_GUIDE.md](docs/DEPLOYMENT_GUIDE.md) - Comprehensive deployment instructions
- [KUBERNETES_DEPLOYMENT_GUIDE.md](docs/KUBERNETES_DEPLOYMENT_GUIDE.md) - Kubernetes-specific guide
- [ENVIRONMENT_CONFIGURATION.md](docs/ENVIRONMENT_CONFIGURATION.md) - Environment configuration details
- [POSTGRESQL_MIGRATION_GUIDE.md](docs/POSTGRESQL_MIGRATION_GUIDE.md) - Database migration guide

## 🤝 Support

For deployment issues:
1. Check the troubleshooting section above
2. Review application logs for error messages
3. Verify environment configuration
4. Check database connectivity
5. Consult the detailed documentation in the `docs/` directory

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.