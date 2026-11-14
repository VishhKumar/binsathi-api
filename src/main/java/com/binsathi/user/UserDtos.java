package com.binsathi.user;

import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.UUID;

public final class UserDtos {
    private UserDtos() {}

    public record UserRequest(
            @NotBlank @Size(max = 200) String name,
            @NotBlank @Email @Size(max = 255) String email,
            @Size(max = 30) String phone,
            @NotNull Role role,
            @NotBlank @Size(min = 8, max = 72) String password) {}

    public record UserUpdateRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 30) String phone,
            @NotBlank @Pattern(regexp = "ACTIVE|INACTIVE") String status) {}

    public record PasswordRequest(@NotBlank @Size(min = 8, max = 72) String password) {}

    public record UserResponse(UUID id, String name, String email, String phone, Role role,
                               String status, Instant lastLoginAt) {
        public static UserResponse from(AppUser u) {
            return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getPhone(), u.getRole(),
                    u.getStatus(), u.getLastLoginAt());
        }
    }
}