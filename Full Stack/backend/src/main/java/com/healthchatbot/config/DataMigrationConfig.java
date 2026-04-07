package com.healthchatbot.config;

import com.healthchatbot.entity.User;
import com.healthchatbot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataMigrationConfig {

    private final UserRepository userRepository;

    /**
     * One-time runner to update users' districts to an Odisha district.
     * Only updates users whose role is USER or HEALTH_WORKER (not ADMIN).
     */
    @Bean
    public CommandLineRunner updateDistricts() {
        return args -> {
            log.info("🚀 Starting data migration: Updating non-admin user districts to Odisha districts...");
            
            List<User> users = userRepository.findAll();
            int count = 0;
            
            for (User user : users) {
                if (user.getRole() != User.Role.ADMIN) {
                    // Update field to Khurda (Odisha district)
                    user.setDistrict("Khurda");
                    userRepository.save(user);
                    count++;
                }
            }
            
            log.info("✅ Migration complete! Updated {} non-admin users to 'Khurda' district.", count);
            log.info("⚠️ Please delete or comment out DataMigrationConfig after the application starts once.");
        };
    }
}
