package com.binsathi.auth;

import com.binsathi.auth.AuthDtos.TokenResponse;
import com.binsathi.auth.AuthDtos.UserSummary;
import com.binsathi.common.ApiException;
import com.binsathi.tenant.OrganisationRepository;
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
import java.util.UUID;

@Service
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AppUserRepository users;
    private final RefreshTokenRepository tokens;
    private final OrganisationRepository organisations;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final LoginAttemptLimiter limiter;
    private final long refreshDays;

    public AuthService(AppUserRepository users, RefreshTokenRepository tokens, OrganisationRepository organisations,
                       PasswordEncoder encoder, JwtService jwt, LoginAttemptLimiter limiter,
                       @Value("${app.jwt.refresh-days:30}") long refreshDays) {
        this.users = users;
        this.tokens = tokens;
        this.organisations = organisations;
        this.encoder = encoder;
        this.jwt = jwt;
        this.limiter = limiter;
        this.refreshDays = refreshDays;
    }

    @Transactional
    public TokenResponse login(String email, String password) {
        String key = email.trim().toLowerCase();
        limiter.checkAllowed(key);
        AppUser user = users.findByEmailIgnoreCase(key).orElse(null);
        if (user == null || !canLogin(user) || !encoder.matches(password, user.getPasswordHash())) {
            limiter.recordFailure(key);
            throw invalidCredentials();
        }
        limiter.reset(key);
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
        if (!canLogin(user)) {
            throw invalidRefresh();
        }
        return issue(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        tokens.findByTokenHash(sha256(rawRefreshToken)).ifPresent(RefreshToken::revoke);
    }

    /** Changes the password and ends every session of the user, so they must log in again. */
    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        AppUser user = users.findById(userId).orElseThrow(AuthService::invalidCredentials);
        if (!encoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ApiException(422, "WRONG_PASSWORD", "The current password is wrong");
        }
        user.setPasswordHash(encoder.encode(newPassword));
        tokens.findByUserIdAndRevokedFalse(userId).forEach(RefreshToken::revoke);
    }

    /** The user must be active, and so must their organisation (the platform admin has none). */
    private boolean canLogin(AppUser user) {
        if (!"ACTIVE".equals(user.getStatus())) {
            return false;
        }
        UUID orgId = user.getOrganisationId();
        return orgId == null || organisations.findById(orgId).map(o -> "ACTIVE".equals(o.getStatus())).orElse(false);
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