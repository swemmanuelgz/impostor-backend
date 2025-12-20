package com.swemmanuelgz.users.impostorbackend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.swemmanuelgz.users.impostorbackend.entity.Game;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GameDto {
    private Long id;
    private String roomCode;
    private String status; // WAITING, IN_PROGRESS, VOTING, FINISHED
    private Long creatorId;
    private String creatorUsername;
    private Instant createdAt;
    private List<GamePlayerDto> players;
    private String category; // Categoría de palabras para la partida
    private String word; // Solo se envía al civil, null para impostor
    private Integer maxPlayers;
    private Integer currentPlayers;

    public static GameDto fromEntity(Game game) {
        if (game == null) return null;
        return GameDto.builder()
                .id(game.getId())
                .roomCode(game.getRoomCode())
                .status(game.getStatus())
                .creatorId(game.getCreator() != null ? game.getCreator().getId() : null)
                .creatorUsername(game.getCreator() != null ? game.getCreator().getUsername() : null)
                .createdAt(game.getCreatedAt())
                .build();
    }

    public static GameDto fromEntityWithPlayers(Game game, List<GamePlayerDto> players) {
        GameDto dto = fromEntity(game);
        if (dto != null) {
            dto.setPlayers(players);
            dto.setCurrentPlayers(players != null ? players.size() : 0);
        }
        return dto;
    }
}
