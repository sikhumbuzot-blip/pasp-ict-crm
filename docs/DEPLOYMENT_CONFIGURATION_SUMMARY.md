# Deployment Configuration Summary

## Overview

This document provides a comprehensive summary of all deployment configurations available for the Sales CRM Application. The application supports multiple deployment scenarios with environment-specific configurations for development, testing, and production environments.

## Deployment Options

### 1. Traditional Server Deployment
- **Target Environment**: Dedicated servers, VMs
- **Database**: PostgreSQL (external installation)
- **Configuration**: JAR file with systemd service
- **Scaling**: Manual vertical scaling
- **Best For**: Small to medium deployments, on-premises infrastructure

### 2. Docker Deployment
- **Target Environment**: Docker-enabled systems
- **Database**: PostgreSQL container
- **Configuration**: Docker Compose stack
- **Scaling**: Manual container scaling
- **Best For**: Development, testing, small production deployments

### 3. Kubernetes Deployment
- **Target Environment**: Kubernetes clusters
- **Database**: PostgreSQL pod with persistent storage
- **Configuration**: Kubernetes manifests
- **Scaling**: Horizontal pod autoscaling
- **Best For**: Large-scale production, cloud-native environments

## Configuration Files Matrix

| Deployment Type | Configuration Files | Purpose |
|----------------|-------------------|---------|
| **Traditional** | `application-prod.properties` | Production Spring Boot configuration |
| | `systemd service file` | System service configuration |
| | Environment variables file | Runtime configuration |
| **Docker** | `Dockerfile` | Application container definition |
| | `docker-compose.yml` | Multi-container stack |
| | `.env` file | Environment variables |
| | `application-prod.properties` | Spring Boot production config |
| **Kubernetes** | `namespace.yaml` | Kubernetes namespace |
| | `configmap.yaml` | Non-sensitive configuration |
| | `secret.yaml` | Sensitive configuration |
| | `*-deployment.yaml` | Application and database deployments |
| | `*-pvc.yaml` | Persistent volume claims |
| | `ingress.yaml` | External access configuration |

## Environment-Specific Configurations

### Development Environment
```properties
# Profile: dev
# Database: H2 in-memory
# Features: Debug logging, H2 console, hot reload
spring.profiles.active=dev
spring.datasource.url=jdbc:h2:mem:crmdb
spring.h2.console.enabled=true
logging.level.com.pasp.ict.salescrm=DEBUG
```

### Testing Environment
```properties
# Profile: test
# Database: H2 in-memory (isolated)
# Features: Minimal logging, fast startup
spring.profiles.active=test
spring.datasource.url=jdbc:h2:mem:testdb
logging.level.root=WARN
```

### Production Environment
```properties
# Profile: prod
# Database: PostgreSQL
# Features: Optimized performance, security hardening
spring.profiles.active=prod
spring.datasource.url=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
spring.jpa.hibernate.ddl-auto=validate
server.servlet.session.cookie.secure=true
```

## Required Environment Variables

### Core Configuration (All Deployments)
```bash
# Database Configuration
DB_HOST=localhost                    # Database host
DB_PORT=5432                        # Database port
DB_NAME=crmdb                       # Database name
DB_USERNAME=crmuser                 # Database username
DB_PASSWORD=secure_password         # Database password (REQUIRED)

# Application Security
ENCRYPTION_KEY=32_character_key     # Encryption key (REQUIRED)
ADMIN_USERNAME=admin                # Default admin username
ADMIN_PASSWORD=secure_password      # Admin password (REQUIRED)
```

### Optional Configuration
```bash
# Email Configuration
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=email@domain.com
MAIL_PASSWORD=email_password

# Backup Configuration
BACKUP_DIRECTORY=/var/backups/salescrm
BACKUP_RETENTION_DAYS=30
BACKUP_ENABLED=true

# Notification Configuration
NOTIFICATION_ENABLED=true
NOTIFICATION_FROM_EMAIL=noreply@company.com
NOTIFICATION_SYSTEM_NAME=Sales CRM System
NOTIFICATION_ADMIN_EMAILS=admin@company.com
```

