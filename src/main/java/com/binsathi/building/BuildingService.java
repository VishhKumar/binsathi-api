package com.binsathi.building;

import com.binsathi.area.AreaRepository;
import com.binsathi.building.BuildingDtos.BuildingRequest;
import com.binsathi.building.BuildingDtos.BuildingResponse;
import com.binsathi.common.ApiException;
import com.binsathi.common.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class BuildingService {

    private final BuildingRepository buildings;
    private final AreaRepository areas;

    public BuildingService(BuildingRepository buildings, AreaRepository areas) {
        this.buildings = buildings;
        this.areas = areas;
    }

    @Transactional(readOnly = true)
    public PageResponse<BuildingResponse> list(UUID organisationId, UUID areaId, Pageable pageable) {
        var page = areaId == null
                ? buildings.findByOrganisationId(organisationId, pageable)
                : buildings.findByOrganisationIdAndAreaId(organisationId, areaId, pageable);
        return PageResponse.from(page.map(BuildingResponse::from));
    }

    @Transactional
    public BuildingResponse create(UUID organisationId, BuildingRequest r) {
        requireArea(organisationId, r.areaId());
        Building b = new Building(organisationId, r.areaId(), r.name().trim(), clean(r.address()),
                r.latitude(), r.longitude());
        return BuildingResponse.from(buildings.save(b));
    }

    @Transactional
    public BuildingResponse update(UUID organisationId, UUID id, BuildingRequest r) {
        Building b = find(organisationId, id);
        requireArea(organisationId, r.areaId());
        b.update(r.areaId(), r.name().trim(), clean(r.address()), r.latitude(), r.longitude());
        return BuildingResponse.from(b);
    }

    @Transactional
    public void delete(UUID organisationId, UUID id) {
        buildings.delete(find(organisationId, id));
        buildings.flush(); // a building that still has households becomes a clean 409
    }

    /** Every lookup includes the organisation id, so one organisation can never reach another's data. */
    private Building find(UUID organisationId, UUID id) {
        return buildings.findByIdAndOrganisationId(id, organisationId)
                .orElseThrow(() -> new ApiException(404, "BUILDING_NOT_FOUND", "Building not found"));
    }

    /** The chosen area must belong to the same organisation. */
    private void requireArea(UUID organisationId, UUID areaId) {
        areas.findByIdAndOrganisationId(areaId, organisationId)
                .orElseThrow(() -> new ApiException(404, "AREA_NOT_FOUND", "Area not found"));
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}