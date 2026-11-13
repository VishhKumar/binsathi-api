package com.binsathi.auth;

import com.binsathi.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record UserSummary(UUID id, UUID organisationId, String name, String email, Role role) {}

    public record TokenResponse(String accessToken, String refreshToken, String tokenType,
                                long expiresInSeconds, UserSummary user) {}
}