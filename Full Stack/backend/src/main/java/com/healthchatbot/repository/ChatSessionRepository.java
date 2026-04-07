package com.healthchatbot.repository;

import com.healthchatbot.entity.ChatSession;
import com.healthchatbot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    List<ChatSession> findByUserOrderByStartedAtDesc(User user);

    Optional<ChatSession> findByIdAndUser(Long id, User user);

    List<ChatSession> findByUserAndActiveTrue(User user);
}
