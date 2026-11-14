package com.binsathi.complaint;

import com.binsathi.auth.AuthUser;
import com.binsathi.common.PageResponse;
import com.binsathi.complaint.ComplaintDtos.*;
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
@RequestMapping("/api/v1/me/complaints")
@PreAuthorize("hasRole('RESIDENT')")
public class ResidentComplaintController {

    private final ComplaintService service;

    public ResidentComplaintController(ComplaintService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ComplaintResponse create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody ComplaintRequest request) {
        return service.create(me.organisationId(), me.userId(), request);
    }

    @GetMapping
    public PageResponse<ComplaintResponse> list(
            @AuthenticationPrincipal AuthUser me,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.listMine(me.organisationId(), me.userId(), pageable);
    }

    @GetMapping("/{id}")
    public ComplaintDetail get(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        return service.getMine(me.organisationId(), me.userId(), id);
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void comment(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id,
                        @Valid @RequestBody CommentRequest request) {
        service.commentMine(me.organisationId(), me.userId(), id, request.message());
    }

    @PatchMapping("/{id}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void close(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        service.closeMine(me.organisationId(), me.userId(), id);
    }

    @PatchMapping("/{id}/reopen")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reopen(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        service.reopenMine(me.organisationId(), me.userId(), id);
    }
}