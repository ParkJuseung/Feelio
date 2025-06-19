package com.test.feelio.repository;

import com.test.feelio.entity.ChatbotType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatbotTypeRepository extends JpaRepository<ChatbotType, Long> {

    //활성화된 챗봇만 조회
    List<ChatbotType> findByActiveTrue(); //active = true인 챗봇만 반환

    //챗봇 이름으로 조회(선택 사항, 추후 필요할 수도있음)
    ChatbotType findByTypeName(String typeName);//TYPE_NAME 값으로 챗봇을 조회
}
