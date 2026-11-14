package com.binsathi.dashboard;

import com.binsathi.area.AreaDtos.AreaRequest;
import com.binsathi.area.AreaService;
import com.binsathi.building.BuildingDtos.BuildingRequest;
import com.binsathi.building.BuildingService;
import com.binsathi.complaint.ComplaintDtos.ComplaintRequest;
import com.binsathi.complaint.ComplaintService;
import com.binsathi.household.HouseholdDtos.HouseholdRequest;
import com.binsathi.household.HouseholdService;
import com.binsathi.pickup.PickupService;
import com.binsathi.schedule.ScheduleDtos.ScheduleRequest;
import com.binsathi.schedule.ScheduleService;
import com.binsathi.tenant.Organisation;
import com.binsathi.tenant.OrganisationRepository;
import com.binsathi.user.Role;
import com.binsathi.user.UserDtos.UserRequest;
import com.binsathi.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class DashboardTest {

    @Autowired DashboardService dashboardService;
    @Autowired PickupService pickupService;
    @Autowired ScheduleService scheduleService;
    @Autowired HouseholdService householdService;
    @Autowired BuildingService buildingService;
    @Autowired AreaService areaService;
    @Autowired UserService userService;
    @Autowired ComplaintService complaintService;
    @Autowired OrganisationRepository organisations;

    @Test
    void countsBelongToTheOrganisationOnly() {
        UUID orgA = organisations.save(new Organisation("A", "SOCIETY", "a@test.local")).getId();
        UUID orgB = organisations.save(new Organisation("B", "SOCIETY", "b@test.local")).getId();
        UUID area = areaService.create(orgA, new AreaRequest("Area", null)).id();
        UUID building = buildingService.create(orgA, new BuildingRequest(area, "Wing", null, null, null)).id();
        UUID flat1 = householdService.create(orgA, new HouseholdRequest(building, "101", BigDecimal.TEN, null)).id();
        householdService.create(orgA, new HouseholdRequest(building, "102", BigDecimal.TEN, null));
        UUID resident = userService.create(orgA,
                new UserRequest("Res", "res@test.local", null, Role.RESIDENT, "Password@123")).id();
        householdService.assignResident(orgA, flat1, resident);
        scheduleService.create(orgA, new ScheduleRequest(area, "WET", "MON,TUE,WED,THU,FRI,SAT,SUN",
                LocalTime.of(6, 0), LocalTime.of(8, 0), null));
        LocalDate today = PickupService.todayIst();
        pickupService.generateForOrganisation(orgA, today, 1);
        UUID first = pickupService.list(orgA, today, today, null, PageRequest.of(0, 10)).content().get(0).id();
        pickupService.complete(orgA, first);
        complaintService.create(orgA, resident, new ComplaintRequest("OTHER", "Test complaint", null));

        DashboardResponse a = dashboardService.get(orgA);
        DashboardResponse b = dashboardService.get(orgB);

        assertEquals(2, a.activeHouseholds());
        assertEquals(1, a.residents());
        assertEquals(1, a.todayCompleted());
        assertEquals(1, a.todayScheduled());
        assertEquals(1, a.openComplaints());
        assertEquals(50, a.completionPercent());
        assertEquals(0, b.activeHouseholds());
        assertEquals(0, b.todayCompleted());
        assertEquals(0, b.openComplaints());
        assertNull(b.completionPercent());
    }
}