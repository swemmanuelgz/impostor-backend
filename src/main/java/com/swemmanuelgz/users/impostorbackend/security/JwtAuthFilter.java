package com.swemmanuelgz.users.impostorbackend.security;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.swemmanuelgz.users.impostorbackend.exception.TokenException;
import com.swemmanuelgz.users.impostorbackend.utils.AnsiColors;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtProvider jwtProvider;

    Logger logger = Logger.getLogger(getClass().getName());

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String token = getTokenFromRequest(request);

        if (token != null && jwtProvider.validateToken(token)){
            Claims claims = jwtProvider.getClaims(token);
            String email = jwtProvider.getEmailFromToken(token);
            String role = claims.get("role",String.class);

            //creamos objeto donde van los claims
            JwtUserDetails userDetails = new JwtUserDetails(email, claims);

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userDetails,null, List.of(new SimpleGrantedAuthority("ROLE_"+role))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

        }
        else {
            //todo: asi funciona pero hay que limpiarlo

            // Token presente pero inválido, detenemos aquí y respondemos directamente
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"codigo\": \"" + TokenException.TOKEN_INVALIDO + "\", \"mensaje\": \"El token no es válido\", \"status\": 401}");
            response.getWriter().flush();
            return; // : no seguir la cadena
        }
        // Si no hay token o no es válido, pasamos al siguiente filtro (ApiKeyAuthFilter)
        filterChain.doFilter(request,response);
    }

    private String getTokenFromRequest(HttpServletRequest request){
        String bearerToken = request.getHeader("Authorization");
        //validadr bearerToken
        if (bearerToken != null && bearerToken.startsWith("Bearer ")){
            AnsiColors.successLog(logger, "Token recibido en request es válido");
            return  bearerToken.substring(7);
        }
       // throw TokenException.tokenInvalido();
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request){
        String path = request.getServletPath();
        return path.contains("/login") || path.contains("/auth") ||
                path.contains("/chat-socket") || path.contains("/refresh");
    }
}
