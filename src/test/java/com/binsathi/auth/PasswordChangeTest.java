package com.binsathi.auth;

import com.binsathi.auth.AuthDtos.TokenResponse;
import com.binsathi.common.ApiException;
import com.binsathi.tenant.OrganisationDtos.OrganisationRequest;
import com.binsathi.tenant.OrganisationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PasswordChangeTest {

    @Autowired AuthService authService;
    @Autowired OrganisationService organisations;

    private TokenResponse newAdminLogin(String email) {
        organisations.create(new OrganisationRequest("Org", "SOCIETY", null, "Admin", email, "OldPass@123"));
        return authService.login(email, "OldPass@123");
    }

    @Test
    void changingPasswordSwitchesLoginAndEndsOldSessions() {
        TokenResponse first = newAdminLogin("pw1@test.local");

        authService.changePassword(first.user().id(), "OldPass@123", "NewPass@456");

        assertEquals(401, assertThrows(ApiException.class,
                () -> authService.login("pw1@test.local", "OldPass@123")).getStatus());
        assertEquals("pw1@test.local", authService.login("pw1@test.local", "NewPass@456").user().email());
        assertEquals(401, assertThrows(ApiException.class,
                () -> authService.refresh(first.refreshToken())).getStatus());
    }

    @Test
    void wrongCurrentPasswordIsRejected() {
        TokenResponse first = newAdminLogin("pw2@test.local");

        ApiException ex = assertThrows(ApiException.class,
                () -> authService.changePassword(first.user().id(), "Wrong@123", "NewPass@456"));

        assertEquals(422, ex.getStatus());
    }
}