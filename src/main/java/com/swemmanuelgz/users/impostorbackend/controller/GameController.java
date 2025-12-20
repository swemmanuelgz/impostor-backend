package com.swemmanuelgz.users.impostorbackend.controller;

import com.swemmanuelgz.users.impostorbackend.dto.CreateGameRequest;
import com.swemmanuelgz.users.impostorbackend.dto.GameDto;
import com.swemmanuelgz.users.impostorbackend.dto.GamePlayerDto;
import com.swemmanuelgz.users.impostorbackend.dto.JoinGameRequest;
import com.swemmanuelgz.users.impostorbackend.entity.Game;
import com.swemmanuelgz.users.impostorbackend.exception.GameException;
import com.swemmanuelgz.users.impostorbackend.service.GameServiceImpl;
import com.swemmanuelgz.users.impostorbackend.utils.AnsiColors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor
public class GameController {
    
    private static final Logger logger = Logger.getLogger(GameController.class.getName());
    
    private final GameServiceImpl gameService;

    /**
     * Crear nueva partida
     * POST /api/game/create
     */
    @PostMapping("/create")
    public ResponseEntity<GameDto> createGame(@RequestBody CreateGameRequest request) {
        AnsiColors.infoLog(logger, "Creando partida para usuario: " + request.getCreatorId());
        
        GameDto gameDto = gameService.createGame(
                request.getCreatorId(),
                request.getCategory(),
                request.getMaxPlayers()
        );
        
        AnsiColors.successLog(logger, "Partida creada con código: " + gameDto.getRoomCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(gameDto);
    }

    /**
     * Unirse a una partida
     * POST /api/game/join
     */
    @PostMapping("/join")
    public ResponseEntity<GameDto> joinGame(@RequestBody JoinGameRequest request) {
        AnsiColors.infoLog(logger, "Usuario " + request.getUserId() + " uniéndose a sala: " + request.getRoomCode());
        
        GameDto gameDto = gameService.joinGame(request.getRoomCode(), request.getUserId());
        
        AnsiColors.successLog(logger, "Usuario unido exitosamente a la partida");
        return ResponseEntity.ok(gameDto);
    }

    /**
     * Salir de una partida
     * POST /api/game/{gameId}/leave/{userId}
     */
    @PostMapping("/{gameId}/leave/{userId}")
    public ResponseEntity<Void> leaveGame(@PathVariable Long gameId, @PathVariable Long userId) {
        AnsiColors.infoLog(logger, "Usuario " + userId + " saliendo de partida " + gameId);
        
        gameService.leaveGame(gameId, userId);
        
        AnsiColors.successLog(logger, "Usuario salió de la partida exitosamente");
        return ResponseEntity.noContent().build();
    }

    /**
     * Iniciar partida (solo el creador)
     * POST /api/game/{gameId}/start
     */
    @PostMapping("/{gameId}/start")
    public ResponseEntity<GameDto> startGame(
            @PathVariable Long gameId,
            @RequestParam Long creatorId,
            @RequestParam String word) {
        
        AnsiColors.infoLog(logger, "Iniciando partida " + gameId + " por usuario " + creatorId);
        
        GameDto gameDto = gameService.startGame(gameId, creatorId, word);
        
        AnsiColors.successLog(logger, "Partida iniciada exitosamente");
        return ResponseEntity.ok(gameDto);
    }

    /**
     * Finalizar partida
     * POST /api/game/{gameId}/end
     */
    @PostMapping("/{gameId}/end")
    public ResponseEntity<Void> endGame(
            @PathVariable Long gameId,
            @RequestParam boolean impostorWins) {
        
        AnsiColors.infoLog(logger, "Finalizando partida " + gameId);
        
        gameService.endGame(gameId, impostorWins);
        
        AnsiColors.successLog(logger, "Partida finalizada");
        return ResponseEntity.noContent().build();
    }

    /**
     * Obtener partida por ID
     * GET /api/game/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<GameDto> getGameById(@PathVariable Long id) {
        Game game = gameService.findById(id)
                .orElseThrow(() -> GameException.gameNoEncontrado(id));
        
        List<GamePlayerDto> players = gameService.getGamePlayers(id);
        GameDto gameDto = GameDto.fromEntityWithPlayers(game, players);
        
        AnsiColors.infoLog(logger, "Partida encontrada con ID: " + id);
        return ResponseEntity.ok(gameDto);
    }

    /**
     * Obtener partida por código de sala
     * GET /api/game/code/{roomCode}
     */
    @GetMapping("/code/{roomCode}")
    public ResponseEntity<GameDto> getGameByRoomCode(@PathVariable String roomCode) {
        Game game = gameService.findByRoomCode(roomCode)
                .orElseThrow(() -> GameException.gameNoEncontradoPorCodigo(roomCode));
        
        List<GamePlayerDto> players = gameService.getGamePlayers(game.getId());
        GameDto gameDto = GameDto.fromEntityWithPlayers(game, players);
        
        AnsiColors.infoLog(logger, "Partida encontrada con código: " + roomCode);
        return ResponseEntity.ok(gameDto);
    }

    /**
     * Listar partidas disponibles (en estado WAITING)
     * GET /api/game/available
     */
    @GetMapping("/available")
    public ResponseEntity<List<GameDto>> getAvailableGames() {
        List<GameDto> games = gameService.findAvailableGames()
                .stream()
                .map(game -> {
                    List<GamePlayerDto> players = gameService.getGamePlayers(game.getId());
                    return GameDto.fromEntityWithPlayers(game, players);
                })
                .collect(Collectors.toList());
        
        AnsiColors.infoLog(logger, "Listando " + games.size() + " partidas disponibles");
        return ResponseEntity.ok(games);
    }

    /**
     * Obtener todas las partidas con paginación
     * GET /api/game?page=0&size=10
     */
    @GetMapping
    public ResponseEntity<Page<GameDto>> getAllGames(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        
        Sort sort = sortDirection.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<GameDto> games = gameService.findAll(pageable)
                .map(game -> {
                    List<GamePlayerDto> players = gameService.getGamePlayers(game.getId());
                    return GameDto.fromEntityWithPlayers(game, players);
                });
        
        AnsiColors.infoLog(logger, "Listando partidas - Página: " + page);
        return ResponseEntity.ok(games);
    }

    /**
     * Obtener jugadores de una partida
     * GET /api/game/{gameId}/players
     */
    @GetMapping("/{gameId}/players")
    public ResponseEntity<List<GamePlayerDto>> getGamePlayers(@PathVariable Long gameId) {
        List<GamePlayerDto> players = gameService.getGamePlayers(gameId);
        
        AnsiColors.infoLog(logger, "Listando " + players.size() + " jugadores de partida " + gameId);
        return ResponseEntity.ok(players);
    }

    /**
     * Obtener rol del jugador en una partida (su palabra o si es impostor)
     * GET /api/game/{gameId}/role/{userId}
     */
    @GetMapping("/{gameId}/role/{userId}")
    public ResponseEntity<GamePlayerDto> getPlayerRole(
            @PathVariable Long gameId,
            @PathVariable Long userId,
            @RequestParam(required = false) String word) {
        
        GamePlayerDto playerRole = gameService.getPlayerRole(gameId, userId);
        
        // Si no es impostor, asignar la palabra
        if (playerRole.getIsImpostor() != null && !playerRole.getIsImpostor() && word != null) {
            playerRole.setWord(word);
        }
        
        AnsiColors.infoLog(logger, "Obteniendo rol del jugador " + userId + " en partida " + gameId);
        return ResponseEntity.ok(playerRole);
    }

    /**
     * Verificar si existe una partida con el código dado
     * GET /api/game/exists/{roomCode}
     */
    @GetMapping("/exists/{roomCode}")
    public ResponseEntity<Boolean> gameExists(@PathVariable String roomCode) {
        boolean exists = gameService.existsByRoomCode(roomCode);
        return ResponseEntity.ok(exists);
    }

    /**
     * Eliminar partida
     * DELETE /api/game/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGame(@PathVariable Long id) {
        gameService.deleteById(id);
        AnsiColors.successLog(logger, "Partida eliminada con ID: " + id);
        return ResponseEntity.noContent().build();
    }
}
