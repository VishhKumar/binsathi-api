package com.binsathi.pickup;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Collection;

public interface PickupRepository extends JpaRepository<Pickup, UUID> {
    Page<Pickup> findByOrganisationIdAndScheduledDateBetween(UUID organisationId, LocalDate from, LocalDate to,
                                                             Pageable pageable);

    Page<Pickup> findByOrganisationIdAndScheduledDateBetweenAndStatus(UUID organisationId, LocalDate from,
                                                                      LocalDate to, String status, Pageable pageable);

    Optional<Pickup> findByIdAndOrganisationId(UUID id, UUID organisationId);

    List<Pickup> findByScheduleIdAndScheduledDateBetween(UUID scheduleId, LocalDate from, LocalDate to);
    Page<Pickup> findByOrganisationIdAndHouseholdIdIn(UUID organisationId, Collection<UUID> householdIds, Pageable pageable);

    boolean existsByHouseholdIdAndWasteTypeAndScheduledDateAndOnDemandTrueAndStatus(
            UUID householdId, String wasteType, LocalDate scheduledDate, String status);
}