package com.binsathi.tenant;

import com.binsathi.common.PageResponse;
import com.binsathi.tenant.OrganisationDtos.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organisations")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class OrganisationController {

    private final OrganisationService service;

    public OrganisationController(OrganisationService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<OrganisationResponse> list(@PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return service.list(pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrganisationResponse create(@Valid @RequestBody OrganisationRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public OrganisationResponse update(@PathVariable UUID id, @Valid @RequestBody OrganisationUpdateRequest request) {
        return service.update(id, request);
    }
}