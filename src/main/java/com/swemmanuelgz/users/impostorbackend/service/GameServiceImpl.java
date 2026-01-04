package com.swemmanuelgz.users.impostorbackend.service;

import com.swemmanuelgz.users.impostorbackend.dto.GameDto;
import com.swemmanuelgz.users.impostorbackend.dto.GamePlayerDto;
import com.swemmanuelgz.users.impostorbackend.entity.Game;
import com.swemmanuelgz.users.impostorbackend.entity.GamePlayer;
import com.swemmanuelgz.users.impostorbackend.entity.User;
import com.swemmanuelgz.users.impostorbackend.exception.GameException;
import com.swemmanuelgz.users.impostorbackend.exception.UserException;
import com.swemmanuelgz.users.impostorbackend.repository.GamePlayerRepository;
import com.swemmanuelgz.users.impostorbackend.repository.GameRepository;
import com.swemmanuelgz.users.impostorbackend.repository.UserRepository;
import com.swemmanuelgz.users.impostorbackend.utils.AnsiColors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameServiceImpl implements GameService {
    
    private static final Logger logger = Logger.getLogger(GameServiceImpl.class.getName());
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 6;
    
    private final GameRepository gameRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final UserRepository userRepository;
    private final Random random = new Random();

    @Override
    @Transactional
    public Game save(Game game) {
        game.setCreatedAt(Instant.now());
        Game savedGame = gameRepository.save(game);
        AnsiColors.successLog(logger, "Partida guardada con ID: " + savedGame.getId());
        return savedGame;
    }

    @Override
    @Transactional
    public Game update(Game game) {
        Game updatedGame = gameRepository.save(game);
        AnsiColors.successLog(logger, "Partida actualizada con ID: " + updatedGame.getId());
        return updatedGame;
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        gameRepository.findById(id)
                .orElseThrow(() -> GameException.gameNoEncontrado(id));
        gameRepository.deleteById(id);
        AnsiColors.successLog(logger, "Partida eliminada con ID: " + id);
    }

    @Override
    public Optional<Game> findById(Long id) {
        return gameRepository.findById(id);
    }

    @Override
    public Page<Game> findAll(Pageable pageable) {
        return gameRepository.findAll(pageable);
    }

    @Override
    public Optional<Game> findByRoomCode(String roomCode) {
        return gameRepository.findByRoomCode(roomCode);
    }

    @Override
    public List<Game> findByStatus(String status) {
        return gameRepository.findByStatus(status);
    }

    @Override
    public List<Game> findAvailableGames() {
        return gameRepository.findAvailableGames();
    }

    @Override
    public List<Game> findActiveGamesByCreator(Long userId) {
        return gameRepository.findActiveGamesByCreator(userId);
    }

    @Override
    @Transactional
    public GameDto createGame(Long creatorId, String category, Integer maxPlayers) {
        AnsiColors.infoLog(logger, "Creando partida para usuario ID: " + creatorId);
        
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> UserException.usuarioNoEncontradoIDLong(creatorId));
        
        // Generar código de sala único
        String roomCode = generateRoomCode();
        while (existsByRoomCode(roomCode)) {
            roomCode = generateRoomCode();
        }
        
        Game game = new Game();
        game.setRoomCode(roomCode);
        game.setStatus("WAITING");
        game.setCreator(creator);
        game.setCreatedAt(Instant.now());
        
        game = gameRepository.save(game);
        AnsiColors.successLog(logger, "Partida creada con código: " + roomCode);
        
        // Añadir al creador como jugador
        GamePlayer creatorPlayer = new GamePlayer();
        creatorPlayer.setGame(game);
        creatorPlayer.setUser(creator);
        creatorPlayer.setIsImpostor(false);
        creatorPlayer.setIsWinner(false);
        gamePlayerRepository.save(creatorPlayer);
        
        AnsiColors.successLog(logger, "Creador añadido a la partida como jugador");
        
        List<GamePlayerDto> players = getGamePlayers(game.getId());
        return GameDto.fromEntityWithPlayers(game, players);
    }

    @Override
    @Transactional
    public GameDto joinGame(String roomCode, Long userId) {
        AnsiColors.infoLog(logger, "Usuario " + userId + " intentando unirse a sala: " + roomCode);
        
        Game game = gameRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> GameException.gameNoEncontradoPorCodigo(roomCode));
        
        // Verificar que la partida esté en estado WAITING
        if (!"WAITING".equals(game.getStatus())) {
            throw GameException.gameYaIniciado(roomCode);
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserException.usuarioNoEncontradoIDLong(userId));
        
        // Verificar si el usuario ya está en la partida
        if (gamePlayerRepository.existsByGameIdAndUserId(game.getId(), userId)) {
            throw GameException.jugadorYaEnPartida(userId);
        }
        
        // Añadir jugador
        GamePlayer player = new GamePlayer();
        player.setGame(game);
        player.setUser(user);
        player.setIsImpostor(false);
        player.setIsWinner(false);
        gamePlayerRepository.save(player);
        
        AnsiColors.successLog(logger, "Usuario " + userId + " se unió a la partida " + roomCode);
        
        List<GamePlayerDto> players = getGamePlayers(game.getId());
        return GameDto.fromEntityWithPlayers(game, players);
    }

    @Override
    @Transactional
    public void leaveGame(Long gameId, Long userId) {
        AnsiColors.infoLog(logger, "Usuario " + userId + " saliendo de partida " + gameId);
        
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> GameException.gameNoEncontrado(gameId));
        
        if (!gamePlayerRepository.existsByGameIdAndUserId(gameId, userId)) {
            throw GameException.jugadorNoEnPartida(userId);
        }
        
        gamePlayerRepository.deleteByGameIdAndUserId(gameId, userId);
        AnsiColors.successLog(logger, "Usuario " + userId + " salió de la partida " + gameId);
        
        // Si no quedan jugadores, eliminar la partida
        int remainingPlayers = gamePlayerRepository.countByGameId(gameId);
        if (remainingPlayers == 0) {
            gameRepository.deleteById(gameId);
            AnsiColors.infoLog(logger, "Partida " + gameId + " eliminada por no tener jugadores");
        }
    }

    @Override
    @Transactional
    public GameDto startGame(Long gameId, Long creatorId, String word) {
        AnsiColors.infoLog(logger, "Iniciando partida " + gameId);
        
        Game game = gameRepository.findByIdWithCreator(gameId)
                .orElseThrow(() -> GameException.gameNoEncontrado(gameId));
        
        // Verificar que quien inicia es el creador
        if (!game.getCreator().getId().equals(creatorId)) {
            throw GameException.noEsCreador(creatorId);
        }
        
        // Verificar estado
        if (!"WAITING".equals(game.getStatus())) {
            throw GameException.gameYaIniciado(game.getRoomCode());
        }
        
        List<GamePlayer> players = gamePlayerRepository.findByGameId(gameId);
        
        if (players.size() < 2) {
            throw new GameException("Se necesitan al menos 3 jugadores para iniciar", "JUGADORES_INSUFICIENTES");
        }
        
        // Seleccionar impostor aleatoriamente
        int impostorIndex = random.nextInt(players.size());
        for (int i = 0; i < players.size(); i++) {
            GamePlayer player = players.get(i);
            player.setIsImpostor(i == impostorIndex);
            gamePlayerRepository.save(player);
        }
        
        // Cambiar estado de la partida
        game.setStatus("IN_PROGRESS");
        game = gameRepository.save(game);
        
        AnsiColors.successLog(logger, "Partida " + gameId + " iniciada. Impostor seleccionado.");
        
        List<GamePlayerDto> playerDtos = getGamePlayers(gameId);
        GameDto gameDto = GameDto.fromEntityWithPlayers(game, playerDtos);
        gameDto.setWord(word); // La palabra se enviará individualmente a cada jugador
        
        return gameDto;
    }

    @Override
    @Transactional
    public void endGame(Long gameId, boolean impostorWins) {
        AnsiColors.infoLog(logger, "Finalizando partida " + gameId + ". Impostor gana: " + impostorWins);
        
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> GameException.gameNoEncontrado(gameId));
        
        List<GamePlayer> players = gamePlayerRepository.findByGameId(gameId);
        
        for (GamePlayer player : players) {
            // Si el impostor gana, solo él es ganador
            // Si los civiles ganan, todos excepto el impostor son ganadores
            boolean isWinner = impostorWins ? player.getIsImpostor() : !player.getIsImpostor();
            player.setIsWinner(isWinner);
            gamePlayerRepository.save(player);
        }
        
        game.setStatus("FINISHED");
        gameRepository.save(game);
        
        AnsiColors.successLog(logger, "Partida " + gameId + " finalizada");
    }

    @Override
    public boolean existsByRoomCode(String roomCode) {
        return gameRepository.existsByRoomCode(roomCode);
    }

    @Override
    public String generateRoomCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return code.toString();
    }

    @Override
    public List<GamePlayerDto> getGamePlayers(Long gameId) {
        return gamePlayerRepository.findByGameId(gameId)
                .stream()
                .map(GamePlayerDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public GamePlayerDto getPlayerRole(Long gameId, Long userId) {
        GamePlayer player = gamePlayerRepository.findByGameIdAndUserId(gameId, userId)
                .orElseThrow(() -> GameException.jugadorNoEnPartida(userId));
        
        // Si es impostor, no tiene palabra
        String word = player.getIsImpostor() ? null : "PALABRA_DEL_JUEGO"; // La palabra real se enviará desde el controlador
        
        return GamePlayerDto.fromEntityWithRole(player, word);
    }
}
