# 🔴 Bugs Críticos y Soluciones Rápidas - Backend Impostor

## 📋 Resumen Ejecutivo

| Bug | Severidad | Ubicación | Estado | Impacto |
|-----|-----------|-----------|--------|---------|
| Palabra correcta en envío | ✅ FUNCIONA | `GameWebSocketController:310` | Arreglado | ~~El impostor recibe la palabra real~~ - El impostor NO ve la palabra (correcto) |
| Votación sin procesamiento | 🔴 CRÍTICO | `GameWebSocketController.castVote()` | No implementado | El juego no continúa después de votar |

---

## � BUG 1: Palabra correcta en envío - VERIFICADO ✅

### Estado Actual
- ✅ El impostor NO ve la palabra (CORRECTO)
- ✅ Los ciudadanos ven la palabra secreta (CORRECTO)
- ✅ Frontend recibe la palabra correctamente

### Conclusión
**Este bug está ARREGLADO** - No es necesario realizar cambios. El código actual en `sendRoleNotifications()` envía la palabra correctamente:
- Impostor recibe: `null` ✅
- Ciudadanos reciben: la palabra real ✅

El parámetro `playerWord` ya está siendo usado correctamente o el bug fue arreglado previamente.

## 🔴 BUG 2: Sistema de votación incompleto

### Síntomas
- ❌ Los jugadores votan pero no pasa nada
- ❌ No se cuenta quién tiene más votos
- ❌ No se revela quién fue eliminado
- ❌ No se revela si era impostor
- ❌ El juego se queda congelado esperando

### Ubicación Exacta
**Archivo**: `src/main/java/com/impostor/controller/GameWebSocketController.java`
**Método**: `castVote(...)`
**Problema**: El método termina sin procesar los resultados

### Código Incorrecto (ACTUAL)
```java
@MessageMapping("/game/{roomCode}/vote")
@SendTo("/topic/game/{roomCode}")
public GameWebSocketMessage castVote(
        @DestinationVariable String roomCode,
        @Header("userId") Long senderUserId,
        GameVoteMessage message) {
    
    // ✅ Valida todo bien
    Game game = gameService.findByRoomCode(roomCode)
        .orElseThrow(() -> GameException.gameNoEncontrado(roomCode));
    
    Long votedUserId = message.getVotedUserId();
    String senderUsername = message.getSenderUsername();
    
    // ✅ Guarda voto y hace broadcast
    return GameWebSocketMessage.voteCast(
        game.getId(),
        roomCode,
        senderUserId,
        senderUsername,
        votedUserId
    );
    
    // ❌ SE TERMINA AQUÍ - FALTA TODO LO IMPORTANTE:
    // ❌ No cuenta si todos votaron
    // ❌ No determina quién fue eliminado
    // ❌ No revela si era impostor
    // ❌ No verifica condiciones de victoria
    // ❌ No inicia nueva ronda
}
```

### Solución Completa (REEMPLAZAR MÉTODO)

