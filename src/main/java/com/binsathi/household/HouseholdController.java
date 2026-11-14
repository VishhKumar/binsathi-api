package com.binsathi.household;

import com.binsathi.auth.AuthUser;
import com.binsathi.common.PageResponse;
import com.binsathi.household.HouseholdDtos.HouseholdRequest;
import com.binsathi.household.HouseholdDtos.HouseholdResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.binsathi.household.HouseholdDtos.ResidentAssignment;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/households")
@PreAuthorize("hasRole('ORG_ADMIN')")
public class HouseholdController {

    private final HouseholdService service;

    public HouseholdController(HouseholdService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<HouseholdResponse> list(@AuthenticationPrincipal AuthUser me,
                                                @RequestParam(required = false) UUID buildingId,
                                                @RequestParam(required = false) String status,
                                                @PageableDefault(size = 20, sort = "unitNumber") Pageable pageable) {
        return service.list(me.organisationId(), buildingId, status, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HouseholdResponse create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody HouseholdRequest request) {
        return service.create(me.organisationId(), request);
    }

    @PutMapping("/{id}")
    public HouseholdResponse update(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id,
                                    @Valid @RequestBody HouseholdRequest request) {
        return service.update(me.organisationId(), id, request);
    }

    @PutMapping("/{id}/resident")
    public HouseholdResponse assignResident(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id,
                                            @RequestBody ResidentAssignment body) {
        return service.assignResident(me.organisationId(), id, body.residentUserId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        service.delete(me.organisationId(), id);
    }
}