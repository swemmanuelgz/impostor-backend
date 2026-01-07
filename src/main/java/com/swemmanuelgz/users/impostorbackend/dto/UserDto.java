package com.swemmanuelgz.users.impostorbackend.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.swemmanuelgz.users.impostorbackend.entity.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private String password;
    private String fullName;
    private String pictureUrl;      // Foto de perfil (OAuth2)
    private String authProvider;    // LOCAL, GOOGLE, etc.
    private Instant createdAt;
    private Instant updatedAt;

    // Estadísticas del jugador (para futuro)
    private Integer gamesPlayed;
    private Integer gamesWon;
    private Integer gamesWonAsImpostor;
    private Integer gamesWonAsCivilian;
    private Integer timesCaughtAsImpostor;

    public static UserDto fromEntity(User user) {
        if (user == null) return null;
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .pictureUrl(user.getPictureUrl())
                .authProvider(user.getAuthProvider())
                // No exponemos password en el DTO
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public User toEntity() {
        User user = new User();
        user.setId(this.id);
        user.setUsername(this.username);
        user.setEmail(this.email);
        user.setPassword(this.password);
        user.setFullName(this.fullName);
        user.setPictureUrl(this.pictureUrl);
        user.setAuthProvider(this.authProvider);
        user.setCreatedAt(this.createdAt);
        user.setUpdatedAt(this.updatedAt);
        return user;
    }
}
