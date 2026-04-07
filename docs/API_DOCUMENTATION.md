# API Documentation

## Overview

The Sales CRM Application provides both web interface endpoints and JSON API endpoints for integration with external systems. All API endpoints require authentication and follow RESTful conventions.

## Authentication

All API endpoints require authentication via session cookies. Users must first authenticate through the web interface at `/login`.

### Session Management

- **Session Timeout**: 30 minutes of inactivity
- **Session Cookie**: `JSESSIONID`
- **CSRF Protection**: Required for POST/PUT/DELETE operations

## Response Formats

### Success Response
```json
{
  "success": true,
  "data": { ... },
  "message": "Operation completed successfully"
}
```

### Error Response
```json
{
  "success": false,
  "error": "Error description",
  "code": "ERROR_CODE"
}
```

## Dashboard API

### Get Real-time Metrics
**Endpoint**: `GET /dashboard/metrics`
**Access**: All authenticated users
**Description**: Returns role-specific metrics and statistics

**Response Example (Admin)**:
```json
{
  "totalUsers": 15,
  "activeUsers": 12,
  "totalCustomers": 245,
  "totalLeads": 89,
  "totalSales": 156,
  "totalRevenue": 125000.00,
  "systemHealth": "HEALTHY",
  "databaseStatus": "CONNECTED"
}
```

**Response Example (Sales)**:
```json
{
  "assignedLeads": 12,
  "openLeads": 8,
  "closedLeads": 4,
  "conversionRate": 33.33,
  "totalSales": 8,
  "totalRevenue": 15000.00,
  "monthlyTarget": 20000.00,
  "targetProgress": 75.0
}
```

### Get System Health
**Endpoint**: `GET /dashboard/health`
**Access**: Admin only
**Description**: Returns detailed system health information

**Response Example**:
```json
{
  "status": "HEALTHY",
  "database": {
    "status": "UP",
    "connectionPool": {
      "active": 2,
      "idle": 8,
      "max": 10
    }
  },
  "memory": {
    "used": "512MB",
    "free": "1024MB",
    "max": "2048MB"
  },
  "disk": {
    "used": "2.5GB",
    "free": "47.5GB",
    "total": "50GB"
  }
}
```

## Sales API

### Get Sales Metrics
**Endpoint**: `GET /sales/api/metrics`
**Access**: Admin, Sales
**Description**: Returns detailed sales performance metrics

**Response Example**:
```json
{
  "totalSales": 156,
  "totalRevenue": 125000.00,
  "averageSaleAmount": 801.28,
  "conversionRate": 65.5,
  "salesByMonth": [
    {"month": "2024-01", "sales": 12, "revenue": 9500.00},
    {"month": "2024-02", "sales": 15, "revenue": 12000.00}
  ],
  "topPerformers": [
    {"user": "john.doe", "sales": 25, "revenue": 20000.00},
    {"user": "jane.smith", "sales": 22, "revenue": 18500.00}
  ]
}
```

### Get Pipeline Data
**Endpoint**: `GET /sales/api/pipeline`
**Access**: Admin, Sales
**Description**: Returns lead counts by pipeline stage

**Response Example**:
```json
{
  "NEW": 15,
  "CONTACTED": 12,
  "QUALIFIED": 8,
  "PROPOSAL": 5,
  "NEGOTIATION": 3,
  "CLOSED_WON": 45,
  "CLOSED_LOST": 22
}
```

## Customer API

### Search Customers
**Endpoint**: `GET /customers/api/search?query={searchTerm}`
**Access**: Admin, Sales
**Description**: Search customers by name, email, or company

**Parameters**:
- `query` (string, required): Search term

**Response Example**:
```json
[
  {
    "id": 1,
    "name": "John Smith",
    "email": "john.smith@example.com",
    "phone": "+1-555-0123",
    "company": "Acme Corp",
    "address": "123 Main St, City, State 12345",
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-02-01T14:20:00"
  }
]
```

### Get Customer Details
**Endpoint**: `GET /customers/api/{id}`
**Access**: Admin, Sales
**Description**: Get detailed customer information

**Response Example**:
```json
{
  "id": 1,
  "name": "John Smith",
  "email": "john.smith@example.com",
  "phone": "+1-555-0123",
  "company": "Acme Corp",
  "address": "123 Main St, City, State 12345",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-02-01T14:20:00",
  "createdBy": {
    "id": 2,
    "username": "sales.user",
    "firstName": "Sales",
    "lastName": "User"
  }
}
```

