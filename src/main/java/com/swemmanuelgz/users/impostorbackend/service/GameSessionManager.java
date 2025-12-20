package com.swemmanuelgz.users.impostorbackend.service;

import com.swemmanuelgz.users.impostorbackend.dto.GameDto;
import com.swemmanuelgz.users.impostorbackend.dto.GamePlayerDto;
import com.swemmanuelgz.users.impostorbackend.exception.WebSocketException;
import com.swemmanuelgz.users.impostorbackend.utils.AnsiColors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Gestor de sesiones WebSocket para el juego del impostor
 * Maneja:
 * - Tracking de jugadores conectados por sala
 * - Sesiones de juego activas
 * - Reconexión de jugadores
 * - Limpieza de sesiones inactivas
 */
@Service
@RequiredArgsConstructor
public class GameSessionManager {
    
    private static final Logger logger = Logger.getLogger(GameSessionManager.class.getName());
    
    /**
     * Máximo de jugadores por sala
     */
    public static final int MAX_PLAYERS_PER_ROOM = 12;
    
    /**
     * Mínimo de jugadores para iniciar partida
     */
    public static final int MIN_PLAYERS_TO_START = 3;
    
    /**
     * Tiempo máximo de desconexión permitido para reconexión (en segundos)
     */
    public static final int RECONNECT_TIMEOUT_SECONDS = 60;
    
    /**
     * Map de roomCode -> Set de userIds conectados
     */
    private final Map<String, Set<Long>> connectedPlayers = new ConcurrentHashMap<>();
    
    /**
     * Map de roomCode -> GameSessionInfo (información adicional de la sesión)
     */
    private final Map<String, GameSessionInfo> gameSessions = new ConcurrentHashMap<>();
    
    /**
     * Map de userId -> última desconexión (para manejar reconexión)
     */
    private final Map<Long, DisconnectionInfo> disconnectedPlayers = new ConcurrentHashMap<>();
    
    /**
     * Map de sessionId (WebSocket) -> userId (para tracking de sesiones)
     */
    private final Map<String, Long> sessionToUser = new ConcurrentHashMap<>();
    
    // ========== Gestión de Conexiones ==========
    
    /**
     * Registra la conexión de un jugador a una sala
     */
    public void playerConnected(String roomCode, Long userId, String sessionId) {
        AnsiColors.infoLog(logger, "Registrando conexión - RoomCode: " + roomCode + ", UserId: " + userId);
        
        // Registrar en el map de sesiones
        sessionToUser.put(sessionId, userId);
        
        // Añadir al set de jugadores conectados
        connectedPlayers.computeIfAbsent(roomCode, k -> new CopyOnWriteArraySet<>()).add(userId);
        
        // Limpiar de desconectados si estaba pendiente de reconexión
        disconnectedPlayers.remove(userId);
        
        // Actualizar info de sesión del juego
        gameSessions.computeIfAbsent(roomCode, k -> new GameSessionInfo(roomCode));
        gameSessions.get(roomCode).updateLastActivity();
        
        int totalConnected = getConnectedPlayersCount(roomCode);
        AnsiColors.successLog(logger, "Jugador " + userId + " conectado a sala " + roomCode + 
            ". Total conectados: " + totalConnected);
    }
    
    /**
     * Registra la desconexión de un jugador
     */
    public void playerDisconnected(String sessionId) {
        Long userId = sessionToUser.remove(sessionId);
        if (userId == null) {
            AnsiColors.warningLog(logger, "Sesión desconocida desconectada: " + sessionId);
            return;
        }
        
        AnsiColors.infoLog(logger, "Jugador " + userId + " desconectado (sessionId: " + sessionId + ")");
        
        // Buscar en qué sala estaba
        String roomCode = findRoomByUserId(userId);
        if (roomCode != null) {
            // Registrar desconexión para posible reconexión
            disconnectedPlayers.put(userId, new DisconnectionInfo(userId, roomCode, Instant.now()));
            
            // Remover de conectados
            Set<Long> players = connectedPlayers.get(roomCode);
            if (players != null) {
                players.remove(userId);
                AnsiColors.infoLog(logger, "Jugador " + userId + " removido de sala " + roomCode + 
                    ". Quedan " + players.size() + " conectados");
            }
        }
    }
    
