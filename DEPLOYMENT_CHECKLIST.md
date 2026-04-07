# Deployment Checklist

Use this checklist to ensure a successful deployment of the Sales CRM Application.

## 📋 Pre-Deployment Checklist

### Environment Setup
- [ ] Java 17+ installed and configured
- [ ] Maven 3.6+ installed
- [ ] Git installed and configured
- [ ] Docker installed (for containerized deployment)
- [ ] kubectl configured (for Kubernetes deployment)

### Code Preparation
- [ ] All tests passing (`mvn test`)
- [ ] Application builds successfully (`mvn clean package`)
- [ ] No critical security vulnerabilities
- [ ] Documentation updated
- [ ] Configuration files reviewed

### Security Configuration
- [ ] Default admin password changed from `admin123`
- [ ] Encryption key configured (32 characters minimum)
- [ ] Database credentials secured
- [ ] Environment variables configured for production
- [ ] Sensitive data excluded from version control

## 🚀 GitHub Deployment Checklist

### Repository Setup
- [ ] GitHub repository created: `https://github.com/sikhumbuzot-blip/pasp-ict-crm`
- [ ] Local Git repository initialized
- [ ] Remote origin configured
- [ ] Git user name and email configured

### Initial Deployment
- [ ] Run: `./scripts/github-deploy.sh init`
- [ ] Run: `./scripts/github-deploy.sh push --message "Initial deployment"`
- [ ] Verify code is visible on GitHub
- [ ] README.md displays correctly on GitHub

### CI/CD Setup (Optional)
- [ ] GitHub secrets configured:
  - [ ] `DOCKER_USERNAME`
  - [ ] `DOCKER_PASSWORD`
  - [ ] `DB_PASSWORD`
  - [ ] `ENCRYPTION_KEY`
  - [ ] `ADMIN_PASSWORD`
- [ ] GitHub Actions workflows enabled
- [ ] First CI/CD pipeline run successful

## 🐳 Docker Deployment Checklist

### Docker Environment
- [ ] Docker daemon running
- [ ] Docker Compose installed
- [ ] Navigate to `docker/postgresql/`
- [ ] Copy `.env.example` to `.env`
- [ ] Configure `.env` file with secure passwords

### Docker Deployment
- [ ] Run: `./scripts/deploy.sh docker --profile prod`
- [ ] All containers started successfully
- [ ] Application accessible at `http://localhost:8080`
- [ ] PgAdmin accessible at `http://localhost:8081`
- [ ] Database connection working
- [ ] Login with admin credentials successful

## ☸️ Kubernetes Deployment Checklist

### Kubernetes Environment
- [ ] Kubernetes cluster accessible
- [ ] kubectl configured and connected
- [ ] Sufficient cluster resources available
- [ ] Storage class configured
- [ ] Ingress controller installed (optional)

### Kubernetes Configuration
- [ ] Navigate to `k8s/` directory
- [ ] Update `secret.yaml` with base64 encoded secrets
- [ ] Update `ingress.yaml` with your domain (if using ingress)
- [ ] Review resource limits in deployment files

### Kubernetes Deployment
- [ ] Run: `./scripts/deploy.sh kubernetes --profile prod`
- [ ] All pods running: `kubectl get pods -n sales-crm`
- [ ] Services accessible: `kubectl get services -n sales-crm`
- [ ] Application health check passing
- [ ] Database connectivity verified

## 🔍 Post-Deployment Verification

### Application Testing
- [ ] Application starts within 30 seconds
- [ ] Login page accessible
- [ ] Admin login successful (username: `admin`)
- [ ] Dashboard loads correctly
- [ ] Database operations working
- [ ] All main features functional

### Security Verification
- [ ] Default credentials changed
- [ ] HTTPS configured (production)
- [ ] Security headers present
- [ ] Input validation working
- [ ] Session management working
- [ ] Audit logging enabled

### Performance Testing
- [ ] Application responsive under normal load
- [ ] Database queries performing well
- [ ] Memory usage within acceptable limits
- [ ] No memory leaks detected
- [ ] Log files rotating properly

### Monitoring Setup
- [ ] Health check endpoints responding
- [ ] Application logs being written
- [ ] Error logging working
- [ ] Backup system operational (if configured)
- [ ] Monitoring alerts configured (if applicable)

## 🔧 Environment-Specific Checklists

### Development Environment
- [ ] H2 console accessible at `/h2-console`
- [ ] Debug logging enabled
- [ ] Hot reload working (if configured)
- [ ] Test data loaded

### Production Environment
- [ ] PostgreSQL database configured
- [ ] SSL/TLS enabled
- [ ] Production logging configured
- [ ] Backup system running
- [ ] Monitoring enabled
- [ ] Security hardening applied

## 🚨 Rollback Plan

### Preparation
- [ ] Previous version backup available
- [ ] Database backup created before deployment
- [ ] Rollback procedure documented
- [ ] Rollback tested in staging environment

### Rollback Steps (if needed)
1. [ ] Stop current application
2. [ ] Restore previous version
3. [ ] Restore database backup (if schema changed)
4. [ ] Verify rollback successful
5. [ ] Notify stakeholders

## 📊 Success Criteria

### Functional Requirements
- [ ] All user roles can log in
- [ ] Sales operations working (leads, transactions)
- [ ] Customer management functional
- [ ] Administrative features accessible
- [ ] Reports generating correctly

### Non-Functional Requirements
- [ ] Application starts within 30 seconds
- [ ] Response time under 2 seconds for standard operations
- [ ] 80%+ test coverage maintained
- [ ] No critical security vulnerabilities
- [ ] Documentation complete and accurate

## 📝 Deployment Notes

### Deployment Date: ________________

### Deployed By: ____________________

### Version/Tag: ____________________

### Environment: ____________________

### Issues Encountered:
```
[Record any issues and their resolutions]
```

### Post-Deployment Actions Required:
```
[List any follow-up actions needed]
```

---

## 🆘 Emergency Contacts

- **Technical Lead**: [Contact Information]
- **Database Administrator**: [Contact Information]
- **DevOps Engineer**: [Contact Information]
- **Project Manager**: [Contact Information]

## 📚 Quick Reference

### Useful Commands
```bash
# Check application status
curl http://localhost:8080/actuator/health

# View application logs
tail -f logs/sales-crm.log

# Check Docker containers
docker-compose ps

# Check Kubernetes pods
kubectl get pods -n sales-crm

# Validate deployment
./scripts/validate-deployment.sh
```

### Important URLs
- **Application**: http://localhost:8080
- **GitHub Repository**: https://github.com/sikhumbuzot-blip/pasp-ict-crm
- **Documentation**: [Link to documentation]

---

**✅ Deployment Complete!**

Once all items are checked, your Sales CRM Application is successfully deployed and ready for use.