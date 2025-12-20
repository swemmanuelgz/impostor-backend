package com.swemmanuelgz.users.impostorbackend.exception;

import lombok.Getter;

@Getter
public class TokenException extends RuntimeException {
    
    public static final String TOKEN_EXPIRADO = "TOKEN_EXPIRADO";
    public static final String TOKEN_INVALIDO = "TOKEN_INVALIDO";
    public static final String TOKEN_NO_PROPORCIONADO = "TOKEN_NO_PROPORCIONADO";
    public static final String REFRESH_TOKEN_EXPIRADO = "REFRESH_TOKEN_EXPIRADO";
    public static final String REFRESH_TOKEN_INVALIDO = "REFRESH_TOKEN_INVALIDO";
    public static final String FIRMA_TOKEN_INVALIDA = "FIRMA_TOKEN_INVALIDA";
    public static final String TOKEN_MALFORMADO = "TOKEN_MALFORMADO";
    public static final String NO_AUTORIZADO = "NO_AUTORIZADO";

    private final String codigo;

    public TokenException(String mensaje, String codigo) {
        super(mensaje);
        this.codigo = codigo;
    }

    public static TokenException tokenExpirado() {
        return new TokenException("El token ha expirado", TOKEN_EXPIRADO);
    }

    public static TokenException tokenInvalido() {
        return new TokenException("El token no es válido", TOKEN_INVALIDO);
    }

    public static TokenException tokenNoProporsionado() {
        return new TokenException("No se ha proporcionado un token", TOKEN_NO_PROPORCIONADO);
    }

    public static TokenException refreshTokenExpirado() {
        return new TokenException("El refresh token ha expirado", REFRESH_TOKEN_EXPIRADO);
    }

    public static TokenException refreshTokenInvalido() {
        return new TokenException("El refresh token no es válido", REFRESH_TOKEN_INVALIDO);
    }

    public static TokenException firmaTokenInvalida() {
        return new TokenException("La firma del token no es válida", FIRMA_TOKEN_INVALIDA);
    }

    public static TokenException noAutorizado() {
        return new TokenException("No tiene autorización para acceder a este recurso", NO_AUTORIZADO);
    }
}
