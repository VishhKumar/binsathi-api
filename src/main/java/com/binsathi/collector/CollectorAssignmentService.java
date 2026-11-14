package com.binsathi.collector;

import com.binsathi.area.AreaRepository;
import com.binsathi.common.ApiException;
import com.binsathi.user.AppUserRepository;
import com.binsathi.user.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** An organisation admin decides which areas each collector serves. */
@Service
public class CollectorAssignmentService {

    private final CollectorAssignmentRepository assignments;
    private final AppUserRepository users;
    private final AreaRepository areas;

    public CollectorAssignmentService(CollectorAssignmentRepository assignments, AppUserRepository users,
                                      AreaRepository areas) {
        this.assignments = assignments;
        this.users = users;
        this.areas = areas;
    }

    @Transactional(readOnly = true)
    public List<UUID> areaIds(UUID organisationId, UUID collectorId) {
        requireCollector(organisationId, collectorId);
        return assignments.findByOrganisationIdAndCollectorId(organisationId, collectorId).stream()
                .map(CollectorAssignment::getAreaId).toList();
    }

    /** Replaces the collector's areas with exactly the given list. */
    @Transactional
    public List<UUID> setAreas(UUID organisationId, UUID collectorId, List<UUID> areaIds) {
        requireCollector(organisationId, collectorId);
        Set<UUID> unique = new LinkedHashSet<>(areaIds);
        for (UUID areaId : unique) {
            areas.findByIdAndOrganisationId(areaId, organisationId)
                    .orElseThrow(() -> new ApiException(404, "AREA_NOT_FOUND", "Area not found"));
        }
        assignments.deleteByOrganisationIdAndCollectorId(organisationId, collectorId);
        assignments.flush(); // delete first, so re-adding the same area does not clash
        assignments.saveAll(unique.stream()
                .map(areaId -> new CollectorAssignment(organisationId, collectorId, areaId)).toList());
        return List.copyOf(unique);
    }

    private void requireCollector(UUID organisationId, UUID collectorId) {
        users.findByIdAndOrganisationId(collectorId, organisationId)
                .filter(u -> u.getRole() == Role.COLLECTOR)
                .orElseThrow(() -> new ApiException(404, "COLLECTOR_NOT_FOUND", "Collector not found"));
    }
}