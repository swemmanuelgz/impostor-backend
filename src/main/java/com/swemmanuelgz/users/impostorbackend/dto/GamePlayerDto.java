package com.swemmanuelgz.users.impostorbackend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.swemmanuelgz.users.impostorbackend.entity.GamePlayer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GamePlayerDto {
    private Long id;
    private Long gameId;
    private Long userId;
    private String username;
    private String pictureUrl;  // Foto de perfil del usuario (OAuth2)
    private Boolean isImpostor;
    private Boolean isWinner;
    private String word; // Palabra asignada (null si es impostor)

    public static GamePlayerDto fromEntity(GamePlayer gamePlayer) {
        if (gamePlayer == null) return null;
        return GamePlayerDto.builder()
                .id(gamePlayer.getId())
                .gameId(gamePlayer.getGame() != null ? gamePlayer.getGame().getId() : null)
                .userId(gamePlayer.getUser() != null ? gamePlayer.getUser().getId() : null)
                .username(gamePlayer.getUser() != null ? gamePlayer.getUser().getUsername() : null)
                .pictureUrl(gamePlayer.getUser() != null ? gamePlayer.getUser().getPictureUrl() : null)
                .isImpostor(gamePlayer.getIsImpostor())
                .isWinner(gamePlayer.getIsWinner())
                .build();
    }

    /**
     * Versión del DTO para enviar al jugador con su rol y palabra
     */
    public static GamePlayerDto fromEntityWithRole(GamePlayer gamePlayer, String word) {
        GamePlayerDto dto = fromEntity(gamePlayer);
        if (dto != null) {
            dto.setWord(word);
        }
        return dto;
    }
}
