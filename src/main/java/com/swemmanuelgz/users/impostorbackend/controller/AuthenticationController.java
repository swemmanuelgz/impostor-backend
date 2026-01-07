package com.swemmanuelgz.users.impostorbackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swemmanuelgz.users.impostorbackend.dto.GoogleAuthRequest;
import com.swemmanuelgz.users.impostorbackend.dto.GoogleUserInfo;
import com.swemmanuelgz.users.impostorbackend.dto.TokenRefreshRequest;
import com.swemmanuelgz.users.impostorbackend.dto.TokenRefreshResponse;
import com.swemmanuelgz.users.impostorbackend.dto.UserDto;
import com.swemmanuelgz.users.impostorbackend.entity.User;
import com.swemmanuelgz.users.impostorbackend.exception.UserException;
import com.swemmanuelgz.users.impostorbackend.security.JwtProvider;
import com.swemmanuelgz.users.impostorbackend.service.GoogleTokenService;
import com.swemmanuelgz.users.impostorbackend.service.OAuth2UserService;
import com.swemmanuelgz.users.impostorbackend.service.UserServiceImpl;
import com.swemmanuelgz.users.impostorbackend.utils.AnsiColors;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    
    private final Logger logger = Logger.getLogger(getClass().getName());
    
    private final JwtProvider jwtProvider;
    private final UserServiceImpl userService;
    private final GoogleTokenService googleTokenService;
    private final OAuth2UserService oAuth2UserService;

    /**
     * Login de usuario
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam("username") String username,
                                   @RequestParam("password") String password) {
        
        AnsiColors.infoLog(logger, "Intento de login para: " + username);
        
        User user = userService.findByEmailOrUsername(username)
                .orElseThrow(() -> UserException.usuarioNoEncontradoEmail(username));
        
        AnsiColors.infoLog(logger, "Usuario encontrado: " + user.getEmail());
        
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        if (!encoder.matches(password, user.getPassword())) {
            AnsiColors.errorLog(logger, "Contraseña incorrecta para usuario: " + username);
            throw UserException.usuarioPasswordIncorrecto(username);
        }
        
        // Generar tokens
        String accessToken = jwtProvider.generateToken(user);
        String refreshToken = jwtProvider.generateRefreshToken(user);
        
        Map<String, Object> response = new HashMap<>();
        response.put("accessToken", accessToken);
        response.put("refreshToken", refreshToken);
        response.put("user", UserDto.fromEntity(user));
        
        AnsiColors.successLog(logger, "Login realizado con éxito para: " + username);
        return ResponseEntity.ok(response);
    }

    /**
     * Refresh token
     * POST /api/auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody TokenRefreshRequest request) {
        AnsiColors.infoLog(logger, "Procesando solicitud de refreshToken");
        
        String requestRefreshToken = request.getRefreshToken();
        
        // Validar el refresh token
        if (!jwtProvider.validateToken(requestRefreshToken)) {
            return ResponseEntity.badRequest().body("Refresh token no válido o expirado");
        }
        
        // Verificar que sea refresh token
        Claims claims = jwtProvider.getClaims(requestRefreshToken);
        Object tokenType = claims.get("tokenType");
        
        if (tokenType == null || !tokenType.equals("refresh")) {
            return ResponseEntity.badRequest().body("Token no es un refresh token válido");
        }
        
        // Generar nuevos tokens
        String email = jwtProvider.getEmailFromToken(requestRefreshToken);
        User user = userService.findByEmail(email)
                .orElseThrow(() -> UserException.usuarioNoEncontradoEmail(email));
        
        String newAccessToken = jwtProvider.generateToken(user);
        String newRefreshToken = jwtProvider.generateRefreshToken(user);
        
        AnsiColors.successLog(logger, "Tokens actualizados para: " + user.getEmail());
        return ResponseEntity.ok(new TokenRefreshResponse(newAccessToken, newRefreshToken));
    }

    /**
     * Registro de usuario
     * POST /api/auth/signup
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody UserDto userDto) {
        AnsiColors.infoLog(logger, "Registro de nuevo usuario: " + userDto.getEmail());
        
        // Verificar que no exista el usuario
        if (userService.existsByEmailOrUsername(userDto.getEmail(), userDto.getUsername())) {
            throw UserException.usuarioYaExiste(userDto.getEmail(), userDto.getUsername());
        }
        
        // Guardar contraseña sin cifrar para login automático
        String rawPassword = userDto.getPassword();
        
        // Encriptar contraseña
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        userDto.setPassword(encoder.encode(userDto.getPassword()));
        
        // Crear usuario
        User user = userDto.toEntity();
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        
        User savedUser = userService.save(user);
        AnsiColors.successLog(logger, "Usuario registrado con ID: " + savedUser.getId());
        
        // Login automático después del registro
        return login(savedUser.getEmail(), rawPassword);
    }

    /**
     * Cambiar contraseña
     * POST /api/auth/change-password
     */
    @PostMapping("/change-password")
    public ResponseEntity<UserDto> changePassword(@RequestParam("userId") Long userId,
                                                   @RequestParam("oldPassword") String oldPassword,
                                                   @RequestParam("newPassword") String newPassword) {
        
        User user = userService.findById(userId)
                .orElseThrow(() -> UserException.usuarioNoEncontradoIDLong(userId));
        
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        if (encoder.matches(oldPassword, user.getPassword())) {
            user.setPassword(encoder.encode(newPassword));
            User updatedUser = userService.update(user);
            AnsiColors.successLog(logger, "Contraseña actualizada para usuario ID: " + userId);
            return ResponseEntity.ok(UserDto.fromEntity(updatedUser));
        }
        
        AnsiColors.errorLog(logger, "Contraseña antigua incorrecta para usuario ID: " + userId);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    /**
     * Login con Google OAuth2
     * POST /api/auth/google
     * 
     * El frontend (Flutter) hace Sign-In con Google y envía el idToken al backend.
     * El backend valida el token, crea/actualiza el usuario, y devuelve JWT propio.
     */
    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody GoogleAuthRequest request) {
        AnsiColors.infoLog(logger, "Procesando login con Google");

        // Verificar si Google OAuth2 está habilitado
        if (!googleTokenService.isEnabled()) {
            AnsiColors.errorLog(logger, "Google OAuth2 no está configurado");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Google OAuth2 no está configurado en el servidor"));
        }

        if (request.getIdToken() == null || request.getIdToken().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "idToken es requerido"));
        }

        // 1. Validar token de Google
        GoogleUserInfo googleUserInfo = googleTokenService.verifyToken(request.getIdToken())
                .orElse(null);

        if (googleUserInfo == null) {
            AnsiColors.errorLog(logger, "Token de Google inválido");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Token de Google inválido o expirado"));
        }

        // 2. Registrar o recuperar usuario
        User user = oAuth2UserService.processGoogleUser(googleUserInfo);

        // 3. Generar tokens JWT propios
        String accessToken = jwtProvider.generateToken(user);
        String refreshToken = jwtProvider.generateRefreshToken(user);

        Map<String, Object> response = new HashMap<>();
        response.put("accessToken", accessToken);
        response.put("refreshToken", refreshToken);
        response.put("user", UserDto.fromEntity(user));
        response.put("isNewUser", user.getCreatedAt().plusSeconds(5).isAfter(Instant.now())); // Usuario creado hace menos de 5 segundos

        AnsiColors.successLog(logger, "Login con Google exitoso para: " + user.getEmail());
        return ResponseEntity.ok(response);
    }
}
