package com.binsathi.collector;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public final class CollectorDtos {
    private CollectorDtos() {}

    public record AreaIdsRequest(@NotNull @Size(max = 200) List<UUID> areaIds) {}

    public record AreaIdsResponse(List<UUID> areaIds) {}

    public record RouteItem(UUID id, String unitNumber, String buildingName, String address, String areaName,
                            String wasteType, LocalTime windowStart, LocalTime windowEnd,
                            String status, String skipReason, boolean onDemand) {}
}