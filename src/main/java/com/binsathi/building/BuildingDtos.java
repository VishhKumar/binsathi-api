package com.binsathi.building;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class BuildingDtos {
    private BuildingDtos() {}

    public record BuildingRequest(
            @NotNull UUID areaId,
            @NotBlank @Size(max = 200) String name,
            @Size(max = 500) String address,
            @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
            @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude) {}

    public record BuildingResponse(UUID id, UUID areaId, String name, String address,
                                   Double latitude, Double longitude) {
        public static BuildingResponse from(Building b) {
            return new BuildingResponse(b.getId(), b.getAreaId(), b.getName(), b.getAddress(),
                    b.getLatitude(), b.getLongitude());
        }
    }
}