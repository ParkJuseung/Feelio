package com.test.feelio.controller;

import com.test.feelio.entity.ChatMessage;
import com.test.feelio.entity.ChatbotType;
import com.test.feelio.entity.User;
import com.test.feelio.service.ChatLogService;
import com.test.feelio.service.ChatbotTypeService;
import com.test.feelio.service.CustomUserDetailsService;
import com.test.feelio.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatbotTypeService chatbotTypeService;
    private final UserService userService;
    private final ChatLogService chatLogService;

    @GetMapping("/chatbot-choice")
    public String showChatbotChoicePage(Model model, Principal principal) {

        if (principal == null) {
            System.out.println("🔴 Principal is NULL");
            throw new IllegalStateException("로그인된 사용자 정보가 없습니다.");
        }
        System.out.println("🟢 Principal name: " + principal.getName());

        List<ChatbotType> chatbotList = chatbotTypeService.getActiveChatbotTypes();

        Map<Long, String> chatbotClassMap = chatbotList.stream()
                .collect(Collectors.toMap(
                        ChatbotType::getId,
                        bot -> mapTypeNameToClass(bot.getTypeName())
                ));

        model.addAttribute("chatbotList", chatbotList);
        model.addAttribute("chatbotClassMap", chatbotClassMap);
        return "pages/chatbot-choice";
    }

    private String mapTypeNameToClass(String typeName) {
        return switch (typeName) {
            case "포근이" -> "emotional";
            case "단호박" -> "realistic";
            case "햇살이" -> "positive";
            case "장꾸" -> "playful";
            default -> "default";
        };
    }

    @GetMapping("/chatbot")
    public String showChatPage(@RequestParam("chatbotTypeId") Long chatbotTypeId,
                               Model model,
                               Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("로그인된 사용자 정보가 없습니다.");
        }

        Object principal = authentication.getPrincipal();
        User user;

        if (principal instanceof CustomUserDetailsService.CustomUserPrincipal customUser) {
            user = customUser.getUser(); // Form 로그인
        } else if (principal instanceof OAuth2User oauth2User) {
            String email = oauth2User.getAttribute("email");
            user = userService.findByEmail(email); // DB에서 해당 사용자 조회
        } else {
            throw new IllegalStateException("알 수 없는 사용자 타입입니다.");
        }

        Long userId = user.getId();
        ChatbotType chatbot = chatbotTypeService.getChatbotTypeById(chatbotTypeId);
        List<ChatMessage> messages = chatLogService.getMessagesForTodaySession(userId, chatbotTypeId);

        model.addAttribute("chatbot", chatbot);
        model.addAttribute("chatMessages", messages);

        return "pages/chatbot";
    }


}