## Deployment Commands Reference

### Traditional Deployment
```bash
# Build application
mvn clean package -DskipTests

# Create directories
sudo mkdir -p /opt/sales-crm/{config,logs,backups}

# Copy JAR file
sudo cp target/sales-crm-*.jar /opt/sales-crm/sales-crm.jar

# Create systemd service
sudo systemctl enable sales-crm
sudo systemctl start sales-crm
```

### Docker Deployment
```bash
# Navigate to Docker configuration
cd docker/postgresql

# Copy environment template
cp .env.example .env

# Edit environment variables
nano .env

# Deploy stack
docker-compose up -d

# View logs
docker-compose logs -f
```

### Kubernetes Deployment
```bash
# Navigate to Kubernetes configuration
cd k8s

# Update secrets and configuration
nano secret.yaml
nano ingress.yaml

# Deploy using script
chmod +x deploy.sh
./deploy.sh deploy

# Or deploy manually
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

## Security Configuration Summary

### Database Security
- **Authentication**: Username/password authentication
- **Encryption**: SSL/TLS connections (configurable)
- **Access Control**: Dedicated application user with minimal privileges
- **Connection Pooling**: HikariCP with connection limits

### Application Security
- **Session Management**: Secure cookies, 30-minute timeout
- **CSRF Protection**: Enabled for all state-changing operations
- **Input Validation**: Comprehensive validation and sanitization
- **Data Encryption**: Sensitive data encrypted at rest
- **Password Policy**: Minimum 8 characters with complexity requirements

### Infrastructure Security
- **Network Isolation**: Private networks for database connections
- **Firewall Rules**: Restricted access to necessary ports only
- **Secret Management**: Environment variables for sensitive data
- **Regular Updates**: Automated security updates where possible

## Performance Configuration

### Database Performance
```properties
# Connection Pool Settings
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.connection-timeout=20000

# PostgreSQL Recommendations
shared_buffers=256MB
effective_cache_size=1GB
work_mem=4MB
maintenance_work_mem=64MB
```

### Application Performance
```bash
# JVM Settings
JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:+UseContainerSupport"

# Caching Settings
spring.thymeleaf.cache=true
spring.web.resources.cache.cachecontrol.max-age=31536000
```

### Container Resource Limits
```yaml
# Kubernetes Resource Limits
resources:
  requests:
    memory: "512Mi"
    cpu: "250m"
  limits:
    memory: "1Gi"
    cpu: "500m"
```

## Monitoring and Health Checks

### Health Endpoints
- `/actuator/health` - Overall application health
- `/actuator/health/db` - Database connectivity
- `/actuator/info` - Application information
- `/actuator/metrics` - Application metrics

### Logging Configuration
```xml
<!-- Production Logging -->
<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/sales-crm.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
        <fileNamePattern>logs/sales-crm.%d{yyyy-MM-dd}.%i.gz</fileNamePattern>
        <maxFileSize>100MB</maxFileSize>
        <maxHistory>30</maxHistory>
    </rollingPolicy>
</appender>
```

### Monitoring Commands
```bash
# Traditional Deployment
sudo systemctl status sales-crm
sudo journalctl -u sales-crm -f

# Docker Deployment
docker-compose logs -f
docker stats

# Kubernetes Deployment
kubectl get pods -n sales-crm
kubectl logs -f deployment/sales-crm-app -n sales-crm
kubectl top pods -n sales-crm
```

## Backup and Recovery

### Backup Strategy
- **Database Backups**: Daily automated backups with 30-day retention
- **Application Data**: File-based backups of logs and configuration
- **Backup Verification**: Automated backup integrity checks
- **Recovery Testing**: Regular restore procedure testing

### Backup Commands
```bash
# Traditional/Docker
pg_dump -h localhost -U crmuser -d crmdb > backup_$(date +%Y%m%d).sql

