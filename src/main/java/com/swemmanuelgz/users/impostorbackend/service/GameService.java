package com.swemmanuelgz.users.impostorbackend.service;

import com.swemmanuelgz.users.impostorbackend.dto.GameDto;
import com.swemmanuelgz.users.impostorbackend.dto.GamePlayerDto;
import com.swemmanuelgz.users.impostorbackend.entity.Game;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface GameService {
    // CRUD básico
    Game save(Game game);
    Game update(Game game);
    void deleteById(Long id);
    Optional<Game> findById(Long id);
    Page<Game> findAll(Pageable pageable);
    
    // Búsquedas específicas
    Optional<Game> findByRoomCode(String roomCode);
    List<Game> findByStatus(String status);
    List<Game> findAvailableGames();
    List<Game> findActiveGamesByCreator(Long userId);
    
    // Lógica del juego
    GameDto createGame(Long creatorId, String category, Integer maxPlayers, Integer duration);
    GameDto joinGame(String roomCode, Long userId);
    void leaveGame(Long gameId, Long userId);
    GameDto startGame(Long gameId, Long creatorId, String word);
    void endGame(Long gameId, boolean impostorWins);
    
    // Verificaciones
    boolean existsByRoomCode(String roomCode);
    String generateRoomCode();
    
    // Obtener jugadores
    List<GamePlayerDto> getGamePlayers(Long gameId);
    GamePlayerDto getPlayerRole(Long gameId, Long userId);
    
    // Reconexión
    Optional<GameDto> getActiveGameForUser(Long userId);
    GameDto rejoinGame(String roomCode, Long userId);
}
