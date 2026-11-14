package com.binsathi.tenant;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organisation")
public class Organisation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(nullable = false)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Organisation() {}

    public Organisation(String name, String type, String contactEmail) {
        this.name = name;
        this.type = type;
        this.contactEmail = contactEmail;
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); updatedAt = createdAt; }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public void update(String name, String type, String contactEmail, String status) {
        this.name = name;
        this.type = type;
        this.contactEmail = contactEmail;
        this.status = status;
    }

    public Instant getCreatedAt() { return createdAt; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getContactEmail() { return contactEmail; }
    public String getLogoUrl() { return logoUrl; }
    public String getStatus() { return status; }
}