# Kubernetes
kubectl exec -it deployment/postgresql -n sales-crm -- pg_dump -U crmuser -d crmdb > backup.sql

# Using backup script
./scripts/postgresql/backup_restore.sh backup full
```

## Scaling Considerations

### Vertical Scaling
- **Traditional**: Increase server resources (CPU, RAM)
- **Docker**: Adjust container resource limits
- **Kubernetes**: Modify resource requests and limits

### Horizontal Scaling
- **Traditional**: Load balancer with multiple application instances
- **Docker**: Docker Swarm or multiple compose stacks
- **Kubernetes**: Horizontal Pod Autoscaler (HPA)

### Database Scaling
- **Read Replicas**: PostgreSQL streaming replication
- **Connection Pooling**: PgBouncer for connection management
- **Managed Services**: Cloud database services (RDS, Cloud SQL)

## Troubleshooting Quick Reference

### Common Issues
| Issue | Traditional | Docker | Kubernetes |
|-------|------------|--------|------------|
| **App won't start** | `journalctl -u sales-crm` | `docker-compose logs` | `kubectl logs deployment/sales-crm-app` |
| **DB connection** | `psql -h localhost -U crmuser` | `docker-compose exec postgresql psql` | `kubectl exec -it deployment/postgresql -- psql` |
| **Port conflicts** | `netstat -tlnp \| grep 8080` | `docker-compose ps` | `kubectl get services` |
| **Storage issues** | `df -h` | `docker volume ls` | `kubectl get pvc` |

### Performance Issues
```bash
# Check resource usage
# Traditional
htop
df -h

# Docker
docker stats
docker system df

# Kubernetes
kubectl top pods -n sales-crm
kubectl top nodes
```

## Migration Between Deployments

### From Traditional to Docker
1. Export database: `pg_dump -U crmuser -d crmdb > migration.sql`
2. Set up Docker environment with `.env` file
3. Deploy Docker stack: `docker-compose up -d`
4. Import database: `docker-compose exec -T postgresql psql -U crmuser -d crmdb < migration.sql`

### From Docker to Kubernetes
1. Export database: `docker-compose exec postgresql pg_dump -U crmuser -d crmdb > migration.sql`
2. Build and push container image to registry
3. Update Kubernetes manifests with image reference
4. Deploy to Kubernetes: `./k8s/deploy.sh deploy`
5. Import database: `kubectl exec -it deployment/postgresql -n sales-crm -- psql -U crmuser -d crmdb < migration.sql`

## Best Practices Summary

### Configuration Management
- Use environment variables for sensitive data
- Maintain separate configurations for each environment
- Version control all configuration files (except secrets)
- Document all configuration options

### Security
- Never commit secrets to version control
- Use strong, unique passwords for all accounts
- Enable SSL/TLS for all connections
- Regular security updates and patches

### Monitoring
- Implement comprehensive health checks
- Set up log aggregation and monitoring
- Configure alerts for critical metrics
- Regular backup verification

### Deployment
- Use blue-green deployment for zero downtime
- Implement proper rollback procedures
- Test deployments in staging environment
- Maintain deployment documentation

## Conclusion

This configuration summary provides a comprehensive overview of all deployment options for the Sales CRM Application. Choose the deployment method that best fits your infrastructure requirements, scaling needs, and operational capabilities.

For detailed instructions, refer to:
- [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) - Traditional and Docker deployment
- [KUBERNETES_DEPLOYMENT_GUIDE.md](KUBERNETES_DEPLOYMENT_GUIDE.md) - Kubernetes deployment
- [ENVIRONMENT_CONFIGURATION.md](ENVIRONMENT_CONFIGURATION.md) - Environment-specific configuration
- [POSTGRESQL_MIGRATION_GUIDE.md](POSTGRESQL_MIGRATION_GUIDE.md) - Database migration