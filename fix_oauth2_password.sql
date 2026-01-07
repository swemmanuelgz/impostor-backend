-- ============================================================================
-- Solución completa para usuarios OAuth2 sin password
-- ============================================================================
-- Este script:
-- 1. Permite que la columna password sea NULL
-- 2. Agrega una constraint CHECK para garantizar que:
--    - Usuarios LOCAL (auth_provider = 'LOCAL') DEBEN tener password
--    - Usuarios OAuth2 (auth_provider != 'LOCAL') PUEDEN tener password NULL
-- ============================================================================

USE impostor; -- Base de datos impostor

-- Paso 1: Modificar la columna password para permitir NULL
ALTER TABLE users MODIFY COLUMN password VARCHAR(255) NULL;

-- Paso 2: Agregar constraint CHECK
-- Si el usuario es LOCAL, password NO puede ser NULL
-- Si el usuario es OAuth2 (GOOGLE, etc.), password PUEDE ser NULL
ALTER TABLE users ADD CONSTRAINT chk_password_required_for_local 
CHECK (
    (auth_provider = 'LOCAL' AND password IS NOT NULL) 
    OR 
    (auth_provider != 'LOCAL')
);

-- Verificar los cambios
DESCRIBE users;

-- Ver las constraints de la tabla
SELECT 
    CONSTRAINT_NAME, 
    CHECK_CLAUSE 
FROM 
    information_schema.CHECK_CONSTRAINTS 
WHERE 
    TABLE_NAME = 'users' 
    AND TABLE_SCHEMA = 'impostor';

-- ============================================================================
-- Ejemplos de inserts que funcionarán:
-- ============================================================================

-- ✅ Usuario LOCAL con password (VÁLIDO)
-- INSERT INTO users (username, email, password, auth_provider, role) 
-- VALUES ('usuario1', 'user1@example.com', 'hashed_password', 'LOCAL', 'USER');

-- ✅ Usuario GOOGLE sin password (VÁLIDO)
-- INSERT INTO users (username, email, password, auth_provider, provider_id, role) 
-- VALUES ('googleuser', 'google@example.com', NULL, 'GOOGLE', 'google-id-123', 'USER');

-- ❌ Usuario LOCAL sin password (INVÁLIDO - violará la constraint)
-- INSERT INTO users (username, email, password, auth_provider, role) 
-- VALUES ('usuario2', 'user2@example.com', NULL, 'LOCAL', 'USER');