### Get Customer Interactions
**Endpoint**: `GET /customers/api/{id}/interactions`
**Access**: Admin, Sales
**Description**: Get customer interaction history

**Response Example**:
```json
[
  {
    "id": 1,
    "type": "CALL",
    "notes": "Discussed product requirements and pricing",
    "timestamp": "2024-02-01T14:30:00",
    "user": {
      "id": 2,
      "username": "sales.user",
      "firstName": "Sales",
      "lastName": "User"
    }
  }
]
```

### Get Customer Statistics
**Endpoint**: `GET /customers/api/{id}/stats`
**Access**: Admin, Sales
**Description**: Get customer statistics and metrics

**Response Example**:
```json
{
  "interactionCount": 15,
  "leadCount": 3,
  "transactionCount": 2,
  "totalRevenue": 5000.00,
  "lastInteraction": "2024-02-01T14:30:00",
  "customerSince": "2024-01-15T10:30:00"
}
```

## Admin API

### Get System Statistics
**Endpoint**: `GET /admin/api/statistics`
**Access**: Admin only
**Description**: Get comprehensive system statistics

**Response Example**:
```json
{
  "totalUsers": 15,
  "activeUsers": 12,
  "totalCustomers": 245,
  "totalLeads": 89,
  "openLeads": 45,
  "totalSales": 156,
  "totalRevenue": 125000.00,
  "averageSaleAmount": 801.28,
  "conversionRate": 65.5,
  "systemUptime": "15 days, 6 hours",
  "databaseSize": "125MB"
}
```

### Get Performance Metrics
**Endpoint**: `GET /admin/api/performance`
**Access**: Admin only
**Description**: Get system performance metrics

**Response Example**:
```json
{
  "responseTime": {
    "average": 150,
    "p95": 300,
    "p99": 500
  },
  "throughput": {
    "requestsPerSecond": 25.5,
    "requestsPerMinute": 1530
  },
  "memory": {
    "heapUsed": 512,
    "heapMax": 2048,
    "nonHeapUsed": 128
  },
  "database": {
    "connectionPoolActive": 2,
    "connectionPoolIdle": 8,
    "queryExecutionTime": 45
  }
}
```

### Get Sales Performance by User
**Endpoint**: `GET /admin/api/sales-performance`
**Access**: Admin only
**Description**: Get sales performance metrics by user

**Response Example**:
```json
{
  "john.doe": {
    "totalSales": 25,
    "totalRevenue": 20000.00,
    "averageSaleAmount": 800.00,
    "conversionRate": 70.0,
    "leadsAssigned": 35,
    "leadsConverted": 25
  },
  "jane.smith": {
    "totalSales": 22,
    "totalRevenue": 18500.00,
    "averageSaleAmount": 840.91,
    "conversionRate": 68.75,
    "leadsAssigned": 32,
    "leadsConverted": 22
  }
}
```

### Get User Activity
**Endpoint**: `GET /admin/api/users/{id}/activity`
**Access**: Admin only
**Description**: Get activity summary for specific user

**Response Example**:
```json
{
  "userId": 2,
  "username": "john.doe",
  "lastLogin": "2024-02-01T09:15:00",
  "loginCount": 45,
  "leadsCreated": 15,
  "salesCompleted": 8,
  "customersCreated": 12,
  "interactionsLogged": 35,
  "activityScore": 85.5
}
```

### Get Recent Activity
**Endpoint**: `GET /admin/api/activity?hours={hours}`
**Access**: Admin only
**Description**: Get recent system activity

**Parameters**:
- `hours` (integer, optional): Number of hours to look back (default: 24)

**Response Example**:
```json
[
  {
    "id": 1,
    "action": "USER_LOGIN",
    "entityType": "USER",
    "entityId": 2,
    "timestamp": "2024-02-01T09:15:00",
    "user": {
      "id": 2,
      "username": "john.doe"
    },
    "details": "Successful login from IP 192.168.1.100"
  }
]
```

### Get Security Events
**Endpoint**: `GET /admin/api/security-events`
**Access**: Admin only
**Description**: Get recent security events and incidents

**Response Example**:
```json
[
  {
    "id": 1,
    "action": "FAILED_LOGIN",
    "entityType": "USER",
    "entityId": null,
    "timestamp": "2024-02-01T08:45:00",
    "details": "Failed login attempt for username 'admin' from IP 192.168.1.200",
    "severity": "MEDIUM"
  }
]
```

