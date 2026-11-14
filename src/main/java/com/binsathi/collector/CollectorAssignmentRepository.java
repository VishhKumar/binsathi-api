package com.binsathi.collector;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CollectorAssignmentRepository extends JpaRepository<CollectorAssignment, UUID> {
    List<CollectorAssignment> findByOrganisationIdAndCollectorId(UUID organisationId, UUID collectorId);

    void deleteByOrganisationIdAndCollectorId(UUID organisationId, UUID collectorId);
}