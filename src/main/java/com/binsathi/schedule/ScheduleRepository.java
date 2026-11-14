package com.binsathi.schedule;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ScheduleRepository extends JpaRepository<Schedule, UUID> {
    Page<Schedule> findByOrganisationId(UUID organisationId, Pageable pageable);

    Optional<Schedule> findByIdAndOrganisationId(UUID id, UUID organisationId);
}