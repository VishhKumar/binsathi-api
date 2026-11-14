package com.binsathi.schedule;

import com.binsathi.auth.AuthUser;
import com.binsathi.common.PageResponse;
import com.binsathi.schedule.ScheduleDtos.ScheduleRequest;
import com.binsathi.schedule.ScheduleDtos.ScheduleResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/schedules")
@PreAuthorize("hasRole('ORG_ADMIN')")
public class ScheduleController {

    private final ScheduleService service;

    public ScheduleController(ScheduleService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<ScheduleResponse> list(@AuthenticationPrincipal AuthUser me,
                                               @PageableDefault(size = 20, sort = "wasteType") Pageable pageable) {
        return service.list(me.organisationId(), pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ScheduleResponse create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody ScheduleRequest request) {
        return service.create(me.organisationId(), request);
    }

    @PutMapping("/{id}")
    public ScheduleResponse update(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id,
                                   @Valid @RequestBody ScheduleRequest request) {
        return service.update(me.organisationId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        service.deactivate(me.organisationId(), id);
    }
}