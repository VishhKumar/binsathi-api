package com.binsathi.auth;

import com.binsathi.user.Role;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "0123456789012345678901234567890123456789";

    @Test
    void roundTripKeepsUserOrgAndRole() {
        JwtService jwt = new JwtService(SECRET, 15);
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        AuthUser parsed = jwt.parse(jwt.createAccessToken(userId, orgId, Role.ORG_ADMIN));

        assertEquals(userId, parsed.userId());
        assertEquals(orgId, parsed.organisationId());
        assertEquals(Role.ORG_ADMIN, parsed.role());
    }

    @Test
    void superAdminTokenHasNoOrganisation() {
        JwtService jwt = new JwtService(SECRET, 15);

        AuthUser parsed = jwt.parse(jwt.createAccessToken(UUID.randomUUID(), null, Role.SUPER_ADMIN));

        assertNull(parsed.organisationId());
        assertEquals(Role.SUPER_ADMIN, parsed.role());
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService jwt = new JwtService(SECRET, 15);
        String token = jwt.createAccessToken(UUID.randomUUID(), UUID.randomUUID(), Role.RESIDENT);
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("A") ? "BB" : "AA");

        assertThrows(JwtException.class, () -> jwt.parse(tampered));
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtService other = new JwtService("ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ", 15);
        JwtService jwt = new JwtService(SECRET, 15);
        String token = other.createAccessToken(UUID.randomUUID(), UUID.randomUUID(), Role.RESIDENT);

        assertThrows(JwtException.class, () -> jwt.parse(token));
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService jwt = new JwtService(SECRET, -1);
        String token = jwt.createAccessToken(UUID.randomUUID(), UUID.randomUUID(), Role.RESIDENT);

        assertThrows(JwtException.class, () -> jwt.parse(token));
    }

    @Test
    void shortSecretIsRejected() {
        assertThrows(IllegalStateException.class, () -> new JwtService("too-short", 15));
    }
}