package com.binsathi.complaint;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ComplaintDtos {
    private ComplaintDtos() {}

    public record ComplaintRequest(
            @NotNull @Pattern(regexp = "MISSED_PICKUP|OVERFLOWING_BIN|ILLEGAL_DUMPING|COLLECTOR_BEHAVIOUR|OTHER")
            String category,
            @NotBlank @Size(max = 1000) String description,
            UUID householdId) {}

    public record CommentRequest(@NotBlank @Size(max = 1000) String message) {}

    public record StatusRequest(@NotNull @Pattern(regexp = "IN_PROGRESS|RESOLVED") String status,
                                @Size(max = 1000) String note) {}

    public record ComplaintResponse(UUID id, UUID householdId, String unitNumber, String category,
                                    String description, String status, String resolutionNote,
                                    Instant createdAt, Instant resolvedAt) {
        public static ComplaintResponse from(Complaint c, String unitNumber) {
            return new ComplaintResponse(c.getId(), c.getHouseholdId(), unitNumber, c.getCategory(),
                    c.getDescription(), c.getStatus(), c.getResolutionNote(), c.getCreatedAt(), c.getResolvedAt());
        }
    }

    public record CommentResponse(UUID id, String authorName, String authorRole, String message, Instant createdAt) {}

    public record ComplaintDetail(ComplaintResponse complaint, List<CommentResponse> comments) {}
}