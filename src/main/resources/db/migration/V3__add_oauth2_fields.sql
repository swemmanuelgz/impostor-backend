-- Migración para soportar OAuth2 (Google Login)
-- Añade campos necesarios para autenticación con providers externos

ALTER TABLE users 
    ADD COLUMN picture_url VARCHAR(500) NULL AFTER full_name,
    ADD COLUMN auth_provider VARCHAR(20) DEFAULT 'LOCAL' AFTER picture_url,
    ADD COLUMN provider_id VARCHAR(100) NULL AFTER auth_provider;

-- Hacer password nullable (usuarios OAuth2 no tienen password)
ALTER TABLE users MODIFY COLUMN password VARCHAR(255) NULL;

-- Índice para búsqueda por provider
CREATE INDEX idx_users_provider ON users(provider_id, auth_provider);

-- Actualizar usuarios existentes para tener auth_provider = 'LOCAL'
UPDATE users SET auth_provider = 'LOCAL' WHERE auth_provider IS NULL;
