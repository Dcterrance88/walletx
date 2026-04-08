package com.walletx.authservice.config.initializer;
import com.walletx.authservice.domain.entity.Role;
import com.walletx.authservice.domain.enums.RoleType;
import com.walletx.authservice.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

/**
 * Initializes required seed data on application startup.
 *
 * <p>Ensures that the default roles exist in the database before
 * any user operation is performed. Runs once every time the
 * application starts — skips insertion if data already exists.</p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        initializeRoles();
    }

    private void initializeRoles() {
        for (RoleType roleType : RoleType.values()) {
            if (roleRepository.findByName(roleType).isEmpty()) {
                Role role = Role.builder()
                        .name(roleType)
                        .build();
                roleRepository.save(role);
                log.info("Role created: {}", roleType.name());
            } else {
                log.debug("Role already exists: {}", roleType.name());
            }
        }
    }

}
