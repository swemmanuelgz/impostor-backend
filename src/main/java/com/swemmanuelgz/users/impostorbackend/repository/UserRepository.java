package com.swemmanuelgz.users.impostorbackend.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swemmanuelgz.users.impostorbackend.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmailOrUsername(String email, String username);
    
    Page<User> findByUsernameContainingIgnoreCase(String username, Pageable pageable);
    
    boolean existsByEmail(String email);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmailOrUsername(String email, String username);

    // OAuth2
    Optional<User> findByProviderIdAndAuthProvider(String providerId, String authProvider);
}
