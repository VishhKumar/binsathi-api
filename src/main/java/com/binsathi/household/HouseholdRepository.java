package com.binsathi.household;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HouseholdRepository extends JpaRepository<Household, UUID> {
    Page<Household> findByOrganisationIdAndStatus(UUID organisationId, String status, Pageable pageable);

    Page<Household> findByOrganisationIdAndBuildingIdAndStatus(UUID organisationId, UUID buildingId,
                                                               String status, Pageable pageable);

    Optional<Household> findByIdAndOrganisationId(UUID id, UUID organisationId);

    /** Ids of active households in all buildings of one area. */
    @Query("select h.id from Household h where h.organisationId = :orgId and h.status = 'ACTIVE' "
            + "and h.buildingId in (select b.id from Building b where b.organisationId = :orgId and b.areaId = :areaId)")
    List<UUID> findActiveIdsByArea(@Param("orgId") UUID orgId, @Param("areaId") UUID areaId);
    List<Household> findByOrganisationIdAndResidentUserIdAndStatus(UUID organisationId, UUID residentUserId, String status);

    long countByOrganisationIdAndStatus(UUID organisationId, String status);
}