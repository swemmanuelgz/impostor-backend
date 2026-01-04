package com.swemmanuelgz.users.impostorbackend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

/**
 * DTO para enviar información del rol de un jugador durante la reconexión
 * Contiene la información necesaria para restaurar el estado de un jugador
 * que reconecta a una partida en progreso
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlayerRoleDto {
    
    /**
     * ID del usuario
     */
    private Long userId;
    
    /**
     * Nombre de usuario
     */
    private String username;
    
    /**
     * Si el jugador es el impostor
     */
    private Boolean isImpostor;
    
    /**
     * La palabra secreta (null si es impostor)
     */
    private String word;
    
    /**
     * Código de la sala
     */
    private String roomCode;
    
    /**
     * ID de la partida
     */
    private Long gameId;
    
    /**
     * Estado actual de la partida
     */
    private String gameStatus;
}