```java
@MessageMapping("/game/{roomCode}/vote")
@SendTo("/topic/game/{roomCode}")
public GameWebSocketMessage castVote(
        @DestinationVariable String roomCode,
        @Header("userId") Long senderUserId,
        GameVoteMessage message) throws GameException {
    
    // 1. VALIDAR JUEGO EXISTE Y ESTÁ EN VOTACIÓN
    Game game = gameService.findByRoomCode(roomCode)
        .orElseThrow(() -> GameException.gameNoEncontrado(roomCode));
    
    if (!GameStatus.VOTING.equals(game.getStatus())) {
        throw new GameException(
            "La partida no está en fase de votación",
            "INVALID_GAME_STATE"
        );
    }
    
    Long votedUserId = message.getVotedUserId();
    String senderUsername = message.getSenderUsername();
    
    // 2. GUARDAR VOTO EN BD
    gameService.recordVote(game.getId(), senderUserId, votedUserId);
    AnsiColors.infoLog(logger, 
        "Voto registrado: " + senderUsername + " → Player " + votedUserId);
    
    // 3. ENVIAR BROADCAST DEL VOTO
    GameWebSocketMessage voteCastMsg = GameWebSocketMessage.voteCast(
        game.getId(),
        roomCode,
        senderUserId,
        senderUsername,
        votedUserId
    );
    messagingTemplate.convertAndSend("/topic/game/" + roomCode, voteCastMsg);
    
    // 4. VERIFICAR SI TODOS VOTARON
    int activePlayerCount = gameService.getActivePlayerCount(game.getId());
    int votedPlayerCount = gameService.getVotedPlayerCount(game.getId());
    
    AnsiColors.infoLog(logger, 
        "Votos: " + votedPlayerCount + "/" + activePlayerCount);
    
    if (votedPlayerCount >= activePlayerCount) {
        AnsiColors.successLog(logger, "¡TODOS VOTARON! Procesando resultados...");
        processVotingResults(roomCode, game.getId());
    }
    
    return voteCastMsg;
}

/**
 * Procesa los resultados de la votación:
 * 1. Determina quién fue eliminado
 * 2. Revela si era impostor
 * 3. Verifica condiciones de victoria
 * 4. Continúa a nueva ronda o termina el juego
 */
private void processVotingResults(String roomCode, Long gameId) throws GameException {
    // 1. ENCONTRAR JUGADOR MÁS VOTADO
    GamePlayer eliminatedPlayer = gameService.getMostVotedPlayer(gameId);
    
    AnsiColors.warningLog(logger, 
        "Jugador eliminado: " + eliminatedPlayer.getUsername() + 
        " (Impostor: " + eliminatedPlayer.isImpostor() + ")");
    
    // 2. BROADCAST: RESULTADO DE VOTACIÓN
    VoteResultMessage voteResult = VoteResultMessage.builder()
        .eliminatedUserId(eliminatedPlayer.getId())
        .eliminatedUsername(eliminatedPlayer.getUsername())
        .wasImpostor(eliminatedPlayer.isImpostor())  // ✅ REVELAR AQUÍ
        .gameId(gameId)
        .roomCode(roomCode)
        .build();
    
    messagingTemplate.convertAndSend(
        "/topic/game/" + roomCode,
        GameWebSocketMessage.voteResult(voteResult)
    );
    
    // 3. ¿ERA IMPOSTOR?
    if (eliminatedPlayer.isImpostor()) {
        // ✅ CIUDADANOS GANAN
        AnsiColors.successLog(logger, 
            "¡IMPOSTOR ELIMINADO! Ciudadanos ganan");
        gameService.endGame(gameId, false);  // false = impostor no gana
        
        GameEndedMessage gameEnded = GameEndedMessage.builder()
            .impostorWins(false)
            .impostorNames(List.of())
            .gameId(gameId)
            .roomCode(roomCode)
            .build();
        
        messagingTemplate.convertAndSend(
            "/topic/game/" + roomCode,
            GameWebSocketMessage.gameEnded(gameEnded)
        );
    } else {
        // 4. NO ERA IMPOSTOR - ELIMINAR Y CONTINUAR
        AnsiColors.infoLog(logger, 
            "Ciudadano eliminado. Continuando juego...");
        
        eliminatedPlayer.setStatus(PlayerStatus.ELIMINATED);
        gameService.save(eliminatedPlayer);
        
        // Broadcast: jugador eliminado
        messagingTemplate.convertAndSend(
            "/topic/game/" + roomCode,
            GameWebSocketMessage.playerEliminated(
                eliminatedPlayer.getId(),
                eliminatedPlayer.getUsername(),
                gameId,
                roomCode
            )
        );
        
        // 5. VERIFICAR SI IMPOSTOR GANA (MAYORÍA)
        if (gameService.checkImpostorWins(gameId)) {
            // ✅ IMPOSTORES GANAN
            AnsiColors.successLog(logger, 
                "¡IMPOSTORES SON MAYORÍA! Impostores ganan");
            gameService.endGame(gameId, true);  // true = impostor gana
            
            List<String> impostorNames = gameService.getImpostorNames(gameId);
            GameEndedMessage gameEnded = GameEndedMessage.builder()
                .impostorWins(true)
                .impostorNames(impostorNames)
                .gameId(gameId)
                .roomCode(roomCode)
                .build();
            
            messagingTemplate.convertAndSend(
                "/topic/game/" + roomCode,
                GameWebSocketMessage.gameEnded(gameEnded)
            );
        } else {
            // 6. NUEVA RONDA
            AnsiColors.infoLog(logger, 
                "Iniciando nueva ronda de votación...");
            gameService.startNewRound(gameId);
            
            messagingTemplate.convertAndSend(
                "/topic/game/" + roomCode,
                GameWebSocketMessage.roundStarted(
                    gameService.getCurrentRound(gameId),
                    gameService.getActivePlayerCount(gameId),
                    gameId,
                    roomCode
                )
            );
        }
    }
}
```

