package com.binsathi.area;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class AreaDtos {
    private AreaDtos() {}

    public record AreaRequest(@NotBlank @Size(max = 200) String name, @Size(max = 100) String city) {}

    public record AreaResponse(UUID id, String name, String city) {
        public static AreaResponse from(Area a) {
            return new AreaResponse(a.getId(), a.getName(), a.getCity());
        }
    }
}