package com.binsathi.pickup;

import com.binsathi.area.AreaDtos.AreaRequest;
import com.binsathi.area.AreaService;
import com.binsathi.building.BuildingDtos.BuildingRequest;
import com.binsathi.building.BuildingService;
import com.binsathi.common.ApiException;
import com.binsathi.household.HouseholdDtos.HouseholdRequest;
import com.binsathi.household.HouseholdService;
import com.binsathi.schedule.ScheduleDtos.ScheduleRequest;
import com.binsathi.schedule.ScheduleService;
import com.binsathi.tenant.Organisation;
import com.binsathi.tenant.OrganisationRepository;
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
class PickupGenerationTest {

    private static final String ALL_DAYS = "MON,TUE,WED,THU,FRI,SAT,SUN";
    private static final LocalDate START = LocalDate.of(2026, 11, 16);

    @Autowired PickupService pickupService;
    @Autowired ScheduleService scheduleService;
    @Autowired HouseholdService householdService;
    @Autowired BuildingService buildingService;
    @Autowired AreaService areaService;
    @Autowired OrganisationRepository organisations;

    private UUID newOrg(String name) {
        return organisations.save(new Organisation(name, "SOCIETY", name + "@test.local")).getId();
    }

    /** Creates an area with one building, the given number of households, and an every-day WET schedule. */
    private UUID setup(UUID orgId, String name, int householdCount) {
        UUID area = areaService.create(orgId, new AreaRequest(name + "-Area", null)).id();
        UUID building = buildingService.create(orgId, new BuildingRequest(area, name + "-Wing", null, null, null)).id();
        for (int i = 1; i <= householdCount; i++) {
            householdService.create(orgId, new HouseholdRequest(building, "10" + i, new BigDecimal("100"), null));
        }
        scheduleService.create(orgId, new ScheduleRequest(area, "WET", ALL_DAYS,
                LocalTime.of(6, 0), LocalTime.of(8, 0), null));
        return building;
    }

    @Test
    void generatesSevenDaysPerActiveHouseholdAndIsRepeatable() {
        UUID orgA = newOrg("A");
        UUID building = setup(orgA, "A", 3);
        var households = householdService.list(orgA, building, null, PageRequest.of(0, 10)).content();
        householdService.delete(orgA, households.get(0).id()); // now only 2 active households

        int first = pickupService.generateForOrganisation(orgA, START, 7);
        int second = pickupService.generateForOrganisation(orgA, START, 7);

        assertEquals(14, first);   // 2 households x 7 days
        assertEquals(0, second);   // running again creates nothing new
        assertEquals(14, pickupService.list(orgA, START, START.plusDays(6), null, PageRequest.of(0, 100)).totalElements());
    }

    @Test
    void organisationOnlySeesAndChangesItsOwnPickups() {
        UUID orgA = newOrg("A");
        UUID orgB = newOrg("B");
        setup(orgA, "A", 1);
        setup(orgB, "B", 1);
        pickupService.generateForOrganisation(orgA, START, 7);
        pickupService.generateForOrganisation(orgB, START, 7);
        UUID aPickup = pickupService.list(orgA, START, START.plusDays(6), null, PageRequest.of(0, 100))
                .content().get(0).id();

        assertEquals(7, pickupService.list(orgB, START, START.plusDays(6), null, PageRequest.of(0, 100)).totalElements());
        assertEquals(404, assertThrows(ApiException.class, () -> pickupService.complete(orgB, aPickup)).getStatus());
        assertEquals(404, assertThrows(ApiException.class, () -> pickupService.skip(orgB, aPickup, "x")).getStatus());
        assertEquals(404, assertThrows(ApiException.class, () -> pickupService.cancel(orgB, aPickup)).getStatus());
    }

    @Test
    void onlyScheduledPickupsCanChange() {
        UUID orgA = newOrg("A");
        setup(orgA, "A", 1);
        pickupService.generateForOrganisation(orgA, START, 7);
        UUID id = pickupService.list(orgA, START, START.plusDays(6), null, PageRequest.of(0, 100)).content().get(0).id();

        pickupService.complete(orgA, id);

        assertEquals(422, assertThrows(ApiException.class, () -> pickupService.skip(orgA, id, "late")).getStatus());
        assertEquals(1, pickupService.list(orgA, START, START.plusDays(6), "COMPLETED", PageRequest.of(0, 100)).totalElements());
    }
}