package com.swemmanuelgz.users.impostorbackend.service;

import com.swemmanuelgz.users.impostorbackend.entity.LoginAttempt;
import com.swemmanuelgz.users.impostorbackend.repository.LoginAttemptRepository;
import com.swemmanuelgz.users.impostorbackend.utils.AnsiColors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.logging.Logger;

/**
 * Servicio para gestionar los intentos de login.
 * Registra todos los intentos y puede verificar si una IP/usuario está bloqueado.
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final Logger logger = Logger.getLogger(LoginAttemptService.class.getName());

    private final LoginAttemptRepository loginAttemptRepository;

    // Configuración de bloqueo
    private static final int MAX_FAILED_ATTEMPTS = 5;           // Máximo intentos fallidos
    private static final int LOCKOUT_DURATION_MINUTES = 5;      // Duración del bloqueo

    /**
     * Registra un intento de login exitoso
     */
    @Transactional
    public void recordSuccessfulLogin(String username, String ipAddress, String userAgent, String authType, Long userId) {
        LoginAttempt attempt = LoginAttempt.builder()
                .username(username)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .authType(authType)
                .userId(userId)
                .success(true)
                .attemptTimestamp(Instant.now())
                .build();

        loginAttemptRepository.save(attempt);
        AnsiColors.successLog(logger, "Login exitoso registrado - Usuario: " + username + ", IP: " + ipAddress);
    }

    /**
     * Registra un intento de login fallido
     */
    @Transactional
    public void recordFailedLogin(String username, String ipAddress, String userAgent, String authType, String failureReason) {
        LoginAttempt attempt = LoginAttempt.builder()
                .username(username)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .authType(authType)
                .success(false)
                .failureReason(failureReason)
                .attemptTimestamp(Instant.now())
                .build();

        loginAttemptRepository.save(attempt);
        
        long failedCount = getFailedAttemptCountForIp(ipAddress);
        AnsiColors.warningLog(logger, "Login fallido registrado - Usuario: " + username + 
                ", IP: " + ipAddress + ", Razón: " + failureReason + 
                ", Intentos fallidos: " + failedCount + "/" + MAX_FAILED_ATTEMPTS);
    }

    /**
     * Verifica si una IP está bloqueada por demasiados intentos fallidos
     */
    public boolean isIpBlocked(String ipAddress) {
        Instant since = Instant.now().minus(LOCKOUT_DURATION_MINUTES, ChronoUnit.MINUTES);
        long failedAttempts = loginAttemptRepository.countFailedAttemptsByIpSince(ipAddress, since);
        return failedAttempts >= MAX_FAILED_ATTEMPTS;
    }

    /**
     * Verifica si un usuario está bloqueado por demasiados intentos fallidos
     */
    public boolean isUserBlocked(String username) {
        Instant since = Instant.now().minus(LOCKOUT_DURATION_MINUTES, ChronoUnit.MINUTES);
        long failedAttempts = loginAttemptRepository.countFailedAttemptsByUsernameSince(username, since);
        return failedAttempts >= MAX_FAILED_ATTEMPTS;
    }

    /**
     * Obtiene cuántos intentos fallidos tiene una IP en el período de bloqueo
     */
    public long getFailedAttemptCountForIp(String ipAddress) {
        Instant since = Instant.now().minus(LOCKOUT_DURATION_MINUTES, ChronoUnit.MINUTES);
        return loginAttemptRepository.countFailedAttemptsByIpSince(ipAddress, since);
    }

    /**
     * Calcula cuántos segundos faltan para que se desbloquee una IP
     */
    public long getSecondsUntilUnblock(String ipAddress) {
        Instant since = Instant.now().minus(LOCKOUT_DURATION_MINUTES, ChronoUnit.MINUTES);
        List<LoginAttempt> failedAttempts = loginAttemptRepository.findFailedAttemptsByIp(ipAddress, since);
        
        if (failedAttempts.isEmpty() || failedAttempts.size() < MAX_FAILED_ATTEMPTS) {
            return 0;
        }

        // El primer intento fallido de la lista (el más reciente) determina cuándo se desbloqueará
        LoginAttempt oldestRelevantAttempt = failedAttempts.get(failedAttempts.size() - 1);
        Instant unblockTime = oldestRelevantAttempt.getAttemptTimestamp()
                .plus(LOCKOUT_DURATION_MINUTES, ChronoUnit.MINUTES);
        
        long secondsRemaining = Instant.now().until(unblockTime, ChronoUnit.SECONDS);
        return Math.max(0, secondsRemaining);
    }

    /**
     * Obtiene los últimos intentos de login para una IP (para auditoría)
     */
    public List<LoginAttempt> getRecentAttemptsForIp(String ipAddress) {
        return loginAttemptRepository.findTop10ByIpAddressOrderByAttemptTimestampDesc(ipAddress);
    }

    /**
     * Obtiene los últimos intentos de login para un usuario (para auditoría)
     */
    public List<LoginAttempt> getRecentAttemptsForUser(String username) {
        return loginAttemptRepository.findTop10ByUsernameOrderByAttemptTimestampDesc(username);
    }

    /**
     * Limpia intentos de login antiguos (llamar desde un job programado)
     */
    @Transactional
    public void cleanOldAttempts(int daysToKeep) {
        Instant cutoff = Instant.now().minus(daysToKeep, ChronoUnit.DAYS);
        loginAttemptRepository.deleteByAttemptTimestampBefore(cutoff);
        AnsiColors.infoLog(logger, "Limpiados intentos de login anteriores a " + cutoff);
    }

    // Getters para configuración (útil para frontend)
    public int getMaxFailedAttempts() {
        return MAX_FAILED_ATTEMPTS;
    }

    public int getLockoutDurationMinutes() {
        return LOCKOUT_DURATION_MINUTES;
    }
}
