package com.binsathi.collector;

import com.binsathi.area.Area;
import com.binsathi.area.AreaRepository;
import com.binsathi.building.Building;
import com.binsathi.building.BuildingRepository;
import com.binsathi.collector.CollectorDtos.RouteItem;
import com.binsathi.common.ApiException;
import com.binsathi.household.Household;
import com.binsathi.household.HouseholdRepository;
import com.binsathi.pickup.Pickup;
import com.binsathi.pickup.PickupRepository;
import com.binsathi.pickup.PickupService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** What a collector can do: see the route for their areas and mark pickups done or skipped. */
@Service
public class CollectorRouteService {

    private final PickupRepository pickups;
    private final HouseholdRepository households;
    private final BuildingRepository buildings;
    private final AreaRepository areas;
    private final CollectorAssignmentRepository assignments;

    public CollectorRouteService(PickupRepository pickups, HouseholdRepository households,
                                 BuildingRepository buildings, AreaRepository areas,
                                 CollectorAssignmentRepository assignments) {
        this.pickups = pickups;
        this.households = households;
        this.buildings = buildings;
        this.areas = areas;
        this.assignments = assignments;
    }

    @Transactional(readOnly = true)
    public List<RouteItem> route(UUID organisationId, UUID collectorId, LocalDate date) {
        List<Pickup> list = pickups.findRoute(organisationId, collectorId, date);
        if (list.isEmpty()) {
            return List.of();
        }
        Map<UUID, Household> hh = households
                .findAllById(list.stream().map(Pickup::getHouseholdId).distinct().toList()).stream()
                .collect(Collectors.toMap(Household::getId, Function.identity()));
        Map<UUID, Building> bb = buildings
                .findAllById(hh.values().stream().map(Household::getBuildingId).distinct().toList()).stream()
                .collect(Collectors.toMap(Building::getId, Function.identity()));
        Map<UUID, Area> aa = areas
                .findAllById(bb.values().stream().map(Building::getAreaId).distinct().toList()).stream()
                .collect(Collectors.toMap(Area::getId, Function.identity()));

        return list.stream().map(p -> {
            Household h = hh.get(p.getHouseholdId());
            Building b = bb.get(h.getBuildingId());
            Area a = aa.get(b.getAreaId());
            return new RouteItem(p.getId(), h.getUnitNumber(), b.getName(), b.getAddress(), a.getName(),
                    p.getWasteType(), p.getWindowStart(), p.getWindowEnd(), p.getStatus(),
                    p.getSkipReason(), p.isOnDemand());
        }).sorted(Comparator.comparing(RouteItem::buildingName).thenComparing(RouteItem::unitNumber)).toList();
    }

    @Transactional
    public void complete(UUID organisationId, UUID collectorId, UUID pickupId) {
        findChangeable(organisationId, collectorId, pickupId).completeBy(collectorId);
    }

    @Transactional
    public void skip(UUID organisationId, UUID collectorId, UUID pickupId, String reason) {
        findChangeable(organisationId, collectorId, pickupId).skipBy(collectorId, reason.trim());
    }

    /** The pickup must be in one of the collector's areas, still scheduled, and not in the future. */
    private Pickup findChangeable(UUID organisationId, UUID collectorId, UUID pickupId) {
        Pickup p = pickups.findByIdAndOrganisationId(pickupId, organisationId).orElseThrow(this::notFound);
        UUID areaId = households.findByIdAndOrganisationId(p.getHouseholdId(), organisationId)
                .flatMap(h -> buildings.findByIdAndOrganisationId(h.getBuildingId(), organisationId))
                .map(Building::getAreaId).orElse(null);
        Set<UUID> assigned = assignments.findByOrganisationIdAndCollectorId(organisationId, collectorId).stream()
                .map(CollectorAssignment::getAreaId).collect(Collectors.toSet());
        if (areaId == null || !assigned.contains(areaId)) {
            throw notFound(); // same answer as "does not exist", so nothing leaks
        }
        if (!"SCHEDULED".equals(p.getStatus())) {
            throw new ApiException(422, "INVALID_STATE", "Only scheduled pickups can be changed");
        }
        if (p.getScheduledDate().isAfter(PickupService.todayIst())) {
            throw new ApiException(422, "FUTURE_PICKUP", "This pickup is not due yet");
        }
        return p;
    }

    private ApiException notFound() {
        return new ApiException(404, "PICKUP_NOT_FOUND", "Pickup not found");
    }
}