package com.swemmanuelgz.users.impostorbackend.service;

import com.swemmanuelgz.users.impostorbackend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface UserService {
    // Métodos CRUD básicos
    User save(User user);
    User update(User user);
    void deleteById(Long id);
    Optional<User> findById(Long id);
    Page<User> findAll(Pageable pageable);
    
    // Métodos de búsqueda específicos
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmailOrUsername(String identifier);
    Page<User> findByUsername(String username, Pageable pageable);
    
    // Métodos de validación
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmailOrUsername(String email, String username);
}
