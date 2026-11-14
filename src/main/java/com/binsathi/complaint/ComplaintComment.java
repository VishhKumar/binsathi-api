package com.binsathi.complaint;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "complaint_comment")
public class ComplaintComment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organisation_id", nullable = false, updatable = false)
    private UUID organisationId;

    @Column(name = "complaint_id", nullable = false, updatable = false)
    private UUID complaintId;

    @Column(name = "author_id", nullable = false, updatable = false)
    private UUID authorId;

    @Column(nullable = false)
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ComplaintComment() {}

    public ComplaintComment(UUID organisationId, UUID complaintId, UUID authorId, String message) {
        this.organisationId = organisationId;
        this.complaintId = complaintId;
        this.authorId = authorId;
        this.message = message;
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getAuthorId() { return authorId; }
    public String getMessage() { return message; }
    public Instant getCreatedAt() { return createdAt; }
}