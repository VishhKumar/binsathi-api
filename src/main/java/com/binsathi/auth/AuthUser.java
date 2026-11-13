package com.binsathi.auth;

import com.binsathi.user.Role;

import java.util.UUID;

/** The logged-in user as read from a valid access token. organisationId is null for SUPER_ADMIN. */
public record AuthUser(UUID userId, UUID organisationId, Role role) {
}