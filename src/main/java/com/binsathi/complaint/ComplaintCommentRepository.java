package com.binsathi.complaint;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComplaintCommentRepository extends JpaRepository<ComplaintComment, UUID> {
    List<ComplaintComment> findByComplaintIdOrderByCreatedAtAsc(UUID complaintId);
}