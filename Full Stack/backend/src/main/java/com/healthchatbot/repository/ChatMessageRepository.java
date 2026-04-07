package com.healthchatbot.repository;

import com.healthchatbot.entity.ChatMessage;
import com.healthchatbot.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySessionOrderBySentAtAsc(ChatSession session);

    List<ChatMessage> findBySessionIdOrderBySentAtAsc(Long sessionId);

    // --- Analytics queries ---

    long countByTypeAndSentAtGreaterThanEqual(ChatMessage.MessageType type, LocalDateTime from);

    long countByType(ChatMessage.MessageType type);

    @Query("SELECT m.intent, COUNT(m) as cnt FROM ChatMessage m " +
           "WHERE m.type = :type AND m.intent IS NOT NULL " +
           "GROUP BY m.intent ORDER BY cnt DESC")
    List<Object[]> countByIntent(@Param("type") ChatMessage.MessageType type);

    long countByTypeAndSentAtBetween(ChatMessage.MessageType type, LocalDateTime from, LocalDateTime to);
}
