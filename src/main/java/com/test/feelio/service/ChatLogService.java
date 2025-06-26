package com.test.feelio.service;

import com.test.feelio.entity.ChatMessage;
import com.test.feelio.entity.ChatSession;
import com.test.feelio.entity.ChatbotType;
import com.test.feelio.entity.User;
import com.test.feelio.repository.ChatMessageRepository;
import com.test.feelio.repository.ChatSessionRepository;
import com.test.feelio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatLogService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    /**
     * 사용자 메시지 저장 (세션도 없으면 자동 생성)
     */
    @Transactional
    public ChatSession saveUserMessage(Long chatbotTypeId, Long userId, String content, ChatbotType chatbotType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. ID: " + userId));

        // ✅ 오늘 날짜 기준 세션 조회
        LocalDateTime startOfToday = LocalDateTime.now().toLocalDate().atStartOfDay();
        ChatSession session = chatSessionRepository
                .findLatestTodaySession(userId, chatbotTypeId, startOfToday)
                .orElseGet(() -> chatSessionRepository.save(
                        ChatSession.builder()
                                .user(user)
                                .chatbotType(chatbotType)
                                .sessionName("기본 세션")
                                .isActive(true)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .lastInteractionAt(LocalDateTime.now())
                                .build()
                ));

        // ✅ 메시지 저장
        ChatMessage userMessage = ChatMessage.builder()
                .chatSession(session)
                .isUserMessage(true)
                .messageContent(content)
                .build();

        chatMessageRepository.save(userMessage);
        return session;
    }


    /**
     * 챗봇 응답 메시지 저장
     */
    @Transactional
    public void saveBotReply(ChatSession session, String reply) {
        ChatMessage botMessage = ChatMessage.builder()
                .chatSession(session)
                .isUserMessage(false)
                .messageContent(reply)
                .build();

        chatMessageRepository.save(botMessage);
    }

    //메시지 조회 메서드
    public List<ChatMessage> getMessagesForTodaySession(Long userId, Long chatbotTypeId) {
        LocalDateTime startOfToday = LocalDateTime.now().toLocalDate().atStartOfDay();
        return chatSessionRepository.findLatestTodaySession(userId, chatbotTypeId, startOfToday)
                .map(session -> chatMessageRepository.findByChatSessionIdOrderByCreatedAtAsc(session.getId()))
                .orElse(List.of());
    }

}
