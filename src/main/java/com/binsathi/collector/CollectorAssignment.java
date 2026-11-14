package com.binsathi.collector;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "collector_assignment")
public class CollectorAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organisation_id", nullable = false, updatable = false)
    private UUID organisationId;

    @Column(name = "collector_id", nullable = false)
    private UUID collectorId;

    @Column(name = "area_id", nullable = false)
    private UUID areaId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CollectorAssignment() {}

    public CollectorAssignment(UUID organisationId, UUID collectorId, UUID areaId) {
        this.organisationId = organisationId;
        this.collectorId = collectorId;
        this.areaId = areaId;
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }

    public UUID getOrganisationId() { return organisationId; }
    public UUID getCollectorId() { return collectorId; }
    public UUID getAreaId() { return areaId; }
}