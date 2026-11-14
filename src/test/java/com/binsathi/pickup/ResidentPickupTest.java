package com.binsathi.pickup;

import com.binsathi.area.AreaDtos.AreaRequest;
import com.binsathi.area.AreaService;
import com.binsathi.building.BuildingDtos.BuildingRequest;
import com.binsathi.building.BuildingService;
import com.binsathi.common.ApiException;
import com.binsathi.household.HouseholdDtos.HouseholdRequest;
import com.binsathi.household.HouseholdDtos.HouseholdResponse;
import com.binsathi.household.HouseholdService;
import com.binsathi.schedule.ScheduleDtos.ScheduleRequest;
import com.binsathi.schedule.ScheduleService;
import com.binsathi.tenant.Organisation;
import com.binsathi.tenant.OrganisationRepository;
import com.binsathi.user.Role;
import com.binsathi.user.UserDtos.UserRequest;
import com.binsathi.user.UserDtos.UserResponse;
import com.binsathi.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ResidentPickupTest {

    private static final LocalDate START = LocalDate.of(2026, 11, 16);

    @Autowired ResidentPickupService residentService;
    @Autowired PickupService pickupService;
    @Autowired ScheduleService scheduleService;
    @Autowired HouseholdService householdService;
    @Autowired BuildingService buildingService;
    @Autowired AreaService areaService;
    @Autowired UserService userService;
    @Autowired OrganisationRepository organisations;

    private UUID newOrg(String name) {
        return organisations.save(new Organisation(name, "SOCIETY", name + "@test.local")).getId();
    }

    private UUID newUser(UUID orgId, String email, Role role) {
        UserResponse u = userService.create(orgId, new UserRequest("Test", email, null, role, "Password@123"));
        return u.id();
    }

    private UUID newBuilding(UUID orgId, String name) {
        UUID area = areaService.create(orgId, new AreaRequest(name + "-Area", null)).id();
        return buildingService.create(orgId, new BuildingRequest(area, name + "-Wing", null, null, null)).id();
    }

    private HouseholdResponse newHousehold(UUID orgId, UUID buildingId, String unit, UUID residentId) {
        HouseholdResponse h = householdService.create(orgId,
                new HouseholdRequest(buildingId, unit, BigDecimal.TEN, null));
        return householdService.assignResident(orgId, h.id(), residentId);
    }

    @Test
    void residentSeesOnlyOwnPickups() {
        UUID org = newOrg("A");
        UUID area = areaService.create(org, new AreaRequest("A-Area", null)).id();
        UUID building = buildingService.create(org, new BuildingRequest(area, "A-Wing", null, null, null)).id();
        UUID r1 = newUser(org, "r1@test.local", Role.RESIDENT);
        UUID r2 = newUser(org, "r2@test.local", Role.RESIDENT);
        newHousehold(org, building, "101", r1);
        newHousehold(org, building, "102", r2);
        scheduleService.create(org, new ScheduleRequest(area, "WET", "MON,TUE,WED,THU,FRI,SAT,SUN",
                LocalTime.of(6, 0), LocalTime.of(8, 0), null));
        pickupService.generateForOrganisation(org, START, 7);

        var mine = residentService.list(org, r1, PageRequest.of(0, 50));

        assertEquals(7, mine.totalElements());
        assertTrue(mine.content().stream().allMatch(p -> "101".equals(p.unitNumber())));
    }

    @Test
    void residentWithoutHouseholdSeesNothingAndOtherOrganisationsCannotBeLinked() {
        UUID orgA = newOrg("A");
        UUID orgB = newOrg("B");
        UUID residentB = newUser(orgB, "rb@test.local", Role.RESIDENT);
        UUID buildingA = newBuilding(orgA, "A");
        HouseholdResponse hA = householdService.create(orgA,
                new HouseholdRequest(buildingA, "101", BigDecimal.TEN, null));

        assertEquals(0, residentService.list(orgB, residentB, PageRequest.of(0, 10)).totalElements());
        assertEquals(404, assertThrows(ApiException.class,
                () -> householdService.assignResident(orgA, hA.id(), residentB)).getStatus());
    }

    @Test
    void onlyResidentsCanBeLinkedToAHousehold() {
        UUID org = newOrg("A");
        UUID collector = newUser(org, "c@test.local", Role.COLLECTOR);
        HouseholdResponse h = householdService.create(org,
                new HouseholdRequest(newBuilding(org, "A"), "101", BigDecimal.TEN, null));

        assertEquals(404, assertThrows(ApiException.class,
                () -> householdService.assignResident(org, h.id(), collector)).getStatus());
    }

    @Test
    void onDemandRequestRules() {
        UUID org = newOrg("A");
        UUID r1 = newUser(org, "r1@test.local", Role.RESIDENT);
        UUID noHousehold = newUser(org, "r3@test.local", Role.RESIDENT);
        newHousehold(org, newBuilding(org, "A"), "101", r1);
        LocalDate soon = PickupService.todayIst().plusDays(2);

        var created = residentService.requestOnDemand(org, r1, "BULKY", soon, null);

        assertTrue(created.onDemand());
        assertEquals("SCHEDULED", created.status());
        assertEquals(409, assertThrows(ApiException.class,
                () -> residentService.requestOnDemand(org, r1, "BULKY", soon, null)).getStatus());
        assertEquals(422, assertThrows(ApiException.class,
                () -> residentService.requestOnDemand(org, r1, "WET", soon, null)).getStatus());
        assertEquals(422, assertThrows(ApiException.class,
                () -> residentService.requestOnDemand(org, r1, "BULKY", PickupService.todayIst().minusDays(1), null)).getStatus());
        assertEquals(422, assertThrows(ApiException.class,
                () -> residentService.requestOnDemand(org, r1, "BULKY", PickupService.todayIst().plusDays(40), null)).getStatus());
        assertEquals(422, assertThrows(ApiException.class,
                () -> residentService.requestOnDemand(org, noHousehold, "BULKY", soon, null)).getStatus());
    }

    @Test
    void residentCanCancelOwnButNotOthersPickups() {
        UUID org = newOrg("A");
        UUID building = newBuilding(org, "A");
        UUID r1 = newUser(org, "r1@test.local", Role.RESIDENT);
        UUID r2 = newUser(org, "r2@test.local", Role.RESIDENT);
        newHousehold(org, building, "101", r1);
        newHousehold(org, building, "102", r2);
        UUID pickupId = residentService.requestOnDemand(org, r1, "GARDEN",
                PickupService.todayIst().plusDays(3), null).id();

        assertEquals(404, assertThrows(ApiException.class,
                () -> residentService.cancel(org, r2, pickupId)).getStatus());

        residentService.cancel(org, r1, pickupId);

        assertEquals("CANCELLED", residentService.list(org, r1, PageRequest.of(0, 10)).content().get(0).status());
        assertEquals(422, assertThrows(ApiException.class,
                () -> residentService.cancel(org, r1, pickupId)).getStatus());
    }
}