-- Simple script to add essential users
-- Password for all users: admin123 (BCrypt encoded)

INSERT INTO users (username, password, email, first_name, last_name, role, active, created_at) VALUES
('admin', '$2a$10$rr/kSxdJkIhXNPe7.jaFUeYlZS7Ip7Pat6l5HkIrLVJ6/ATUWMRIi', 'admin@salescrm.com', 'System', 'Administrator', 'ADMIN', true, CURRENT_TIMESTAMP),
('thulani.dube', '$2a$10$rr/kSxdJkIhXNPe7.jaFUeYlZS7Ip7Pat6l5HkIrLVJ6/ATUWMRIi', 'thulani.dube@salescrm.com', 'Thulani', 'Dube', 'ADMIN', true, CURRENT_TIMESTAMP);

-- Verify users were created
SELECT id, username, email, first_name, last_name, role, active FROM users;