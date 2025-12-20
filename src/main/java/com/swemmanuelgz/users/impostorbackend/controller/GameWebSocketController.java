package com.swemmanuelgz.users.impostorbackend.controller;

import com.swemmanuelgz.users.impostorbackend.dto.*;
import com.swemmanuelgz.users.impostorbackend.entity.Game;
import com.swemmanuelgz.users.impostorbackend.exception.GameException;
import com.swemmanuelgz.users.impostorbackend.exception.WebSocketException;
import com.swemmanuelgz.users.impostorbackend.service.GameServiceImpl;
import com.swemmanuelgz.users.impostorbackend.service.GameSessionManager;
import com.swemmanuelgz.users.impostorbackend.utils.AnsiColors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.List;
import java.util.logging.Logger;

/**
 * Controlador WebSocket para el juego del impostor
 * Maneja comunicación en tiempo real entre jugadores de una sala
 * 
 * Endpoints STOMP:
 * - /app/game/{roomCode}/join       → Unirse a sala
 * - /app/game/{roomCode}/leave      → Salir de sala
 * - /app/game/{roomCode}/start      → Iniciar partida (solo creador)
 * - /app/game/{roomCode}/end        → Finalizar partida
 * - /app/game/{roomCode}/chat       → Enviar mensaje de chat
 * - /app/game/{roomCode}/vote       → Votar por un jugador
 * - /app/game/{roomCode}/reconnect  → Reconectar a partida
 * - /app/game/{roomCode}/heartbeat  → Heartbeat para mantener conexión
 * 
 * Suscripciones:
 * - /topic/game/{roomCode}                    → Mensajes broadcast a toda la sala
 * - /user/{userId}/queue/game-notifications   → Notificaciones personales (rol, palabra)
 */
@Controller
@RequiredArgsConstructor
public class GameWebSocketController {
    
    private static final Logger logger = Logger.getLogger(GameWebSocketController.class.getName());
    
    private final GameServiceImpl gameService;
    private final GameSessionManager sessionManager;
    private final SimpMessagingTemplate messagingTemplate;

    // ========== Eventos de Conexión/Desconexión ==========
    
    /**
     * Evento cuando un cliente se conecta al WebSocket
     */
    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
        String sessionId = headers.getSessionId();
        Principal user = headers.getUser();
        
