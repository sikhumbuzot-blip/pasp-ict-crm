-- Test data initialization for H2 database
-- This script is automatically executed when spring.sql.init.mode=always

-- Insert test users with encoded passwords (Password123)
INSERT INTO users (id, username, password, email, first_name, last_name, role, active, created_at, last_login) VALUES
(1, 'admin', '$2a$04$rr/kSxdJkIhXNPe7.jaFUeYlZS7Ip7Pat6l5HkIrLVJ6/ATUWMRIi', 'admin@salescrm.com', 'System', 'Administrator', 'ADMIN', true, '2024-01-01 10:00:00', '2024-01-15 09:00:00'),
(2, 'sales1', '$2a$04$rr/kSxdJkIhXNPe7.jaFUeYlZS7Ip7Pat6l5HkIrLVJ6/ATUWMRIi', 'sales1@salescrm.com', 'John', 'Smith', 'SALES', true, '2024-01-02 10:00:00', '2024-01-15 08:00:00'),
(3, 'sales2', '$2a$04$rr/kSxdJkIhXNPe7.jaFUeYlZS7Ip7Pat6l5HkIrLVJ6/ATUWMRIi', 'sales2@salescrm.com', 'Jane', 'Johnson', 'SALES', true, '2024-01-03 10:00:00', '2024-01-15 07:00:00'),
(4, 'regular1', '$2a$04$rr/kSxdJkIhXNPe7.jaFUeYlZS7Ip7Pat6l5HkIrLVJ6/ATUWMRIi', 'regular1@salescrm.com', 'Bob', 'Wilson', 'REGULAR', true, '2024-01-04 10:00:00', '2024-01-15 06:00:00'),
(5, 'inactive', '$2a$04$rr/kSxdJkIhXNPe7.jaFUeYlZS7Ip7Pat6l5HkIrLVJ6/ATUWMRIi', 'inactive@salescrm.com', 'Inactive', 'User', 'REGULAR', false, '2024-01-05 10:00:00', null);

-- Insert test customers
INSERT INTO customers (id, name, email, phone, company, address, created_at, updated_at, created_by_id) VALUES
(1, 'John Doe', 'contact@acme.com', '555-0101', 'Acme Corporation', '123 Business Ave, Suite 100', '2024-01-06 10:00:00', '2024-01-10 10:00:00', 2),
(2, 'Jane Smith', 'info@techstart.com', '555-0102', 'TechStart Inc', '456 Innovation Dr', '2024-01-07 10:00:00', '2024-01-11 10:00:00', 2),
(3, 'Mike Johnson', 'sales@globalsolutions.com', '555-0103', 'Global Solutions LLC', '789 Enterprise Blvd', '2024-01-08 10:00:00', '2024-01-12 10:00:00', 3),
(4, 'Sarah Wilson', 'owner@localbiz.com', '555-0104', 'Local Business Co', '321 Main Street', '2024-01-09 10:00:00', '2024-01-13 10:00:00', 2),
(5, 'David Brown', 'procurement@enterprise.com', '555-0105', 'Enterprise Client Corp', '654 Corporate Plaza', '2024-01-10 10:00:00', '2024-01-14 10:00:00', 3);

-- Insert test leads with various statuses
INSERT INTO leads (id, title, description, status, estimated_value, created_at, updated_at, customer_id, assigned_to_id) VALUES
(1, 'Software License Renewal', 'Annual software license renewal opportunity', 'NEW', 15000.00, '2024-01-11 10:00:00', '2024-01-11 10:00:00', 1, 2),
(2, 'Cloud Migration Project', 'Migration to cloud infrastructure', 'CONTACTED', 50000.00, '2024-01-12 10:00:00', '2024-01-13 10:00:00', 2, 2),
(3, 'Security Audit Services', 'Comprehensive security audit and compliance', 'QUALIFIED', 25000.00, '2024-01-13 10:00:00', '2024-01-14 10:00:00', 3, 3),
(4, 'Training Program', 'Employee training and development program', 'PROPOSAL', 8000.00, '2024-01-14 10:00:00', '2024-01-15 10:00:00', 4, 2),
(5, 'System Integration', 'Integration of existing systems', 'NEGOTIATION', 75000.00, '2024-01-15 10:00:00', '2024-01-16 10:00:00', 5, 3),
(6, 'Mobile App Development', 'Custom mobile application development', 'CLOSED_WON', 30000.00, '2024-01-16 10:00:00', '2024-01-20 10:00:00', 1, 2),
(7, 'Data Analytics Platform', 'Business intelligence and analytics solution', 'CLOSED_LOST', 40000.00, '2024-01-17 10:00:00', '2024-01-21 10:00:00', 3, 3);

