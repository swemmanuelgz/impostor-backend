package com.swemmanuelgz.users.impostorbackend.dto;

/**
 * Información del usuario extraída del token de Google.
 */
public record GoogleUserInfo(
        String googleId,        // ID único en Google (sub claim)
        String email,
        String name,            // Nombre completo
        String givenName,       // Nombre
        String familyName,      // Apellido
        String pictureUrl,      // URL de la foto de perfil
        Boolean emailVerified
) {
    /**
     * Genera un username único basado en el email
     */
    public String generateUsername() {
        if (email == null) return "user_" + googleId.substring(0, 8);
        String base = email.split("@")[0];
        // Limitar a 45 caracteres para dejar espacio para sufijos
        if (base.length() > 45) {
            base = base.substring(0, 45);
        }
        return base;
    }
}
