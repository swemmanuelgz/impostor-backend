package com.swemmanuelgz.users.impostorbackend.controller;

import com.swemmanuelgz.users.impostorbackend.dto.UserDto;
import com.swemmanuelgz.users.impostorbackend.entity.User;
import com.swemmanuelgz.users.impostorbackend.exception.UserException;
import com.swemmanuelgz.users.impostorbackend.service.UserServiceImpl;
import com.swemmanuelgz.users.impostorbackend.utils.AnsiColors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Logger;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    
    private static final Logger logger = Logger.getLogger(UserController.class.getName());
    
    private final UserServiceImpl userService;

    /**
     * Obtener todos los usuarios con paginación
     * GET /api/user?page=0&size=10&sortBy=createdAt&sortDirection=DESC
     */
    @GetMapping
    public ResponseEntity<Page<UserDto>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        
        Sort sort = sortDirection.equalsIgnoreCase("ASC") 
                ? Sort.by(sortBy).ascending() 
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<UserDto> users = userService.findAll(pageable)
                .map(UserDto::fromEntity);
        
        AnsiColors.infoLog(logger, "Listando usuarios - Página: " + page + ", Tamaño: " + size);
        return ResponseEntity.ok(users);
    }

    /**
     * Obtener usuario por ID
     * GET /api/user/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        User user = userService.findById(id)
                .orElseThrow(() -> UserException.usuarioNoEncontradoIDLong(id));
        
        AnsiColors.infoLog(logger, "Usuario encontrado con ID: " + id);
        return ResponseEntity.ok(UserDto.fromEntity(user));
    }

    /**
     * Obtener usuario por email
     * GET /api/user/email/{email}
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<UserDto> getUserByEmail(@PathVariable String email) {
        User user = userService.findByEmail(email)
                .orElseThrow(() -> UserException.usuarioNoEncontradoEmail(email));
        
        AnsiColors.infoLog(logger, "Usuario encontrado con email: " + email);
        return ResponseEntity.ok(UserDto.fromEntity(user));
    }

    /**
     * Obtener usuario por username
     * GET /api/user/username/{username}
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<UserDto> getUserByUsername(@PathVariable String username) {
        User user = userService.findByUsername(username)
                .orElseThrow(() -> UserException.usuarioNoEncontradoUsername(username));
        
        AnsiColors.infoLog(logger, "Usuario encontrado con username: " + username);
        return ResponseEntity.ok(UserDto.fromEntity(user));
    }

    /**
     * Crear nuevo usuario
     * POST /api/user
     */
    @PostMapping
    public ResponseEntity<UserDto> createUser(@RequestBody UserDto userDto) {
        // Validar que no exista el usuario
        if (userService.existsByEmailOrUsername(userDto.getEmail(), userDto.getUsername())) {
            throw UserException.usuarioYaExiste(userDto.getEmail(), userDto.getUsername());
        }
        
        // Encriptar contraseña
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        userDto.setPassword(encoder.encode(userDto.getPassword()));
        
        // Crear usuario
        User user = userDto.toEntity();
        AnsiColors.infoLog(logger, "Creando usuario: " + userDto.getUsername());
        User savedUser = userService.save(user);
        AnsiColors.successLog(logger, "Usuario creado con ID: " + savedUser.getId());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(UserDto.fromEntity(savedUser));
    }

    /**
     * Actualizar usuario existente
     * PUT /api/user/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(@PathVariable Long id, @RequestBody UserDto userDto) {
        User existingUser = userService.findById(id)
                .orElseThrow(() -> UserException.usuarioNoEncontradoIDLong(id));
        
        // Actualizar campos
        if (userDto.getUsername() != null) {
            existingUser.setUsername(userDto.getUsername());
        }
        if (userDto.getEmail() != null) {
            existingUser.setEmail(userDto.getEmail());
        }
        
        // Si se envía nueva contraseña, encriptarla
        if (userDto.getPassword() != null && !userDto.getPassword().isEmpty()) {
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            existingUser.setPassword(encoder.encode(userDto.getPassword()));
        }
        
        AnsiColors.infoLog(logger, "Actualizando usuario con ID: " + id);
        User updatedUser = userService.update(existingUser);
        AnsiColors.successLog(logger, "Usuario actualizado con ID: " + id);
        
        return ResponseEntity.ok(UserDto.fromEntity(updatedUser));
    }

    /**
     * Actualización parcial de usuario
     * PATCH /api/user/{id}
     */
    @PatchMapping("/{id}")
    public ResponseEntity<UserDto> partialUpdateUser(
            @PathVariable Long id,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email) {
        
        User existingUser = userService.findById(id)
                .orElseThrow(() -> UserException.usuarioNoEncontradoIDLong(id));
        
        if (username != null) {
            existingUser.setUsername(username);
        }
        if (email != null) {
            existingUser.setEmail(email);
        }
        
        User updatedUser = userService.update(existingUser);
        AnsiColors.successLog(logger, "Usuario parcialmente actualizado con ID: " + id);
        
        return ResponseEntity.ok(UserDto.fromEntity(updatedUser));
    }

    /**
     * Eliminar usuario por ID
     * DELETE /api/user/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteById(id);
        AnsiColors.successLog(logger, "Usuario eliminado con ID: " + id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Verificar si existe un usuario por email o username
     * GET /api/user/exists?identifier=usuario@email.com
     */
    @GetMapping("/exists")
    public ResponseEntity<Boolean> userExists(@RequestParam String identifier) {
        boolean exists = userService.existsByEmailOrUsername(identifier, identifier);
        return ResponseEntity.ok(exists);
    }
}
