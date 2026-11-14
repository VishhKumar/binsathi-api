package com.binsathi.area;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AreaRepository extends JpaRepository<Area, UUID> {
    Page<Area> findByOrganisationId(UUID organisationId, Pageable pageable);

    Optional<Area> findByIdAndOrganisationId(UUID id, UUID organisationId);
}