-- Insert test sale transactions
INSERT INTO sale_transactions (id, amount, sale_date, description, customer_id, sales_user_id, lead_id) VALUES
(1, 30000.00, '2024-01-20 15:30:00', 'Mobile App Development - Completed', 1, 2, 6),
(2, 22000.00, '2023-12-15 14:00:00', 'Previous Software License', 1, 2, null),
(3, 18000.00, '2024-01-05 16:45:00', 'Consulting Services', 3, 3, null),
(4, 35000.00, '2023-11-20 11:30:00', 'System Upgrade', 5, 3, null);

-- Insert test interaction logs
INSERT INTO interaction_logs (id, type, notes, timestamp, customer_id, user_id) VALUES
(1, 'CALL', 'Initial contact call - discussed requirements', '2024-01-15 14:00:00', 1, 2),
(2, 'EMAIL', 'Sent proposal document and pricing information', '2024-01-15 15:30:00', 1, 2),
(3, 'MEETING', 'In-person meeting to review technical specifications', '2024-01-14 10:00:00', 2, 2),
(4, 'CALL', 'Follow-up call regarding contract terms', '2024-01-15 11:00:00', 3, 3),
(5, 'EMAIL', 'Sent updated proposal with revised timeline', '2024-01-13 16:00:00', 4, 2),
(6, 'MEETING', 'Demo session for the proposed solution', '2024-01-12 14:30:00', 5, 3),
(7, 'NOTE', 'Customer requested additional features', '2024-01-15 09:30:00', 1, 2),
(8, 'CALL', 'Closing call - finalized terms and conditions', '2024-01-08 16:00:00', 1, 2);

-- Insert test audit logs
INSERT INTO audit_logs (id, action, entity_type, entity_id, old_values, new_values, timestamp, user_id) VALUES
(1, 'USER_CREATED', 'User', 4, null, 'username=regular1,role=REGULAR', '2024-01-04 10:00:00', 1),
(2, 'CUSTOMER_CREATED', 'Customer', 1, null, 'name=John Doe,email=contact@acme.com', '2024-01-06 10:00:00', 2),
(3, 'LEAD_CREATED', 'Lead', 1, null, 'title=Software License Renewal,status=NEW', '2024-01-11 10:00:00', 2),
(4, 'LEAD_STATUS_UPDATED', 'Lead', 2, 'status=NEW', 'status=CONTACTED', '2024-01-13 10:00:00', 2),
(5, 'SALE_CREATED', 'SaleTransaction', 1, null, 'amount=30000.00,customer_id=1', '2024-01-20 15:30:00', 2),
(6, 'CUSTOMER_UPDATED', 'Customer', 2, 'phone=555-0102', 'phone=555-0199', '2024-01-11 10:00:00', 2),
(7, 'USER_ROLE_UPDATED', 'User', 5, 'role=REGULAR,active=true', 'role=REGULAR,active=false', '2024-01-07 10:00:00', 1);

-- Reset sequences to avoid conflicts
ALTER SEQUENCE users_seq RESTART WITH 10;
ALTER SEQUENCE customers_seq RESTART WITH 10;
ALTER SEQUENCE leads_seq RESTART WITH 10;
ALTER SEQUENCE sale_transactions_seq RESTART WITH 10;
ALTER SEQUENCE interaction_logs_seq RESTART WITH 10;
ALTER SEQUENCE audit_logs_seq RESTART WITH 10;