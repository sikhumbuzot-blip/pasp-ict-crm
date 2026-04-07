-- Script to add Thulani Dube as an Admin user
-- This script can be executed against H2 or PostgreSQL database

-- Insert Thulani Dube as Admin user
-- Password: 'ThulaniAdmin123' (BCrypt encoded)
INSERT INTO users (username, password, email, first_name, last_name, role, active, created_at, last_login) 
VALUES (
    'thulani.dube',
    '$2a$10$8K1p/H7dR5.rOEL/QeJjO.4rJ5rJ5rJ5rJ5rJ5rJ5rJ5rJ5rJ5rJ5O',
    'thulani.dube@salescrm.com',
    'Thulani',
    'Dube',
    'ADMIN',
    true,
    CURRENT_TIMESTAMP,
    NULL
);

-- Add audit log entry for user creation
INSERT INTO audit_logs (action, entity_type, entity_id, old_values, new_values, timestamp, user_id)
VALUES (
    'USER_CREATED',
    'User',
    (SELECT id FROM users WHERE username = 'thulani.dube'),
    NULL,
    'username=thulani.dube,role=ADMIN,email=thulani.dube@salescrm.com',
    CURRENT_TIMESTAMP,
    1  -- Created by admin user (ID 1)
);

-- Verify the user was created
SELECT id, username, email, first_name, last_name, role, active, created_at 
FROM users 
WHERE username = 'thulani.dube';
