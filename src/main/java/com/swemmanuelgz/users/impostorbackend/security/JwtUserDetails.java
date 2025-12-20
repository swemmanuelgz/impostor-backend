package com.swemmanuelgz.users.impostorbackend.security;

import io.jsonwebtoken.Claims;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtUserDetails {
    private final String email;
    private final Claims claims;

    public Integer getId() {
        return Integer.valueOf(claims.get("id").toString());
    }
}
