package com.binsathi.area;

import com.binsathi.area.AreaDtos.AreaRequest;
import com.binsathi.area.AreaDtos.AreaResponse;
import com.binsathi.auth.AuthUser;
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
@RequestMapping("/api/v1/areas")
@PreAuthorize("hasRole('ORG_ADMIN')")
public class AreaController {

    private final AreaService service;

    public AreaController(AreaService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<AreaResponse> list(@AuthenticationPrincipal AuthUser me,
                                           @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return service.list(me.organisationId(), pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AreaResponse create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody AreaRequest request) {
        return service.create(me.organisationId(), request);
    }

    @PutMapping("/{id}")
    public AreaResponse update(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id,
                               @Valid @RequestBody AreaRequest request) {
        return service.update(me.organisationId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        service.delete(me.organisationId(), id);
    }
}