package com.test.feelio.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.feelio.dto.ChatMessageDto;
import com.test.feelio.dto.ChatResponseDto;
import com.test.feelio.entity.ChatSession;
import com.test.feelio.entity.ChatbotType;
import com.test.feelio.entity.User;
import com.test.feelio.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper(); // JSON 변환용

    private final ChatbotTypeService chatbotTypeService;
    private final ChatLogService chatLogService;
    private final OpenAIService openAIService;
    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>(); //사용자별로 WebSocket 연결을 저장하고 관리
    private final UserService userService;


    //private final Long tempUserId = 1L; // 🔸 실제론 로그인 사용자 ID로 바꿔야 함 (세션/인증 연동)

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Object context = session.getAttributes().get("SPRING_SECURITY_CONTEXT");

        log.info("🔍 WebSocket 연결: 세션 Attributes = {}", session.getAttributes());

        if (context instanceof SecurityContext securityContext) {
            Object principal = securityContext.getAuthentication().getPrincipal();

            try {
                String email = null;

                if (principal instanceof OAuth2User oauthUser) {
                    // 소셜 로그인 사용자
                    email = oauthUser.getAttribute("email");
                    System.out.println("🟢 소셜 로그인 사용자: " + email);

                } else if (principal instanceof CustomUserDetailsService.CustomUserPrincipal customUser) {
                    // 일반 로그인 사용자
                    email = customUser.getUsername(); // 일반 로그인 시 username = email로 사용했을 가능성 높음
                    System.out.println("🔵 일반 로그인 사용자: " + email);
                }

                if (email != null) {
                    User user = userService.findByEmail(email);
                    Long userId = user.getId();

                    session.getAttributes().put("userId", userId);
                    userSessions.put(userId, session);

                    log.info("✅ WebSocket 연결됨 - userId: {}, email: {}", userId, email);
                } else {
                    log.warn("⚠️ 인증된 사용자이지만 이메일을 확인할 수 없음: principal = {}", principal);
                }

            } catch (Exception e) {
                log.warn("❌ 사용자 조회 실패", e);
            }

        } else {
            log.warn("⚠️ SPRING_SECURITY_CONTEXT가 세션에 없음");
        }
    }



    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            log.info("📨 수신 메시지: {}", message.getPayload());

            // 1. 메시지 파싱
            ChatMessageDto chatMessage = objectMapper.readValue(message.getPayload(), ChatMessageDto.class);

            // 2. 사용자 ID 추출
            Long userId = (Long) session.getAttributes().get("userId");
            if (userId == null) {
                session.sendMessage(new TextMessage("{\"error\": \"인증된 사용자를 찾을 수 없습니다.\"}"));
                return;
            }

            // 3. 챗봇 유형 조회
            ChatbotType chatbotType = chatbotTypeService.getChatbotTypeById(chatMessage.getChatbotTypeId());

            // 4. 사용자 메시지 저장
            ChatSession chatSession = chatLogService.saveUserMessage(
                    chatMessage.getChatbotTypeId(),
                    userId,
                    chatMessage.getMessage(),
                    chatbotType
            );

            // 5. OpenAI 응답 생성
            String reply = openAIService.getChatbotReply(chatMessage.getMessage(), chatbotType.getPersonaDescription());

            // 6. 챗봇 응답 저장
            chatLogService.saveBotReply(chatSession, reply);

            // 7. 클라이언트에 응답 전송
            ChatResponseDto responseDto = new ChatResponseDto(reply);
            String jsonResponse = objectMapper.writeValueAsString(responseDto);
            session.sendMessage(new TextMessage(jsonResponse));

        } catch (Exception e) {
            log.error("❌ WebSocket 메시지 처리 중 오류", e);
            try {
                session.sendMessage(new TextMessage("{\"error\": \"메시지를 처리하는 중 오류가 발생했습니다.\"}"));
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("WebSocket 연결 종료됨: " + session.getId());

        // 세션 제거
        userSessions.entrySet().removeIf(entry -> entry.getValue().getId().equals(session.getId()));
    }

}
