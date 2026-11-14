package com.binsathi.user;

import com.binsathi.common.ApiException;
import com.binsathi.common.PageResponse;
import com.binsathi.user.UserDtos.*;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** An organisation admin manages only collectors and residents of their own organisation. */
@Service
public class UserService {

    private static final Set<Role> MANAGEABLE = EnumSet.of(Role.COLLECTOR, Role.RESIDENT);

    private final AppUserRepository users;
    private final PasswordEncoder encoder;

    public UserService(AppUserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(UUID organisationId, Role role, Pageable pageable) {
        List<Role> roles = role == null ? List.copyOf(MANAGEABLE)
                : MANAGEABLE.contains(role) ? List.of(role) : List.of();
        return PageResponse.from(users.findByOrganisationIdAndRoleIn(organisationId, roles, pageable)
                .map(UserResponse::from));
    }

    @Transactional
    public UserResponse create(UUID organisationId, UserRequest r) {
        if (!MANAGEABLE.contains(r.role())) {
            throw new ApiException(422, "ROLE_NOT_ALLOWED", "Only COLLECTOR and RESIDENT users can be created here");
        }
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ApiException(409, "EMAIL_EXISTS", "A user with this email already exists");
        }
        AppUser user = new AppUser(organisationId, r.name().trim(), email, clean(r.phone()),
                encoder.encode(r.password()), r.role());
        return UserResponse.from(users.saveAndFlush(user));
    }

    @Transactional
    public UserResponse update(UUID organisationId, UUID id, UserUpdateRequest r) {
        AppUser user = find(organisationId, id);
        user.updateProfile(r.name().trim(), clean(r.phone()));
        user.setStatus(r.status());
        return UserResponse.from(user);
    }

    /** Soft delete: the user can no longer log in, but their history is kept. */
    @Transactional
    public void deactivate(UUID organisationId, UUID id) {
        find(organisationId, id).setStatus("INACTIVE");
    }

    @Transactional
    public void resetPassword(UUID organisationId, UUID id, String newPassword) {
        find(organisationId, id).setPasswordHash(encoder.encode(newPassword));
    }

    /** Every lookup includes the organisation id, and admins cannot touch other admins. */
    private AppUser find(UUID organisationId, UUID id) {
        return users.findByIdAndOrganisationId(id, organisationId)
                .filter(u -> MANAGEABLE.contains(u.getRole()))
                .orElseThrow(() -> new ApiException(404, "USER_NOT_FOUND", "User not found"));
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}