package com.binsathi.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<AppUser> findByIdAndOrganisationId(UUID id, UUID organisationId);

    Page<AppUser> findByOrganisationIdAndRoleIn(UUID organisationId, Collection<Role> roles, Pageable pageable);
    long countByOrganisationIdAndRoleAndStatus(UUID organisationId, Role role, String status);
    boolean existsByRole(Role role);
}