package com.healthchatbot.repository;

import com.healthchatbot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findByRoleAndPhoneIsNotNull(User.Role role);

    // ── Feature #3: District-targeted outbreak notifications ────────────────

    /**
     * Find users registered in a specific district (case-insensitive) with a phone number.
     * Used by OutbreakSchedulerService to send targeted outbreak alerts.
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.district) = LOWER(:district) AND u.phone IS NOT NULL AND u.active = true")
    List<User> findByDistrictAndPhoneIsNotNull(@Param("district") String district);

    /**
     * Find all health workers and admins with phone numbers for emergency broadcasts.
     */
    @Query("SELECT u FROM User u WHERE (u.role = com.healthchatbot.entity.User.Role.HEALTH_WORKER OR u.role = com.healthchatbot.entity.User.Role.ADMIN) AND u.phone IS NOT NULL AND u.active = true")
    List<User> findAllHealthWorkersAndAdmins();

    /**
     * Find users by preferred language for localized alert messages.
     */
    List<User> findByPreferredLanguageAndPhoneIsNotNull(String preferredLanguage);
}
