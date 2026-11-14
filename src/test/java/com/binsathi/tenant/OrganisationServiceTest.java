package com.binsathi.tenant;

import com.binsathi.auth.AuthService;
import com.binsathi.common.ApiException;
import com.binsathi.tenant.OrganisationDtos.OrganisationRequest;
import com.binsathi.tenant.OrganisationDtos.OrganisationUpdateRequest;
import com.binsathi.user.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class OrganisationServiceTest {

    @Autowired OrganisationService service;
    @Autowired AuthService authService;

    private OrganisationRequest request(String name, String adminEmail) {
        return new OrganisationRequest(name, "SOCIETY", "office@test.local", "Admin One", adminEmail, "Welcome@123");
    }

    @Test
    void createMakesOrganisationAndWorkingAdmin() {
        var org = service.create(request("Green Park", "admin1@test.local"));

        var login = authService.login("admin1@test.local", "Welcome@123");

        assertEquals(Role.ORG_ADMIN, login.user().role());
        assertEquals(org.id(), login.user().organisationId());
    }

    @Test
    void duplicateAdminEmailIsRejected() {
        service.create(request("Org One", "same@test.local"));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.create(request("Org Two", "SAME@test.local")));

        assertEquals(409, ex.getStatus());
    }

    @Test
    void inactiveOrganisationBlocksLoginUntilReactivated() {
        var org = service.create(request("Blue Park", "admin2@test.local"));

        service.update(org.id(), new OrganisationUpdateRequest("Blue Park", "SOCIETY", null, "INACTIVE"));
        assertEquals(401, assertThrows(ApiException.class,
                () -> authService.login("admin2@test.local", "Welcome@123")).getStatus());

        service.update(org.id(), new OrganisationUpdateRequest("Blue Park", "SOCIETY", null, "ACTIVE"));
        assertEquals("admin2@test.local", authService.login("admin2@test.local", "Welcome@123").user().email());
    }
}