package com.binsathi.pickup;

import com.binsathi.common.ApiException;
import com.binsathi.common.PageResponse;
import com.binsathi.household.Household;
import com.binsathi.household.HouseholdRepository;
import com.binsathi.pickup.PickupDtos.PickupResponse;
import com.binsathi.schedule.Schedule;
import com.binsathi.schedule.ScheduleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PickupService {

    public static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final Set<String> STATUSES = Set.of("SCHEDULED", "COMPLETED", "SKIPPED", "RESCHEDULED", "CANCELLED");

    private final PickupRepository pickups;
    private final HouseholdRepository households;
    private final ScheduleRepository schedules;
    private final PickupGenerator generator;

    public PickupService(PickupRepository pickups, HouseholdRepository households,
                         ScheduleRepository schedules, PickupGenerator generator) {
        this.pickups = pickups;
        this.households = households;
        this.schedules = schedules;
        this.generator = generator;
    }

    public static LocalDate todayIst() {
        return LocalDate.now(IST);
    }

    @Transactional(readOnly = true)
    public PageResponse<PickupResponse> list(UUID organisationId, LocalDate from, LocalDate to,
                                             String status, Pageable pageable) {
        if (status != null && !STATUSES.contains(status)) {
            throw new ApiException(400, "BAD_STATUS", "Unknown status");
        }
        Page<Pickup> page = status == null
                ? pickups.findByOrganisationIdAndScheduledDateBetween(organisationId, from, to, pageable)
                : pickups.findByOrganisationIdAndScheduledDateBetweenAndStatus(organisationId, from, to, status, pageable);
        Map<UUID, String> units = households.findAllById(
                        page.getContent().stream().map(Pickup::getHouseholdId).distinct().toList())
                .stream().collect(Collectors.toMap(Household::getId, Household::getUnitNumber));
        return PageResponse.from(page.map(p -> PickupResponse.from(p, units.get(p.getHouseholdId()))));
    }

    /** Creates pickups for every active schedule of the organisation. Returns how many were created. */
    public int generateForOrganisation(UUID organisationId, LocalDate from, int days) {
        int created = 0;
        for (Schedule s : schedules.findByOrganisationIdAndActiveTrue(organisationId)) {
            created += generator.generateForSchedule(s.getId(), from, days);
        }
        return created;
    }

    @Transactional
    public void complete(UUID organisationId, UUID id) {
        findScheduled(organisationId, id).markCompleted();
    }

    @Transactional
    public void skip(UUID organisationId, UUID id, String reason) {
        findScheduled(organisationId, id).markSkipped(reason.trim());
    }

    @Transactional
    public void cancel(UUID organisationId, UUID id) {
        findScheduled(organisationId, id).markCancelled();
    }

    /** Every lookup includes the organisation id, and only SCHEDULED pickups can change. */
    private Pickup findScheduled(UUID organisationId, UUID id) {
        Pickup p = pickups.findByIdAndOrganisationId(id, organisationId)
                .orElseThrow(() -> new ApiException(404, "PICKUP_NOT_FOUND", "Pickup not found"));
        if (!"SCHEDULED".equals(p.getStatus())) {
            throw new ApiException(422, "INVALID_STATE", "Only scheduled pickups can be changed");
        }
        return p;
    }
}