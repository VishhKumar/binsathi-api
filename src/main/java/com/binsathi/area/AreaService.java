package com.binsathi.area;

import com.binsathi.area.AreaDtos.AreaRequest;
import com.binsathi.area.AreaDtos.AreaResponse;
import com.binsathi.common.ApiException;
import com.binsathi.common.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AreaService {

    private final AreaRepository areas;

    public AreaService(AreaRepository areas) {
        this.areas = areas;
    }

    @Transactional(readOnly = true)
    public PageResponse<AreaResponse> list(UUID organisationId, Pageable pageable) {
        return PageResponse.from(areas.findByOrganisationId(organisationId, pageable).map(AreaResponse::from));
    }

    @Transactional
    public AreaResponse create(UUID organisationId, AreaRequest request) {
        Area area = new Area(organisationId, request.name().trim(), clean(request.city()));
        return AreaResponse.from(areas.save(area));
    }

    @Transactional
    public AreaResponse update(UUID organisationId, UUID id, AreaRequest request) {
        Area area = find(organisationId, id);
        area.update(request.name().trim(), clean(request.city()));
        return AreaResponse.from(area);
    }

    @Transactional
    public void delete(UUID organisationId, UUID id) {
        Area area = find(organisationId, id);
        areas.delete(area);
        areas.flush(); // surfaces "still in use" errors here, so they become a clean 409
    }

    /** Every lookup includes the organisation id, so one organisation can never reach another's data. */
    private Area find(UUID organisationId, UUID id) {
        return areas.findByIdAndOrganisationId(id, organisationId)
                .orElseThrow(() -> new ApiException(404, "AREA_NOT_FOUND", "Area not found"));
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}