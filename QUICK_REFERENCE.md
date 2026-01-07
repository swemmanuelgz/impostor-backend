# ⚡ Backend Quick Reference - Impostor

## 🚀 Inicio Rápido

```bash
# 1. Verificar Java 21
java -version

# 2. Configurar spring.env
cat > spring.env << EOF
DB_HOST=localhost
DB_PORT=3306
DB_NAME=impostor_db
DB_USERNAME=root
DB_PASSWORD=password
JWT_SECRET=tu-clave-super-secreta-de-256-bits
SERVER_PORT=8080
EOF

# 3. Crear BD MySQL
mysql -u root -p -e "CREATE DATABASE impostor_db;"

# 4. Ejecutar migraciones (si existen)
./gradlew migrate

# 5. Iniciar servidor
./gradlew bootRun

# 6. Verificar que funciona
curl http://localhost:8080/api/auth/login
# Debería responder con error 401/403 (JWT requerido)
```

---

## 🛣️ Rutas API Principales

### Autenticación
```
POST   /api/auth/signup
       Body: { "email": "user@example.com", "password": "123456", "username": "Player1" }
       Response: { "accessToken": "...", "refreshToken": "...", "user": {...} }

POST   /api/auth/login
       Body: { "email": "user@example.com", "password": "123456" }
       Response: { "accessToken": "...", "refreshToken": "...", "user": {...} }

POST   /api/auth/refresh
       Body: { "refreshToken": "..." }
       Response: { "accessToken": "...", "refreshToken": "..." }
```

### Juego
```
POST   /api/games
       Header: Authorization: Bearer <accessToken>
       Body: { "word": "PERRO", "impostorCount": 1 }
       Response: { "id": 123, "roomCode": "ABCDEF", "status": "WAITING" }

GET    /api/games/{roomCode}
       Header: Authorization: Bearer <accessToken>
       Response: { "id": 123, "roomCode": "ABCDEF", "status": "WAITING", ... }

POST   /api/games/{roomCode}/join
       Header: Authorization: Bearer <accessToken>
       Body: { "username": "Player2" }
       Response: { "id": 123, "roomCode": "ABCDEF", ... }

POST   /api/games/{roomCode}/start
       Header: Authorization: Bearer <accessToken>
       Body: {}
       Response: { "id": 123, "status": "IN_PROGRESS", ... }

POST   /api/games/{roomCode}/leave
       Header: Authorization: Bearer <accessToken>
       Response: { "success": true }

GET    /api/games/{roomCode}/players
       Header: Authorization: Bearer <accessToken>
       Response: [{ "id": 1, "username": "P1", "isImpostor": false }, ...]

GET    /api/games/{roomCode}/player
       Header: Authorization: Bearer <accessToken>
       Response: { "id": 1, "username": "P1", "isImpostor": true, "assignedWord": null }
```

---

## 🔌 WebSocket Endpoints

### Suscripciones
```
SUBSCRIBE /topic/game/{roomCode}
   → Recibe: PLAYER_JOINED, VOTE_CAST, GAME_STARTED, etc. (broadcast)

SUBSCRIBE /queue/user/{userId}
   → Recibe: WORD_REVEALED, notificaciones personales
```

### Envío
```
SEND /app/game/{roomCode}/vote
   Body: { "votedUserId": 456, "senderUsername": "Player1" }
   → Procesa voto y broadcast VOTE_CAST

SEND /app/game/{roomCode}/message
   Body: { "content": "Creo que es el rojo", "senderUsername": "Player1" }
   → Broadcast CHAT_MESSAGE
```

---

## 📂 Estructura de Archivos Clave

### Controllers
```
controller/
├── AuthController.java              # POST login, signup, refresh
├── GameController.java              # GET/POST games, join, start, leave
├── GameWebSocketController.java     # WebSocket @MessageMapping
└── PlayerController.java            # GET player info
```

### Services
```
service/
├── AuthService.java                 # Interface
├── AuthServiceImpl.java              # Login, token generation
├── GameService.java                 # Interface
├── GameServiceImpl.java              # Game logic, voting
└── PlayerService.java               # Player management
```

### Entities
```
entity/
├── User.java
├── Game.java
├── GamePlayer.java
├── GameVote.java (opcional)
└── enums/
    ├── GameStatus.java              # WAITING, IN_PROGRESS, VOTING, FINISHED
    └── PlayerStatus.java            # ACTIVE, ELIMINATED, DISCONNECTED
```

### DTOs
```
dto/
├── GameDto.java
├── GamePlayerDto.java
├── GameNotificationDto.java
├── AuthResponseDto.java
├── VoteResultMessage.java
└── GameEndedMessage.java
```

---

## 🔐 JWT Tokens

### Access Token
```json
Header: {
  "alg": "HS512",
  "typ": "JWT"
}

Payload: {
  "sub": "user@example.com",
  "id": 123,
  "email": "user@example.com",
  "role": "USER",
  "iat": 1704067200,
  "exp": 1704070800
}

Signature: [HS512-signed]
```