### Métodos Necesarios en GameService

```java
// En GameService.java (Interface)
public interface GameService {
    // Votación
    void recordVote(Long gameId, Long voterId, Long votedForId);
    int getVotedPlayerCount(Long gameId);
    int getActivePlayerCount(Long gameId);
    GamePlayer getMostVotedPlayer(Long gameId);
    
    // Eliminación y victoria
    void eliminatePlayer(Long gameId, Long playerId);
    boolean checkImpostorWins(Long gameId);
    List<String> getImpostorNames(Long gameId);
    void startNewRound(Long gameId);
    int getCurrentRound(Long gameId);
    void endGame(Long gameId, boolean impostorWins);
    
    // Persistencia
    void save(GamePlayer player);
}
```

```java
// En GameServiceImpl.java (Implementación)
@Override
public void recordVote(Long gameId, Long voterId, Long votedForId) {
    GamePlayer voter = gamePlayerRepository.findById(voterId).orElseThrow();
    voter.setVotedForId(votedForId);
    voter.setHasVoted(true);
    gamePlayerRepository.save(voter);
}

@Override
public int getVotedPlayerCount(Long gameId) {
    return (int) gamePlayerRepository
        .countByGameIdAndStatusAndHasVotedTrue(gameId, PlayerStatus.ACTIVE);
}

@Override
public int getActivePlayerCount(Long gameId) {
    return (int) gamePlayerRepository
        .countByGameIdAndStatus(gameId, PlayerStatus.ACTIVE);
}

@Override
public GamePlayer getMostVotedPlayer(Long gameId) {
    return gamePlayerRepository.findById(gameId).stream()
        .flatMap(game -> game.getGamePlayers().stream()
            .filter(p -> p.getStatus() == PlayerStatus.ACTIVE))
        .max(Comparator.comparingInt(p -> 
            (int) gamePlayerRepository.countByGameIdAndVotedForId(gameId, p.getId())))
        .orElseThrow(() -> new GameException("No hay votantes", "NO_VOTED_PLAYERS"));
}

@Override
public boolean checkImpostorWins(Long gameId) {
    long activeCount = gamePlayerRepository
        .countByGameIdAndStatus(gameId, PlayerStatus.ACTIVE);
    long impostorCount = gamePlayerRepository
        .countByGameIdAndStatusAndIsImpostorTrue(gameId, PlayerStatus.ACTIVE);
    
    return impostorCount >= (activeCount - impostorCount);
}

@Override
public List<String> getImpostorNames(Long gameId) {
    return gamePlayerRepository
        .findByGameIdAndStatusAndIsImpostorTrue(gameId, PlayerStatus.ACTIVE)
        .stream()
        .map(GamePlayer::getUsername)
        .collect(Collectors.toList());
}

@Override
public void startNewRound(Long gameId) {
    Game game = gameRepository.findById(gameId).orElseThrow();
    game.setCurrentRound(game.getCurrentRound() + 1);
    game.setStatus(GameStatus.IN_PROGRESS);
    
    // Limpiar votos
    gamePlayerRepository.findByGameId(gameId).forEach(p -> {
        p.setHasVoted(false);
        p.setVotedForId(null);
        p.setVotesCast(0);
    });
    
    gameRepository.save(game);
}

@Override
public void endGame(Long gameId, boolean impostorWins) {
    Game game = gameRepository.findById(gameId).orElseThrow();
    game.setStatus(GameStatus.FINISHED);
    
    // Marcar ganadores
    game.getGamePlayers().forEach(p -> {
        boolean isImpostor = p.isImpostor();
        p.setWinner((isImpostor && impostorWins) || (!isImpostor && !impostorWins));
    });
    
    gameRepository.save(game);
}

@Override
public int getCurrentRound(Long gameId) {
    return gameRepository.findById(gameId)
        .map(Game::getCurrentRound)
        .orElse(1);
}

@Override
public void save(GamePlayer player) {
    gamePlayerRepository.save(player);
}
```

