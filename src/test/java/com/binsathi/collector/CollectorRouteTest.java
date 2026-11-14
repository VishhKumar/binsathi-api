package com.binsathi.collector;

import com.binsathi.area.AreaDtos.AreaRequest;
import com.binsathi.area.AreaService;
import com.binsathi.building.BuildingDtos.BuildingRequest;
import com.binsathi.building.BuildingService;
import com.binsathi.common.ApiException;
import com.binsathi.household.HouseholdDtos.HouseholdRequest;
import com.binsathi.household.HouseholdService;
import com.binsathi.pickup.PickupService;
import com.binsathi.schedule.ScheduleDtos.ScheduleRequest;
import com.binsathi.schedule.ScheduleService;
import com.binsathi.tenant.Organisation;
import com.binsathi.tenant.OrganisationRepository;
import com.binsathi.user.Role;
import com.binsathi.user.UserDtos.UserRequest;
import com.binsathi.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class CollectorRouteTest {

    private static final String ALL_DAYS = "MON,TUE,WED,THU,FRI,SAT,SUN";

    @Autowired CollectorRouteService routeService;
    @Autowired CollectorAssignmentService assignmentService;
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
        return userService.create(orgId, new UserRequest("Test", email, null, role, "Password@123")).id();
    }

    /** Creates an area with one building, the given flats, and an every-day WET schedule. */
    private UUID areaWithFlats(UUID orgId, String name, String... units) {
        UUID area = areaService.create(orgId, new AreaRequest(name + "-Area", null)).id();
        UUID building = buildingService.create(orgId, new BuildingRequest(area, name + "-Wing", "Sector 5", null, null)).id();
        for (String unit : units) {
            householdService.create(orgId, new HouseholdRequest(building, unit, BigDecimal.TEN, null));
        }
        scheduleService.create(orgId, new ScheduleRequest(area, "WET", ALL_DAYS,
                LocalTime.of(6, 0), LocalTime.of(8, 0), null));
        return area;
    }

    @Test
    void collectorSeesOnlyTodaysPickupsInAssignedAreas() {
        UUID org = newOrg("A");
        UUID area1 = areaWithFlats(org, "One", "101");
        areaWithFlats(org, "Two", "201");
        UUID collector = newUser(org, "c@test.local", Role.COLLECTOR);
        assignmentService.setAreas(org, collector, List.of(area1));
        LocalDate today = PickupService.todayIst();
        pickupService.generateForOrganisation(org, today, 1);

        var route = routeService.route(org, collector, today);

        assertEquals(1, route.size());
        assertEquals("101", route.get(0).unitNumber());
        assertEquals("One-Wing", route.get(0).buildingName());
        assertEquals(0, routeService.route(org, collector, today.plusDays(1)).size());
    }

    @Test
    void unassignedCollectorAndOtherOrganisationsSeeNothing() {
        UUID orgA = newOrg("A");
        UUID orgB = newOrg("B");
        areaWithFlats(orgA, "One", "101");
        UUID collectorA = newUser(orgA, "ca@test.local", Role.COLLECTOR);
        UUID collectorB = newUser(orgB, "cb@test.local", Role.COLLECTOR);
        UUID areaB = areaWithFlats(orgB, "Two", "201");
        assignmentService.setAreas(orgB, collectorB, List.of(areaB));
        LocalDate today = PickupService.todayIst();
        pickupService.generateForOrganisation(orgA, today, 1);
        pickupService.generateForOrganisation(orgB, today, 1);

        assertEquals(0, routeService.route(orgA, collectorA, today).size());
        assertEquals("201", routeService.route(orgB, collectorB, today).get(0).unitNumber());
    }

    @Test
    void collectorCanChangeOnlyAssignedPickups() {
        UUID org = newOrg("A");
        UUID area1 = areaWithFlats(org, "One", "101", "102");
        UUID area2 = areaWithFlats(org, "Two", "201");
        UUID c1 = newUser(org, "c1@test.local", Role.COLLECTOR);
        UUID c2 = newUser(org, "c2@test.local", Role.COLLECTOR);
        assignmentService.setAreas(org, c1, List.of(area1));
        assignmentService.setAreas(org, c2, List.of(area2));
        LocalDate today = PickupService.todayIst();
        pickupService.generateForOrganisation(org, today, 1);
        var route = routeService.route(org, c1, today);
        UUID first = route.get(0).id();
        UUID second = route.get(1).id();

        assertEquals(404, assertThrows(ApiException.class, () -> routeService.complete(org, c2, first)).getStatus());
        assertEquals(404, assertThrows(ApiException.class, () -> routeService.skip(org, c2, first, "x")).getStatus());

        routeService.complete(org, c1, first);
        routeService.skip(org, c1, second, "Gate locked");

        var after = routeService.route(org, c1, today);
        assertEquals("COMPLETED", after.get(0).status());
        assertEquals("SKIPPED", after.get(1).status());
        assertEquals("Gate locked", after.get(1).skipReason());
        assertEquals(422, assertThrows(ApiException.class, () -> routeService.complete(org, c1, first)).getStatus());
    }

    @Test
    void futurePickupsCannotBeCompleted() {
        UUID org = newOrg("A");
        UUID area = areaWithFlats(org, "One", "101");
        UUID collector = newUser(org, "c@test.local", Role.COLLECTOR);
        assignmentService.setAreas(org, collector, List.of(area));
        LocalDate tomorrow = PickupService.todayIst().plusDays(1);
        pickupService.generateForOrganisation(org, tomorrow, 1);
        UUID id = pickupService.list(org, tomorrow, tomorrow, null, PageRequest.of(0, 10)).content().get(0).id();

        assertEquals(422, assertThrows(ApiException.class, () -> routeService.complete(org, collector, id)).getStatus());
    }

    @Test
    void assignmentRules() {
        UUID orgA = newOrg("A");
        UUID orgB = newOrg("B");
        UUID area1 = areaWithFlats(orgA, "One", "101");
        UUID area2 = areaWithFlats(orgA, "Two", "201");
        UUID areaB = areaWithFlats(orgB, "Other", "301");
        UUID collector = newUser(orgA, "c@test.local", Role.COLLECTOR);
        UUID resident = newUser(orgA, "r@test.local", Role.RESIDENT);

        assertEquals(404, assertThrows(ApiException.class,
                () -> assignmentService.setAreas(orgA, collector, List.of(areaB))).getStatus());
        assertEquals(404, assertThrows(ApiException.class,
                () -> assignmentService.setAreas(orgA, resident, List.of(area1))).getStatus());
        assertEquals(404, assertThrows(ApiException.class,
                () -> assignmentService.areaIds(orgB, collector)).getStatus());

        assignmentService.setAreas(orgA, collector, List.of(area1));
        assignmentService.setAreas(orgA, collector, List.of(area2));
        assertEquals(List.of(area2), assignmentService.areaIds(orgA, collector));
        assignmentService.setAreas(orgA, collector, List.of());
        assertTrue(assignmentService.areaIds(orgA, collector).isEmpty());
    }
}