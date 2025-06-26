package com.test.feelio.repository;

import com.test.feelio.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    // 가장 최근 세션 하나 조회 (유저 ID + 챗봇 ID 기준)
    Optional<ChatSession> findTopByUserIdAndChatbotTypeIdOrderByCreatedAtDesc(Long userId, Long chatbotTypeId);

    //오늘 생성된 채팅 세션만 조회
    @Query("SELECT cs FROM ChatSession cs " +
            "WHERE cs.user.id = :userId AND cs.chatbotType.id = :chatbotTypeId " +
            "AND cs.createdAt >= :startOfDay " +
            "ORDER BY cs.createdAt DESC")
    Optional<ChatSession> findLatestTodaySession(Long userId, Long chatbotTypeId, LocalDateTime startOfDay);

}
