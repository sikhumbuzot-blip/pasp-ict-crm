# Sales CRM Application - Deployment Guide

## Overview

This guide provides step-by-step instructions for deploying the Sales CRM Application with PostgreSQL database in various environments.

## Deployment Options

### 1. Traditional Server Deployment
- Manual PostgreSQL installation and configuration
- JAR file deployment with systemd service
- Suitable for dedicated servers and VMs

### 2. Docker Deployment
- Containerized application with PostgreSQL
- Easy scaling and management
- Suitable for cloud environments and development

### 3. Cloud Deployment
- AWS, Azure, or GCP deployment
- Managed database services
- Auto-scaling and high availability

## Prerequisites

### System Requirements
- **CPU**: 2+ cores recommended
- **RAM**: 4GB minimum, 8GB recommended
- **Storage**: 20GB minimum, 50GB recommended
- **OS**: Ubuntu 20.04+, CentOS 8+, or compatible Linux distribution

### Software Requirements
- Java 17 or higher
- PostgreSQL 12 or higher (for traditional deployment)
- Docker and Docker Compose (for containerized deployment)
- Maven 3.6+ (for building from source)

## Traditional Server Deployment

### Step 1: Install Java 17

#### Ubuntu/Debian
```bash
sudo apt update
sudo apt install openjdk-17-jdk
java -version
```

#### CentOS/RHEL
```bash
sudo yum install java-17-openjdk-devel
java -version
```

### Step 2: Install and Configure PostgreSQL

Follow the detailed instructions in [POSTGRESQL_MIGRATION_GUIDE.md](POSTGRESQL_MIGRATION_GUIDE.md).

### Step 3: Build the Application

```bash
# Clone the repository
git clone https://github.com/sikhumbuzot-blip/pasp-ict-crm.git
cd pasp-ict-crm

# Build the application
mvn clean package -DskipTests

# The JAR file will be created in target/sales-crm-0.0.1-SNAPSHOT.jar
```

### Step 4: Configure the Application

Create a production configuration directory:
```bash
sudo mkdir -p /opt/sales-crm/config
sudo mkdir -p /opt/sales-crm/logs
sudo mkdir -p /opt/sales-crm/backups
```

Copy the JAR file:
```bash
sudo cp target/sales-crm-0.0.1-SNAPSHOT.jar /opt/sales-crm/sales-crm.jar
```

Create environment configuration:
```bash
sudo nano /opt/sales-crm/config/application.env
```

Add the following content:
```bash
# Database Configuration
DB_USERNAME=crmuser
DB_PASSWORD=your_secure_password
DB_HOST=localhost
DB_PORT=5432
DB_NAME=crmdb

# Application Configuration
ENCRYPTION_KEY=your_32_character_encryption_key
ADMIN_USERNAME=admin
ADMIN_PASSWORD=your_admin_password

# Email Configuration
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_email_password

# Backup Configuration
BACKUP_DIRECTORY=/opt/sales-crm/backups
BACKUP_RETENTION_DAYS=30
BACKUP_ENABLED=true
```

### Step 5: Create Systemd Service

Create a systemd service file:
```bash
sudo nano /etc/systemd/system/sales-crm.service
```

Add the following content:
```ini
[Unit]
Description=Sales CRM Application
After=network.target postgresql.service
Requires=postgresql.service

[Service]
Type=simple
User=salescrm
Group=salescrm
WorkingDirectory=/opt/sales-crm
ExecStart=/usr/bin/java -Xmx512m -Xms256m -XX:+UseG1GC -jar /opt/sales-crm/sales-crm.jar --spring.profiles.active=prod
EnvironmentFile=/opt/sales-crm/config/application.env
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=sales-crm

[Install]
WantedBy=multi-user.target
```

Create the application user:
```bash
sudo useradd -r -s /bin/false salescrm
sudo chown -R salescrm:salescrm /opt/sales-crm
```

### Step 6: Start the Service

```bash
# Reload systemd configuration
sudo systemctl daemon-reload

# Enable the service to start on boot
sudo systemctl enable sales-crm

# Start the service
sudo systemctl start sales-crm

# Check service status
sudo systemctl status sales-crm

# View logs
sudo journalctl -u sales-crm -f
```

### Step 7: Configure Nginx (Optional)

Install and configure Nginx as a reverse proxy:

```bash
sudo apt install nginx

# Create Nginx configuration
sudo nano /etc/nginx/sites-available/sales-crm
```

Add the following configuration:
```nginx
server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Static resources
    location /css/ {
        proxy_pass http://localhost:8080/css/;
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    location /js/ {
        proxy_pass http://localhost:8080/js/;
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # Health check endpoint
    location /actuator/health {
        proxy_pass http://localhost:8080/actuator/health;
        access_log off;
    }
}
```

