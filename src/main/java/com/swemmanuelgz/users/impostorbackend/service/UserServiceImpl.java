package com.swemmanuelgz.users.impostorbackend.service;

import com.swemmanuelgz.users.impostorbackend.entity.User;
import com.swemmanuelgz.users.impostorbackend.exception.UserException;
import com.swemmanuelgz.users.impostorbackend.repository.UserRepository;
import com.swemmanuelgz.users.impostorbackend.utils.AnsiColors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    
    private static final Logger logger = Logger.getLogger(UserServiceImpl.class.getName());
    private final UserRepository repository;

    @Override
    @Transactional
    public User save(User user) {
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        User savedUser = repository.save(user);
        AnsiColors.successLog(logger, "Usuario guardado con ID: " + savedUser.getId());
        return savedUser;
    }

    @Override
    @Transactional
    public User update(User user) {
        user.setUpdatedAt(Instant.now());
        User updatedUser = repository.save(user);
        AnsiColors.successLog(logger, "Usuario actualizado con ID: " + updatedUser.getId());
        return updatedUser;
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        repository.findById(id)
                .orElseThrow(() -> UserException.usuarioNoEncontradoIDLong(id));
        repository.deleteById(id);
        AnsiColors.successLog(logger, "Usuario eliminado con ID: " + id);
    }

    @Override
    public Optional<User> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public Page<User> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return repository.findByEmail(email);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return repository.findByUsername(username);
    }

    @Override
    public Optional<User> findByEmailOrUsername(String identifier) {
        return repository.findByEmailOrUsername(identifier, identifier);
    }

    @Override
    public Page<User> findByUsername(String username, Pageable pageable) {
        return repository.findByUsernameContainingIgnoreCase(username, pageable);
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    @Override
    public boolean existsByUsername(String username) {
        return repository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmailOrUsername(String email, String username) {
        return repository.existsByEmailOrUsername(email, username);
    }
}
