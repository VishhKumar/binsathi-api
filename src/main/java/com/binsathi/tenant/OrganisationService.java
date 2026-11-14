package com.binsathi.tenant;

import com.binsathi.common.ApiException;
import com.binsathi.common.PageResponse;
import com.binsathi.tenant.OrganisationDtos.*;
import com.binsathi.user.AppUser;
import com.binsathi.user.AppUserRepository;
import com.binsathi.user.Role;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Platform owner: creates customer organisations together with their first admin. */
@Service
public class OrganisationService {

    private final OrganisationRepository organisations;
    private final AppUserRepository users;
    private final PasswordEncoder encoder;

    public OrganisationService(OrganisationRepository organisations, AppUserRepository users, PasswordEncoder encoder) {
        this.organisations = organisations;
        this.users = users;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public PageResponse<OrganisationResponse> list(Pageable pageable) {
        return PageResponse.from(organisations.findAll(pageable).map(OrganisationResponse::from));
    }

    @Transactional
    public OrganisationResponse create(OrganisationRequest r) {
        String adminEmail = r.adminEmail().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(adminEmail)) {
            throw new ApiException(409, "EMAIL_EXISTS", "A user with this email already exists");
        }
        Organisation org = organisations.save(new Organisation(r.name().trim(), r.type(), clean(r.contactEmail())));
        users.save(new AppUser(org.getId(), r.adminName().trim(), adminEmail, null,
                encoder.encode(r.adminPassword()), Role.ORG_ADMIN));
        return OrganisationResponse.from(org);
    }

    @Transactional
    public OrganisationResponse update(UUID id, OrganisationUpdateRequest r) {
        Organisation org = organisations.findById(id)
                .orElseThrow(() -> new ApiException(404, "ORGANISATION_NOT_FOUND", "Organisation not found"));
        org.update(r.name().trim(), r.type(), clean(r.contactEmail()), r.status());
        return OrganisationResponse.from(org);
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}