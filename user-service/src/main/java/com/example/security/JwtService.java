package com.example.security;

import com.example.entity.User;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.Instant;
import java.util.Set;

@ApplicationScoped
public class JwtService {

    @ConfigProperty(name = "smallrye.jwt.new-token.lifespan", defaultValue = "3600")
    long accessTokenLifespan;

    @ConfigProperty(name = "app.jwt.refresh-lifespan", defaultValue = "604800")
    long refreshTokenLifespan;

    @ConfigProperty(name = "smallrye.jwt.new-token.issuer", defaultValue = "bakery-shop")
    String issuer;

    // ================= ACCESS TOKEN =================

    public String generateAccessToken(User user) {
        return Jwt.issuer(issuer)
                .subject(user.getId())
                .groups(Set.of(user.getRole().name()))
                .claim("username", user.getUsername())
                .claim("email", user.getEmail())
                .claim("type", "access")
                .expiresAt(Instant.now().plusSeconds(accessTokenLifespan))
                .sign();
    }

    // ================= REFRESH TOKEN =================

    public String generateRefreshToken(User user) {
        return Jwt.issuer(issuer)
                .subject(user.getId())
                .groups(Set.of(user.getRole().name())) // QUAN TRỌNG
                .claim("type", "refresh")
                .expiresAt(Instant.now().plusSeconds(refreshTokenLifespan))
                .sign();
    }

    // ================= VALIDATION =================

    public boolean isRefreshToken(JsonWebToken jwt) {
        if (jwt == null) return false;

        String type = jwt.getClaim("type");
        return "refresh".equals(type);
    }

    public long getAccessTokenLifespan() {
        return accessTokenLifespan;
    }
}