package com.swemmanuelgz.users.impostorbackend.service;

import com.swemmanuelgz.users.impostorbackend.dto.GoogleUserInfo;
import com.swemmanuelgz.users.impostorbackend.entity.User;
import com.swemmanuelgz.users.impostorbackend.repository.UserRepository;
import com.swemmanuelgz.users.impostorbackend.utils.AnsiColors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Servicio para manejar autenticación OAuth2.
 * Registra nuevos usuarios o devuelve usuarios existentes.
 */
@Service
@RequiredArgsConstructor
public class OAuth2UserService {

    private static final Logger logger = Logger.getLogger(OAuth2UserService.class.getName());

    private final UserRepository userRepository;

    /**
     * Procesa el login/registro con Google.
     * Si el usuario existe (por email o providerId), lo retorna.
     * Si no existe, crea uno nuevo.
     *
     * @param googleUserInfo Información del usuario de Google
     * @return Usuario existente o nuevo
     */
    @Transactional
    public User processGoogleUser(GoogleUserInfo googleUserInfo) {
        // 1. Buscar por providerId (más seguro)
        Optional<User> existingByProviderId = userRepository.findByProviderIdAndAuthProvider(
                googleUserInfo.googleId(), "GOOGLE");
        
        if (existingByProviderId.isPresent()) {
            AnsiColors.infoLog(logger, "Usuario Google existente encontrado por providerId: " + googleUserInfo.email());
            return updateGoogleUserInfo(existingByProviderId.get(), googleUserInfo);
        }

        // 2. Buscar por email (para vincular cuentas existentes)
        Optional<User> existingByEmail = userRepository.findByEmail(googleUserInfo.email());
        
        if (existingByEmail.isPresent()) {
            User user = existingByEmail.get();
            
            // Si el usuario existía con LOGIN local, vinculamos la cuenta de Google
            if ("LOCAL".equals(user.getAuthProvider())) {
                AnsiColors.infoLog(logger, "Vinculando cuenta Google a usuario local existente: " + googleUserInfo.email());
                user.setAuthProvider("GOOGLE");
                user.setProviderId(googleUserInfo.googleId());
                return updateGoogleUserInfo(user, googleUserInfo);
            }
            
            // Si ya es usuario de otro provider, solo actualizamos info
            return updateGoogleUserInfo(user, googleUserInfo);
        }

        // 3. Crear nuevo usuario
        AnsiColors.infoLog(logger, "Creando nuevo usuario desde Google: " + googleUserInfo.email());
        return createGoogleUser(googleUserInfo);
    }

    /**
     * Crea un nuevo usuario a partir de la info de Google.
     */
    private User createGoogleUser(GoogleUserInfo googleUserInfo) {
        User user = new User();
        user.setEmail(googleUserInfo.email());
        user.setUsername(generateUniqueUsername(googleUserInfo.generateUsername()));
        user.setFullName(googleUserInfo.name());
        user.setPictureUrl(googleUserInfo.pictureUrl());
        user.setAuthProvider("GOOGLE");
        user.setProviderId(googleUserInfo.googleId());
        user.setRole("USER");
        user.setPassword(null); // Usuarios OAuth2 no tienen password
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());

        User savedUser = userRepository.save(user);
        AnsiColors.successLog(logger, "Usuario Google creado con ID: " + savedUser.getId());
        return savedUser;
    }

    /**
     * Actualiza la información del usuario con datos de Google.
     */
    private User updateGoogleUserInfo(User user, GoogleUserInfo googleUserInfo) {
        // Actualizar foto y nombre si cambiaron
        if (googleUserInfo.pictureUrl() != null) {
            user.setPictureUrl(googleUserInfo.pictureUrl());
        }
        if (googleUserInfo.name() != null && (user.getFullName() == null || user.getFullName().isBlank())) {
            user.setFullName(googleUserInfo.name());
        }
        user.setUpdatedAt(Instant.now());
        
        return userRepository.save(user);
    }

    /**
     * Genera un username único añadiendo números si es necesario.
     */
    private String generateUniqueUsername(String baseUsername) {
        String username = baseUsername;
        int counter = 1;
        
        while (userRepository.existsByUsername(username)) {
            username = baseUsername + counter;
            counter++;
            
            // Evitar loop infinito
            if (counter > 1000) {
                username = baseUsername + "_" + System.currentTimeMillis();
                break;
            }
        }
        
        return username;
    }
}
