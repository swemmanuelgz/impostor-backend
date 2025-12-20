package com.swemmanuelgz.users.impostorbackend.config;


import com.swemmanuelgz.users.impostorbackend.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {



    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public BCryptPasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    //todo: cambiar y poner los endpoints en un array de strings
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
            http
                    .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Añadir esta línea
                    .csrf(csrf -> csrf.disable())
                    .sessionManagement( session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) //todo: ajustar numero de sesiones por user
                    .authorizeHttpRequests(authorize -> authorize
                            .requestMatchers(HttpMethod.OPTIONS,"/**").permitAll()
                            .requestMatchers("/api/auth/login","/api/auth/refresh","/api/auth/signup").permitAll() //todo: revisar permisos
                            .requestMatchers("/chat-socket/**","/topic/**","/app/**","/chat-socket").permitAll() //TODO:REVISAR
                            .anyRequest().authenticated()
                    ) //aqui se verifica si el usuario está autenticado por JWT
                    .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
                    //y aquí si el JWT no es válido , verificamos el APIKEY

            return http.build();
    }

    // Añade esta funcion para configurar CORS
    //TODO: BORRAR ANTES DE DEPLOY , ES SOLO PARA PRUEBAS
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));  // Permitir todos los orígenes, igual que tu @CrossOrigin
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);  // false porque usamos "*" en los orígenes
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
