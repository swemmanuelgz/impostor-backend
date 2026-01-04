package com.swemmanuelgz.users.impostorbackend.service;

import com.swemmanuelgz.users.impostorbackend.repository.GameRepository;
import com.swemmanuelgz.users.impostorbackend.utils.AnsiColors;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.logging.Logger;

/**
 * Servicio programado para limpieza automática de partidas
 * - Cierra partidas con más de 1 hora de antigüedad
 * - Limpia sesiones de WebSocket inactivas
 */
@Service
@RequiredArgsConstructor
public class GameCleanupScheduler {
    
    private static final Logger logger = Logger.getLogger(GameCleanupScheduler.class.getName());
    
    /**
     * Tiempo máximo de vida de una partida en horas
     */
    private static final int MAX_GAME_DURATION_HOURS = 1;
    
    private final GameRepository gameRepository;
    private final GameSessionManager gameSessionManager;
    
    /**
     * Tarea programada que se ejecuta cada hora
     * Cierra todas las partidas activas que tengan más de 1 hora de antigüedad
     */
    @Scheduled(fixedRate = 3600000) // Cada hora (3600000 ms)
    @Transactional
    public void closeStaleGames() {
        AnsiColors.infoLog(logger, "⏰ Ejecutando limpieza programada de partidas antiguas...");
        
        Instant cutoffTime = Instant.now().minus(MAX_GAME_DURATION_HOURS, ChronoUnit.HOURS);
        
        try {
            int closedCount = gameRepository.closeStaleGames(cutoffTime);
            
            if (closedCount > 0) {
                AnsiColors.successLog(logger, "✅ Cerradas " + closedCount + " partidas antiguas (>1 hora)");
            } else {
                AnsiColors.infoLog(logger, "📋 No hay partidas antiguas para cerrar");
            }
        } catch (Exception e) {
            AnsiColors.errorLog(logger, "❌ Error cerrando partidas antiguas: " + e.getMessage());
        }
    }
    
    /**
     * Tarea programada que se ejecuta cada 5 minutos
     * Limpia sesiones WebSocket inactivas
     */
    @Scheduled(fixedRate = 300000) // Cada 5 minutos (300000 ms)
    public void cleanupInactiveSessions() {
        try {
            gameSessionManager.cleanupInactiveSessions();
        } catch (Exception e) {
            AnsiColors.errorLog(logger, "❌ Error limpiando sesiones inactivas: " + e.getMessage());
        }
    }
    
    /**
     * Método para cerrar manualmente una partida específica si está vacía
     * @param roomCode Código de la sala
     * @return true si la partida fue cerrada, false si no
     */
    @Transactional
    public boolean closeGameIfEmpty(String roomCode) {
        if (!gameSessionManager.hasConnectedPlayers(roomCode)) {
            AnsiColors.infoLog(logger, "🚪 Cerrando partida vacía: " + roomCode);
            
            return gameRepository.findByRoomCode(roomCode)
                    .map(game -> {
                        if (!"FINISHED".equals(game.getStatus())) {
                            game.setStatus("FINISHED");
                            gameRepository.save(game);
                            gameSessionManager.markGameEnded(roomCode);
                            AnsiColors.successLog(logger, "✅ Partida " + roomCode + " cerrada por estar vacía");
                            return true;
                        }
                        return false;
                    })
                    .orElse(false);
        }
        return false;
    }
}
