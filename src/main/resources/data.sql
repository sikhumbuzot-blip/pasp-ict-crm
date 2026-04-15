-- Data initialization for PostgreSQL database
-- This script is automatically executed when spring.sql.init.mode=always

-- Insert default users with encoded passwords
-- Default password for all users: admin123 (BCrypt encoded)
INSERT INTO users (username, password, email, first_name, last_name, role, active, created_at, last_login) VALUES
('admin', '$2a$10$rr/kSxdJkIhXNPe7.jaFUeYlZS7Ip7Pat6l5HkIrLVJ6/ATUWMRIi', 'admin@salescrm.com', 'System', 'Administrator', 'ADMIN', true, TIMESTAMP '2024-01-01 10:00:00', TIMESTAMP '2024-01-15 09:00:00'),
('thulani.dube', '$2a$10$rr/kSxdJkIhXNPe7.jaFUeYlZS7Ip7Pat6l5HkIrLVJ6/ATUWMRIi', 'thulani.dube@salescrm.com', 'Thulani', 'Dube', 'ADMIN', true, TIMESTAMP '2024-04-07 10:00:00', NULL),
('sales', '$2a$10$rr/kSxdJkIhXNPe7.jaFUeYlZS7Ip7Pat6l5HkIrLVJ6/ATUWMRIi', 'sales@salescrm.com', 'Sales', 'User', 'SALES', true, TIMESTAMP '2024-01-02 10:00:00', TIMESTAMP '2024-01-15 08:00:00'),
('user', '$2a$10$rr/kSxdJkIhXNPe7.jaFUeYlZS7Ip7Pat6l5HkIrLVJ6/ATUWMRIi', 'user@salescrm.com', 'Regular', 'User', 'REGULAR', true, TIMESTAMP '2024-01-03 10:00:00', TIMESTAMP '2024-01-15 07:00:00')
ON CONFLICT (username) DO NOTHING;

-- Insert sample customers
INSERT INTO customers (name, email, phone, company, address, created_at, updated_at, created_by_id) VALUES
('John Doe', 'contact@acme.com', '555-0101', 'Acme Corporation', '123 Business Ave, Suite 100', TIMESTAMP '2024-01-06 10:00:00', TIMESTAMP '2024-01-10 10:00:00', 3),
('Jane Smith', 'info@techstart.com', '555-0102', 'TechStart Inc', '456 Innovation Dr', TIMESTAMP '2024-01-07 10:00:00', TIMESTAMP '2024-01-11 10:00:00', 3),
('Mike Johnson', 'sales@globalsolutions.com', '555-0103', 'Global Solutions LLC', '789 Enterprise Blvd', TIMESTAMP '2024-01-08 10:00:00', TIMESTAMP '2024-01-12 10:00:00', 3)
ON CONFLICT (email) DO NOTHING;

-- Insert sample leads
INSERT INTO leads (title, description, status, estimated_value, created_at, updated_at, customer_id, assigned_to_id) VALUES
('Software License Renewal', 'Annual software license renewal opportunity', 'NEW', 15000.00, TIMESTAMP '2024-01-11 10:00:00', TIMESTAMP '2024-01-11 10:00:00', 1, 3),
('Cloud Migration Project', 'Migration to cloud infrastructure', 'CONTACTED', 50000.00, TIMESTAMP '2024-01-12 10:00:00', TIMESTAMP '2024-01-13 10:00:00', 2, 3),
('Security Audit Services', 'Comprehensive security audit and compliance', 'QUALIFIED', 25000.00, TIMESTAMP '2024-01-13 10:00:00', TIMESTAMP '2024-01-14 10:00:00', 3, 3)
ON CONFLICT DO NOTHING;

-- Insert sample sale transactions
INSERT INTO sale_transactions (amount, sale_date, description, customer_id, sales_user_id, lead_id) VALUES
(30000.00, TIMESTAMP '2024-01-20 15:30:00', 'Software License - Completed', 1, 3, 1),
(22000.00, TIMESTAMP '2023-12-15 14:00:00', 'Previous Consulting Services', 2, 3, null)
ON CONFLICT DO NOTHING;

-- Insert sample interaction logs
INSERT INTO interaction_logs (type, notes, timestamp, customer_id, user_id) VALUES
('CALL', 'Initial contact call - discussed requirements', TIMESTAMP '2024-01-15 14:00:00', 1, 3),
('EMAIL', 'Sent proposal document and pricing information', TIMESTAMP '2024-01-15 15:30:00', 1, 3),
('MEETING', 'In-person meeting to review technical specifications', TIMESTAMP '2024-01-14 10:00:00', 2, 3)
ON CONFLICT DO NOTHING;

-- Insert audit logs
INSERT INTO audit_logs (action, entity_type, entity_id, old_values, new_values, timestamp, user_id) VALUES
('USER_CREATED', 'User', 2, null, 'username=thulani.dube,role=ADMIN,email=thulani.dube@salescrm.com', TIMESTAMP '2024-04-07 10:00:00', 1),
('USER_CREATED', 'User', 3, null, 'username=sales,role=SALES', TIMESTAMP '2024-01-02 10:00:00', 1),
('USER_CREATED', 'User', 4, null, 'username=user,role=REGULAR', TIMESTAMP '2024-01-03 10:00:00', 1),
('CUSTOMER_CREATED', 'Customer', 1, null, 'name=John Doe,email=contact@acme.com', TIMESTAMP '2024-01-06 10:00:00', 3),
('LEAD_CREATED', 'Lead', 1, null, 'title=Software License Renewal,status=NEW', TIMESTAMP '2024-01-11 10:00:00', 3)
ON CONFLICT DO NOTHING;