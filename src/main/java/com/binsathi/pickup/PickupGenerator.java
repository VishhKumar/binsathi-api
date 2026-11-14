package com.binsathi.pickup;

import com.binsathi.household.HouseholdRepository;
import com.binsathi.schedule.Schedule;
import com.binsathi.schedule.ScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Creates pickups from a schedule. Safe to run many times: existing pickups are never duplicated. */
@Service
public class PickupGenerator {

    private final ScheduleRepository schedules;
    private final HouseholdRepository households;
    private final PickupRepository pickups;

    public PickupGenerator(ScheduleRepository schedules, HouseholdRepository households, PickupRepository pickups) {
        this.schedules = schedules;
        this.households = households;
        this.pickups = pickups;
    }

    @Transactional
    public int generateForSchedule(UUID scheduleId, LocalDate from, int days) {
        Schedule s = schedules.findById(scheduleId).filter(Schedule::isActive).orElse(null);
        if (s == null) {
            return 0;
        }
        LocalDate to = from.plusDays(days - 1L);
        List<UUID> householdIds = households.findActiveIdsByArea(s.getOrganisationId(), s.getAreaId());
        if (householdIds.isEmpty()) {
            return 0;
        }

        Set<String> existing = new HashSet<>();
        for (Pickup p : pickups.findByScheduleIdAndScheduledDateBetween(s.getId(), from, to)) {
            existing.add(p.getHouseholdId() + "|" + p.getScheduledDate());
        }
        Set<String> dayNames = Set.of(s.getDaysOfWeek().split(","));

        List<Pickup> toCreate = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            String day = d.getDayOfWeek().name().substring(0, 3); // MON, TUE, ...
            if (!dayNames.contains(day)) {
                continue;
            }
            for (UUID householdId : householdIds) {
                if (!existing.contains(householdId + "|" + d)) {
                    toCreate.add(new Pickup(s.getOrganisationId(), householdId, s.getId(), s.getWasteType(),
                            d, s.getWindowStart(), s.getWindowEnd()));
                }
            }
        }
        pickups.saveAll(toCreate);
        return toCreate.size();
    }
}