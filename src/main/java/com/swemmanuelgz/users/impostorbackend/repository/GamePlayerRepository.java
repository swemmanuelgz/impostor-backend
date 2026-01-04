package com.swemmanuelgz.users.impostorbackend.repository;

import com.swemmanuelgz.users.impostorbackend.entity.GamePlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GamePlayerRepository extends JpaRepository<GamePlayer, Long> {
    
    @Query("SELECT gp FROM GamePlayer gp JOIN FETCH gp.user JOIN FETCH gp.game WHERE gp.game.id = :gameId")
    List<GamePlayer> findByGameId(@Param("gameId") Long gameId);
    
    @Query("SELECT gp FROM GamePlayer gp JOIN FETCH gp.user WHERE gp.user.id = :userId")
    List<GamePlayer> findByUserId(@Param("userId") Long userId);
    
    @Query("SELECT gp FROM GamePlayer gp JOIN FETCH gp.user JOIN FETCH gp.game WHERE gp.game.id = :gameId AND gp.user.id = :userId")
    Optional<GamePlayer> findByGameIdAndUserId(@Param("gameId") Long gameId, @Param("userId") Long userId);
    
    @Query("SELECT COUNT(gp) FROM GamePlayer gp WHERE gp.game.id = :gameId")
    int countByGameId(@Param("gameId") Long gameId);
    
    @Query("SELECT gp FROM GamePlayer gp WHERE gp.game.id = :gameId AND gp.isImpostor = true")
    List<GamePlayer> findImpostorsByGameId(@Param("gameId") Long gameId);
    
    @Query("SELECT CASE WHEN COUNT(gp) > 0 THEN true ELSE false END FROM GamePlayer gp WHERE gp.game.id = :gameId AND gp.user.id = :userId")
    boolean existsByGameIdAndUserId(@Param("gameId") Long gameId, @Param("userId") Long userId);
    
    void deleteByGameIdAndUserId(Long gameId, Long userId);
    
    // Estadísticas del jugador
    @Query("SELECT COUNT(gp) FROM GamePlayer gp WHERE gp.user.id = :userId")
    int countGamesPlayedByUserId(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(gp) FROM GamePlayer gp WHERE gp.user.id = :userId AND gp.isWinner = true")
    int countGamesWonByUserId(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(gp) FROM GamePlayer gp WHERE gp.user.id = :userId AND gp.isImpostor = true AND gp.isWinner = true")
    int countGamesWonAsImpostorByUserId(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(gp) FROM GamePlayer gp WHERE gp.user.id = :userId AND gp.isImpostor = false AND gp.isWinner = true")
    int countGamesWonAsCivilianByUserId(@Param("userId") Long userId);
    
    // Reconexión: buscar partida activa más reciente del usuario
    @Query("SELECT gp FROM GamePlayer gp JOIN FETCH gp.game g JOIN FETCH gp.user WHERE gp.user.id = :userId AND g.status IN ('WAITING', 'IN_PROGRESS', 'VOTING') ORDER BY g.createdAt DESC")
    List<GamePlayer> findActiveGamesByUserId(@Param("userId") Long userId);
}
