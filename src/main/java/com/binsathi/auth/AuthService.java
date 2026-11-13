package com.binsathi.auth;

import com.binsathi.auth.AuthDtos.TokenResponse;
import com.binsathi.auth.AuthDtos.UserSummary;
import com.binsathi.common.ApiException;
import com.binsathi.user.AppUser;
import com.binsathi.user.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AppUserRepository users;
    private final RefreshTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final long refreshDays;

    public AuthService(AppUserRepository users, RefreshTokenRepository tokens, PasswordEncoder encoder,
                       JwtService jwt, @Value("${app.jwt.refresh-days:30}") long refreshDays) {
        this.users = users;
        this.tokens = tokens;
        this.encoder = encoder;
        this.jwt = jwt;
        this.refreshDays = refreshDays;
    }

    @Transactional
    public TokenResponse login(String email, String password) {
        AppUser user = users.findByEmailIgnoreCase(email.trim()).orElseThrow(AuthService::invalidCredentials);
        if (!"ACTIVE".equals(user.getStatus()) || !encoder.matches(password, user.getPasswordHash())) {
            throw invalidCredentials();
        }
        user.setLastLoginAt(Instant.now());
        return issue(user);
    }

    /** Each refresh token works once; a new pair is issued (rotation). */
    @Transactional
    public TokenResponse refresh(String rawRefreshToken) {
        RefreshToken stored = tokens.findByTokenHash(sha256(rawRefreshToken)).orElseThrow(AuthService::invalidRefresh);
        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw invalidRefresh();
        }
        stored.revoke();
        AppUser user = users.findById(stored.getUserId()).orElseThrow(AuthService::invalidRefresh);
        if (!"ACTIVE".equals(user.getStatus())) {
            throw invalidRefresh();
        }
        return issue(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        tokens.findByTokenHash(sha256(rawRefreshToken)).ifPresent(RefreshToken::revoke);
    }

    private TokenResponse issue(AppUser user) {
        String access = jwt.createAccessToken(user.getId(), user.getOrganisationId(), user.getRole());
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.save(new RefreshToken(user.getId(), sha256(raw), Instant.now().plus(refreshDays, ChronoUnit.DAYS)));
        return new TokenResponse(access, raw, "Bearer", jwt.accessSeconds(),
                new UserSummary(user.getId(), user.getOrganisationId(), user.getName(), user.getEmail(), user.getRole()));
    }

    private static ApiException invalidCredentials() {
        return new ApiException(401, "INVALID_CREDENTIALS", "Invalid email or password");
    }

    private static ApiException invalidRefresh() {
        return new ApiException(401, "INVALID_REFRESH_TOKEN", "Session expired, please log in again");
    }

    private static String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}