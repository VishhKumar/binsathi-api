package com.binsathi.area;

import com.binsathi.area.AreaDtos.AreaRequest;
import com.binsathi.area.AreaDtos.AreaResponse;
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

/** Proves that one organisation can never see or change another organisation's areas. */
@SpringBootTest
@Transactional
class AreaTenantIsolationTest {

    @Autowired AreaService areaService;
    @Autowired OrganisationRepository organisations;

    @Test
    void organisationOnlySeesItsOwnAreas() {
        UUID orgA = organisations.save(new Organisation("Org A", "SOCIETY", "a@test.local")).getId();
        UUID orgB = organisations.save(new Organisation("Org B", "SOCIETY", "b@test.local")).getId();

        areaService.create(orgA, new AreaRequest("A-North", "Panvel"));
        areaService.create(orgB, new AreaRequest("B-South", "Mumbai"));

        var listA = areaService.list(orgA, PageRequest.of(0, 50));
        var listB = areaService.list(orgB, PageRequest.of(0, 50));

        assertEquals(1, listA.totalElements());
        assertEquals("A-North", listA.content().get(0).name());
        assertEquals(1, listB.totalElements());
        assertEquals("B-South", listB.content().get(0).name());
    }

    @Test
    void organisationCannotUpdateOrDeleteAnotherOrganisationsArea() {
        UUID orgA = organisations.save(new Organisation("Org A", "SOCIETY", "a@test.local")).getId();
        UUID orgB = organisations.save(new Organisation("Org B", "SOCIETY", "b@test.local")).getId();
        AreaResponse bArea = areaService.create(orgB, new AreaRequest("B-South", "Mumbai"));

        ApiException update = assertThrows(ApiException.class,
                () -> areaService.update(orgA, bArea.id(), new AreaRequest("Hacked", null)));
        ApiException delete = assertThrows(ApiException.class,
                () -> areaService.delete(orgA, bArea.id()));

        assertEquals(404, update.getStatus());
        assertEquals(404, delete.getStatus());
        assertEquals("B-South", areaService.list(orgB, PageRequest.of(0, 50)).content().get(0).name());
    }
}