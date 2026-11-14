package com.binsathi.complaint;

import com.binsathi.common.ApiException;
import com.binsathi.common.PageResponse;
import com.binsathi.complaint.ComplaintDtos.*;
import com.binsathi.household.Household;
import com.binsathi.household.HouseholdRepository;
import com.binsathi.user.AppUser;
import com.binsathi.user.AppUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ComplaintService {

    private static final Set<String> STATUSES = Set.of("OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED", "REOPENED");

    private final ComplaintRepository complaints;
    private final ComplaintCommentRepository comments;
    private final HouseholdRepository households;
    private final AppUserRepository users;

    public ComplaintService(ComplaintRepository complaints, ComplaintCommentRepository comments,
                            HouseholdRepository households, AppUserRepository users) {
        this.complaints = complaints;
        this.comments = comments;
        this.households = households;
        this.users = users;
    }

    // ---------- resident ----------

    @Transactional
    public ComplaintResponse create(UUID orgId, UUID userId, ComplaintRequest r) {
        Household h = chooseHousehold(myHouseholds(orgId, userId), r.householdId());
        Complaint c = complaints.save(new Complaint(orgId, h.getId(), r.category(), r.description().trim()));
        return ComplaintResponse.from(c, h.getUnitNumber());
    }

    @Transactional(readOnly = true)
    public PageResponse<ComplaintResponse> listMine(UUID orgId, UUID userId, Pageable pageable) {
        List<Household> mine = myHouseholds(orgId, userId);
        if (mine.isEmpty()) {
            return new PageResponse<>(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0L, 0);
        }
        Map<UUID, String> units = mine.stream().collect(Collectors.toMap(Household::getId, Household::getUnitNumber));
        Page<Complaint> page = complaints.findByOrganisationIdAndHouseholdIdIn(orgId, units.keySet(), pageable);
        return PageResponse.from(page.map(c -> ComplaintResponse.from(c, units.get(c.getHouseholdId()))));
    }

    @Transactional(readOnly = true)
    public ComplaintDetail getMine(UUID orgId, UUID userId, UUID id) {
        return detail(owned(orgId, userId, id));
    }

    @Transactional
    public void commentMine(UUID orgId, UUID userId, UUID id, String message) {
        addComment(owned(orgId, userId, id), userId, message);
    }

    /** The resident confirms the problem is fixed. */
    @Transactional
    public void closeMine(UUID orgId, UUID userId, UUID id) {
        Complaint c = owned(orgId, userId, id);
        requireStatus(c, "RESOLVED");
        c.close();
    }

    /** The resident says the problem is not fixed. */
    @Transactional
    public void reopenMine(UUID orgId, UUID userId, UUID id) {
        Complaint c = owned(orgId, userId, id);
        requireStatus(c, "RESOLVED");
        c.reopen();
    }

    // ---------- admin ----------

    @Transactional(readOnly = true)
    public PageResponse<ComplaintResponse> list(UUID orgId, String status, Pageable pageable) {
        if (status != null && !STATUSES.contains(status)) {
            throw new ApiException(400, "BAD_STATUS", "Unknown status");
        }
        Page<Complaint> page = status == null
                ? complaints.findByOrganisationId(orgId, pageable)
                : complaints.findByOrganisationIdAndStatus(orgId, status, pageable);
        Map<UUID, String> units = households
                .findAllById(page.getContent().stream().map(Complaint::getHouseholdId).distinct().toList())
                .stream().collect(Collectors.toMap(Household::getId, Household::getUnitNumber));
        return PageResponse.from(page.map(c -> ComplaintResponse.from(c, units.get(c.getHouseholdId()))));
    }

    @Transactional(readOnly = true)
    public ComplaintDetail get(UUID orgId, UUID id) {
        return detail(find(orgId, id));
    }

    @Transactional
    public void commentAdmin(UUID orgId, UUID adminId, UUID id, String message) {
        addComment(find(orgId, id), adminId, message);
    }

    @Transactional
    public void changeStatus(UUID orgId, UUID id, String status, String note) {
        Complaint c = find(orgId, id);
        String current = c.getStatus();
        boolean open = current.equals("OPEN") || current.equals("REOPENED");
        if (status.equals("IN_PROGRESS")) {
            if (!open) throw invalidState();
            c.startWork();
        } else if (status.equals("RESOLVED")) {
            if (!(open || current.equals("IN_PROGRESS"))) throw invalidState();
            if (note == null || note.isBlank()) {
                throw new ApiException(422, "NOTE_REQUIRED", "Add a note explaining how it was resolved");
            }
            c.resolve(note.trim());
        } else {
            throw invalidState();
        }
    }

    // ---------- helpers ----------

    /** Every lookup includes the organisation id, so one organisation can never reach another's data. */
    private Complaint find(UUID orgId, UUID id) {
        return complaints.findByIdAndOrganisationId(id, orgId).orElseThrow(ComplaintService::notFound);
    }

    /** A resident can only reach complaints of their own household(s). */
    private Complaint owned(UUID orgId, UUID userId, UUID id) {
        Complaint c = find(orgId, id);
        boolean mine = myHouseholds(orgId, userId).stream().anyMatch(h -> h.getId().equals(c.getHouseholdId()));
        if (!mine) {
            throw notFound(); // same answer as "does not exist", so nothing leaks
        }
        return c;
    }

    private void addComment(Complaint c, UUID authorId, String message) {
        if (c.getStatus().equals("CLOSED")) {
            throw new ApiException(422, "COMPLAINT_CLOSED", "This complaint is closed");
        }
        comments.save(new ComplaintComment(c.getOrganisationId(), c.getId(), authorId, message.trim()));
    }

    private ComplaintDetail detail(Complaint c) {
        String unit = households.findById(c.getHouseholdId()).map(Household::getUnitNumber).orElse(null);
        List<ComplaintComment> list = comments.findByComplaintIdOrderByCreatedAtAsc(c.getId());
        Map<UUID, AppUser> authors = users
                .findAllById(list.stream().map(ComplaintComment::getAuthorId).distinct().toList())
                .stream().collect(Collectors.toMap(AppUser::getId, Function.identity()));
        List<CommentResponse> out = list.stream().map(cm -> {
            AppUser a = authors.get(cm.getAuthorId());
            return new CommentResponse(cm.getId(), a == null ? "Unknown" : a.getName(),
                    a == null ? "" : a.getRole().name(), cm.getMessage(), cm.getCreatedAt());
        }).toList();
        return new ComplaintDetail(ComplaintResponse.from(c, unit), out);
    }

    private List<Household> myHouseholds(UUID orgId, UUID userId) {
        return households.findByOrganisationIdAndResidentUserIdAndStatus(orgId, userId, "ACTIVE");
    }

    private Household chooseHousehold(List<Household> mine, UUID householdId) {
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

    private void requireStatus(Complaint c, String expected) {
        if (!c.getStatus().equals(expected)) throw invalidState();
    }

    private static ApiException invalidState() {
        return new ApiException(422, "INVALID_STATE", "This change is not allowed for the complaint's current status");
    }

    private static ApiException notFound() {
        return new ApiException(404, "COMPLAINT_NOT_FOUND", "Complaint not found");
    }
}