package com.swemmanuelgz.users.impostorbackend.exception;

import com.swemmanuelgz.users.impostorbackend.utils.AnsiColors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = Logger.getLogger(GlobalExceptionHandler.class.getName());

    @ExceptionHandler(UserException.class)
    public ResponseEntity<Map<String, Object>> handleUserException(UserException ex) {
        AnsiColors.errorLog(logger, "UserException: " + ex.getMessage());
        
        HttpStatus status;
        switch (ex.getCodigo()) {
            case UserException.USUARIO_NO_ENCONTRADO:
                status = HttpStatus.NOT_FOUND;
                break;
            case UserException.USUARIO_YA_EXISTE:
                status = HttpStatus.CONFLICT;
                break;
            case UserException.PASSWORD_INCORRECTO:
                status = HttpStatus.UNAUTHORIZED;
                break;
            case UserException.USUARIO_OAUTH:
                status = HttpStatus.BAD_REQUEST;  // Usuario OAuth intenta login con contraseña
                break;
            case UserException.JSON_INVALIDO:
            case UserException.EMAIL_INVALIDO:
            case UserException.USERNAME_INVALIDO:
                status = HttpStatus.BAD_REQUEST;
                break;
            default:
                status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        Map<String, Object> response = new HashMap<>();
        response.put("codigo", ex.getCodigo());
        response.put("mensaje", ex.getMessage());
        response.put("status", status.value());

        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(TokenException.class)
    public ResponseEntity<Map<String, Object>> handleTokenException(TokenException ex) {
        AnsiColors.errorLog(logger, "TokenException: " + ex.getMessage());

        HttpStatus status;
        switch (ex.getCodigo()) {
            case TokenException.TOKEN_EXPIRADO:
            case TokenException.TOKEN_INVALIDO:
            case TokenException.TOKEN_NO_PROPORCIONADO:
            case TokenException.REFRESH_TOKEN_EXPIRADO:
            case TokenException.REFRESH_TOKEN_INVALIDO:
            case TokenException.FIRMA_TOKEN_INVALIDA:
                status = HttpStatus.UNAUTHORIZED;
                break;
            case TokenException.TOKEN_MALFORMADO:
                status = HttpStatus.BAD_REQUEST;
                break;
            case TokenException.NO_AUTORIZADO:
                status = HttpStatus.FORBIDDEN;
                break;
            default:
                status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        Map<String, Object> response = new HashMap<>();
        response.put("codigo", ex.getCodigo());
        response.put("mensaje", ex.getMessage());
        response.put("status", status.value());

        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(GameException.class)
    public ResponseEntity<Map<String, Object>> handleGameException(GameException ex) {
        AnsiColors.errorLog(logger, "GameException: " + ex.getMessage());

        HttpStatus status;
        switch (ex.getCodigo()) {
            case GameException.GAME_NO_ENCONTRADO:
            case GameException.JUGADOR_NO_EN_PARTIDA:
                status = HttpStatus.NOT_FOUND;
                break;
            case GameException.GAME_LLENO:
            case GameException.GAME_YA_INICIADO:
            case GameException.JUGADOR_YA_EN_PARTIDA:
                status = HttpStatus.CONFLICT;
                break;
            case GameException.NO_ES_CREADOR:
                status = HttpStatus.FORBIDDEN;
                break;
            case GameException.ROOM_CODE_INVALIDO:
                status = HttpStatus.BAD_REQUEST;
                break;
            default:
                status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        Map<String, Object> response = new HashMap<>();
        response.put("codigo", ex.getCodigo());
        response.put("mensaje", ex.getMessage());
        response.put("status", status.value());

        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        AnsiColors.errorLog(logger, "Exception genérica: " + ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("codigo", "ERROR_INTERNO");
        response.put("mensaje", "Ha ocurrido un error interno en el servidor");
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
