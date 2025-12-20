package com.swemmanuelgz.users.impostorbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para unirse a una partida
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinGameRequest {
    private String roomCode;
    private Long userId;
    private String nickname; // Nickname temporal para jugadores sin cuenta (futuro)
}
