package com.swemmanuelgz.users.impostorbackend.exception;

import lombok.Getter;

@Getter
public class UserException extends RuntimeException {
    
    public static final String USUARIO_NO_ENCONTRADO = "USUARIO_NO_ENCONTRADO";
    public static final String USUARIO_YA_EXISTE = "USUARIO_YA_EXISTE";
    public static final String PASSWORD_INCORRECTO = "PASSWORD_INCORRECTO";
    public static final String JSON_INVALIDO = "JSON_INVALIDO";
    public static final String EMAIL_INVALIDO = "EMAIL_INVALIDO";
    public static final String USERNAME_INVALIDO = "USERNAME_INVALIDO";
    public static final String USUARIO_OAUTH = "USUARIO_OAUTH";

    private final String codigo;

    public UserException(String mensaje, String codigo) {
        super(mensaje);
        this.codigo = codigo;
    }

    public static UserException usuarioNoEncontradoIDLong(Long id) {
        return new UserException("Usuario no encontrado con ID: " + id, USUARIO_NO_ENCONTRADO);
    }

    public static UserException usuarioNoEncontradoEmail(String email) {
        return new UserException("Usuario no encontrado con email: " + email, USUARIO_NO_ENCONTRADO);
    }

    public static UserException usuarioNoEncontradoUsername(String username) {
        return new UserException("Usuario no encontrado con username: " + username, USUARIO_NO_ENCONTRADO);
    }

    public static UserException usuarioYaExiste(String email, String username) {
        return new UserException("Ya existe un usuario con email: " + email + " o username: " + username, USUARIO_YA_EXISTE);
    }

    public static UserException usuarioPasswordIncorrecto(String username) {
        return new UserException("Contraseña incorrecta para el usuario: " + username, PASSWORD_INCORRECTO);
    }

    public static UserException jsonInvalido() {
        return new UserException("El JSON proporcionado no es válido", JSON_INVALIDO);
    }

    public static UserException emailInvalido(String email) {
        return new UserException("El email no es válido: " + email, EMAIL_INVALIDO);
    }

    public static UserException usernameInvalido(String username) {
        return new UserException("El username no es válido: " + username, USERNAME_INVALIDO);
    }

    public static UserException usuarioOAuth(String provider) {
        return new UserException("Esta cuenta fue creada con " + provider + ". Por favor, usa el botón de " + provider + " para iniciar sesión.", USUARIO_OAUTH);
    }
}