### Verificación Post-Fix
1. 3+ jugadores se unen
2. Host inicia
3. Durante discusión: Los ciudadanos ven palabra, impostor ve "?"
4. Tiempo se acaba → fase de votación
5. Todos los jugadores votan
6. Se revela quién fue eliminado y si era impostor
7. Si era impostor → FIN (ciudadanos ganan)
8. Si NO era impostor → Nueva ronda o los impostores ganan

---

## 🟡 IMPORTANTE: Campos necesarios en Entity GamePlayer

```java
@Entity
@Table(name = "game_players")
public class GamePlayer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "game_id")
    private Game game;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    private String username;
    private boolean isImpostor;
    private String assignedWord;
    
    @Enumerated(EnumType.STRING)
    private PlayerStatus status = PlayerStatus.ACTIVE;  // ✅ NECESARIO
    
    private Integer votesCast = 0;        // ✅ NECESARIO
    private boolean hasVoted = false;     // ✅ NECESARIO
    private Long votedForId;              // ✅ NECESARIO - ID del jugador al que votó
    private boolean isWinner = false;     // ✅ NECESARIO
}

public enum PlayerStatus {
    ACTIVE,
    ELIMINATED,
    DISCONNECTED
}
```

---

## 📋 Checklist de Implementación

### BUG 1: Palabra incorrecta
- [ ] Abrir `GameWebSocketController.java`
- [ ] Ir a línea ~310 en `sendRoleNotifications()`
- [ ] Cambiar `word` por `playerWord` en `GameNotificationDto.roleReveal()`
- [ ] Guardar
- [ ] Ejecutar: `./gradlew bootRun`
- [ ] Probar: 1 jugador debería ver palabra, otro no

### BUG 2: Votación incompleto
- [ ] Verificar que `GamePlayer` tiene campos: `votedForId`, `hasVoted`, `votesCast`, `status`
- [ ] Si faltan campos, agregarlos a la entidad
- [ ] Reemplazar método `castVote()` en `GameWebSocketController`
- [ ] Agregar método `processVotingResults()` en `GameWebSocketController`
- [ ] Implementar métodos en `GameServiceImpl` (8 métodos nuevos)
- [ ] Agregar queries necesarias en `GamePlayerRepository`
- [ ] Compilar: `./gradlew build`
- [ ] Ejecutar: `./gradlew bootRun`
- [ ] Probar: 3 jugadores → iniciar → votar → debe revelar resultado y continuar

---

## 🧪 Casos de Prueba

### Test 1: Palabra correcta
```
1. Crear juego
2. Player A se une
3. Player B se une
4. Host inicia
5. VERIFICAR:
   - Player A recibe palabra (ej: "PERRO")
   - Player B recibe null o "?"
   - Uno de ellos debe ser el impostor (el que recibe null)
```

### Test 2: Votación completa
```
1. Crear juego
2. Players A, B, C se unen (3 mínimo)
3. Host inicia
4. Esperar a fase de votación
5. Player A vota a Player B
6. Player B vota a Player C
7. Player C vota a Player B
8. VERIFICAR:
   - Se revela quién tiene más votos (B con 2)
   - Se revela si B era impostor o ciudadano
   - Si era ciudadano: Nueva ronda (¡ROUND_STARTED!)
   - Si era impostor: Juego termina, ciudadanos ganan
```

---

## 📞 Debugging

Si algo no funciona:

1. **Revisar logs**: `./gradlew bootRun | grep -i "vot\|elimina\|voted"`
2. **Verificar BD**: Ver si los votos se guardan en `game_players.voted_for_id`
3. **WebSocket**: Asegurar que los mensajes llegan al frontend (check console del navegador)
4. **Excepciones**: Buscar `GameException` en los logs

