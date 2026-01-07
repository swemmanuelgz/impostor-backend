package com.swemmanuelgz.users.impostorbackend.repository;

import com.swemmanuelgz.users.impostorbackend.entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    /**
     * Cuenta los intentos fallidos desde una IP en un período de tiempo
     */
    @Query("SELECT COUNT(la) FROM LoginAttempt la WHERE la.ipAddress = :ip AND la.success = false AND la.attemptTimestamp > :since")
    long countFailedAttemptsByIpSince(@Param("ip") String ipAddress, @Param("since") Instant since);

    /**
     * Cuenta los intentos fallidos para un username en un período de tiempo
     */
    @Query("SELECT COUNT(la) FROM LoginAttempt la WHERE la.username = :username AND la.success = false AND la.attemptTimestamp > :since")
    long countFailedAttemptsByUsernameSince(@Param("username") String username, @Param("since") Instant since);

    /**
     * Obtiene los últimos N intentos de login para una IP
     */
    List<LoginAttempt> findTop10ByIpAddressOrderByAttemptTimestampDesc(String ipAddress);

    /**
     * Obtiene los últimos N intentos de login para un username
     */
    List<LoginAttempt> findTop10ByUsernameOrderByAttemptTimestampDesc(String username);

    /**
     * Obtiene el último intento de login para una IP
     */
    LoginAttempt findFirstByIpAddressOrderByAttemptTimestampDesc(String ipAddress);

    /**
     * Obtiene todos los intentos de login en las últimas N horas (para auditoría)
     */
    @Query("SELECT la FROM LoginAttempt la WHERE la.attemptTimestamp > :since ORDER BY la.attemptTimestamp DESC")
    List<LoginAttempt> findRecentAttempts(@Param("since") Instant since);

    /**
     * Obtiene intentos fallidos de una IP específica (para detectar ataques)
     */
    @Query("SELECT la FROM LoginAttempt la WHERE la.ipAddress = :ip AND la.success = false AND la.attemptTimestamp > :since ORDER BY la.attemptTimestamp DESC")
    List<LoginAttempt> findFailedAttemptsByIp(@Param("ip") String ipAddress, @Param("since") Instant since);

    /**
     * Limpia intentos de login antiguos (para mantenimiento)
     */
    void deleteByAttemptTimestampBefore(Instant before);
}
