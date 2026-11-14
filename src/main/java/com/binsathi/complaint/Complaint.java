package com.binsathi.complaint;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "complaint")
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organisation_id", nullable = false, updatable = false)
    private UUID organisationId;

    @Column(name = "household_id", nullable = false)
    private UUID householdId;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String status = "OPEN";

    @Column(name = "resolution_note")
    private String resolutionNote;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Complaint() {}

    public Complaint(UUID organisationId, UUID householdId, String category, String description) {
        this.organisationId = organisationId;
        this.householdId = householdId;
        this.category = category;
        this.description = description;
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); updatedAt = createdAt; }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public void startWork() { this.status = "IN_PROGRESS"; }

    public void resolve(String note) {
        this.status = "RESOLVED";
        this.resolutionNote = note;
        this.resolvedAt = Instant.now();
    }

    public void close() { this.status = "CLOSED"; }

    public void reopen() { this.status = "REOPENED"; this.resolvedAt = null; }

    public UUID getId() { return id; }
    public UUID getOrganisationId() { return organisationId; }
    public UUID getHouseholdId() { return householdId; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public String getResolutionNote() { return resolutionNote; }
    public Instant getResolvedAt() { return resolvedAt; }
    public Instant getCreatedAt() { return createdAt; }
}