        if (user != null) {
            AnsiColors.successLog(logger, "=== CONEXIÓN WEBSOCKET ===");
            AnsiColors.infoLog(logger, "SessionId: " + sessionId + ", UserId: " + user.getName());
        } else {
            AnsiColors.warningLog(logger, "Conexión anónima - SessionId: " + sessionId);
        }
    }
    
    /**
     * Evento cuando un cliente se desconecta del WebSocket
     */
    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
        String sessionId = headers.getSessionId();
        Principal user = headers.getUser();
        
        AnsiColors.warningLog(logger, "=== DESCONEXIÓN WEBSOCKET ===");
        AnsiColors.infoLog(logger, "SessionId: " + sessionId);
        
        // Registrar desconexión en el gestor de sesiones
        sessionManager.playerDisconnected(sessionId);
        
        // Si el usuario estaba en una sala, notificar a los demás
        if (user != null) {
            try {
                Long userId = Long.parseLong(user.getName());
                String roomCode = sessionManager.findRoomByUserId(userId);
                
                if (roomCode != null) {
                    // Notificar desconexión a la sala
                    GameWebSocketMessage disconnectMsg = GameWebSocketMessage.builder()
                            .type("PLAYER_DISCONNECTED")
                            .roomCode(roomCode)
                            .senderId(userId)
                            .content("Jugador desconectado temporalmente")
                            .build();
                    
                    messagingTemplate.convertAndSend("/topic/game/" + roomCode, disconnectMsg);
                    AnsiColors.infoLog(logger, "Notificada desconexión del jugador " + userId + " a sala " + roomCode);
                }
            } catch (NumberFormatException e) {
                AnsiColors.errorLog(logger, "UserId no válido: " + user.getName());
            }
        }
    }

    // ========== Endpoints de Gestión de Sala ==========
    
    /**
     * Unirse a una sala de juego
     * Cliente envía a: /app/game/{roomCode}/join
     * Broadcast a: /topic/game/{roomCode}
     */
    @MessageMapping("/game/{roomCode}/join")
    @SendTo("/topic/game/{roomCode}")
    public GameWebSocketMessage joinGame(
            @DestinationVariable String roomCode,
            @Payload GameWebSocketMessage message,
            SimpMessageHeaderAccessor headerAccessor) {
        
        AnsiColors.infoLog(logger, "=== JOIN GAME ===");
        AnsiColors.infoLog(logger, "RoomCode: " + roomCode + ", UserId: " + message.getSenderId());
        
        try {
            // Validar que se puede unir
            sessionManager.validateCanJoin(roomCode, message.getSenderId());
            
            // Unirse a la partida via servicio (persiste en BD)
            GameDto gameDto = gameService.joinGame(roomCode, message.getSenderId());
            
            // Registrar conexión en el gestor de sesiones
            String sessionId = headerAccessor.getSessionId();
            sessionManager.playerConnected(roomCode, message.getSenderId(), sessionId);
            
            // Obtener datos del jugador que se unió
            List<GamePlayerDto> players = gameDto.getPlayers();
            GamePlayerDto newPlayer = players.stream()
                    .filter(p -> p.getUserId().equals(message.getSenderId()))
                    .findFirst()
                    .orElse(null);
            
            AnsiColors.successLog(logger, "Jugador " + message.getSenderUsername() + " se unió a sala " + roomCode);
            
            return GameWebSocketMessage.playerJoined(gameDto, newPlayer);
            
        } catch (GameException e) {
            AnsiColors.errorLog(logger, "Error GameException al unirse: " + e.getMessage());
            return GameWebSocketMessage.error(null, roomCode, e.getCodigo(), e.getMessage());
        } catch (WebSocketException e) {
            AnsiColors.errorLog(logger, "Error WebSocketException al unirse: " + e.getMessage());
            return GameWebSocketMessage.error(null, roomCode, e.getCodigo(), e.getMessage());
        } catch (Exception e) {
            AnsiColors.errorLog(logger, "Error inesperado al unirse: " + e.getMessage());
            e.printStackTrace();
            return GameWebSocketMessage.error(null, roomCode, "ERROR_DESCONOCIDO", e.getMessage());
        }
    }
    
    /**
     * Salir de una sala de juego
     * Cliente envía a: /app/game/{roomCode}/leave
     * Broadcast a: /topic/game/{roomCode}
     */
    @MessageMapping("/game/{roomCode}/leave")
    @SendTo("/topic/game/{roomCode}")
    public GameWebSocketMessage leaveGame(
            @DestinationVariable String roomCode,
            @Payload GameWebSocketMessage message,
            SimpMessageHeaderAccessor headerAccessor) {
        
        AnsiColors.infoLog(logger, "=== LEAVE GAME ===");
        AnsiColors.infoLog(logger, "RoomCode: " + roomCode + ", UserId: " + message.getSenderId());
        
        try {
            // Obtener juego antes de salir
            Game game = gameService.findByRoomCode(roomCode)
                    .orElseThrow(() -> WebSocketException.salaNoEncontrada(roomCode));
            
            // Salir de la partida
            gameService.leaveGame(game.getId(), message.getSenderId());
            
            // Desregistrar del gestor de sesiones
            sessionManager.playerDisconnected(headerAccessor.getSessionId());
            
            // Obtener estado actualizado
            GameDto updatedGame = null;
            try {
                Game updatedGameEntity = gameService.findByRoomCode(roomCode).orElse(null);
                if (updatedGameEntity != null) {
                    List<GamePlayerDto> players = gameService.getGamePlayers(updatedGameEntity.getId());
                    updatedGame = GameDto.fromEntityWithPlayers(updatedGameEntity, players);
                }
            } catch (Exception e) {
                // La partida puede haber sido eliminada si no quedan jugadores
                AnsiColors.infoLog(logger, "Partida eliminada o no encontrada después de leave");
            }
            
            AnsiColors.successLog(logger, "Jugador " + message.getSenderId() + " salió de sala " + roomCode);
            
            return GameWebSocketMessage.playerLeft(
                updatedGame != null ? updatedGame : GameDto.builder().roomCode(roomCode).build(),
                message.getSenderId(),
                message.getSenderUsername()
            );
            
        } catch (Exception e) {
            AnsiColors.errorLog(logger, "Error al salir: " + e.getMessage());
            return GameWebSocketMessage.error(null, roomCode, "ERROR_LEAVE", e.getMessage());
        }
    }
    
    /**
     * Iniciar partida (solo el creador puede hacerlo)
     * Cliente envía a: /app/game/{roomCode}/start
     * Broadcast a: /topic/game/{roomCode}
     * + Notificaciones individuales a cada jugador con su rol
     */
    @MessageMapping("/game/{roomCode}/start")
    public void startGame(
            @DestinationVariable String roomCode,
            @Payload GameWebSocketMessage message) {
        
        AnsiColors.infoLog(logger, "=== START GAME ===");
        AnsiColors.infoLog(logger, "RoomCode: " + roomCode + ", CreatorId: " + message.getSenderId());
        AnsiColors.infoLog(logger, "Word: " + message.getContent());
        
        try {
            // Validar mínimo de jugadores
            if (!sessionManager.canStartGame(roomCode)) {
                int current = sessionManager.getConnectedPlayersCount(roomCode);
                throw WebSocketException.minimoJugadores(GameSessionManager.MIN_PLAYERS_TO_START, current);
            }
            
            // Obtener juego
            Game game = gameService.findByRoomCode(roomCode)
                    .orElseThrow(() -> WebSocketException.salaNoEncontrada(roomCode));
            
            // Iniciar partida (esto selecciona al impostor aleatoriamente)
            String word = message.getContent();
            if (word == null || word.trim().isEmpty()) {
                throw WebSocketException.mensajeInvalido("La palabra secreta es requerida");
            }
            
            GameDto gameDto = gameService.startGame(game.getId(), message.getSenderId(), word);
            
            // Marcar sesión como iniciada
            sessionManager.markGameStarted(roomCode, word);
            
            // Enviar mensaje broadcast de que el juego inició
            GameWebSocketMessage startedMsg = GameWebSocketMessage.gameStarted(gameDto, message.getSenderId());
            messagingTemplate.convertAndSend("/topic/game/" + roomCode, startedMsg);
            
            AnsiColors.successLog(logger, "Partida " + roomCode + " iniciada. Enviando roles a jugadores...");
            
            // Enviar notificación individual a cada jugador con su rol y palabra
            sendRoleNotifications(game.getId(), roomCode, word);
            
        } catch (GameException e) {
            AnsiColors.errorLog(logger, "GameException al iniciar: " + e.getMessage());
            sendErrorToRoom(roomCode, e.getCodigo(), e.getMessage());
        } catch (WebSocketException e) {
            AnsiColors.errorLog(logger, "WebSocketException al iniciar: " + e.getMessage());
            sendErrorToRoom(roomCode, e.getCodigo(), e.getMessage());
        } catch (Exception e) {
            AnsiColors.errorLog(logger, "Error inesperado al iniciar: " + e.getMessage());
            e.printStackTrace();
            sendErrorToRoom(roomCode, "ERROR_START", e.getMessage());
        }
    }
    
    /**
     * Envía notificaciones de rol a cada jugador
     */
    private void sendRoleNotifications(Long gameId, String roomCode, String word) {
        List<GamePlayerDto> players = gameService.getGamePlayers(gameId);
        
        for (GamePlayerDto player : players) {
            GameNotificationDto notification = GameNotificationDto.roleReveal(
                    gameId,
                    roomCode,
                    player.getUserId(),
                    player.getIsImpostor(),
                    word
            );
            
            // Enviar notificación personal
            String destination = "/queue/game-notifications";
            String userIdStr = player.getUserId().toString();
            
            messagingTemplate.convertAndSendToUser(userIdStr, destination, notification);
            
            AnsiColors.successLog(logger, "Rol enviado a jugador " + player.getUserId() + 
                " - Impostor: " + player.getIsImpostor());
        }
    }
    
    /**
     * Finalizar partida
     * Cliente envía a: /app/game/{roomCode}/end
     * Broadcast a: /topic/game/{roomCode}
     */
    @MessageMapping("/game/{roomCode}/end")
    @SendTo("/topic/game/{roomCode}")
    public GameWebSocketMessage endGame(
            @DestinationVariable String roomCode,
            @Payload GameWebSocketMessage message) {
        
        AnsiColors.infoLog(logger, "=== END GAME ===");
        AnsiColors.infoLog(logger, "RoomCode: " + roomCode + ", Winner: " + message.getContent());
        
        try {
            Game game = gameService.findByRoomCode(roomCode)
                    .orElseThrow(() -> WebSocketException.salaNoEncontrada(roomCode));
            
            boolean impostorWins = "IMPOSTOR_WINS".equals(message.getContent());
            gameService.endGame(game.getId(), impostorWins);
            
            // Limpiar sesión
            sessionManager.markGameEnded(roomCode);
            
            // Obtener estado final
            List<GamePlayerDto> players = gameService.getGamePlayers(game.getId());
            GameDto finalGame = GameDto.fromEntityWithPlayers(game, players);
            
            AnsiColors.successLog(logger, "Partida " + roomCode + " finalizada. Ganador: " + 
                (impostorWins ? "IMPOSTOR" : "CIVILES"));
            
            return GameWebSocketMessage.gameEnded(finalGame, impostorWins);
            
        } catch (Exception e) {
            AnsiColors.errorLog(logger, "Error al finalizar: " + e.getMessage());
            return GameWebSocketMessage.error(null, roomCode, "ERROR_END", e.getMessage());
        }
    }

    // ========== Chat en tiempo real ==========
    
    /**
     * Enviar mensaje de chat a la sala
     * Cliente envía a: /app/game/{roomCode}/chat
     * Broadcast a: /topic/game/{roomCode}
     */
    @MessageMapping("/game/{roomCode}/chat")
    @SendTo("/topic/game/{roomCode}")
    public GameWebSocketMessage sendChatMessage(
            @DestinationVariable String roomCode,
            @Payload GameWebSocketMessage message) {
        
        AnsiColors.infoLog(logger, "=== CHAT MESSAGE ===");
        AnsiColors.infoLog(logger, "RoomCode: " + roomCode + ", From: " + message.getSenderUsername());
        
        // Validar mensaje
        if (message.getContent() == null || message.getContent().trim().isEmpty()) {
            throw WebSocketException.payloadVacio();
        }
        
        // Validar que el jugador está en la sala
        if (!sessionManager.isPlayerConnected(roomCode, message.getSenderId())) {
            AnsiColors.warningLog(logger, "Jugador " + message.getSenderId() + " no está conectado a sala " + roomCode);
            // Permitir el mensaje de todas formas (puede ser reconexión)
        }
        
        Game game = gameService.findByRoomCode(roomCode).orElse(null);
        Long gameId = game != null ? game.getId() : null;
        
        return GameWebSocketMessage.chatMessage(
                gameId,
                roomCode,
                message.getSenderId(),
                message.getSenderUsername(),
                message.getContent()
        );
    }

    // ========== Votación ==========
    
    /**
     * Votar por un jugador sospechoso
     * Cliente envía a: /app/game/{roomCode}/vote
     * Broadcast a: /topic/game/{roomCode}
     */
    @MessageMapping("/game/{roomCode}/vote")
    @SendTo("/topic/game/{roomCode}")
    public GameWebSocketMessage castVote(
            @DestinationVariable String roomCode,
            @Payload GameWebSocketMessage message) {
        
        AnsiColors.infoLog(logger, "=== VOTE CAST ===");
        AnsiColors.infoLog(logger, "RoomCode: " + roomCode + ", Voter: " + message.getSenderId() + 
            ", VotedFor: " + message.getContent());
        
        try {
            Game game = gameService.findByRoomCode(roomCode)
                    .orElseThrow(() -> WebSocketException.salaNoEncontrada(roomCode));
            
            // Validar que el juego está en progreso
            if (!"IN_PROGRESS".equals(game.getStatus()) && !"VOTING".equals(game.getStatus())) {
                throw WebSocketException.juegoNoIniciado(roomCode);
            }
            
            Long votedUserId = Long.parseLong(message.getContent());
            
            return GameWebSocketMessage.voteCast(game.getId(), roomCode, message.getSenderId(), votedUserId);
            
        } catch (NumberFormatException e) {
            return GameWebSocketMessage.error(null, roomCode, WebSocketException.VOTACION_INVALIDA, 
                "ID de jugador votado inválido");
        } catch (Exception e) {
            return GameWebSocketMessage.error(null, roomCode, "ERROR_VOTE", e.getMessage());
        }
    }

    // ========== Reconexión y Heartbeat ==========
    
    /**
     * Intentar reconexión a una partida
     * Cliente envía a: /app/game/{roomCode}/reconnect
     */
    @MessageMapping("/game/{roomCode}/reconnect")
    public void attemptReconnect(
            @DestinationVariable String roomCode,
            @Payload GameWebSocketMessage message,
            SimpMessageHeaderAccessor headerAccessor) {
        
        AnsiColors.infoLog(logger, "=== RECONNECT ATTEMPT ===");
        AnsiColors.infoLog(logger, "RoomCode: " + roomCode + ", UserId: " + message.getSenderId());
        
        try {
            String sessionId = headerAccessor.getSessionId();
            boolean reconnected = sessionManager.attemptReconnect(message.getSenderId(), roomCode, sessionId);
            
            if (reconnected) {
                // Obtener estado actual del juego
                Game game = gameService.findByRoomCode(roomCode)
                        .orElseThrow(() -> WebSocketException.salaNoEncontrada(roomCode));
                
                List<GamePlayerDto> players = gameService.getGamePlayers(game.getId());
                GameDto gameDto = GameDto.fromEntityWithPlayers(game, players);
                
                // Enviar estado actual al jugador reconectado
                GameWebSocketMessage reconnectMsg = GameWebSocketMessage.reconnected(gameDto, message.getSenderId());
                
                // Enviar a la cola personal del usuario
                messagingTemplate.convertAndSendToUser(
                        message.getSenderId().toString(),
                        "/queue/game-notifications",
                        reconnectMsg
                );
                
                // Notificar a la sala
                GameWebSocketMessage notifyMsg = GameWebSocketMessage.builder()
                        .type("PLAYER_RECONNECTED")
                        .roomCode(roomCode)
                        .gameId(game.getId())
                        .senderId(message.getSenderId())
                        .senderUsername(message.getSenderUsername())
                        .build();
                
                messagingTemplate.convertAndSend("/topic/game/" + roomCode, notifyMsg);
                
                // Si el juego está en progreso, reenviar rol
                if ("IN_PROGRESS".equals(game.getStatus())) {
                    GamePlayerDto playerInfo = gameService.getPlayerRole(game.getId(), message.getSenderId());
                    GameNotificationDto roleNotification = GameNotificationDto.roleReveal(
                            game.getId(),
                            roomCode,
                            message.getSenderId(),
                            playerInfo.getIsImpostor(),
                            playerInfo.getWord()
                    );
                    
                    messagingTemplate.convertAndSendToUser(
                            message.getSenderId().toString(),
                            "/queue/game-notifications",
                            roleNotification
                    );
                }
                
                AnsiColors.successLog(logger, "Reconexión exitosa para " + message.getSenderId());
                
            } else {
                // Reconexión fallida
                GameWebSocketMessage errorMsg = GameWebSocketMessage.error(
                        null, roomCode, 
                        WebSocketException.RECONEXION_FALLIDA,
                        "No se pudo reconectar. Tiempo de espera excedido o sala no válida."
                );
                
                messagingTemplate.convertAndSendToUser(
                        message.getSenderId().toString(),
                        "/queue/game-notifications",
                        errorMsg
                );
                
                AnsiColors.warningLog(logger, "Reconexión fallida para " + message.getSenderId());
            }
            
        } catch (Exception e) {
            AnsiColors.errorLog(logger, "Error en reconexión: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Heartbeat para mantener la conexión activa
     * Cliente envía a: /app/game/{roomCode}/heartbeat
     */
    @MessageMapping("/game/{roomCode}/heartbeat")
    public void handleHeartbeat(
            @DestinationVariable String roomCode,
            @Payload GameWebSocketMessage message,
            SimpMessageHeaderAccessor headerAccessor) {
        
        // Solo log en nivel debug para no saturar
        AnsiColors.infoLog(logger, "Heartbeat recibido - Room: " + roomCode + ", User: " + message.getSenderId());
        
        // Actualizar última actividad en el gestor de sesiones
        sessionManager.getGameSession(roomCode).ifPresent(session -> session.updateLastActivity());
        
        // Responder con heartbeat para confirmar que el servidor está vivo
        GameWebSocketMessage response = GameWebSocketMessage.heartbeat(roomCode);
        
        messagingTemplate.convertAndSendToUser(
                message.getSenderId().toString(),
                "/queue/game-notifications",
                response
        );
    }
    
    // ========== Métodos de Utilidad ==========
    
    /**
     * Envía un mensaje de error a toda la sala
     */
    private void sendErrorToRoom(String roomCode, String errorCode, String message) {
        GameWebSocketMessage errorMsg = GameWebSocketMessage.error(null, roomCode, errorCode, message);
        messagingTemplate.convertAndSend("/topic/game/" + roomCode, errorMsg);
    }
    
    /**
     * Envía un mensaje a un usuario específico
     */
    private void sendToUser(Long userId, Object message) {
        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/game-notifications",
                message
        );
    }
}
