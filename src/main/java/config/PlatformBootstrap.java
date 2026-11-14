package com.binsathi.config;

import com.binsathi.user.AppUser;
import com.binsathi.user.AppUserRepository;
import com.binsathi.user.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * If BOOTSTRAP_ADMIN_EMAIL and BOOTSTRAP_ADMIN_PASSWORD are set and no platform admin exists yet,
 * creates one. Remove both variables after the first login.
 */
@Component
public class PlatformBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PlatformBootstrap.class);

    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final String email;
    private final String password;

    public PlatformBootstrap(AppUserRepository users, PasswordEncoder encoder,
                             @Value("${app.bootstrap.admin-email:}") String email,
                             @Value("${app.bootstrap.admin-password:}") String password) {
        this.users = users;
        this.encoder = encoder;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank() || users.existsByRole(Role.SUPER_ADMIN)) {
            return;
        }
        if (password.length() < 12) {
            log.warn("BOOTSTRAP_ADMIN_PASSWORD is shorter than 12 characters, so no platform admin was created");
            return;
        }
        users.save(new AppUser(null, "Platform Admin", email.trim().toLowerCase(), null,
                encoder.encode(password), Role.SUPER_ADMIN));
        log.info("Platform admin created for {}", email.trim().toLowerCase());
    }
}