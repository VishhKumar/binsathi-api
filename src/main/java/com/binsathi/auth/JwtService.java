package com.binsathi.auth;

import com.binsathi.user.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey key;
    private final long accessMinutes;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.access-minutes:15}") long accessMinutes) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 characters long");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.accessMinutes = accessMinutes;
    }

    public String createAccessToken(UUID userId, UUID organisationId, Role role) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(userId.toString())
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessMinutes * 60)));
        if (organisationId != null) {
            builder.claim("org", organisationId.toString());
        }
        return builder.signWith(key).compact();
    }

    /** Throws io.jsonwebtoken.JwtException if the token is invalid, tampered with, or expired. */
    public AuthUser parse(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        String org = claims.get("org", String.class);
        return new AuthUser(
                UUID.fromString(claims.getSubject()),
                org == null ? null : UUID.fromString(org),
                Role.valueOf(claims.get("role", String.class)));
    }

    public long accessSeconds() {
        return accessMinutes * 60;
    }
}