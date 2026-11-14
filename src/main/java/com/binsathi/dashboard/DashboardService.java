package com.binsathi.dashboard;

import com.binsathi.complaint.ComplaintRepository;
import com.binsathi.household.HouseholdRepository;
import com.binsathi.pickup.PickupRepository;
import com.binsathi.pickup.PickupService;
import com.binsathi.user.AppUserRepository;
import com.binsathi.user.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class DashboardService {

    private final HouseholdRepository households;
    private final AppUserRepository users;
    private final PickupRepository pickups;
    private final ComplaintRepository complaints;

    public DashboardService(HouseholdRepository households, AppUserRepository users,
                            PickupRepository pickups, ComplaintRepository complaints) {
        this.households = households;
        this.users = users;
        this.pickups = pickups;
        this.complaints = complaints;
    }

    @Transactional(readOnly = true)
    public DashboardResponse get(UUID orgId) {
        LocalDate today = PickupService.todayIst();
        LocalDate weekStart = today.minusDays(6);
        long weekCompleted = count(orgId, weekStart, today, "COMPLETED");
        long weekSkipped = count(orgId, weekStart, today, "SKIPPED");
        long weekPending = count(orgId, weekStart, today, "SCHEDULED");
        long base = weekCompleted + weekSkipped + weekPending;
        Integer percent = base == 0 ? null : (int) Math.round(weekCompleted * 100.0 / base);
        return new DashboardResponse(
                households.countByOrganisationIdAndStatus(orgId, "ACTIVE"),
                users.countByOrganisationIdAndRoleAndStatus(orgId, Role.RESIDENT, "ACTIVE"),
                users.countByOrganisationIdAndRoleAndStatus(orgId, Role.COLLECTOR, "ACTIVE"),
                count(orgId, today, today, "SCHEDULED"),
                count(orgId, today, today, "COMPLETED"),
                count(orgId, today, today, "SKIPPED"),
                weekCompleted, weekSkipped, weekPending, percent,
                complaints.countByOrganisationIdAndStatusIn(orgId, List.of("OPEN", "IN_PROGRESS", "REOPENED")));
    }

    private long count(UUID orgId, LocalDate from, LocalDate to, String status) {
        return pickups.countByOrganisationIdAndScheduledDateBetweenAndStatus(orgId, from, to, status);
    }
}