### Export Reports
**Endpoint**: `GET /admin/api/reports/export?type={type}&format={format}&period={period}`
**Access**: Admin only
**Description**: Export reports in PDF or CSV format

**Parameters**:
- `type` (string, required): Report type (sales, users, customers)
- `format` (string, required): Export format (PDF, CSV)
- `period` (string, optional): Time period (daily, weekly, monthly, quarterly, yearly)

**Response**: Binary data (PDF or CSV file)

### Get Backup Status
**Endpoint**: `GET /admin/api/backup-status`
**Access**: Admin only
**Description**: Get current backup status

**Response Example**:
```json
{
  "status": "COMPLETED",
  "lastBackup": "2024-02-01T02:00:00",
  "nextBackup": "2024-02-02T02:00:00",
  "backupSize": "125MB",
  "backupLocation": "/app/backups/backup_2024-02-01_02-00-00"
}
```

### Create Manual Backup
**Endpoint**: `POST /admin/backups/create`
**Access**: Admin only
**Description**: Create a manual backup

**Response Example**:
```json
{
  "success": true,
  "message": "Backup created successfully",
  "backupId": "backup_2024-02-01_14-30-15"
}
```

### Verify Backup
**Endpoint**: `POST /admin/backups/{backupId}/verify`
**Access**: Admin only
**Description**: Verify backup integrity

**Response Example**:
```json
{
  "valid": true,
  "message": "Backup is valid",
  "checksum": "a1b2c3d4e5f6...",
  "verifiedAt": "2024-02-01T14:35:00"
}
```

### Test Notifications
**Endpoint**: `POST /admin/notifications/test`
**Access**: Admin only
**Description**: Test notification system

**Response Example**:
```json
{
  "success": true,
  "message": "Test notification sent successfully",
  "timestamp": "2024-02-01T14:40:00"
}
```

## Error Codes

| Code | Description |
|------|-------------|
| `AUTH_REQUIRED` | Authentication required |
| `ACCESS_DENIED` | Insufficient permissions |
| `INVALID_INPUT` | Input validation failed |
| `NOT_FOUND` | Resource not found |
| `DUPLICATE_ENTRY` | Duplicate data entry |
| `SYSTEM_ERROR` | Internal system error |
| `DATABASE_ERROR` | Database operation failed |
| `BACKUP_FAILED` | Backup operation failed |
| `VALIDATION_ERROR` | Data validation error |

## Rate Limiting

API endpoints are subject to rate limiting to prevent abuse:

- **General endpoints**: 100 requests per minute per user
- **Search endpoints**: 50 requests per minute per user
- **Export endpoints**: 10 requests per minute per user
- **Admin endpoints**: 200 requests per minute per admin user

## Integration Examples

### JavaScript/AJAX Example
```javascript
// Get customer data
fetch('/customers/api/1', {
    method: 'GET',
    credentials: 'same-origin', // Include session cookie
    headers: {
        'Content-Type': 'application/json'
    }
})
.then(response => response.json())
.then(data => console.log(data))
.catch(error => console.error('Error:', error));
```

### cURL Example
```bash
# Login first to establish session
curl -c cookies.txt -X POST \
  -d "username=admin&password=admin123" \
  http://localhost:8080/login

# Use session cookie for API calls
curl -b cookies.txt \
  http://localhost:8080/customers/api/1
```

### Python Example
```python
import requests

# Create session
session = requests.Session()

# Login
login_data = {'username': 'admin', 'password': 'admin123'}
session.post('http://localhost:8080/login', data=login_data)

# Make API calls
response = session.get('http://localhost:8080/customers/api/1')
customer_data = response.json()
print(customer_data)
```

## Webhook Support

The application supports webhooks for real-time notifications (future enhancement):

### Webhook Events
- `customer.created`
- `customer.updated`
- `lead.created`
- `lead.status_changed`
- `sale.completed`
- `user.login`
- `security.incident`

### Webhook Payload Example
```json
{
  "event": "lead.status_changed",
  "timestamp": "2024-02-01T14:30:00Z",
  "data": {
    "leadId": 123,
    "oldStatus": "QUALIFIED",
    "newStatus": "PROPOSAL",
    "changedBy": "john.doe"
  }
}
```

## API Versioning

Currently, the API is version 1.0. Future versions will be supported through URL versioning:

- Current: `/api/...` (implied v1)
- Future: `/api/v2/...`

## Support

For API support and questions:
- Review this documentation
- Check the main README.md troubleshooting section
- Create GitHub issues for bugs or feature requests
- Examine controller source code for detailed implementation