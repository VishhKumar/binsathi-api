package com.binsathi.complaint;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ComplaintRepository extends JpaRepository<Complaint, UUID> {
    Page<Complaint> findByOrganisationId(UUID organisationId, Pageable pageable);

    Page<Complaint> findByOrganisationIdAndStatus(UUID organisationId, String status, Pageable pageable);

    Page<Complaint> findByOrganisationIdAndHouseholdIdIn(UUID organisationId, Collection<UUID> householdIds,
                                                         Pageable pageable);

    Optional<Complaint> findByIdAndOrganisationId(UUID id, UUID organisationId);

    long countByOrganisationIdAndStatusIn(UUID organisationId, Collection<String> statuses);
}