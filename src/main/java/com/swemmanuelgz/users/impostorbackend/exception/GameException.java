package com.swemmanuelgz.users.impostorbackend.exception;

import lombok.Getter;

@Getter
public class GameException extends RuntimeException {
    
    public static final String GAME_NO_ENCONTRADO = "GAME_NO_ENCONTRADO";
    public static final String GAME_YA_EXISTE = "GAME_YA_EXISTE";
    public static final String ROOM_CODE_INVALIDO = "ROOM_CODE_INVALIDO";
    public static final String GAME_LLENO = "GAME_LLENO";
    public static final String GAME_YA_INICIADO = "GAME_YA_INICIADO";
    public static final String JUGADOR_YA_EN_PARTIDA = "JUGADOR_YA_EN_PARTIDA";
    public static final String JUGADOR_NO_EN_PARTIDA = "JUGADOR_NO_EN_PARTIDA";
    public static final String NO_ES_CREADOR = "NO_ES_CREADOR";

    private final String codigo;

    public GameException(String mensaje, String codigo) {
        super(mensaje);
        this.codigo = codigo;
    }

    public static GameException gameNoEncontrado(Long id) {
        return new GameException("Partida no encontrada con ID: " + id, GAME_NO_ENCONTRADO);
    }

    public static GameException gameNoEncontradoPorCodigo(String roomCode) {
        return new GameException("Partida no encontrada con código: " + roomCode, GAME_NO_ENCONTRADO);
    }

    public static GameException roomCodeInvalido(String roomCode) {
        return new GameException("Código de sala inválido: " + roomCode, ROOM_CODE_INVALIDO);
    }

    public static GameException gameLleno(String roomCode) {
        return new GameException("La partida con código " + roomCode + " está llena", GAME_LLENO);
    }

    public static GameException gameYaIniciado(String roomCode) {
        return new GameException("La partida con código " + roomCode + " ya ha iniciado", GAME_YA_INICIADO);
    }

    public static GameException jugadorYaEnPartida(Long userId) {
        return new GameException("El jugador con ID " + userId + " ya está en la partida", JUGADOR_YA_EN_PARTIDA);
    }

    public static GameException jugadorNoEnPartida(Long userId) {
        return new GameException("El jugador con ID " + userId + " no está en la partida", JUGADOR_NO_EN_PARTIDA);
    }

    public static GameException noEsCreador(Long userId) {
        return new GameException("El usuario con ID " + userId + " no es el creador de la partida", NO_ES_CREADOR);
    }
}