Enable the site:
```bash
sudo ln -s /etc/nginx/sites-available/sales-crm /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

## Docker Deployment

### Step 1: Install Docker and Docker Compose

#### Ubuntu
```bash
# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Logout and login again to apply group changes
```

### Step 2: Prepare Docker Environment

```bash
# Clone the repository
git clone https://github.com/sikhumbuzot-blip/pasp-ict-crm.git
cd pasp-ict-crm

# Navigate to Docker configuration
cd docker/postgresql

# Copy environment template
cp .env.example .env

# Edit environment variables
nano .env
```

Update the `.env` file with your secure passwords and configuration.

### Step 3: Deploy with Docker Compose

```bash
# Build and start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Check service status
docker-compose ps

# Access the application
# Application: http://localhost:8080
# PgAdmin: http://localhost:8081
```

### Step 4: Initialize Database (First Time Only)

```bash
# Execute database schema creation
docker-compose exec postgresql psql -U crmuser -d crmdb -f /docker-entrypoint-initdb.d/01-init-database.sql

# Or copy and execute schema manually
docker cp ../../scripts/postgresql/create_schema.sql sales-crm-postgres:/tmp/
docker-compose exec postgresql psql -U crmuser -d crmdb -f /tmp/create_schema.sql
```

### Step 5: Backup and Restore (Docker)

```bash
# Create backup
docker-compose exec postgresql pg_dump -U crmuser -d crmdb > backup.sql

# Restore backup
docker-compose exec -T postgresql psql -U crmuser -d crmdb < backup.sql

# Use the backup script
docker-compose exec sales-crm-app /app/scripts/backup_restore.sh backup full
```

## Cloud Deployment

### AWS Deployment

#### Using AWS RDS and EC2

1. **Create RDS PostgreSQL Instance**
```bash
# Create RDS instance using AWS CLI
aws rds create-db-instance \
    --db-instance-identifier sales-crm-db \
    --db-instance-class db.t3.micro \
    --engine postgres \
    --engine-version 15.3 \
    --master-username crmuser \
    --master-user-password your_secure_password \
    --allocated-storage 20 \
    --vpc-security-group-ids sg-xxxxxxxxx \
    --db-subnet-group-name default \
    --backup-retention-period 7 \
    --storage-encrypted
```

2. **Launch EC2 Instance**
```bash
# Launch EC2 instance
aws ec2 run-instances \
    --image-id ami-0c02fb55956c7d316 \
    --instance-type t3.small \
    --key-name your-key-pair \
    --security-group-ids sg-xxxxxxxxx \
    --subnet-id subnet-xxxxxxxxx \
    --user-data file://user-data.sh
```

3. **User Data Script** (`user-data.sh`)
```bash
#!/bin/bash
yum update -y
yum install -y java-17-openjdk-devel

# Download and install the application
wget https://github.com/sikhumbuzot-blip/pasp-ict-crm/releases/latest/download/sales-crm.jar
mkdir -p /opt/sales-crm
mv sales-crm.jar /opt/sales-crm/

# Create systemd service
cat > /etc/systemd/system/sales-crm.service << 'EOF'
[Unit]
Description=Sales CRM Application
After=network.target

