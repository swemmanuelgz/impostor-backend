package com.swemmanuelgz.users.impostorbackend.security;


import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.swemmanuelgz.users.impostorbackend.entity.User;
import com.swemmanuelgz.users.impostorbackend.exception.UserException;
import com.swemmanuelgz.users.impostorbackend.service.UserServiceImpl;
import com.swemmanuelgz.users.impostorbackend.utils.AnsiColors;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtProvider {
    private final Logger logger = Logger.getLogger(getClass().getName());

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private Integer jwtExpirationInMs;

    @Value("${jwt.refresh.expiration}")
    private Integer jwtRefreshTokenExpirationMs;

    private final UserServiceImpl userService;

    public String generateToken(User user){
        Date now = new Date(); //fecha actual
        Date expireDate = new Date(now.getTime() + jwtExpirationInMs); //caducidad del token

        Map<String,Object> claims = new HashMap<>();
        claims.put("id",user.getId());
        claims.put("email",user.getEmail());
        claims.put("role",user.getRole()); //aquí van los datos del claims

        AnsiColors.infoLog(logger,"Claims token generados para usuario: "+user.getEmail());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getEmail())
                .setIssuedAt(now)
                .setExpiration(expireDate)
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()), SignatureAlgorithm.HS512)
                .compact(); //<- em esta lambda van los datos que llevará el token
    }

    public String generateRefreshToken(User user){
        Date now = new Date(); //fecha actual
        Date expireDate = new Date(now.getTime() + jwtRefreshTokenExpirationMs); //caducidad del token

        Map<String,Object> claims = new HashMap<>();
        claims.put("id",user.getId());
        claims.put("email",user.getEmail());
        claims.put("tokenType","refresh"); //aquí va el identificador de que el token es tipo refresh

        AnsiColors.infoLog(logger,"Refresh token generados para usuario: "+user.getEmail());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getEmail())
                .setIssuedAt(now)
                .setExpiration(expireDate)
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()), SignatureAlgorithm.HS512)
                .compact(); //<- em esta lambda van los datos que llevará el token
    }
    public String generateTokenFromEmail(String email){
        User user = userService.findByEmail(email)
                .orElseThrow(() -> UserException.usuarioNoEncontradoEmail(email));
        return generateToken(user);
    }

    public boolean validateToken(String token){
       // AnsiColors.infoLog(logger,"Token Secret: "+jwtSecret);
        try {
            Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                    .build()
                    .parseClaimsJws(token);
            AnsiColors.infoLog(logger,"Token validado");
            return true;
        } catch (ExpiredJwtException e) {
            AnsiColors.errorLog(logger, "Token expirado: " + e.getMessage());
            return false;
        } catch (Exception e) {
            AnsiColors.errorLog(logger, "Token inválido: " + e.getMessage());
            return false;
        }
    }
    public Claims getClaims(String token){
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String getEmailFromToken(String token){
        return getClaims(token).getSubject();
    }

}
