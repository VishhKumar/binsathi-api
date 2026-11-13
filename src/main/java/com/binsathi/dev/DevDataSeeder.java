package com.binsathi.dev;

import com.binsathi.tenant.Organisation;
import com.binsathi.tenant.OrganisationRepository;
import com.binsathi.user.AppUser;
import com.binsathi.user.AppUserRepository;
import com.binsathi.user.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Creates demo data for local development only. Never runs with the "prod" profile. */
@Component
@Profile("!prod")
public class DevDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);
    private static final String DEMO_PASSWORD = "Password@123";

    private final OrganisationRepository orgs;
    private final AppUserRepository users;
    private final PasswordEncoder encoder;

    public DevDataSeeder(OrganisationRepository orgs, AppUserRepository users, PasswordEncoder encoder) {
        this.orgs = orgs;
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            return;
        }
        String hash = encoder.encode(DEMO_PASSWORD);
        Organisation org = orgs.save(new Organisation("Demo Society", "SOCIETY", "admin@demo.local"));
        users.save(new AppUser(null, "Platform Admin", "superadmin@binsathi.local", null, hash, Role.SUPER_ADMIN));
        users.save(new AppUser(org.getId(), "Demo Admin", "admin@demo.local", null, hash, Role.ORG_ADMIN));
        users.save(new AppUser(org.getId(), "Demo Collector", "collector@demo.local", null, hash, Role.COLLECTOR));
        users.save(new AppUser(org.getId(), "Demo Resident", "resident@demo.local", null, hash, Role.RESIDENT));
        log.info("Dev data created. Login with e.g. admin@demo.local / {}", DEMO_PASSWORD);
    }
}