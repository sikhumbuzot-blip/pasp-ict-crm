-- PostgreSQL Database Creation Script
-- Run this script as PostgreSQL superuser to set up the CRM database

-- Create database
CREATE DATABASE crmdb
    WITH 
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.UTF-8'
    LC_CTYPE = 'en_US.UTF-8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;

-- Create application user
CREATE USER crmuser WITH
    LOGIN
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    INHERIT
    NOREPLICATION
    CONNECTION LIMIT -1
    PASSWORD 'crmpass';

-- Grant privileges to the user
GRANT CONNECT ON DATABASE crmdb TO crmuser;
GRANT USAGE ON SCHEMA public TO crmuser;
GRANT CREATE ON SCHEMA public TO crmuser;

-- Connect to the database to set additional privileges
\c crmdb

-- Grant all privileges on the public schema
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO crmuser;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO crmuser;
GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public TO crmuser;

-- Grant privileges on future objects
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO crmuser;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO crmuser;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON FUNCTIONS TO crmuser;

-- Create extension for UUID generation (if needed in future)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Display confirmation
SELECT 'Database crmdb created successfully with user crmuser' AS status;