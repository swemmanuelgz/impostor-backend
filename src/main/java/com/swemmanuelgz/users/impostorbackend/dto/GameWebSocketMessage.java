package com.swemmanuelgz.users.impostorbackend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO para mensajes WebSocket entrantes/salientes del juego
 * Usado para comunicación en tiempo real entre clientes
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GameWebSocketMessage {
    
    /**
     * Tipo de mensaje (PLAYER_JOINED, PLAYER_LEFT, GAME_STARTED, GAME_ENDED, 
     * VOTE_CAST, CHAT_MESSAGE, WORD_REVEALED, ERROR, RECONNECT, HEARTBEAT)
     */
    private String type;
    
    /**
     * ID de la partida
     */
    private Long gameId;
    
    /**
     * Código de la sala
     */
    private String roomCode;
    
    /**
     * ID del usuario que envía el mensaje
     */
    private Long senderId;
    
    /**
     * Nombre del usuario que envía el mensaje
     */
    private String senderUsername;
    
    /**
     * Contenido del mensaje (para chat o información adicional)
     */
    private String content;
    
    /**
     * Datos adicionales del juego (GameDto serializado)
     */
    private GameDto gameData;
    
    /**
     * Datos del jugador (GamePlayerDto)
     */
    private GamePlayerDto playerData;
    
    /**
     * Timestamp del mensaje
     */
    private Instant timestamp;
    
    /**
     * Código de error (si aplica)
     */
    private String errorCode;
    
    /**
     * Número de jugadores actuales
     */
    private Integer currentPlayers;
    
    /**
     * Número máximo de jugadores
     */
    private Integer maxPlayers;
    
    /**
     * Datos adicionales genéricos (Map flexible para información extra)
     */
    private java.util.Map<String, Object> data;
    
    // ========== Factory Methods para crear mensajes ==========
    
    public static GameWebSocketMessage playerJoined(GameDto game, GamePlayerDto player) {
        return GameWebSocketMessage.builder()
                .type("PLAYER_JOINED")
                .gameId(game.getId())
                .roomCode(game.getRoomCode())
                .senderId(player.getUserId())
                .senderUsername(player.getUsername())
                .content("El usuario " + player.getUsername() + " se ha conectado a la partida")
                .gameData(game)
                .playerData(player)
                .currentPlayers(game.getCurrentPlayers())
                .maxPlayers(game.getMaxPlayers())
                .timestamp(Instant.now())
                .build();
    }
    
    public static GameWebSocketMessage playerLeft(GameDto game, Long userId, String username) {
        return GameWebSocketMessage.builder()
                .type("PLAYER_LEFT")
                .gameId(game.getId())
                .roomCode(game.getRoomCode())
                .senderId(userId)
                .senderUsername(username)
                .gameData(game)
                .currentPlayers(game.getCurrentPlayers())
                .timestamp(Instant.now())
                .build();
    }
    
    public static GameWebSocketMessage gameStarted(GameDto game, Long startedBy) {
        return GameWebSocketMessage.builder()
                .type("GAME_STARTED")
                .gameId(game.getId())
                .roomCode(game.getRoomCode())
                .senderId(startedBy)
                .gameData(game)
                .timestamp(Instant.now())
                .build();
    }
    
    public static GameWebSocketMessage gameEnded(GameDto game, boolean impostorWins) {
        return GameWebSocketMessage.builder()
                .type("GAME_ENDED")
                .gameId(game.getId())
                .roomCode(game.getRoomCode())
                .gameData(game)
                .content(impostorWins ? "IMPOSTOR_WINS" : "CIVILIANS_WIN")
                .timestamp(Instant.now())
                .build();
    }
    
    public static GameWebSocketMessage wordRevealed(Long gameId, String roomCode, Long userId, String word, Boolean isImpostor) {
        return GameWebSocketMessage.builder()
                .type("WORD_REVEALED")
                .gameId(gameId)
                .roomCode(roomCode)
                .senderId(userId)
                .content(isImpostor ? null : word)
                .timestamp(Instant.now())
                .build();
    }
    
    public static GameWebSocketMessage chatMessage(Long gameId, String roomCode, Long senderId, String username, String content) {
        return GameWebSocketMessage.builder()
                .type("CHAT_MESSAGE")
                .gameId(gameId)
                .roomCode(roomCode)
                .senderId(senderId)
                .senderUsername(username)
                .content(content)
                .timestamp(Instant.now())
                .build();
    }
    
    public static GameWebSocketMessage error(Long gameId, String roomCode, String errorCode, String message) {
        return GameWebSocketMessage.builder()
                .type("ERROR")
                .gameId(gameId)
                .roomCode(roomCode)
                .errorCode(errorCode)
                .content(message)
                .timestamp(Instant.now())
                .build();
    }
    
    public static GameWebSocketMessage reconnected(GameDto game, Long userId) {
        return GameWebSocketMessage.builder()
                .type("RECONNECTED")
                .gameId(game.getId())
                .roomCode(game.getRoomCode())
                .senderId(userId)
                .gameData(game)
                .timestamp(Instant.now())
                .build();
    }
    
    public static GameWebSocketMessage heartbeat(String roomCode) {
        return GameWebSocketMessage.builder()
                .type("HEARTBEAT")
                .roomCode(roomCode)
                .timestamp(Instant.now())
                .build();
    }
    
    public static GameWebSocketMessage voteCast(Long gameId, String roomCode, Long voterId, Long votedUserId) {
        return GameWebSocketMessage.builder()
                .type("VOTE_CAST")
                .gameId(gameId)
                .roomCode(roomCode)
                .senderId(voterId)
                .content(String.valueOf(votedUserId))
                .timestamp(Instant.now())
                .build();
    }
    
    /**
     * Crea mensaje de desconexión de jugador con info actualizada del juego
     * @param game Juego actualizado (puede ser null si el juego fue eliminado)
     * @param userId ID del jugador desconectado
     * @param username Nombre del jugador desconectado
     * @param isHost true si el jugador desconectado es el anfitrión
     * @param reconnectTimeout segundos restantes para reconexión (0 si no aplica)
     */
    public static GameWebSocketMessage playerDisconnected(GameDto game, Long userId, String username, 
                                                          boolean isHost, int reconnectTimeout) {
        GameWebSocketMessage.GameWebSocketMessageBuilder builder = GameWebSocketMessage.builder()
                .type(isHost ? "HOST_DISCONNECTED" : "PLAYER_DISCONNECTED")
                .senderId(userId)
                .senderUsername(username)
                .content(isHost 
                    ? "El anfitrión " + username + " se ha desconectado. Tiene " + reconnectTimeout + " segundos para reconectarse."
                    : "El usuario " + username + " se ha desconectado de la partida")
                .timestamp(Instant.now());
        
        if (game != null) {
            builder.gameId(game.getId())
                   .roomCode(game.getRoomCode())
                   .gameData(game)
                   .currentPlayers(game.getCurrentPlayers())
                   .maxPlayers(game.getMaxPlayers());
        }
        
        return builder.build();
    }
    
    /**
     * Crea mensaje cuando el anfitrión se reconecta dentro del tiempo límite
     */
    public static GameWebSocketMessage hostReconnected(GameDto game, Long userId, String username) {
        return GameWebSocketMessage.builder()
                .type("HOST_RECONNECTED")
                .gameId(game.getId())
                .roomCode(game.getRoomCode())
                .senderId(userId)
                .senderUsername(username)
                .gameData(game)
                .content("El anfitrión " + username + " se ha reconectado")
                .timestamp(Instant.now())
                .build();
    }
    
    /**
     * Crea mensaje cuando expira el tiempo de reconexión del anfitrión
     */
    public static GameWebSocketMessage hostTimeoutExpired(String roomCode, String username) {
        return GameWebSocketMessage.builder()
                .type("HOST_TIMEOUT_EXPIRED")
                .roomCode(roomCode)
                .senderUsername(username)
                .content("El tiempo de reconexión del anfitrión " + username + " ha expirado. La partida será cancelada.")
                .timestamp(Instant.now())
                .build();
    }
}