### Refresh Token
```json
Payload: {
  "sub": "user@example.com",
  "id": 123,
  "tokenType": "refresh",
  "iat": 1704067200,
  "exp": 1704672000
}
```

---

## 🎮 Flujo de Juego Detallado

### 1. Crear Partida
```
[Frontend]
POST /api/games
{
  "word": "PERRO",
  "impostorCount": 1
}

[Backend - GameController.createGame()]
→ GameService.createGame()
  → Generar roomCode (6 caracteres)
  → Crear Game entity
  → Guardar en BD
  → Response: { id, roomCode, status: "WAITING" }

[Response]
{
  "id": 123,
  "roomCode": "ABCDEF",
  "status": "WAITING",
  "creator": { "id": 1, "username": "Player1" },
  "players": [{ "id": 1, "username": "Player1", "isImpostor": false }]
}
```

### 2. Unirse a Partida
```
[Frontend]
POST /api/games/ABCDEF/join
{
  "username": "Player2"
}

[Backend - GameController.joinGame()]
→ GameService.findByRoomCode("ABCDEF")
→ Crear GamePlayer (User + Game + username)
→ Guardar en BD
→ WebSocket BROADCAST:
  /topic/game/ABCDEF
  MessageType: PLAYER_JOINED
  { "playerId": 2, "username": "Player2" }
```

### 3. Iniciar Juego
```
[Frontend]
POST /api/games/ABCDEF/start

[Backend - GameController.startGame()]
→ GameService.startGame()
  1. Validar estado = WAITING
  2. Contar jugadores
  3. Seleccionar impostor(es) random
  4. Asignar palabra a todos
  5. Cambiar status = IN_PROGRESS
  6. Guardar en BD

→ GameWebSocketController.sendRoleNotifications()
  Para cada jugador:
    String playerWord = isImpostor ? null : word
    Enviar a /queue/user/{userId}:
    {
      "type": "WORD_REVEALED",
      "gameId": 123,
      "roomCode": "ABCDEF",
      "isImpostor": false,
      "word": "PERRO"  ← ⚠️ BUG: Debe ser 'playerWord'
    }

→ WebSocket BROADCAST:
  /topic/game/ABCDEF
  MessageType: GAME_STARTED
  { "gameId": 123, "roomCode": "ABCDEF" }
```

### 4. Votación
```
[Frontend - Durante votación]
SEND /app/game/ABCDEF/vote
{
  "votedUserId": 2,
  "senderUsername": "Player1"
}

[Backend - GameWebSocketController.castVote()]
1. Validar game existe
2. Validar status = VOTING
3. Guardar voto: GamePlayer.votedForId = 2
4. Broadcast VOTE_CAST a /topic/game/ABCDEF
5. ⚠️ BUG: NO procesa si todos votaron
   → Debería llamar processVotingResults()

[Backend - processVotingResults() ← NO IMPLEMENTADO]
1. Contar votos
2. Encontrar más votado
3. Broadcast VOTE_RESULT
4. Si era impostor → endGame(false)
5. Si NO era impostor → startNewRound() o endGame(true)
```

---

## 📊 Modelos de Datos

### Game
```java
@Entity
public class Game {
    private Long id;
    private String roomCode;              // 6 chars random
    private String secretWord;            // Palabra para ciudadanos
    private GameStatus status;            // WAITING, IN_PROGRESS, VOTING, FINISHED
    
    @ManyToOne
    private User creator;                 // Usuario que creó la partida
    
    @OneToMany(mappedBy = "game")
    private List<GamePlayer> players;
    
    private int currentRound = 1;
    private int impostorCount;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
```

### GamePlayer
```java
@Entity
public class GamePlayer {
    private Long id;
    
    @ManyToOne
    private Game game;
    
    @ManyToOne
    private User user;
    
    private String username;              // Copia para facilitar queries
    private boolean isImpostor;           // Rol asignado
    private String assignedWord;          // null para impostor, palabra para ciudadano
    
    @Enumerated(EnumType.STRING)
    private PlayerStatus status;          // ACTIVE, ELIMINATED, DISCONNECTED
    
    private Integer votesCast = 0;        // Cantidad de votos recibidos
    private boolean hasVoted = false;     // Ya votó en esta ronda ✅ NECESARIO
    private Long votedForId;              // ID del jugador al que votó ✅ NECESARIO
    private boolean isWinner = false;     // Es ganador
    
    private LocalDateTime joinedAt;
    private LocalDateTime eliminatedAt;
}
```

### User
```java
@Entity
public class User {
    private Long id;
    private String email;
    private String username;
    private String passwordHash;
    private LocalDateTime createdAt;
    
    @OneToMany(mappedBy = "creator")
    private List<Game> createdGames;
    
    @OneToMany(mappedBy = "user")
    private List<GamePlayer> gamePlayers;
}
```

---

## 🔧 Configuración de Spring

