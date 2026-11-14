package com.binsathi.household;

import com.binsathi.area.AreaDtos.AreaRequest;
import com.binsathi.area.AreaService;
import com.binsathi.building.BuildingDtos.BuildingRequest;
import com.binsathi.building.BuildingService;
import com.binsathi.common.ApiException;
import com.binsathi.household.HouseholdDtos.HouseholdRequest;
import com.binsathi.household.HouseholdDtos.HouseholdResponse;
import com.binsathi.tenant.Organisation;
import com.binsathi.tenant.OrganisationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Proves that one organisation can never see or change another organisation's households. */
@SpringBootTest
@Transactional
class HouseholdTenantIsolationTest {

    @Autowired HouseholdService householdService;
    @Autowired BuildingService buildingService;
    @Autowired AreaService areaService;
    @Autowired OrganisationRepository organisations;

    private UUID newOrg(String name) {
        return organisations.save(new Organisation(name, "SOCIETY", name + "@test.local")).getId();
    }

    private UUID newBuilding(UUID orgId, String name) {
        UUID area = areaService.create(orgId, new AreaRequest(name + "-Area", null)).id();
        return buildingService.create(orgId, new BuildingRequest(area, name + "-Wing", null, null, null)).id();
    }

    private HouseholdRequest request(UUID buildingId, String unit) {
        return new HouseholdRequest(buildingId, unit, new BigDecimal("150.00"), null);
    }

    @Test
    void organisationOnlySeesItsOwnHouseholds() {
        UUID orgA = newOrg("A");
        UUID orgB = newOrg("B");
        householdService.create(orgA, request(newBuilding(orgA, "A"), "101"));
        householdService.create(orgB, request(newBuilding(orgB, "B"), "201"));

        var listA = householdService.list(orgA, null, null, PageRequest.of(0, 50));

        assertEquals(1, listA.totalElements());
        assertEquals("101", listA.content().get(0).unitNumber());
    }

    @Test
    void cannotCreateHouseholdInAnotherOrganisationsBuilding() {
        UUID orgA = newOrg("A");
        UUID orgB = newOrg("B");
        UUID buildingB = newBuilding(orgB, "B");

        ApiException ex = assertThrows(ApiException.class,
                () -> householdService.create(orgA, request(buildingB, "999")));

        assertEquals(404, ex.getStatus());
    }

    @Test
    void cannotUpdateOrDeleteAnotherOrganisationsHousehold() {
        UUID orgA = newOrg("A");
        UUID orgB = newOrg("B");
        UUID buildingA = newBuilding(orgA, "A");
        UUID buildingB = newBuilding(orgB, "B");
        HouseholdResponse bHousehold = householdService.create(orgB, request(buildingB, "201"));

        ApiException update = assertThrows(ApiException.class,
                () -> householdService.update(orgA, bHousehold.id(), request(buildingA, "Hacked")));
        ApiException delete = assertThrows(ApiException.class,
                () -> householdService.delete(orgA, bHousehold.id()));

        assertEquals(404, update.getStatus());
        assertEquals(404, delete.getStatus());
        assertEquals("ACTIVE", householdService.list(orgB, null, null, PageRequest.of(0, 50)).content().get(0).status());
    }

    @Test
    void duplicateFlatNumberInSameBuildingIsRejected() {
        UUID orgA = newOrg("A");
        UUID buildingA = newBuilding(orgA, "A");
        householdService.create(orgA, request(buildingA, "101"));

        assertThrows(DataIntegrityViolationException.class,
                () -> householdService.create(orgA, request(buildingA, "101")));
    }
}