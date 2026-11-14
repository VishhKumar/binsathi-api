package com.binsathi.pickup;

import com.binsathi.auth.AuthUser;
import com.binsathi.common.PageResponse;
import com.binsathi.pickup.PickupDtos.PickupResponse;
import com.binsathi.pickup.ResidentDtos.OnDemandRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/me/pickups")
@PreAuthorize("hasRole('RESIDENT')")
public class ResidentPickupController {

    private final ResidentPickupService service;

    public ResidentPickupController(ResidentPickupService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<PickupResponse> list(
            @AuthenticationPrincipal AuthUser me,
            @PageableDefault(size = 20, sort = "scheduledDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.list(me.organisationId(), me.userId(), pageable);
    }

    @PostMapping("/on-demand")
    @ResponseStatus(HttpStatus.CREATED)
    public PickupResponse requestOnDemand(@AuthenticationPrincipal AuthUser me,
                                          @Valid @RequestBody OnDemandRequest request) {
        return service.requestOnDemand(me.organisationId(), me.userId(), request.wasteType(),
                request.date(), request.householdId());
    }

    @PatchMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        service.cancel(me.organisationId(), me.userId(), id);
    }
}