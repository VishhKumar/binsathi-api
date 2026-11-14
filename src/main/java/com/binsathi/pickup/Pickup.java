package com.binsathi.pickup;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "pickup")
public class Pickup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organisation_id", nullable = false, updatable = false)
    private UUID organisationId;

    @Column(name = "household_id", nullable = false)
    private UUID householdId;

    @Column(name = "schedule_id")
    private UUID scheduleId;

    @Column(name = "waste_type", nullable = false)
    private String wasteType;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "window_start", nullable = false)
    private LocalTime windowStart;

    @Column(name = "window_end", nullable = false)
    private LocalTime windowEnd;

    @Column(nullable = false)
    private String status = "SCHEDULED";

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "skip_reason")
    private String skipReason;

    @Column(name = "collector_id")
    private UUID collectorId;

    @Column(name = "is_on_demand", nullable = false)
    private boolean onDemand;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Pickup() {}

    public Pickup(UUID organisationId, UUID householdId, UUID scheduleId, String wasteType,
                  LocalDate scheduledDate, LocalTime windowStart, LocalTime windowEnd) {
        this.organisationId = organisationId;
        this.householdId = householdId;
        this.scheduleId = scheduleId;
        this.wasteType = wasteType;
        this.scheduledDate = scheduledDate;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); updatedAt = createdAt; }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
    public static Pickup onDemand(UUID organisationId, UUID householdId, String wasteType,
                                  LocalDate scheduledDate, LocalTime windowStart, LocalTime windowEnd) {
        Pickup p = new Pickup(organisationId, householdId, null, wasteType, scheduledDate, windowStart, windowEnd);
        p.onDemand = true;
        return p;
    }

    public void markCompleted() { this.status = "COMPLETED"; this.completedAt = Instant.now(); }
    public void markSkipped(String reason) { this.status = "SKIPPED"; this.skipReason = reason; }
    public void markCancelled() { this.status = "CANCELLED"; }
    public void completeBy(UUID collectorId) {
        this.status = "COMPLETED";
        this.completedAt = Instant.now();
        this.collectorId = collectorId;
    }

    public void skipBy(UUID collectorId, String reason) {
        this.status = "SKIPPED";
        this.skipReason = reason;
        this.collectorId = collectorId;
    }

    public UUID getCollectorId() { return collectorId; }

    public UUID getId() { return id; }
    public UUID getOrganisationId() { return organisationId; }
    public UUID getHouseholdId() { return householdId; }
    public UUID getScheduleId() { return scheduleId; }
    public String getWasteType() { return wasteType; }
    public LocalDate getScheduledDate() { return scheduledDate; }
    public LocalTime getWindowStart() { return windowStart; }
    public LocalTime getWindowEnd() { return windowEnd; }
    public String getStatus() { return status; }
    public String getSkipReason() { return skipReason; }
    public boolean isOnDemand() { return onDemand; }
}