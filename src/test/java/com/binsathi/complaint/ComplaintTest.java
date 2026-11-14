package com.binsathi.complaint;

import com.binsathi.area.AreaDtos.AreaRequest;
import com.binsathi.area.AreaService;
import com.binsathi.building.BuildingDtos.BuildingRequest;
import com.binsathi.building.BuildingService;
import com.binsathi.common.ApiException;
import com.binsathi.complaint.ComplaintDtos.ComplaintRequest;
import com.binsathi.complaint.ComplaintDtos.ComplaintResponse;
import com.binsathi.household.HouseholdDtos.HouseholdRequest;
import com.binsathi.household.HouseholdService;
import com.binsathi.tenant.Organisation;
import com.binsathi.tenant.OrganisationRepository;
import com.binsathi.user.AppUser;
import com.binsathi.user.AppUserRepository;
import com.binsathi.user.Role;
import com.binsathi.user.UserDtos.UserRequest;
import com.binsathi.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ComplaintTest {

    @Autowired ComplaintService service;
    @Autowired HouseholdService householdService;
    @Autowired BuildingService buildingService;
    @Autowired AreaService areaService;
    @Autowired UserService userService;
    @Autowired AppUserRepository users;
    @Autowired OrganisationRepository organisations;

    private UUID newOrg(String name) {
        return organisations.save(new Organisation(name, "SOCIETY", name + "@test.local")).getId();
    }

    private UUID newResident(UUID org, String email) {
        return userService.create(org, new UserRequest("Test", email, null, Role.RESIDENT, "Password@123")).id();
    }

    private UUID newAdmin(UUID org, String email) {
        return users.save(new AppUser(org, "Admin", email, null, "x", Role.ORG_ADMIN)).getId();
    }

    private void linkFlat(UUID org, UUID resident, String unit) {
        UUID area = areaService.create(org, new AreaRequest(unit + "-Area", null)).id();
        UUID building = buildingService.create(org, new BuildingRequest(area, unit + "-Wing", null, null, null)).id();
        UUID household = householdService.create(org, new HouseholdRequest(building, unit, BigDecimal.TEN, null)).id();
        householdService.assignResident(org, household, resident);
    }

    private ComplaintResponse file(UUID org, UUID resident) {
        return service.create(org, resident, new ComplaintRequest("MISSED_PICKUP", "Not collected today", null));
    }

    @Test
    void residentSeesOnlyOwnComplaints() {
        UUID org = newOrg("A");
        UUID r1 = newResident(org, "r1@test.local");
        UUID r2 = newResident(org, "r2@test.local");
        linkFlat(org, r1, "101");
        linkFlat(org, r2, "102");
        file(org, r1);
        UUID other = file(org, r2).id();

        var mine = service.listMine(org, r1, PageRequest.of(0, 10));

        assertEquals(1, mine.totalElements());
        assertEquals("101", mine.content().get(0).unitNumber());
        assertEquals(404, assertThrows(ApiException.class, () -> service.getMine(org, r1, other)).getStatus());
        assertEquals(404, assertThrows(ApiException.class, () -> service.commentMine(org, r1, other, "hi")).getStatus());
        assertEquals(404, assertThrows(ApiException.class, () -> service.closeMine(org, r1, other)).getStatus());
    }

    @Test
    void organisationCannotSeeOrChangeAnotherOrganisationsComplaints() {
        UUID orgA = newOrg("A");
        UUID orgB = newOrg("B");
        UUID resident = newResident(orgA, "r@test.local");
        linkFlat(orgA, resident, "101");
        UUID id = file(orgA, resident).id();
        UUID adminB = newAdmin(orgB, "admin-b@test.local");

        assertEquals(0, service.list(orgB, null, PageRequest.of(0, 10)).totalElements());
        assertEquals(404, assertThrows(ApiException.class, () -> service.get(orgB, id)).getStatus());
        assertEquals(404, assertThrows(ApiException.class,
                () -> service.changeStatus(orgB, id, "IN_PROGRESS", null)).getStatus());
        assertEquals(404, assertThrows(ApiException.class,
                () -> service.commentAdmin(orgB, adminB, id, "hi")).getStatus());
        assertEquals(1, service.list(orgA, null, PageRequest.of(0, 10)).totalElements());
    }

    @Test
    void complaintLifecycle() {
        UUID org = newOrg("A");
        UUID r = newResident(org, "r@test.local");
        linkFlat(org, r, "101");
        UUID id = file(org, r).id();

        assertEquals("OPEN", service.getMine(org, r, id).complaint().status());
        assertEquals(422, assertThrows(ApiException.class,
                () -> service.changeStatus(org, id, "RESOLVED", " ")).getStatus());

        service.changeStatus(org, id, "IN_PROGRESS", null);
        assertEquals(422, assertThrows(ApiException.class,
                () -> service.changeStatus(org, id, "IN_PROGRESS", null)).getStatus());

        service.changeStatus(org, id, "RESOLVED", "Collected the next morning");
        assertEquals("RESOLVED", service.getMine(org, r, id).complaint().status());

        service.reopenMine(org, r, id);
        assertEquals("REOPENED", service.getMine(org, r, id).complaint().status());

        service.changeStatus(org, id, "RESOLVED", "Collected again");
        service.closeMine(org, r, id);
        assertEquals("CLOSED", service.getMine(org, r, id).complaint().status());

        assertEquals(422, assertThrows(ApiException.class, () -> service.commentMine(org, r, id, "late")).getStatus());
        assertEquals(422, assertThrows(ApiException.class, () -> service.closeMine(org, r, id)).getStatus());
    }

    @Test
    void residentAndAdminSeeTheSameConversation() {
        UUID org = newOrg("A");
        UUID r = newResident(org, "r@test.local");
        UUID admin = newAdmin(org, "admin@test.local");
        linkFlat(org, r, "101");
        UUID id = file(org, r).id();

        service.commentMine(org, r, id, "Please check");
        service.commentAdmin(org, admin, id, "We are on it");

        var comments = service.getMine(org, r, id).comments();
        assertEquals(2, comments.size());
        assertEquals("RESIDENT", comments.get(0).authorRole());
        assertEquals("ORG_ADMIN", comments.get(1).authorRole());
        assertEquals(2, service.get(org, id).comments().size());
    }

    @Test
    void residentWithoutHouseholdCannotComplain() {
        UUID org = newOrg("A");
        UUID r = newResident(org, "r@test.local");

        assertEquals(422, assertThrows(ApiException.class, () -> file(org, r)).getStatus());
    }
}