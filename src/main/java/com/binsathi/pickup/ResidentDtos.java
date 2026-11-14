package com.binsathi.pickup;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.UUID;

public final class ResidentDtos {
    private ResidentDtos() {}

    public record OnDemandRequest(
            @NotNull @Pattern(regexp = "BULKY|E_WASTE|GARDEN|HAZARDOUS") String wasteType,
            @NotNull LocalDate date,
            UUID householdId) {}
}