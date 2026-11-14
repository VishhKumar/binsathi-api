package com.binsathi.schedule;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "schedule")
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organisation_id", nullable = false, updatable = false)
    private UUID organisationId;

    @Column(name = "area_id", nullable = false)
    private UUID areaId;

    @Column(name = "waste_type", nullable = false)
    private String wasteType;

    @Column(name = "days_of_week", nullable = false)
    private String daysOfWeek;

    @Column(name = "window_start", nullable = false)
    private LocalTime windowStart;

    @Column(name = "window_end", nullable = false)
    private LocalTime windowEnd;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Schedule() {}

    public Schedule(UUID organisationId, UUID areaId, String wasteType, String daysOfWeek,
                    LocalTime windowStart, LocalTime windowEnd) {
        this.organisationId = organisationId;
        this.areaId = areaId;
        this.wasteType = wasteType;
        this.daysOfWeek = daysOfWeek;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); updatedAt = createdAt; }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public void update(UUID areaId, String wasteType, String daysOfWeek,
                       LocalTime windowStart, LocalTime windowEnd, boolean active) {
        this.areaId = areaId;
        this.wasteType = wasteType;
        this.daysOfWeek = daysOfWeek;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.active = active;
    }

    public void deactivate() { this.active = false; }

    public UUID getId() { return id; }
    public UUID getOrganisationId() { return organisationId; }
    public UUID getAreaId() { return areaId; }
    public String getWasteType() { return wasteType; }
    public String getDaysOfWeek() { return daysOfWeek; }
    public LocalTime getWindowStart() { return windowStart; }
    public LocalTime getWindowEnd() { return windowEnd; }
    public boolean isActive() { return active; }
}