package com.binsathi.tenant;

import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.UUID;

public final class OrganisationDtos {
    private OrganisationDtos() {}

    public record OrganisationRequest(
            @NotBlank @Size(max = 200) String name,
            @NotNull @Pattern(regexp = "SOCIETY|COMPANY|MUNICIPALITY") String type,
            @Email @Size(max = 255) String contactEmail,
            @NotBlank @Size(max = 200) String adminName,
            @NotBlank @Email @Size(max = 255) String adminEmail,
            @NotBlank @Size(min = 8, max = 72) String adminPassword) {}

    public record OrganisationUpdateRequest(
            @NotBlank @Size(max = 200) String name,
            @NotNull @Pattern(regexp = "SOCIETY|COMPANY|MUNICIPALITY") String type,
            @Email @Size(max = 255) String contactEmail,
            @NotNull @Pattern(regexp = "ACTIVE|INACTIVE") String status) {}

    public record OrganisationResponse(UUID id, String name, String type, String contactEmail,
                                       String status, Instant createdAt) {
        public static OrganisationResponse from(Organisation o) {
            return new OrganisationResponse(o.getId(), o.getName(), o.getType(), o.getContactEmail(),
                    o.getStatus(), o.getCreatedAt());
        }
    }
}