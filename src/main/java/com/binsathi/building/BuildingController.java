package com.binsathi.building;

import com.binsathi.auth.AuthUser;
import com.binsathi.building.BuildingDtos.BuildingRequest;
import com.binsathi.building.BuildingDtos.BuildingResponse;
import com.binsathi.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/buildings")
@PreAuthorize("hasRole('ORG_ADMIN')")
public class BuildingController {

    private final BuildingService service;

    public BuildingController(BuildingService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<BuildingResponse> list(@AuthenticationPrincipal AuthUser me,
                                               @RequestParam(required = false) UUID areaId,
                                               @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return service.list(me.organisationId(), areaId, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BuildingResponse create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody BuildingRequest request) {
        return service.create(me.organisationId(), request);
    }

    @PutMapping("/{id}")
    public BuildingResponse update(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id,
                                   @Valid @RequestBody BuildingRequest request) {
        return service.update(me.organisationId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        service.delete(me.organisationId(), id);
    }
}