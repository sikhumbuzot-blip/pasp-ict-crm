-- PostgreSQL Data Migration Script
-- This script helps migrate data from H2 to PostgreSQL
-- Run this after exporting data from H2 to CSV files

-- Disable foreign key checks temporarily for data import
SET session_replication_role = replica;

-- Import data from CSV files (adjust paths as needed)
-- Note: CSV files should be exported from H2 database first

-- Import users data
-- \copy users(id, username, password, email, first_name, last_name, role, active, created_at, last_login) FROM '/path/to/users.csv' WITH CSV HEADER;

-- Import customers data
-- \copy customers(id, name, email, phone, company, address, created_at, updated_at, created_by_id) FROM '/path/to/customers.csv' WITH CSV HEADER;

-- Import leads data
-- \copy leads(id, title, description, status, estimated_value, created_at, updated_at, customer_id, assigned_to_id) FROM '/path/to/leads.csv' WITH CSV HEADER;

-- Import sale_transactions data
-- \copy sale_transactions(id, amount, sale_date, description, created_at, customer_id, sales_user_id, lead_id) FROM '/path/to/sale_transactions.csv' WITH CSV HEADER;

-- Import interaction_logs data
-- \copy interaction_logs(id, type, notes, timestamp, customer_id, user_id) FROM '/path/to/interaction_logs.csv' WITH CSV HEADER;

-- Import audit_logs data
-- \copy audit_logs(id, action, entity_type, entity_id, old_values, new_values, timestamp, ip_address, user_agent, user_id) FROM '/path/to/audit_logs.csv' WITH CSV HEADER;

-- Re-enable foreign key checks
SET session_replication_role = DEFAULT;

-- Update sequences to current maximum values
SELECT setval('users_seq', COALESCE((SELECT MAX(id) FROM users), 1));
SELECT setval('customers_seq', COALESCE((SELECT MAX(id) FROM customers), 1));
SELECT setval('leads_seq', COALESCE((SELECT MAX(id) FROM leads), 1));
SELECT setval('sale_transactions_seq', COALESCE((SELECT MAX(id) FROM sale_transactions), 1));
SELECT setval('interaction_logs_seq', COALESCE((SELECT MAX(id) FROM interaction_logs), 1));
SELECT setval('audit_logs_seq', COALESCE((SELECT MAX(id) FROM audit_logs), 1));

-- Verify data integrity
DO $$
DECLARE
    user_count INTEGER;
    customer_count INTEGER;
    lead_count INTEGER;
    transaction_count INTEGER;
    interaction_count INTEGER;
    audit_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO user_count FROM users;
    SELECT COUNT(*) INTO customer_count FROM customers;
    SELECT COUNT(*) INTO lead_count FROM leads;
    SELECT COUNT(*) INTO transaction_count FROM sale_transactions;
    SELECT COUNT(*) INTO interaction_count FROM interaction_logs;
    SELECT COUNT(*) INTO audit_count FROM audit_logs;
    
    RAISE NOTICE 'Data migration summary:';
    RAISE NOTICE 'Users: %', user_count;
    RAISE NOTICE 'Customers: %', customer_count;
    RAISE NOTICE 'Leads: %', lead_count;
    RAISE NOTICE 'Sale Transactions: %', transaction_count;
    RAISE NOTICE 'Interaction Logs: %', interaction_count;
    RAISE NOTICE 'Audit Logs: %', audit_count;
END $$;

-- Verify foreign key constraints
DO $$
DECLARE
    constraint_violations INTEGER := 0;
