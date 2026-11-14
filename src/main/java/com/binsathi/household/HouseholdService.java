package com.binsathi.household;

import com.binsathi.building.BuildingRepository;
import com.binsathi.common.ApiException;
import com.binsathi.common.PageResponse;
import com.binsathi.household.HouseholdDtos.HouseholdRequest;
import com.binsathi.household.HouseholdDtos.HouseholdResponse;
import com.binsathi.user.AppUserRepository;
import com.binsathi.user.Role;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class HouseholdService {

    private final HouseholdRepository households;
    private final BuildingRepository buildings;
    private final AppUserRepository users;

    public HouseholdService(HouseholdRepository households, BuildingRepository buildings, AppUserRepository users) {
        this.households = households;
        this.buildings = buildings;
        this.users = users;
    }

    /** Lists active households by default; pass status INACTIVE to see removed ones. */
    @Transactional(readOnly = true)
    public PageResponse<HouseholdResponse> list(UUID organisationId, UUID buildingId, String status, Pageable pageable) {
        String s = status == null ? "ACTIVE" : status;
        var page = buildingId == null
                ? households.findByOrganisationIdAndStatus(organisationId, s, pageable)
                : households.findByOrganisationIdAndBuildingIdAndStatus(organisationId, buildingId, s, pageable);
        return PageResponse.from(page.map(HouseholdResponse::from));
    }

    @Transactional
    public HouseholdResponse create(UUID organisationId, HouseholdRequest r) {
        requireBuilding(organisationId, r.buildingId());
        Household h = new Household(organisationId, r.buildingId(), r.unitNumber().trim(), r.monthlyFee());
        return HouseholdResponse.from(households.saveAndFlush(h)); // a duplicate flat number becomes a clean 409
    }

    @Transactional
    public HouseholdResponse update(UUID organisationId, UUID id, HouseholdRequest r) {
        Household h = find(organisationId, id);
        requireBuilding(organisationId, r.buildingId());
        h.update(r.buildingId(), r.unitNumber().trim(), r.monthlyFee(), r.status() == null ? "ACTIVE" : r.status());
        households.flush();
        return HouseholdResponse.from(h);
    }

    /** Soft delete: the household is kept for history but no longer listed. */
    @Transactional
    public void delete(UUID organisationId, UUID id) {
        find(organisationId, id).deactivate();
    }

    /** Links an active resident of the same organisation to the household (null removes the link). */
    @Transactional
    public HouseholdResponse assignResident(UUID organisationId, UUID id, UUID residentUserId) {
        Household h = find(organisationId, id);
        if (residentUserId != null) {
            users.findByIdAndOrganisationId(residentUserId, organisationId)
                    .filter(u -> u.getRole() == Role.RESIDENT && "ACTIVE".equals(u.getStatus()))
                    .orElseThrow(() -> new ApiException(404, "RESIDENT_NOT_FOUND", "Resident not found"));
        }
        h.assignResident(residentUserId);
        return HouseholdResponse.from(h);
    }

    /** Every lookup includes the organisation id, so one organisation can never reach another's data. */
    private Household find(UUID organisationId, UUID id) {
        return households.findByIdAndOrganisationId(id, organisationId)
                .orElseThrow(() -> new ApiException(404, "HOUSEHOLD_NOT_FOUND", "Household not found"));
    }

    /** The chosen building must belong to the same organisation. */
    private void requireBuilding(UUID organisationId, UUID buildingId) {
        buildings.findByIdAndOrganisationId(buildingId, organisationId)
                .orElseThrow(() -> new ApiException(404, "BUILDING_NOT_FOUND", "Building not found"));
    }
}