package com.swemmanuelgz.users.impostorbackend.exception;

import lombok.Getter;

/**
 * Excepción para errores de WebSocket en el juego del impostor
 * Incluye códigos de error tipados para manejo específico en el cliente
 */
@Getter
public class WebSocketException extends RuntimeException {
    
    // ========== Códigos de Error de Conexión ==========
    public static final String CONEXION_FALLIDA = "WS_CONEXION_FALLIDA";
    public static final String SESION_EXPIRADA = "WS_SESION_EXPIRADA";
    public static final String RECONEXION_FALLIDA = "WS_RECONEXION_FALLIDA";
    public static final String USUARIO_NO_AUTENTICADO = "WS_USUARIO_NO_AUTENTICADO";
    
    // ========== Códigos de Error de Sala ==========
    public static final String SALA_NO_ENCONTRADA = "WS_SALA_NO_ENCONTRADA";
    public static final String SALA_LLENA = "WS_SALA_LLENA";
    public static final String SALA_YA_INICIADA = "WS_SALA_YA_INICIADA";
    public static final String MAX_JUGADORES_ALCANZADO = "WS_MAX_JUGADORES";
    
    // ========== Códigos de Error de Jugador ==========
    public static final String JUGADOR_NO_EN_SALA = "WS_JUGADOR_NO_EN_SALA";
    public static final String JUGADOR_YA_EN_SALA = "WS_JUGADOR_YA_EN_SALA";
    public static final String JUGADOR_DESCONECTADO = "WS_JUGADOR_DESCONECTADO";
    public static final String NO_PERMISOS = "WS_NO_PERMISOS";
    
    // ========== Códigos de Error de Mensaje ==========
    public static final String MENSAJE_INVALIDO = "WS_MENSAJE_INVALIDO";
    public static final String TIPO_MENSAJE_DESCONOCIDO = "WS_TIPO_MENSAJE_DESCONOCIDO";
    public static final String PAYLOAD_VACIO = "WS_PAYLOAD_VACIO";
    
    // ========== Códigos de Error de Juego ==========
    public static final String JUEGO_NO_INICIADO = "WS_JUEGO_NO_INICIADO";
    public static final String JUEGO_YA_TERMINADO = "WS_JUEGO_YA_TERMINADO";
    public static final String MINIMO_JUGADORES = "WS_MINIMO_JUGADORES";
    public static final String VOTACION_INVALIDA = "WS_VOTACION_INVALIDA";

    private final String codigo;

    public WebSocketException(String mensaje, String codigo) {
        super(mensaje);
        this.codigo = codigo;
    }

    // ========== Factory Methods - Conexión ==========
    
    public static WebSocketException conexionFallida(String detalle) {
        return new WebSocketException("Error de conexión WebSocket: " + detalle, CONEXION_FALLIDA);
    }
    
    public static WebSocketException sesionExpirada(String sessionId) {
        return new WebSocketException("La sesión WebSocket ha expirado: " + sessionId, SESION_EXPIRADA);
    }
    
    public static WebSocketException reconexionFallida(Long userId, String roomCode) {
        return new WebSocketException(
            "No se pudo reconectar al usuario " + userId + " a la sala " + roomCode, 
            RECONEXION_FALLIDA
        );
    }
    
    public static WebSocketException usuarioNoAutenticado() {
        return new WebSocketException("Usuario no autenticado para WebSocket", USUARIO_NO_AUTENTICADO);
    }

    // ========== Factory Methods - Sala ==========
    
    public static WebSocketException salaNoEncontrada(String roomCode) {
        return new WebSocketException("Sala no encontrada con código: " + roomCode, SALA_NO_ENCONTRADA);
    }
    
    public static WebSocketException salaLlena(String roomCode) {
        return new WebSocketException("La sala " + roomCode + " está llena", SALA_LLENA);
    }
    
    public static WebSocketException salaYaIniciada(String roomCode) {
        return new WebSocketException("La partida en sala " + roomCode + " ya ha iniciado", SALA_YA_INICIADA);
    }
    
    public static WebSocketException maxJugadoresAlcanzado(String roomCode, int max) {
        return new WebSocketException(
            "Máximo de " + max + " jugadores alcanzado en sala " + roomCode, 
            MAX_JUGADORES_ALCANZADO
        );
    }

    // ========== Factory Methods - Jugador ==========
    
    public static WebSocketException jugadorNoEnSala(Long userId, String roomCode) {
        return new WebSocketException(
            "El jugador " + userId + " no está en la sala " + roomCode, 
            JUGADOR_NO_EN_SALA
        );
    }
    
    public static WebSocketException jugadorYaEnSala(Long userId, String roomCode) {
        return new WebSocketException(
            "El jugador " + userId + " ya está en la sala " + roomCode, 
            JUGADOR_YA_EN_SALA
        );
    }
    
    public static WebSocketException jugadorDesconectado(Long userId) {
        return new WebSocketException("El jugador " + userId + " está desconectado", JUGADOR_DESCONECTADO);
    }
    
    public static WebSocketException sinPermisos(Long userId, String accion) {
        return new WebSocketException(
            "El usuario " + userId + " no tiene permisos para: " + accion, 
            NO_PERMISOS
        );
    }

    // ========== Factory Methods - Mensaje ==========
    
    public static WebSocketException mensajeInvalido(String detalle) {
        return new WebSocketException("Mensaje WebSocket inválido: " + detalle, MENSAJE_INVALIDO);
    }
    
    public static WebSocketException tipoMensajeDesconocido(String tipo) {
        return new WebSocketException("Tipo de mensaje desconocido: " + tipo, TIPO_MENSAJE_DESCONOCIDO);
    }
    
    public static WebSocketException payloadVacio() {
        return new WebSocketException("El payload del mensaje está vacío", PAYLOAD_VACIO);
    }

    // ========== Factory Methods - Juego ==========
    
    public static WebSocketException juegoNoIniciado(String roomCode) {
        return new WebSocketException("El juego en sala " + roomCode + " aún no ha iniciado", JUEGO_NO_INICIADO);
    }
    
    public static WebSocketException juegoYaTerminado(String roomCode) {
        return new WebSocketException("El juego en sala " + roomCode + " ya ha terminado", JUEGO_YA_TERMINADO);
    }
    
    public static WebSocketException minimoJugadores(int minimo, int actual) {
        return new WebSocketException(
            "Se necesitan mínimo " + minimo + " jugadores para iniciar. Actuales: " + actual, 
            MINIMO_JUGADORES
        );
    }
    
    public static WebSocketException votacionInvalida(String detalle) {
        return new WebSocketException("Votación inválida: " + detalle, VOTACION_INVALIDA);
    }
}
