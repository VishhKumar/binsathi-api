package com.binsathi.schedule;

import com.binsathi.area.AreaRepository;
import com.binsathi.common.ApiException;
import com.binsathi.common.PageResponse;
import com.binsathi.schedule.ScheduleDtos.ScheduleRequest;
import com.binsathi.schedule.ScheduleDtos.ScheduleResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ScheduleService {

    private final ScheduleRepository schedules;
    private final AreaRepository areas;

    public ScheduleService(ScheduleRepository schedules, AreaRepository areas) {
        this.schedules = schedules;
        this.areas = areas;
    }

    @Transactional(readOnly = true)
    public PageResponse<ScheduleResponse> list(UUID organisationId, Pageable pageable) {
        return PageResponse.from(schedules.findByOrganisationId(organisationId, pageable)
                .map(ScheduleResponse::from));
    }

    @Transactional
    public ScheduleResponse create(UUID organisationId, ScheduleRequest r) {
        requireArea(organisationId, r.areaId());
        requireValidWindow(r);
        Schedule s = new Schedule(organisationId, r.areaId(), r.wasteType(), r.daysOfWeek(),
                r.windowStart(), r.windowEnd());
        return ScheduleResponse.from(schedules.saveAndFlush(s)); // same area + waste type twice becomes a 409
    }

    @Transactional
    public ScheduleResponse update(UUID organisationId, UUID id, ScheduleRequest r) {
        Schedule s = find(organisationId, id);
        requireArea(organisationId, r.areaId());
        requireValidWindow(r);
        s.update(r.areaId(), r.wasteType(), r.daysOfWeek(), r.windowStart(), r.windowEnd(),
                r.active() == null || r.active());
        schedules.flush();
        return ScheduleResponse.from(s);
    }

    /** Soft delete: stops generating new pickups but keeps the history. */
    @Transactional
    public void deactivate(UUID organisationId, UUID id) {
        find(organisationId, id).deactivate();
    }

    /** Every lookup includes the organisation id, so one organisation can never reach another's data. */
    private Schedule find(UUID organisationId, UUID id) {
        return schedules.findByIdAndOrganisationId(id, organisationId)
                .orElseThrow(() -> new ApiException(404, "SCHEDULE_NOT_FOUND", "Schedule not found"));
    }

    private void requireArea(UUID organisationId, UUID areaId) {
        areas.findByIdAndOrganisationId(areaId, organisationId)
                .orElseThrow(() -> new ApiException(404, "AREA_NOT_FOUND", "Area not found"));
    }

    private static void requireValidWindow(ScheduleRequest r) {
        if (!r.windowEnd().isAfter(r.windowStart())) {
            throw new ApiException(422, "INVALID_WINDOW", "End time must be after start time");
        }
    }
}