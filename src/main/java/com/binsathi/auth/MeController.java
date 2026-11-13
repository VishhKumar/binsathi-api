package com.binsathi.auth;

import com.binsathi.auth.AuthDtos.UserSummary;
import com.binsathi.common.ApiException;
import com.binsathi.user.AppUser;
import com.binsathi.user.AppUserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class MeController {

    private final AppUserRepository users;

    public MeController(AppUserRepository users) {
        this.users = users;
    }

    @GetMapping("/me")
    public UserSummary me(@AuthenticationPrincipal AuthUser principal) {
        AppUser user = users.findById(principal.userId())
                .orElseThrow(() -> new ApiException(404, "USER_NOT_FOUND", "User not found"));
        return new UserSummary(user.getId(), user.getOrganisationId(), user.getName(), user.getEmail(), user.getRole());
    }
}