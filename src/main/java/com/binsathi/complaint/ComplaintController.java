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
@RequestMapping("/api/v1/complaints")
@PreAuthorize("hasRole('ORG_ADMIN')")
public class ComplaintController {

    private final ComplaintService service;

    public ComplaintController(ComplaintService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<ComplaintResponse> list(
            @AuthenticationPrincipal AuthUser me,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.list(me.organisationId(), status, pageable);
    }

    @GetMapping("/{id}")
    public ComplaintDetail get(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        return service.get(me.organisationId(), id);
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void comment(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id,
                        @Valid @RequestBody CommentRequest request) {
        service.commentAdmin(me.organisationId(), me.userId(), id, request.message());
    }

    @PatchMapping("/{id}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void status(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id,
                       @Valid @RequestBody StatusRequest request) {
        service.changeStatus(me.organisationId(), id, request.status(), request.note());
    }
}