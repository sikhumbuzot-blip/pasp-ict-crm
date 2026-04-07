-- PostgreSQL Schema Creation Script
-- Run this script to create the complete database schema for the Sales CRM application

-- Create sequences for primary keys
CREATE SEQUENCE IF NOT EXISTS users_seq 
    START 1 
    INCREMENT 1 
    NO MINVALUE 
    NO MAXVALUE 
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS customers_seq 
    START 1 
    INCREMENT 1 
    NO MINVALUE 
    NO MAXVALUE 
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS leads_seq 
    START 1 
    INCREMENT 1 
    NO MINVALUE 
    NO MAXVALUE 
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS sale_transactions_seq 
    START 1 
    INCREMENT 1 
    NO MINVALUE 
    NO MAXVALUE 
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS interaction_logs_seq 
    START 1 
    INCREMENT 1 
    NO MINVALUE 
    NO MAXVALUE 
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS audit_logs_seq 
    START 1 
    INCREMENT 1 
    NO MINVALUE 
    NO MAXVALUE 
    CACHE 1;

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY DEFAULT nextval('users_seq'),
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(500) UNIQUE NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'SALES', 'REGULAR')),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP
);

-- Create customers table
CREATE TABLE IF NOT EXISTS customers (
    id BIGINT PRIMARY KEY DEFAULT nextval('customers_seq'),
    name VARCHAR(100) NOT NULL,
    email VARCHAR(500) UNIQUE NOT NULL,
    phone VARCHAR(255),
    company VARCHAR(100),
    address VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by_id BIGINT NOT NULL,
    CONSTRAINT fk_customers_created_by FOREIGN KEY (created_by_id) REFERENCES users(id)
);

