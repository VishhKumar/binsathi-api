package com.binsathi.collector;

import com.binsathi.auth.AuthUser;
import com.binsathi.collector.CollectorDtos.AreaIdsRequest;
import com.binsathi.collector.CollectorDtos.AreaIdsResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/collectors")
@PreAuthorize("hasRole('ORG_ADMIN')")
public class CollectorAssignmentController {

    private final CollectorAssignmentService service;

    public CollectorAssignmentController(CollectorAssignmentService service) {
        this.service = service;
    }

    @GetMapping("/{collectorId}/areas")
    public AreaIdsResponse get(@AuthenticationPrincipal AuthUser me, @PathVariable UUID collectorId) {
        return new AreaIdsResponse(service.areaIds(me.organisationId(), collectorId));
    }

    @PutMapping("/{collectorId}/areas")
    public AreaIdsResponse set(@AuthenticationPrincipal AuthUser me, @PathVariable UUID collectorId,
                               @Valid @RequestBody AreaIdsRequest request) {
        return new AreaIdsResponse(service.setAreas(me.organisationId(), collectorId, request.areaIds()));
    }
}