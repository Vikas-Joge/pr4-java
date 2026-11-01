-- Run once in MySQL (same database the Python project used)
CREATE DATABASE IF NOT EXISTS authentication_db;
USE authentication_db;

CREATE TABLE IF NOT EXISTS users (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    email      VARCHAR(150) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- User used by the app (matches the default settings in the code)
-- CREATE USER 'personal'@'localhost' IDENTIFIED BY 'MyNewPassword123!';
-- GRANT ALL PRIVILEGES ON authentication_db.* TO 'personal'@'localhost';
