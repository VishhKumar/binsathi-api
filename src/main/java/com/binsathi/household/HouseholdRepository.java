package com.binsathi.household;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface HouseholdRepository extends JpaRepository<Household, UUID> {
    Page<Household> findByOrganisationIdAndStatus(UUID organisationId, String status, Pageable pageable);

    Page<Household> findByOrganisationIdAndBuildingIdAndStatus(UUID organisationId, UUID buildingId,
                                                               String status, Pageable pageable);

    Optional<Household> findByIdAndOrganisationId(UUID id, UUID organisationId);
}