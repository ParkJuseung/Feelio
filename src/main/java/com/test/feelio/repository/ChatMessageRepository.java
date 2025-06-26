package com.test.feelio.repository;

import com.test.feelio.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    // 기본적으로 save()만 사용됨

    List<ChatMessage> findByChatSessionIdOrderByCreatedAtAsc(Long sessionId);
}
