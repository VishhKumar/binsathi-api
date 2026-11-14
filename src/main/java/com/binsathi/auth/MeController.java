package com.binsathi.auth;

import com.binsathi.auth.AuthDtos.PasswordChangeRequest;
import com.binsathi.auth.AuthDtos.UserSummary;
import com.binsathi.common.ApiException;
import com.binsathi.user.AppUser;
import com.binsathi.user.AppUserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class MeController {

    private final AppUserRepository users;
    private final AuthService authService;

    public MeController(AppUserRepository users, AuthService authService) {
        this.users = users;
        this.authService = authService;
    }

    @GetMapping("/me")
    public UserSummary me(@AuthenticationPrincipal AuthUser principal) {
        AppUser user = users.findById(principal.userId())
                .orElseThrow(() -> new ApiException(404, "USER_NOT_FOUND", "User not found"));
        return new UserSummary(user.getId(), user.getOrganisationId(), user.getName(), user.getEmail(), user.getRole());
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal AuthUser principal,
                               @Valid @RequestBody PasswordChangeRequest request) {
        authService.changePassword(principal.userId(), request.currentPassword(), request.newPassword());
    }
}