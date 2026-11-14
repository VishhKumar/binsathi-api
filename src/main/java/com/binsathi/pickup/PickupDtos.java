package com.binsathi.pickup;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public final class PickupDtos {
    private PickupDtos() {}

    public record SkipRequest(@NotBlank @Size(max = 300) String reason) {}

    public record GenerateResponse(int created) {}

    public record PickupResponse(UUID id, UUID householdId, String unitNumber, String wasteType,
                                 LocalDate scheduledDate, LocalTime windowStart, LocalTime windowEnd,
                                 String status, String skipReason, boolean onDemand) {
        public static PickupResponse from(Pickup p, String unitNumber) {
            return new PickupResponse(p.getId(), p.getHouseholdId(), unitNumber, p.getWasteType(),
                    p.getScheduledDate(), p.getWindowStart(), p.getWindowEnd(), p.getStatus(),
                    p.getSkipReason(), p.isOnDemand());
        }
    }
}