package com.swemmanuelgz.users.impostorbackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Entidad para registrar todos los intentos de login.
 * Útil para:
 * - Auditoría de accesos
 * - Detección de ataques brute force
 * - Análisis de patrones de acceso
 */
@Entity
@Table(name = "login_attempts", indexes = {
        @Index(name = "idx_login_attempts_ip", columnList = "ip_address"),
        @Index(name = "idx_login_attempts_username", columnList = "username"),
        @Index(name = "idx_login_attempts_timestamp", columnList = "attempt_timestamp"),
        @Index(name = "idx_login_attempts_ip_timestamp", columnList = "ip_address, attempt_timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Username o email usado en el intento de login
     */
    @Column(name = "username", nullable = false, length = 100)
    private String username;

    /**
     * Dirección IP del cliente
     */
    @Column(name = "ip_address", nullable = false, length = 45) // IPv6 puede tener hasta 45 chars
    private String ipAddress;

    /**
     * Si el login fue exitoso o no
     */
    @Column(name = "success", nullable = false)
    private Boolean success;

    /**
     * Razón del fallo (si aplica)
     */
    @Column(name = "failure_reason", length = 100)
    private String failureReason;

    /**
     * Timestamp del intento
     */
    @Column(name = "attempt_timestamp", nullable = false)
    private Instant attemptTimestamp;

    /**
     * User-Agent del navegador/app
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * Tipo de autenticación: LOCAL, GOOGLE, etc.
     */
    @Column(name = "auth_type", length = 20)
    private String authType;

    /**
     * ID del usuario si el login fue exitoso
     */
    @Column(name = "user_id")
    private Long userId;

    // Constantes para failure_reason
    public static final String FAILURE_INVALID_PASSWORD = "INVALID_PASSWORD";
    public static final String FAILURE_USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String FAILURE_OAUTH_USER = "OAUTH_USER_PASSWORD_LOGIN";
    public static final String FAILURE_RATE_LIMITED = "RATE_LIMITED";
    public static final String FAILURE_ACCOUNT_LOCKED = "ACCOUNT_LOCKED";

    // Constantes para auth_type
    public static final String AUTH_TYPE_LOCAL = "LOCAL";
    public static final String AUTH_TYPE_GOOGLE = "GOOGLE";
}