BEGIN
    -- Check customers.created_by_id references users.id
    SELECT COUNT(*) INTO constraint_violations
    FROM customers c
    LEFT JOIN users u ON c.created_by_id = u.id
    WHERE u.id IS NULL;
    
    IF constraint_violations > 0 THEN
        RAISE EXCEPTION 'Foreign key constraint violation: % customers have invalid created_by_id', constraint_violations;
    END IF;
    
    -- Check leads.customer_id references customers.id
    SELECT COUNT(*) INTO constraint_violations
    FROM leads l
    LEFT JOIN customers c ON l.customer_id = c.id
    WHERE c.id IS NULL;
    
    IF constraint_violations > 0 THEN
        RAISE EXCEPTION 'Foreign key constraint violation: % leads have invalid customer_id', constraint_violations;
    END IF;
    
    -- Check leads.assigned_to_id references users.id
    SELECT COUNT(*) INTO constraint_violations
    FROM leads l
    LEFT JOIN users u ON l.assigned_to_id = u.id
    WHERE l.assigned_to_id IS NOT NULL AND u.id IS NULL;
    
    IF constraint_violations > 0 THEN
        RAISE EXCEPTION 'Foreign key constraint violation: % leads have invalid assigned_to_id', constraint_violations;
    END IF;
    
    -- Check sale_transactions.customer_id references customers.id
    SELECT COUNT(*) INTO constraint_violations
    FROM sale_transactions st
    LEFT JOIN customers c ON st.customer_id = c.id
    WHERE c.id IS NULL;
    
    IF constraint_violations > 0 THEN
        RAISE EXCEPTION 'Foreign key constraint violation: % sale_transactions have invalid customer_id', constraint_violations;
    END IF;
    
    -- Check sale_transactions.sales_user_id references users.id
    SELECT COUNT(*) INTO constraint_violations
    FROM sale_transactions st
    LEFT JOIN users u ON st.sales_user_id = u.id
    WHERE u.id IS NULL;
    
    IF constraint_violations > 0 THEN
        RAISE EXCEPTION 'Foreign key constraint violation: % sale_transactions have invalid sales_user_id', constraint_violations;
    END IF;
    
    -- Check sale_transactions.lead_id references leads.id
    SELECT COUNT(*) INTO constraint_violations
    FROM sale_transactions st
    LEFT JOIN leads l ON st.lead_id = l.id
    WHERE st.lead_id IS NOT NULL AND l.id IS NULL;
    
    IF constraint_violations > 0 THEN
        RAISE EXCEPTION 'Foreign key constraint violation: % sale_transactions have invalid lead_id', constraint_violations;
    END IF;
    
    -- Check interaction_logs.customer_id references customers.id
    SELECT COUNT(*) INTO constraint_violations
    FROM interaction_logs il
    LEFT JOIN customers c ON il.customer_id = c.id
    WHERE c.id IS NULL;
    
    IF constraint_violations > 0 THEN
        RAISE EXCEPTION 'Foreign key constraint violation: % interaction_logs have invalid customer_id', constraint_violations;
    END IF;
    
    -- Check interaction_logs.user_id references users.id
    SELECT COUNT(*) INTO constraint_violations
    FROM interaction_logs il
    LEFT JOIN users u ON il.user_id = u.id
    WHERE u.id IS NULL;
    
    IF constraint_violations > 0 THEN
        RAISE EXCEPTION 'Foreign key constraint violation: % interaction_logs have invalid user_id', constraint_violations;
    END IF;
    
    -- Check audit_logs.user_id references users.id
    SELECT COUNT(*) INTO constraint_violations
    FROM audit_logs al
    LEFT JOIN users u ON al.user_id = u.id
    WHERE al.user_id IS NOT NULL AND u.id IS NULL;
    
    IF constraint_violations > 0 THEN
        RAISE EXCEPTION 'Foreign key constraint violation: % audit_logs have invalid user_id', constraint_violations;
    END IF;
    
    RAISE NOTICE 'All foreign key constraints verified successfully';
END $$;