### application.properties
```properties
# Server
server.port=8080

# Database (via spring.env)
spring.datasource.url=jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

# Jackson (JSON)
spring.jackson.serialization.write-dates-as-timestamps=false
spring.jackson.default-timezone=Europe/Madrid

# JWT
jwt.secret=${JWT_SECRET}
jwt.access-token-expiration=3600000   # 1 hora
jwt.refresh-token-expiration=604800000 # 7 días

# WebSocket
websocket.stomp.endpoint=/chat-socket
websocket.stomp.broker-relay=/topic,/queue,/user
websocket.stomp.app-destination-prefix=/app
websocket.heartbeat=25000
```

### WebSocketConfig.java
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue", "/user");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/chat-socket")
                .setAllowedOrigins("*")
                .withSockJS();
    }
}
```

---

## 🧪 Testing

### Test: Crear Juego
```java
@Test
public void testCreateGame() {
    // Arrange
    User user = new User("test@example.com", "password", "TestUser");
    userRepository.save(user);
    
    // Act
    Game game = gameService.createGame(user, "PERRO", 1);
    
    // Assert
    assertThat(game.getRoomCode()).hasLength(6);
    assertThat(game.getStatus()).isEqualTo(GameStatus.WAITING);
    assertThat(game.getCreator()).isEqualTo(user);
}
```

### Test: Votación
```java
@Test
public void testVoting() {
    // Arrange
    Game game = setupGameWithPlayers(3);
    game.setStatus(GameStatus.VOTING);
    
    // Act
    gameService.recordVote(game.getId(), player1.getId(), player2.getId());
    gameService.recordVote(game.getId(), player2.getId(), player3.getId());
    gameService.recordVote(game.getId(), player3.getId(), player2.getId());
    
    // Assert
    GamePlayer mostVoted = gameService.getMostVotedPlayer(game.getId());
    assertThat(mostVoted.getId()).isEqualTo(player2.getId());
}
```

---

## 🐛 Debugging Avanzado

### Ver SQL Queries
```properties
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

### Ver WebSocket Frames
```properties
logging.level.org.springframework.web.socket=DEBUG
logging.level.org.springframework.messaging.simp=DEBUG
```

### Ver JWT Claims
```java
// En JwtProvider.java
Claims claims = Jwts.parserBuilder()
    .setSigningKey(key)
    .build()
    .parseClaimsJws(token)
    .getBody();

logger.info("JWT Claims: " + claims); // Usar AnsiColors
```

---

## 📋 Métodos Más Importantes

### GameServiceImpl.recordVote()
```java
public void recordVote(Long gameId, Long voterId, Long votedForId) {
    GamePlayer voter = gamePlayerRepository.findById(voterId)
        .orElseThrow(() -> new GameException("Votante no encontrado", "VOTER_NOT_FOUND"));
    
    voter.setVotedForId(votedForId);
    voter.setHasVoted(true);
    gamePlayerRepository.save(voter);
}
```

### GameServiceImpl.getMostVotedPlayer()
```java
public GamePlayer getMostVotedPlayer(Long gameId) {
    // Algoritmo:
    // 1. Obtener todos los votos
    // 2. Agrupar por votedForId
    // 3. Contar
    // 4. Retornar el más votado
    
    List<GamePlayer> activePlayers = gamePlayerRepository
        .findByGameIdAndStatus(gameId, PlayerStatus.ACTIVE);
    
    return activePlayers.stream()
        .max(Comparator.comparingInt(p -> 
            countVotesForPlayer(gameId, p.getId())))
        .orElseThrow();
}
```

### GameServiceImpl.checkImpostorWins()
```java
public boolean checkImpostorWins(Long gameId) {
    long activePlayers = gamePlayerRepository
        .countByGameIdAndStatus(gameId, PlayerStatus.ACTIVE);
    
    long activeImpostors = gamePlayerRepository
        .countByGameIdAndStatusAndIsImpostorTrue(gameId, PlayerStatus.ACTIVE);
    
    // Impostores ganan si son mayoría o igual
    return activeImpostors >= (activePlayers - activeImpostors);
}
```

---

## 🚀 Deploy

### Compilar
```bash
./gradlew clean build
```

### Ejecutar JAR
```bash
java -jar build/libs/impostor-backend-0.0.1.jar
```

### Docker
```dockerfile
FROM openjdk:21
COPY build/libs/impostor-backend-0.0.1.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 📞 Información Rápida

| Concepto | Valor |
|----------|-------|
| Puerto | 8080 |
| Base de datos | MySQL 8 |
| Java | 21 |
| Spring Boot | 3.4 |
| JWT Algoritmo | HS512 |
| Access Token | 1 hora |
| Refresh Token | 7 días |
| Room Code Length | 6 caracteres |
| WebSocket Heartbeat | 25 segundos |
| Timezone | Europe/Madrid |

---

Ver [BUGS_AND_FIXES.md](BUGS_AND_FIXES.md) para soluciones de bugs críticos.

