package com.binsathi.schedule;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalTime;
import java.util.UUID;

public final class ScheduleDtos {
    private ScheduleDtos() {}

    private static final String WASTE = "WET|DRY|RECYCLABLE|HAZARDOUS|E_WASTE|BULKY|GARDEN";
    private static final String DAY = "(MON|TUE|WED|THU|FRI|SAT|SUN)";

    public record ScheduleRequest(
            @NotNull UUID areaId,
            @NotNull @Pattern(regexp = WASTE) String wasteType,
            @NotNull @Pattern(regexp = DAY + "(," + DAY + ")*") String daysOfWeek,
            @NotNull LocalTime windowStart,
            @NotNull LocalTime windowEnd,
            Boolean active) {}

    public record ScheduleResponse(UUID id, UUID areaId, String wasteType, String daysOfWeek,
                                   LocalTime windowStart, LocalTime windowEnd, boolean active) {
        public static ScheduleResponse from(Schedule s) {
            return new ScheduleResponse(s.getId(), s.getAreaId(), s.getWasteType(), s.getDaysOfWeek(),
                    s.getWindowStart(), s.getWindowEnd(), s.isActive());
        }
    }
}