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
    
    Optional<Game> findByRoomCode(String roomCode);
    
    boolean existsByRoomCode(String roomCode);
    
    List<Game> findByStatus(String status);
    
    Page<Game> findByStatus(String status, Pageable pageable);
    
    @Query("SELECT g FROM Game g WHERE g.creator.id = :creatorId")
    List<Game> findByCreatorId(@Param("creatorId") Long creatorId);
    
    @Query("SELECT g FROM Game g WHERE g.status = 'WAITING' ORDER BY g.createdAt DESC")
    List<Game> findAvailableGames();
    
    @Query("SELECT g FROM Game g WHERE g.status IN ('WAITING', 'IN_PROGRESS') AND g.creator.id = :userId")
    List<Game> findActiveGamesByCreator(@Param("userId") Long userId);
}