    /**
     * Intenta reconectar un jugador desconectado
     * @return true si la reconexión fue exitosa
     */
    public boolean attemptReconnect(Long userId, String roomCode, String newSessionId) {
        DisconnectionInfo disconnectInfo = disconnectedPlayers.get(userId);
        
        if (disconnectInfo == null) {
            AnsiColors.warningLog(logger, "No hay info de desconexión para usuario " + userId);
            return false;
        }
        
        // Verificar que sea la misma sala
        if (!disconnectInfo.roomCode.equals(roomCode)) {
            AnsiColors.errorLog(logger, "Usuario " + userId + " intentó reconectar a sala diferente. " +
                "Original: " + disconnectInfo.roomCode + ", Intentada: " + roomCode);
            return false;
        }
        
        // Verificar timeout de reconexión
        long secondsSinceDisconnect = java.time.Duration.between(
            disconnectInfo.disconnectedAt, Instant.now()
        ).getSeconds();
        
        if (secondsSinceDisconnect > RECONNECT_TIMEOUT_SECONDS) {
            AnsiColors.warningLog(logger, "Timeout de reconexión excedido para usuario " + userId + 
                ". Segundos: " + secondsSinceDisconnect);
            disconnectedPlayers.remove(userId);
            return false;
        }
        
        // Reconectar exitosamente
        playerConnected(roomCode, userId, newSessionId);
        AnsiColors.successLog(logger, "Reconexión exitosa para usuario " + userId + " en sala " + roomCode);
        return true;
    }
    
    // ========== Consultas de Estado ==========
    
    /**
     * Obtiene los IDs de todos los jugadores conectados en una sala
     */
    public Set<Long> getConnectedPlayerIds(String roomCode) {
        return connectedPlayers.getOrDefault(roomCode, Set.of());
    }
    
    /**
     * Obtiene el número de jugadores conectados en una sala
     */
    public int getConnectedPlayersCount(String roomCode) {
        Set<Long> players = connectedPlayers.get(roomCode);
        return players != null ? players.size() : 0;
    }
    
    /**
     * Verifica si un jugador está conectado a una sala
     */
    public boolean isPlayerConnected(String roomCode, Long userId) {
        Set<Long> players = connectedPlayers.get(roomCode);
        return players != null && players.contains(userId);
    }
    
    /**
     * Verifica si un jugador está desconectado pero puede reconectarse
     */
    public boolean canPlayerReconnect(Long userId) {
        DisconnectionInfo info = disconnectedPlayers.get(userId);
        if (info == null) return false;
        
        long secondsSinceDisconnect = java.time.Duration.between(
            info.disconnectedAt, Instant.now()
        ).getSeconds();
        
        return secondsSinceDisconnect <= RECONNECT_TIMEOUT_SECONDS;
    }
    
