package com.binsathi.household;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public final class HouseholdDtos {
    private HouseholdDtos() {}

    public record HouseholdRequest(
            @NotNull UUID buildingId,
            @NotBlank @Size(max = 50) String unitNumber,
            @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal monthlyFee,
            @Pattern(regexp = "ACTIVE|INACTIVE") String status) {}

    /** residentUserId may be null, which removes the link. */
    public record ResidentAssignment(UUID residentUserId) {}

    public record HouseholdResponse(UUID id, UUID buildingId, String unitNumber,
                                    BigDecimal monthlyFee, String status, UUID residentUserId) {
        public static HouseholdResponse from(Household h) {
            return new HouseholdResponse(h.getId(), h.getBuildingId(), h.getUnitNumber(),
                    h.getMonthlyFee(), h.getStatus(), h.getResidentUserId());
        }
    }
}