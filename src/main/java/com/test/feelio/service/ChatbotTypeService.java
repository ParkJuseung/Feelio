package com.test.feelio.service;

import com.test.feelio.entity.ChatbotType;
import com.test.feelio.repository.ChatbotTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatbotTypeService {

    private final ChatbotTypeRepository chatbotTypeRepository;

    //활성화된 챗봇 목록 조회
    public List<ChatbotType> getActiveChatbotTypes() {
        return chatbotTypeRepository.findByActiveTrue();
    }

    //ID로 챗봇 유형 상세 조회
    public ChatbotType getChatbotTypeById(Long id) {

        return chatbotTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 챗봇 유형이 존재하지 않습니다. ID=" + id + ""));
    }
}
