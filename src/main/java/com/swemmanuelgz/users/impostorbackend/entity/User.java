package com.swemmanuelgz.users.impostorbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "password", nullable = true, length = 255)
    private String password; // Nullable para usuarios OAuth2, obligatorio para usuarios LOCAL

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "role", length = 20)
    @ColumnDefault("'USER'")
    private String role = "USER";

    @Column(name = "picture_url", length = 500)
    private String pictureUrl; // Foto de perfil de Google

    @Column(name = "auth_provider", length = 20)
    @ColumnDefault("'LOCAL'")
    private String authProvider = "LOCAL"; // LOCAL, GOOGLE, etc.

    @Column(name = "provider_id", length = 100)
    private String providerId; // ID del usuario en Google (sub claim)

    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp; // Última IP desde la que inició sesión

    @Column(name = "last_login_at")
    private Instant lastLoginAt; // Timestamp del último login exitoso

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Verifica si el usuario se registró con OAuth2
     */
    public boolean isOAuth2User() {
        return !"LOCAL".equals(authProvider);
    }
}