package com.binsathi.pickup;

import com.binsathi.common.ApiException;
import com.binsathi.common.PageResponse;
import com.binsathi.household.Household;
import com.binsathi.household.HouseholdRepository;
import com.binsathi.pickup.PickupDtos.PickupResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** What a resident can do: see their own pickups, request special pickups, cancel upcoming ones. */
@Service
public class ResidentPickupService {

    private static final Set<String> ON_DEMAND_TYPES = Set.of("BULKY", "E_WASTE", "GARDEN", "HAZARDOUS");
    private static final LocalTime WINDOW_START = LocalTime.of(9, 0);
    private static final LocalTime WINDOW_END = LocalTime.of(18, 0);

    private final PickupRepository pickups;
    private final HouseholdRepository households;

    public ResidentPickupService(PickupRepository pickups, HouseholdRepository households) {
        this.pickups = pickups;
        this.households = households;
    }

    @Transactional(readOnly = true)
    public PageResponse<PickupResponse> list(UUID organisationId, UUID userId, Pageable pageable) {
        List<Household> mine = myHouseholds(organisationId, userId);
        if (mine.isEmpty()) {
            return new PageResponse<>(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0L, 0);
        }
        Map<UUID, String> units = mine.stream().collect(Collectors.toMap(Household::getId, Household::getUnitNumber));
        Page<Pickup> page = pickups.findByOrganisationIdAndHouseholdIdIn(organisationId, units.keySet(), pageable);
        return PageResponse.from(page.map(p -> PickupResponse.from(p, units.get(p.getHouseholdId()))));
    }

    @Transactional
    public PickupResponse requestOnDemand(UUID organisationId, UUID userId, String wasteType,
                                          LocalDate date, UUID householdId) {
        if (!ON_DEMAND_TYPES.contains(wasteType)) {
            throw new ApiException(422, "WASTE_TYPE_NOT_ALLOWED", "This waste type cannot be requested on demand");
        }
        LocalDate today = PickupService.todayIst();
        if (date.isBefore(today) || date.isAfter(today.plusDays(30))) {
            throw new ApiException(422, "INVALID_DATE", "Choose a date within the next 30 days");
        }
        Household h = chooseHousehold(organisationId, userId, householdId);
        if (pickups.existsByHouseholdIdAndWasteTypeAndScheduledDateAndOnDemandTrueAndStatus(
                h.getId(), wasteType, date, "SCHEDULED")) {
            throw new ApiException(409, "DUPLICATE_REQUEST", "You already requested this pickup");
        }
        Pickup p = pickups.save(Pickup.onDemand(organisationId, h.getId(), wasteType, date, WINDOW_START, WINDOW_END));
        return PickupResponse.from(p, h.getUnitNumber());
    }

    @Transactional
    public void cancel(UUID organisationId, UUID userId, UUID pickupId) {
        Pickup p = pickups.findByIdAndOrganisationId(pickupId, organisationId)
                .orElseThrow(ResidentPickupService::notFound);
        Set<UUID> mine = myHouseholds(organisationId, userId).stream().map(Household::getId).collect(Collectors.toSet());
        if (!mine.contains(p.getHouseholdId())) {
            throw notFound(); // same answer as "does not exist", so nothing leaks
        }
        if (!"SCHEDULED".equals(p.getStatus())) {
            throw new ApiException(422, "INVALID_STATE", "Only scheduled pickups can be cancelled");
        }
        if (p.getScheduledDate().isBefore(PickupService.todayIst())) {
            throw new ApiException(422, "PAST_PICKUP", "This pickup date has passed");
        }
        p.markCancelled();
    }

    private List<Household> myHouseholds(UUID organisationId, UUID userId) {
        return households.findByOrganisationIdAndResidentUserIdAndStatus(organisationId, userId, "ACTIVE");
    }

    private Household chooseHousehold(UUID organisationId, UUID userId, UUID householdId) {
        List<Household> mine = myHouseholds(organisationId, userId);
        if (mine.isEmpty()) {
            throw new ApiException(422, "NO_HOUSEHOLD", "No household is linked to your account");
        }
        if (householdId != null) {
            return mine.stream().filter(h -> h.getId().equals(householdId)).findFirst()
                    .orElseThrow(() -> new ApiException(404, "HOUSEHOLD_NOT_FOUND", "Household not found"));
        }
        if (mine.size() == 1) {
            return mine.get(0);
        }
        throw new ApiException(422, "HOUSEHOLD_REQUIRED", "Choose which household this is for");
    }

    private static ApiException notFound() {
        return new ApiException(404, "PICKUP_NOT_FOUND", "Pickup not found");
    }
}