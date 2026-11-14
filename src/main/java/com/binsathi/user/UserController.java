package com.binsathi.user;

import com.binsathi.auth.AuthUser;
import com.binsathi.common.PageResponse;
import com.binsathi.user.UserDtos.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasRole('ORG_ADMIN')")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<UserResponse> list(@AuthenticationPrincipal AuthUser me,
                                           @RequestParam(required = false) Role role,
                                           @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return service.list(me.organisationId(), role, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody UserRequest request) {
        return service.create(me.organisationId(), request);
    }

    @PutMapping("/{id}")
    public UserResponse update(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id,
                               @Valid @RequestBody UserUpdateRequest request) {
        return service.update(me.organisationId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        service.deactivate(me.organisationId(), id);
    }

    @PostMapping("/{id}/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id,
                              @Valid @RequestBody PasswordRequest request) {
        service.resetPassword(me.organisationId(), id, request.password());
    }
}