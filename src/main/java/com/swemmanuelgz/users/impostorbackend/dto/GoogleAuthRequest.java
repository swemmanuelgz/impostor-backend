package com.swemmanuelgz.users.impostorbackend.dto;

import lombok.Data;

/**
 * Request para login/registro con Google.
 * El frontend envía el idToken obtenido de Google Sign-In.
 */
@Data
public class GoogleAuthRequest {
    /**
     * Token de ID de Google obtenido del frontend (Flutter).
     * Este token contiene la información del usuario y está firmado por Google.
     */
    private String idToken;
}
