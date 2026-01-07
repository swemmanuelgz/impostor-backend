package com.swemmanuelgz.users.impostorbackend.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.swemmanuelgz.users.impostorbackend.dto.GoogleUserInfo;
import com.swemmanuelgz.users.impostorbackend.utils.AnsiColors;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Servicio para validar tokens de ID de Google.
 * Usado cuando el frontend (Flutter) hace login con Google y envía el idToken al backend.
 * 
 * NOTA: Si GOOGLE_CLIENT_ID no está configurado, el servicio queda deshabilitado.
 */
@Service
@RequiredArgsConstructor
public class GoogleTokenService {

    private static final Logger logger = Logger.getLogger(GoogleTokenService.class.getName());

    @Value("${google.oauth2.client-id:}")
    private String webClientId;

    @Value("${google.oauth2.android-client-id:}")
    private String androidClientId;

    @Value("${google.oauth2.ios-client-id:}")
    private String iosClientId;

    private GoogleIdTokenVerifier verifier;
    private boolean enabled = false;

    @PostConstruct
    public void init() {
        // Si no hay client ID configurado, el servicio queda deshabilitado
        if (webClientId == null || webClientId.isBlank()) {
            AnsiColors.infoLog(logger, "Google OAuth2 DESHABILITADO - GOOGLE_CLIENT_ID no configurado");
            return;
        }

        // Construir lista de client IDs válidos (web + mobile)
        List<String> validClientIds = buildValidClientIds();
        
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance())
                .setAudience(validClientIds)
                .build();
        
        this.enabled = true;
        AnsiColors.successLog(logger, "Google OAuth2 HABILITADO con " + validClientIds.size() + " client IDs");
    }

    private List<String> buildValidClientIds() {
        List<String> clientIds = new ArrayList<>();
        clientIds.add(webClientId);
        
        if (androidClientId != null && !androidClientId.isBlank()) {
            clientIds.add(androidClientId);
        }
        if (iosClientId != null && !iosClientId.isBlank()) {
            clientIds.add(iosClientId);
        }
        
        return clientIds;
    }

    /**
     * Verifica si el servicio de Google OAuth2 está habilitado.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Valida un token de ID de Google y extrae la información del usuario.
     *
     * @param idTokenString El token de ID recibido del frontend
     * @return Optional con la info del usuario si el token es válido
     */
    public Optional<GoogleUserInfo> verifyToken(String idTokenString) {
        if (!enabled) {
            AnsiColors.errorLog(logger, "Google OAuth2 no está habilitado");
            return Optional.empty();
        }

        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            
            if (idToken == null) {
                AnsiColors.errorLog(logger, "Token de Google inválido o expirado");
                return Optional.empty();
            }

            GoogleIdToken.Payload payload = idToken.getPayload();

            // Verificar que el email esté verificado
            Boolean emailVerified = payload.getEmailVerified();
            if (emailVerified == null || !emailVerified) {
                AnsiColors.errorLog(logger, "Email de Google no verificado para: " + payload.getEmail());
                return Optional.empty();
            }

            GoogleUserInfo userInfo = new GoogleUserInfo(
                    payload.getSubject(),           // ID único de Google
                    payload.getEmail(),
                    (String) payload.get("name"),
                    (String) payload.get("given_name"),
                    (String) payload.get("family_name"),
                    (String) payload.get("picture"),
                    payload.getEmailVerified()
            );

            AnsiColors.successLog(logger, "Token de Google validado para: " + userInfo.email());
            return Optional.of(userInfo);

        } catch (GeneralSecurityException | IOException e) {
            AnsiColors.errorLog(logger, "Error al verificar token de Google: " + e.getMessage());
            return Optional.empty();
        }
    }
}
