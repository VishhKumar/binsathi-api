package com.binsathi.building;

import com.binsathi.area.AreaDtos.AreaRequest;
import com.binsathi.area.AreaService;
import com.binsathi.building.BuildingDtos.BuildingRequest;
import com.binsathi.building.BuildingDtos.BuildingResponse;
import com.binsathi.common.ApiException;
import com.binsathi.tenant.Organisation;
import com.binsathi.tenant.OrganisationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Proves that one organisation can never see or change another organisation's buildings. */
@SpringBootTest
@Transactional
class BuildingTenantIsolationTest {

    @Autowired BuildingService buildingService;
    @Autowired AreaService areaService;
    @Autowired OrganisationRepository organisations;

    private UUID newOrg(String name) {
        return organisations.save(new Organisation(name, "SOCIETY", name + "@test.local")).getId();
    }

    @Test
    void organisationOnlySeesItsOwnBuildings() {
        UUID orgA = newOrg("A");
        UUID orgB = newOrg("B");
        UUID areaA = areaService.create(orgA, new AreaRequest("A-Area", null)).id();
        UUID areaB = areaService.create(orgB, new AreaRequest("B-Area", null)).id();
        buildingService.create(orgA, new BuildingRequest(areaA, "A-Wing", null, null, null));
        buildingService.create(orgB, new BuildingRequest(areaB, "B-Wing", null, null, null));

        var listA = buildingService.list(orgA, null, PageRequest.of(0, 50));

        assertEquals(1, listA.totalElements());
        assertEquals("A-Wing", listA.content().get(0).name());
    }

    @Test
    void cannotCreateBuildingInAnotherOrganisationsArea() {
        UUID orgA = newOrg("A");
        UUID orgB = newOrg("B");
        UUID areaB = areaService.create(orgB, new AreaRequest("B-Area", null)).id();

        ApiException ex = assertThrows(ApiException.class,
                () -> buildingService.create(orgA, new BuildingRequest(areaB, "Sneaky", null, null, null)));

        assertEquals(404, ex.getStatus());
    }

    @Test
    void cannotUpdateOrDeleteAnotherOrganisationsBuilding() {
        UUID orgA = newOrg("A");
        UUID orgB = newOrg("B");
        UUID areaA = areaService.create(orgA, new AreaRequest("A-Area", null)).id();
        UUID areaB = areaService.create(orgB, new AreaRequest("B-Area", null)).id();
        BuildingResponse bBuilding = buildingService.create(orgB, new BuildingRequest(areaB, "B-Wing", null, null, null));

        ApiException update = assertThrows(ApiException.class,
                () -> buildingService.update(orgA, bBuilding.id(), new BuildingRequest(areaA, "Hacked", null, null, null)));
        ApiException delete = assertThrows(ApiException.class,
                () -> buildingService.delete(orgA, bBuilding.id()));

        assertEquals(404, update.getStatus());
        assertEquals(404, delete.getStatus());
        assertEquals("B-Wing", buildingService.list(orgB, null, PageRequest.of(0, 50)).content().get(0).name());
    }
}