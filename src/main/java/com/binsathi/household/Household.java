package com.binsathi.household;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "household")
public class Household {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organisation_id", nullable = false, updatable = false)
    private UUID organisationId;

    @Column(name = "building_id", nullable = false)
    private UUID buildingId;

    @Column(name = "unit_number", nullable = false)
    private String unitNumber;

    @Column(name = "monthly_fee", nullable = false)
    private BigDecimal monthlyFee;

    @Column(nullable = false)
    private String status = "ACTIVE";

    @Column(name = "resident_user_id")
    private UUID residentUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Household() {}

    public Household(UUID organisationId, UUID buildingId, String unitNumber, BigDecimal monthlyFee) {
        this.organisationId = organisationId;
        this.buildingId = buildingId;
        this.unitNumber = unitNumber;
        this.monthlyFee = monthlyFee;
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); updatedAt = createdAt; }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public void update(UUID buildingId, String unitNumber, BigDecimal monthlyFee, String status) {
        this.buildingId = buildingId;
        this.unitNumber = unitNumber;
        this.monthlyFee = monthlyFee;
        this.status = status;
    }

    public void deactivate() { this.status = "INACTIVE"; }

    public UUID getId() { return id; }
    public void assignResident(UUID residentUserId) { this.residentUserId = residentUserId; }

    public UUID getResidentUserId() { return residentUserId; }
    public UUID getOrganisationId() { return organisationId; }
    public UUID getBuildingId() { return buildingId; }
    public String getUnitNumber() { return unitNumber; }
    public BigDecimal getMonthlyFee() { return monthlyFee; }
    public String getStatus() { return status; }
}