    /**
     * Busca la sala en la que está un usuario
     */
    public String findRoomByUserId(Long userId) {
        for (Map.Entry<String, Set<Long>> entry : connectedPlayers.entrySet()) {
            if (entry.getValue().contains(userId)) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    /**
     * Verifica si se puede iniciar la partida (mínimo de jugadores)
     */
    public boolean canStartGame(String roomCode) {
        int count = getConnectedPlayersCount(roomCode);
        boolean canStart = count >= MIN_PLAYERS_TO_START;
        AnsiColors.infoLog(logger, "Verificando inicio de partida en " + roomCode + 
            ": " + count + "/" + MIN_PLAYERS_TO_START + " jugadores. CanStart: " + canStart);
        return canStart;
    }
    
    /**
     * Verifica si hay espacio para más jugadores
     */
    public boolean hasRoomForPlayer(String roomCode) {
        int count = getConnectedPlayersCount(roomCode);
        return count < MAX_PLAYERS_PER_ROOM;
    }
    
    /**
     * Valida que se puede añadir un jugador a la sala
     */
    public void validateCanJoin(String roomCode, Long userId) {
        // Verificar espacio disponible
        if (!hasRoomForPlayer(roomCode)) {
            throw WebSocketException.maxJugadoresAlcanzado(roomCode, MAX_PLAYERS_PER_ROOM);
        }
        
        // Verificar si ya está en la sala
        if (isPlayerConnected(roomCode, userId)) {
            throw WebSocketException.jugadorYaEnSala(userId, roomCode);
        }
    }
    
    // ========== Gestión de Sesión de Juego ==========
    
    /**
     * Obtiene la información de sesión de un juego
     */
    public Optional<GameSessionInfo> getGameSession(String roomCode) {
        return Optional.ofNullable(gameSessions.get(roomCode));
    }
    
    /**
     * Marca una partida como iniciada
     */
    public void markGameStarted(String roomCode, String word) {
        GameSessionInfo session = gameSessions.get(roomCode);
        if (session != null) {
            session.setStatus("IN_PROGRESS");
            session.setSecretWord(word);
            session.setStartedAt(Instant.now());
            AnsiColors.successLog(logger, "Partida marcada como iniciada: " + roomCode);
        }
    }
    
    /**
     * Marca una partida como finalizada y limpia recursos
     */
    public void markGameEnded(String roomCode) {
        AnsiColors.infoLog(logger, "Finalizando sesión de juego: " + roomCode);
        
        // Limpiar jugadores conectados
        Set<Long> players = connectedPlayers.remove(roomCode);
        if (players != null) {
            AnsiColors.infoLog(logger, "Limpiados " + players.size() + " jugadores de sala " + roomCode);
        }
        
        // Limpiar info de sesión
        GameSessionInfo session = gameSessions.remove(roomCode);
        if (session != null) {
            AnsiColors.successLog(logger, "Sesión de juego " + roomCode + " limpiada");
        }
    }
    
    /**
     * Limpia sesiones inactivas (llamar periódicamente)
     */
    public void cleanupInactiveSessions() {
        AnsiColors.infoLog(logger, "Limpiando sesiones inactivas...");
        
        Instant cutoff = Instant.now().minusSeconds(RECONNECT_TIMEOUT_SECONDS * 2);
        
        // Limpiar desconexiones antiguas
        int removedDisconnects = 0;
        for (Map.Entry<Long, DisconnectionInfo> entry : disconnectedPlayers.entrySet()) {
            if (entry.getValue().disconnectedAt.isBefore(cutoff)) {
                disconnectedPlayers.remove(entry.getKey());
                removedDisconnects++;
            }
        }
        
        if (removedDisconnects > 0) {
            AnsiColors.infoLog(logger, "Limpiadas " + removedDisconnects + " desconexiones antiguas");
        }
    }
    
    // ========== Clases internas para datos de sesión ==========
    
    /**
     * Información de una sesión de juego
     */
    public static class GameSessionInfo {
        private final String roomCode;
        private String status = "WAITING";
        private String secretWord;
        private Instant createdAt = Instant.now();
        private Instant startedAt;
        private Instant lastActivity = Instant.now();
        
        public GameSessionInfo(String roomCode) {
            this.roomCode = roomCode;
        }
        
        public void updateLastActivity() {
            this.lastActivity = Instant.now();
        }
        
        // Getters y Setters
        public String getRoomCode() { return roomCode; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getSecretWord() { return secretWord; }
        public void setSecretWord(String secretWord) { this.secretWord = secretWord; }
        public Instant getCreatedAt() { return createdAt; }
        public Instant getStartedAt() { return startedAt; }
        public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
        public Instant getLastActivity() { return lastActivity; }
    }
    
    /**
     * Información de desconexión para reconexión
     */
    private static class DisconnectionInfo {
        final Long userId;
        final String roomCode;
        final Instant disconnectedAt;
        
        DisconnectionInfo(Long userId, String roomCode, Instant disconnectedAt) {
            this.userId = userId;
            this.roomCode = roomCode;
            this.disconnectedAt = disconnectedAt;
        }
    }
}