[Service]
Type=simple
User=ec2-user
ExecStart=/usr/bin/java -jar /opt/sales-crm/sales-crm.jar --spring.profiles.active=prod
Environment=DB_HOST=your-rds-endpoint
Environment=DB_USERNAME=crmuser
Environment=DB_PASSWORD=your_secure_password
Restart=always

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable sales-crm
systemctl start sales-crm
```

#### Using AWS ECS with Fargate

1. **Create Task Definition**
```json
{
  "family": "sales-crm-task",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "executionRoleArn": "arn:aws:iam::account:role/ecsTaskExecutionRole",
  "containerDefinitions": [
    {
      "name": "sales-crm",
      "image": "your-account.dkr.ecr.region.amazonaws.com/sales-crm:latest",
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "SPRING_PROFILES_ACTIVE",
          "value": "prod"
        },
        {
          "name": "DB_HOST",
          "value": "your-rds-endpoint"
        }
      ],
      "secrets": [
        {
          "name": "DB_PASSWORD",
          "valueFrom": "arn:aws:secretsmanager:region:account:secret:sales-crm-db-password"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/sales-crm",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ]
}
```

2. **Create ECS Service**
```bash
aws ecs create-service \
    --cluster sales-crm-cluster \
    --service-name sales-crm-service \
    --task-definition sales-crm-task:1 \
    --desired-count 2 \
    --launch-type FARGATE \
    --network-configuration "awsvpcConfiguration={subnets=[subnet-xxxxxxxxx],securityGroups=[sg-xxxxxxxxx],assignPublicIp=ENABLED}"
```

### Azure Deployment

#### Using Azure Database for PostgreSQL and App Service

1. **Create Resource Group**
```bash
az group create --name sales-crm-rg --location eastus
```

2. **Create PostgreSQL Server**
```bash
az postgres server create \
    --resource-group sales-crm-rg \
    --name sales-crm-db \
    --location eastus \
    --admin-user crmuser \
    --admin-password your_secure_password \
    --sku-name GP_Gen5_2 \
    --version 15
```

3. **Create App Service Plan**
```bash
az appservice plan create \
    --name sales-crm-plan \
    --resource-group sales-crm-rg \
    --sku B1 \
    --is-linux
```

4. **Create Web App**
```bash
az webapp create \
    --resource-group sales-crm-rg \
    --plan sales-crm-plan \
    --name sales-crm-app \
    --runtime "JAVA|17-java17"
```

5. **Configure App Settings**
```bash
az webapp config appsettings set \
    --resource-group sales-crm-rg \
    --name sales-crm-app \
    --settings \
    SPRING_PROFILES_ACTIVE=prod \
    DB_HOST=sales-crm-db.postgres.database.azure.com \
    DB_USERNAME=crmuser@sales-crm-db \
    DB_PASSWORD=your_secure_password
```

## Monitoring and Maintenance

### Application Monitoring

#### Health Checks
```bash
# Check application health
curl http://localhost:8080/actuator/health

# Check database connectivity
curl http://localhost:8080/actuator/health/db
```

#### Log Monitoring
```bash
# Traditional deployment
sudo journalctl -u sales-crm -f

# Docker deployment
docker-compose logs -f sales-crm-app

# View specific container logs
docker logs sales-crm-application -f
```

### Database Monitoring

#### PostgreSQL Statistics
```sql
-- Check database size
SELECT pg_size_pretty(pg_database_size('crmdb'));

-- Check table sizes
SELECT schemaname,tablename,pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
FROM pg_tables WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- Check active connections
SELECT count(*) FROM pg_stat_activity WHERE state = 'active';
```

#### Performance Monitoring
```bash
# Use the backup script to show statistics
./scripts/postgresql/backup_restore.sh stats

# Monitor PostgreSQL logs
tail -f /var/log/postgresql/postgresql-*.log
```

### Backup Strategy

#### Automated Backups
```bash
# Create daily backup cron job
crontab -e

# Add this line for daily backups at 2 AM
0 2 * * * /opt/sales-crm/scripts/backup_restore.sh backup full
```

#### Backup Verification
```bash
# Verify backup integrity
./scripts/postgresql/backup_restore.sh verify /path/to/backup.backup

# Test restore in separate environment
./scripts/postgresql/backup_restore.sh restore /path/to/backup.backup full
```

## Security Considerations

### Network Security
- Configure firewall rules to restrict database access
- Use VPN or private networks for database connections
- Enable SSL/TLS for all connections

### Application Security
- Use strong passwords for all accounts
- Regularly rotate encryption keys and passwords
- Keep the application and dependencies updated
- Monitor security logs for suspicious activity

### Database Security
- Enable PostgreSQL SSL connections
- Use connection pooling with proper limits
- Regularly update PostgreSQL to latest version
- Monitor and audit database access

## Troubleshooting

### Common Issues

#### Application Won't Start
```bash
# Check Java version
java -version

# Check application logs
sudo journalctl -u sales-crm -n 50

# Verify database connectivity
telnet localhost 5432
```

#### Database Connection Issues
```bash
# Check PostgreSQL status
sudo systemctl status postgresql

# Test database connection
psql -h localhost -U crmuser -d crmdb

# Check PostgreSQL logs
sudo tail -f /var/log/postgresql/postgresql-*.log
```

#### Performance Issues
```bash
# Check system resources
htop
df -h
free -m

# Check database performance
./scripts/postgresql/backup_restore.sh stats

# Analyze slow queries
SELECT query, mean_time, calls FROM pg_stat_statements ORDER BY mean_time DESC LIMIT 10;
```

### Getting Help

- Check application logs for error messages
- Review PostgreSQL logs for database issues
- Consult the [POSTGRESQL_MIGRATION_GUIDE.md](POSTGRESQL_MIGRATION_GUIDE.md) for database-specific issues
- Check the GitHub repository for known issues and updates

## Conclusion

This deployment guide provides comprehensive instructions for deploying the Sales CRM Application in various environments. Choose the deployment method that best fits your infrastructure and requirements. Always test deployments in a staging environment before deploying to production.