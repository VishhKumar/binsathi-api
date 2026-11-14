package com.binsathi.building;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BuildingRepository extends JpaRepository<Building, UUID> {
    Page<Building> findByOrganisationId(UUID organisationId, Pageable pageable);

    Page<Building> findByOrganisationIdAndAreaId(UUID organisationId, UUID areaId, Pageable pageable);

    Optional<Building> findByIdAndOrganisationId(UUID id, UUID organisationId);
}