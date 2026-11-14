package com.binsathi.user;

import com.binsathi.common.ApiException;
import com.binsathi.tenant.Organisation;
import com.binsathi.tenant.OrganisationRepository;
import com.binsathi.user.UserDtos.UserRequest;
import com.binsathi.user.UserDtos.UserResponse;
import com.binsathi.user.UserDtos.UserUpdateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Proves that one organisation can never see or change another organisation's users. */
@SpringBootTest
@Transactional
class UserTenantIsolationTest {

    @Autowired UserService userService;
    @Autowired OrganisationRepository organisations;

    private UUID newOrg(String name) {
        return organisations.save(new Organisation(name, "SOCIETY", name + "@test.local")).getId();
    }

    private UserRequest request(String email, Role role) {
        return new UserRequest("Test User", email, null, role, "Password@123");
    }

    @Test
    void organisationOnlySeesItsOwnUsers() {
        UUID orgA = newOrg("A");
        UUID orgB = newOrg("B");
        userService.create(orgA, request("a-res@test.local", Role.RESIDENT));
        userService.create(orgB, request("b-res@test.local", Role.RESIDENT));

        var listA = userService.list(orgA, null, PageRequest.of(0, 50));

        assertEquals(1, listA.totalElements());
        assertEquals("a-res@test.local", listA.content().get(0).email());
    }

    @Test
    void cannotChangeAnotherOrganisationsUser() {
        UUID orgA = newOrg("A");
        UUID orgB = newOrg("B");
        UserResponse bUser = userService.create(orgB, request("b-col@test.local", Role.COLLECTOR));

        assertEquals(404, assertThrows(ApiException.class,
                () -> userService.update(orgA, bUser.id(), new UserUpdateRequest("Hacked", null, "ACTIVE"))).getStatus());
        assertEquals(404, assertThrows(ApiException.class,
                () -> userService.deactivate(orgA, bUser.id())).getStatus());
        assertEquals(404, assertThrows(ApiException.class,
                () -> userService.resetPassword(orgA, bUser.id(), "NewPassword@1")).getStatus());
    }

    @Test
    void cannotCreateAdminUsers() {
        UUID orgA = newOrg("A");

        ApiException ex = assertThrows(ApiException.class,
                () -> userService.create(orgA, request("sneaky@test.local", Role.ORG_ADMIN)));

        assertEquals(422, ex.getStatus());
    }

    @Test
    void duplicateEmailIsRejected() {
        UUID orgA = newOrg("A");
        userService.create(orgA, request("dup@test.local", Role.RESIDENT));

        ApiException ex = assertThrows(ApiException.class,
                () -> userService.create(orgA, request("DUP@test.local", Role.COLLECTOR)));

        assertEquals(409, ex.getStatus());
    }
}