-- Create sample data if no data was imported
DO $$
DECLARE
    user_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO user_count FROM users;
    
    IF user_count = 0 THEN
        RAISE NOTICE 'No data found. Creating sample data...';
        
        -- Insert sample users with encoded passwords (Password123)
        INSERT INTO users (username, password, email, first_name, last_name, role, active, created_at, last_login) VALUES
        ('admin', '$2a$04$rr/kSxdJkIhXNPe7.jaFUeYlZS7Ip7Pat6l5HkIrLVJ6/ATUWMRIi', 'admin@salescrm.com', 'System', 'Administrator', 'ADMIN', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
        ('sales1', '$2a$04$rr/kSxdJkIhXNPe7.jaFUeYlZS7Ip7Pat6l5HkIrLVJ6/ATUWMRIi', 'sales1@salescrm.com', 'John', 'Smith', 'SALES', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
        ('sales2', '$2a$04$rr/kSxdJkIhXNPe7.jaFUeYlZS7Ip7Pat6l5HkIrLVJ6/ATUWMRIi', 'sales2@salescrm.com', 'Jane', 'Johnson', 'SALES', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
        ('regular1', '$2a$04$rr/kSxdJkIhXNPe7.jaFUeYlZS7Ip7Pat6l5HkIrLVJ6/ATUWMRIi', 'regular1@salescrm.com', 'Bob', 'Wilson', 'REGULAR', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
        
        -- Insert sample customers
        INSERT INTO customers (name, email, phone, company, address, created_at, updated_at, created_by_id) VALUES
        ('John Doe', 'contact@acme.com', '555-0101', 'Acme Corporation', '123 Business Ave, Suite 100', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 2),
        ('Jane Smith', 'info@techstart.com', '555-0102', 'TechStart Inc', '456 Innovation Dr', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 2),
        ('Mike Johnson', 'sales@globalsolutions.com', '555-0103', 'Global Solutions LLC', '789 Enterprise Blvd', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 3);
        
        -- Insert sample leads
        INSERT INTO leads (title, description, status, estimated_value, created_at, updated_at, customer_id, assigned_to_id) VALUES
        ('Software License Renewal', 'Annual software license renewal opportunity', 'NEW', 15000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 2),
        ('Cloud Migration Project', 'Migration to cloud infrastructure', 'CONTACTED', 50000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 2, 2),
        ('Security Audit Services', 'Comprehensive security audit and compliance', 'QUALIFIED', 25000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 3, 3);
        
        -- Insert sample sale transactions
        INSERT INTO sale_transactions (amount, sale_date, description, customer_id, sales_user_id, lead_id) VALUES
        (30000.00, CURRENT_TIMESTAMP, 'Mobile App Development - Completed', 1, 2, null),
        (22000.00, CURRENT_TIMESTAMP - INTERVAL '30 days', 'Previous Software License', 2, 2, null);
        
        -- Insert sample interaction logs
        INSERT INTO interaction_logs (type, notes, timestamp, customer_id, user_id) VALUES
        ('CALL', 'Initial contact call - discussed requirements', CURRENT_TIMESTAMP, 1, 2),
        ('EMAIL', 'Sent proposal document and pricing information', CURRENT_TIMESTAMP, 2, 2),
        ('MEETING', 'In-person meeting to review technical specifications', CURRENT_TIMESTAMP, 3, 3);
        
        -- Insert sample audit logs
        INSERT INTO audit_logs (action, entity_type, entity_id, old_values, new_values, timestamp, user_id) VALUES
        ('USER_CREATED', 'User', 4, null, 'username=regular1,role=REGULAR', CURRENT_TIMESTAMP, 1),
        ('CUSTOMER_CREATED', 'Customer', 1, null, 'name=John Doe,email=contact@acme.com', CURRENT_TIMESTAMP, 2),
        ('LEAD_CREATED', 'Lead', 1, null, 'title=Software License Renewal,status=NEW', CURRENT_TIMESTAMP, 2);
        
        -- Update sequences after sample data insertion
        SELECT setval('users_seq', (SELECT MAX(id) FROM users));
        SELECT setval('customers_seq', (SELECT MAX(id) FROM customers));
        SELECT setval('leads_seq', (SELECT MAX(id) FROM leads));
        SELECT setval('sale_transactions_seq', (SELECT MAX(id) FROM sale_transactions));
        SELECT setval('interaction_logs_seq', (SELECT MAX(id) FROM interaction_logs));
        SELECT setval('audit_logs_seq', (SELECT MAX(id) FROM audit_logs));
        
        RAISE NOTICE 'Sample data created successfully';
    END IF;
END $$;

-- Final verification queries
SELECT 'Migration completed successfully' AS status;
SELECT 'Total users: ' || COUNT(*) FROM users;
SELECT 'Total customers: ' || COUNT(*) FROM customers;
SELECT 'Total leads: ' || COUNT(*) FROM leads;
SELECT 'Total sale transactions: ' || COUNT(*) FROM sale_transactions;
SELECT 'Total interaction logs: ' || COUNT(*) FROM interaction_logs;
SELECT 'Total audit logs: ' || COUNT(*) FROM audit_logs;