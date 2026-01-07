-- Migración para agregar campos de votación a game_players
-- Ejecutar en MySQL

ALTER TABLE game_players 
ADD COLUMN has_voted BOOLEAN DEFAULT FALSE,
ADD COLUMN voted_for_id BIGINT NULL,
ADD COLUMN status VARCHAR(20) DEFAULT 'ACTIVE';

-- Actualizar registros existentes
UPDATE game_players SET has_voted = FALSE WHERE has_voted IS NULL;
UPDATE game_players SET status = 'ACTIVE' WHERE status IS NULL;
