package com.binsathi.schedule;

import com.binsathi.area.AreaDtos.AreaRequest;
import com.binsathi.area.AreaService;
import com.binsathi.common.ApiException;
import com.binsathi.schedule.ScheduleDtos.ScheduleRequest;
import com.binsathi.schedule.ScheduleDtos.ScheduleResponse;
import com.binsathi.tenant.Organisation;
import com.binsathi.tenant.OrganisationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Proves that one organisation can never see or change another organisation's schedules. */
@SpringBootTest
@Transactional
class ScheduleTenantIsolationTest {

    @Autowired ScheduleService scheduleService;
    @Autowired AreaService areaService;
    @Autowired OrganisationRepository organisations;

    private UUID newOrg(String name) {
        return organisations.save(new Organisation(name, "SOCIETY", name + "@test.local")).getId();
    }

    private UUID newArea(UUID orgId, String name) {
        return areaService.create(orgId, new AreaRequest(name, null)).id();
    }

    private ScheduleRequest request(UUID areaId, String waste) {
        return new ScheduleRequest(areaId, waste, "MON,THU", LocalTime.of(6, 0), LocalTime.of(8, 0), null);
    }

    @Test
    void organisationOnlySeesItsOwnSchedules() {
        UUID orgA = newOrg("A");
        UUID orgB = newOrg("B");
        scheduleService.create(orgA, request(newArea(orgA, "A-Area"), "WET"));
        scheduleService.create(orgB, request(newArea(orgB, "B-Area"), "DRY"));

        var listA = scheduleService.list(orgA, PageRequest.of(0, 50));

        assertEquals(1, listA.totalElements());
        assertEquals("WET", listA.content().get(0).wasteType());
    }

    @Test
    void cannotCreateScheduleInAnotherOrganisationsArea() {
        UUID orgA = newOrg("A");
        UUID orgB = newOrg("B");
        UUID areaB = newArea(orgB, "B-Area");

        ApiException ex = assertThrows(ApiException.class,
                () -> scheduleService.create(orgA, request(areaB, "WET")));

        assertEquals(404, ex.getStatus());
    }

    @Test
    void cannotUpdateOrDeactivateAnotherOrganisationsSchedule() {
        UUID orgA = newOrg("A");
        UUID orgB = newOrg("B");
        UUID areaA = newArea(orgA, "A-Area");
        UUID areaB = newArea(orgB, "B-Area");
        ScheduleResponse bSchedule = scheduleService.create(orgB, request(areaB, "WET"));

        assertEquals(404, assertThrows(ApiException.class,
                () -> scheduleService.update(orgA, bSchedule.id(), request(areaA, "DRY"))).getStatus());
        assertEquals(404, assertThrows(ApiException.class,
                () -> scheduleService.deactivate(orgA, bSchedule.id())).getStatus());
        assertTrue(scheduleService.list(orgB, PageRequest.of(0, 50)).content().get(0).active());
    }

    @Test
    void endTimeMustBeAfterStartTime() {
        UUID orgA = newOrg("A");
        UUID areaA = newArea(orgA, "A-Area");

        ApiException ex = assertThrows(ApiException.class, () -> scheduleService.create(orgA,
                new ScheduleRequest(areaA, "WET", "MON", LocalTime.of(9, 0), LocalTime.of(8, 0), null)));

        assertEquals(422, ex.getStatus());
    }

    @Test
    void sameAreaAndWasteTypeTwiceIsRejected() {
        UUID orgA = newOrg("A");
        UUID areaA = newArea(orgA, "A-Area");
        scheduleService.create(orgA, request(areaA, "WET"));

        assertThrows(DataIntegrityViolationException.class,
                () -> scheduleService.create(orgA, request(areaA, "WET")));
    }
}