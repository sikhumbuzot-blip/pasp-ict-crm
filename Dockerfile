# Multi-stage Dockerfile for Sales CRM Application

# Build stage
FROM maven:3.9-openjdk-17-slim AS build

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM openjdk:17-jre-slim

# Install required packages
RUN apt-get update && apt-get install -y \
    curl \
    postgresql-client \
    && rm -rf /var/lib/apt/lists/*

# Create application user
RUN groupadd -r salescrm && useradd -r -g salescrm salescrm

# Set working directory
WORKDIR /app

# Copy the built JAR file
COPY --from=build /app/target/sales-crm-*.jar app.jar

# Create directories for logs and backups
RUN mkdir -p /app/logs /app/backups && \
    chown -R salescrm:salescrm /app

# Copy configuration files
COPY src/main/resources/application-prod.properties /app/config/
COPY scripts/postgresql/backup_restore.sh /app/scripts/

# Make scripts executable
RUN chmod +x /app/scripts/backup_restore.sh

# Switch to application user
USER salescrm

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Set JVM options
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:+UseContainerSupport"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=prod"]