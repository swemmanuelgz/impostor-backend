package com.swemmanuelgz.users.impostorbackend.repository;

import com.swemmanuelgz.users.impostorbackend.entity.Game;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    
    @Query("SELECT g FROM Game g LEFT JOIN FETCH g.creator WHERE g.roomCode = :roomCode")
    Optional<Game> findByRoomCode(@Param("roomCode") String roomCode);
    
    @Query("SELECT g FROM Game g LEFT JOIN FETCH g.creator WHERE g.id = :id")
    Optional<Game> findByIdWithCreator(@Param("id") Long id);
    
    boolean existsByRoomCode(String roomCode);
    
    List<Game> findByStatus(String status);
    
    Page<Game> findByStatus(String status, Pageable pageable);
    
    @Query("SELECT g FROM Game g LEFT JOIN FETCH g.creator WHERE g.creator.id = :creatorId")
    List<Game> findByCreatorId(@Param("creatorId") Long creatorId);
    
    @Query("SELECT g FROM Game g LEFT JOIN FETCH g.creator WHERE g.status = 'WAITING' ORDER BY g.createdAt DESC")
    List<Game> findAvailableGames();
    
    @Query("SELECT g FROM Game g LEFT JOIN FETCH g.creator WHERE g.status IN ('WAITING', 'IN_PROGRESS') AND g.creator.id = :userId")
    List<Game> findActiveGamesByCreator(@Param("userId") Long userId);
    
    // Buscar partidas activas antiguas para limpieza
    @Query("SELECT g FROM Game g WHERE g.status IN ('WAITING', 'IN_PROGRESS', 'VOTING') AND g.createdAt < :cutoffTime")
    List<Game> findStaleActiveGames(@Param("cutoffTime") java.time.Instant cutoffTime);
    
    // Cerrar todas las partidas antiguas de una vez
    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Game g SET g.status = 'FINISHED' WHERE g.status IN ('WAITING', 'IN_PROGRESS', 'VOTING') AND g.createdAt < :cutoffTime")
    int closeStaleGames(@Param("cutoffTime") java.time.Instant cutoffTime);
}
