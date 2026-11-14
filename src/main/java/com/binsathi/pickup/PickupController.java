package com.binsathi.pickup;

import com.binsathi.auth.AuthUser;
import com.binsathi.common.PageResponse;
import com.binsathi.pickup.PickupDtos.GenerateResponse;
import com.binsathi.pickup.PickupDtos.PickupResponse;
import com.binsathi.pickup.PickupDtos.SkipRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pickups")
@PreAuthorize("hasRole('ORG_ADMIN')")
public class PickupController {

    private final PickupService service;

    public PickupController(PickupService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<PickupResponse> list(
            @AuthenticationPrincipal AuthUser me,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "scheduledDate") Pageable pageable) {
        LocalDate start = from != null ? from : PickupService.todayIst();
        LocalDate end = to != null ? to : start.plusDays(6);
        return service.list(me.organisationId(), start, end, status, pageable);
    }

    @PostMapping("/generate")
    public GenerateResponse generate(@AuthenticationPrincipal AuthUser me) {
        return new GenerateResponse(service.generateForOrganisation(me.organisationId(), PickupService.todayIst(), 7));
    }

    @PatchMapping("/{id}/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void complete(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        service.complete(me.organisationId(), id);
    }

    @PatchMapping("/{id}/skip")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void skip(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id,
                     @Valid @RequestBody SkipRequest request) {
        service.skip(me.organisationId(), id, request.reason());
    }

    @PatchMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        service.cancel(me.organisationId(), id);
    }
}