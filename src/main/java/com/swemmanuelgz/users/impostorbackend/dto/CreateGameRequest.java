package com.swemmanuelgz.users.impostorbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para crear una nueva partida
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGameRequest {
    private Long creatorId;
    private String category; // Categoría de palabras
    private Integer maxPlayers; // Máximo de jugadores (opcional, default 8)
    private Integer duration; // Duración de la partida en minutos (10, 20, 30, 40)
}
