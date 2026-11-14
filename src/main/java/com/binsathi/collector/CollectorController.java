package com.binsathi.collector;

import com.binsathi.auth.AuthUser;
import com.binsathi.collector.CollectorDtos.RouteItem;
import com.binsathi.pickup.PickupDtos.SkipRequest;
import com.binsathi.pickup.PickupService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/collector")
@PreAuthorize("hasRole('COLLECTOR')")
public class CollectorController {

    private final CollectorRouteService service;

    public CollectorController(CollectorRouteService service) {
        this.service = service;
    }

    @GetMapping("/route")
    public List<RouteItem> route(@AuthenticationPrincipal AuthUser me,
                                 @RequestParam(required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.route(me.organisationId(), me.userId(), date != null ? date : PickupService.todayIst());
    }

    @PatchMapping("/pickups/{id}/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void complete(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        service.complete(me.organisationId(), me.userId(), id);
    }

    @PatchMapping("/pickups/{id}/skip")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void skip(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id,
                     @Valid @RequestBody SkipRequest request) {
        service.skip(me.organisationId(), me.userId(), id, request.reason());
    }
}