-- Create leads table
CREATE TABLE IF NOT EXISTS leads (
    id BIGINT PRIMARY KEY DEFAULT nextval('leads_seq'),
    title VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(20) NOT NULL DEFAULT 'NEW' 
        CHECK (status IN ('NEW', 'CONTACTED', 'QUALIFIED', 'PROPOSAL', 'NEGOTIATION', 'CLOSED_WON', 'CLOSED_LOST')),
    estimated_value NUMERIC(10,2) CHECK (estimated_value > 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    customer_id BIGINT NOT NULL,
    assigned_to_id BIGINT,
    CONSTRAINT fk_leads_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_leads_assigned_to FOREIGN KEY (assigned_to_id) REFERENCES users(id)
);

-- Create sale_transactions table
CREATE TABLE IF NOT EXISTS sale_transactions (
    id BIGINT PRIMARY KEY DEFAULT nextval('sale_transactions_seq'),
    amount NUMERIC(10,2) NOT NULL CHECK (amount > 0),
    sale_date TIMESTAMP NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    customer_id BIGINT NOT NULL,
    sales_user_id BIGINT NOT NULL,
    lead_id BIGINT,
    CONSTRAINT fk_sale_transactions_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_sale_transactions_sales_user FOREIGN KEY (sales_user_id) REFERENCES users(id),
    CONSTRAINT fk_sale_transactions_lead FOREIGN KEY (lead_id) REFERENCES leads(id)
);

-- Create interaction_logs table
CREATE TABLE IF NOT EXISTS interaction_logs (
    id BIGINT PRIMARY KEY DEFAULT nextval('interaction_logs_seq'),
    type VARCHAR(20) NOT NULL CHECK (type IN ('CALL', 'EMAIL', 'MEETING', 'NOTE')),
    notes VARCHAR(2000),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    customer_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    CONSTRAINT fk_interaction_logs_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_interaction_logs_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Create audit_logs table
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT PRIMARY KEY DEFAULT nextval('audit_logs_seq'),
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT,
    old_values VARCHAR(5000),
    new_values VARCHAR(5000),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    user_id BIGINT,
    CONSTRAINT fk_audit_logs_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Create indexes for better performance

-- Users table indexes
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_active ON users(active);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at);

-- Customers table indexes
CREATE INDEX IF NOT EXISTS idx_customers_email ON customers(email);
CREATE INDEX IF NOT EXISTS idx_customers_company ON customers(company);
CREATE INDEX IF NOT EXISTS idx_customers_created_by ON customers(created_by_id);
CREATE INDEX IF NOT EXISTS idx_customers_created_at ON customers(created_at);
CREATE INDEX IF NOT EXISTS idx_customers_name ON customers(name);

-- Leads table indexes
CREATE INDEX IF NOT EXISTS idx_leads_status ON leads(status);
CREATE INDEX IF NOT EXISTS idx_leads_customer ON leads(customer_id);
CREATE INDEX IF NOT EXISTS idx_leads_assigned_to ON leads(assigned_to_id);
CREATE INDEX IF NOT EXISTS idx_leads_created_at ON leads(created_at);
CREATE INDEX IF NOT EXISTS idx_leads_estimated_value ON leads(estimated_value);

-- Sale transactions table indexes
CREATE INDEX IF NOT EXISTS idx_sale_transactions_customer ON sale_transactions(customer_id);
CREATE INDEX IF NOT EXISTS idx_sale_transactions_sales_user ON sale_transactions(sales_user_id);
CREATE INDEX IF NOT EXISTS idx_sale_transactions_sale_date ON sale_transactions(sale_date);
CREATE INDEX IF NOT EXISTS idx_sale_transactions_lead ON sale_transactions(lead_id);
CREATE INDEX IF NOT EXISTS idx_sale_transactions_amount ON sale_transactions(amount);

-- Interaction logs table indexes
CREATE INDEX IF NOT EXISTS idx_interaction_logs_customer ON interaction_logs(customer_id);
CREATE INDEX IF NOT EXISTS idx_interaction_logs_user ON interaction_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_interaction_logs_timestamp ON interaction_logs(timestamp);
CREATE INDEX IF NOT EXISTS idx_interaction_logs_type ON interaction_logs(type);

-- Audit logs table indexes
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_timestamp ON audit_logs(timestamp);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON audit_logs(action);

-- Create views for common queries

-- Active users view
CREATE OR REPLACE VIEW active_users AS
SELECT id, username, email, first_name, last_name, role, created_at, last_login
FROM users 
WHERE active = true;

-- Sales performance view
CREATE OR REPLACE VIEW sales_performance AS
SELECT 
    u.id as user_id,
    u.username,
    u.first_name,
    u.last_name,
    COUNT(st.id) as total_sales,
    COALESCE(SUM(st.amount), 0) as total_revenue,
    AVG(st.amount) as average_sale_amount,
    MAX(st.sale_date) as last_sale_date
FROM users u
LEFT JOIN sale_transactions st ON u.id = st.sales_user_id
WHERE u.role = 'SALES' AND u.active = true
GROUP BY u.id, u.username, u.first_name, u.last_name;

-- Lead pipeline view
CREATE OR REPLACE VIEW lead_pipeline AS
SELECT 
    status,
    COUNT(*) as lead_count,
    COALESCE(SUM(estimated_value), 0) as total_estimated_value,
    AVG(estimated_value) as average_estimated_value
FROM leads
WHERE status NOT IN ('CLOSED_WON', 'CLOSED_LOST')
GROUP BY status
ORDER BY 
    CASE status
        WHEN 'NEW' THEN 1
        WHEN 'CONTACTED' THEN 2
        WHEN 'QUALIFIED' THEN 3
        WHEN 'PROPOSAL' THEN 4
        WHEN 'NEGOTIATION' THEN 5
    END;

-- Customer interaction summary view
CREATE OR REPLACE VIEW customer_interaction_summary AS
SELECT 
    c.id as customer_id,
    c.name as customer_name,
    c.company,
    COUNT(il.id) as total_interactions,
    MAX(il.timestamp) as last_interaction_date,
    COUNT(CASE WHEN il.type = 'CALL' THEN 1 END) as call_count,
    COUNT(CASE WHEN il.type = 'EMAIL' THEN 1 END) as email_count,
    COUNT(CASE WHEN il.type = 'MEETING' THEN 1 END) as meeting_count
FROM customers c
LEFT JOIN interaction_logs il ON c.id = il.customer_id
GROUP BY c.id, c.name, c.company;

-- Grant permissions on sequences and views to application user
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO crmuser;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO crmuser;

-- Display confirmation
SELECT 'PostgreSQL schema created successfully with all tables, indexes, and views' AS status;