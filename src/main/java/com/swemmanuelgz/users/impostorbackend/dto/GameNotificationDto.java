package com.swemmanuelgz.users.impostorbackend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO para notificaciones personales enviadas a usuarios específicos
 * Enviado a /user/{userId}/queue/game-notifications
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GameNotificationDto {
    
    /**
     * Tipo de notificación:
     * - YOUR_ROLE: Revela si el jugador es impostor o civil
     * - YOUR_WORD: Revela la palabra secreta (solo civiles)
     * - GAME_INVITE: Invitación a unirse a una partida
     * - KICKED: El jugador fue expulsado
     * - GAME_UPDATE: Actualización general del juego
     */
    private String type;
    
    private Long gameId;
    private String roomCode;
    
    /**
     * ID del usuario receptor
     */
    private Long recipientId;
    
    /**
     * Si el receptor es el impostor
     */
    private Boolean isImpostor;
    
    /**
     * La palabra secreta (null si es impostor)
     */
    private String word;
    
    /**
     * Mensaje descriptivo de la notificación
     */
    private String message;
    
    /**
     * Datos completos del juego (opcional)
     */
    private GameDto gameData;
    
    private Instant timestamp;
    
    // ========== Factory Methods ==========
    
    /**
     * Notificación del rol del jugador al iniciar la partida
     */
    public static GameNotificationDto roleReveal(Long gameId, String roomCode, Long userId, Boolean isImpostor, String word) {
        String message = isImpostor 
            ? "¡Eres el IMPOSTOR! Intenta descubrir la palabra secreta sin ser detectado."
            : "Eres un CIVIL. Tu palabra secreta es: " + word;
            
        return GameNotificationDto.builder()
                .type("YOUR_ROLE")
                .gameId(gameId)
                .roomCode(roomCode)
                .recipientId(userId)
                .isImpostor(isImpostor)
                .word(isImpostor ? null : word)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }
    
    /**
     * Notificación cuando el jugador es expulsado
     */
    public static GameNotificationDto kicked(Long gameId, String roomCode, Long userId, String reason) {
        return GameNotificationDto.builder()
                .type("KICKED")
                .gameId(gameId)
                .roomCode(roomCode)
                .recipientId(userId)
                .message("Has sido expulsado de la partida: " + reason)
                .timestamp(Instant.now())
                .build();
    }
    
    /**
     * Invitación a unirse a una partida
     */
    public static GameNotificationDto gameInvite(Long gameId, String roomCode, Long recipientId, String inviterName) {
        return GameNotificationDto.builder()
                .type("GAME_INVITE")
                .gameId(gameId)
                .roomCode(roomCode)
                .recipientId(recipientId)
                .message(inviterName + " te ha invitado a unirte a una partida. Código: " + roomCode)
                .timestamp(Instant.now())
                .build();
    }
    
    /**
     * Actualización general del juego
     */
    public static GameNotificationDto gameUpdate(Long gameId, String roomCode, Long recipientId, GameDto gameData, String message) {
        return GameNotificationDto.builder()
                .type("GAME_UPDATE")
                .gameId(gameId)
                .roomCode(roomCode)
                .recipientId(recipientId)
                .gameData(gameData)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }
}
