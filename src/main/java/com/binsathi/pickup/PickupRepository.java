package com.binsathi.pickup;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    /** Pickups on one date in the areas a collector is assigned to. */
    @Query("select p from Pickup p where p.organisationId = :orgId and p.scheduledDate = :date "
            + "and p.householdId in (select h.id from Household h where h.organisationId = :orgId "
            + "and h.buildingId in (select b.id from Building b where b.organisationId = :orgId "
            + "and b.areaId in (select ca.areaId from CollectorAssignment ca "
            + "where ca.organisationId = :orgId and ca.collectorId = :collectorId)))")
    List<Pickup> findRoute(@Param("orgId") UUID orgId, @Param("collectorId") UUID collectorId,
                           @Param("date") LocalDate date);
}