package com.test.feelio.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.feelio.dto.*;
import com.test.feelio.entity.*;
import com.test.feelio.repository.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmotionAnalysisService {

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.openai.com")
            .build();

    private final EmotionRepository emotionRepository;
    private final DiaryEmotionRepository diaryEmotionRepository;
    private final SentenceEmotionRepository sentenceEmotionRepository;
    private final FeedbackTemplateRepository feedbackTemplateRepository;
    private final ActivityEffectRepository activityEffectRepository;

    @Value("${openai.api.key}")
    private String openaiApiKey;

    @Value("${openai.model}")
    private String model;

    public EmotionAnalysisDTO analyzeEmotion(String diaryContent, Long diaryId) {
        try {
            // OpenAI API 호출
            String prompt = createAnalysisPrompt(diaryContent);
            String response = callOpenAI(prompt);

            // 응답 파싱
            EmotionAnalysisResult result = parseOpenAIResponse(response);

            // 데이터베이스에 저장
            saveEmotionAnalysis(diaryId, result);

            // DTO 변환 후 반환
            return convertToDTO(result);

        } catch (Exception e) {
            log.error("감정 분석 중 오류 발생: ", e);
            throw new RuntimeException("감정 분석에 실패했습니다", e);
        }
    }

    private String createAnalysisPrompt(String diaryContent) {
        return """
            다음 일기 내용을 분석해주세요.
            
            일기 내용:
            %s
            
            다음 작업을 수행해주세요:
            1. 전체 감정 분석: 행복, 만족, 설렘, 평온, 불안, 슬픔, 분노 중에서 감정 비율(%%)을 계산
            2. 문장별 감정 분석: 각 문장의 감정과 행동 유형 분석
            3. 긍정적 피드백 메시지 생성
            4. 감정-행동 기반 활동 추천 제공
            
            응답 형식 (JSON):
            {
                "overallEmotions": [
                    {"emotion": "행복", "percentage": 35.5},
                    {"emotion": "불안", "percentage": 25.0}
                ],
                "sentenceAnalysis": [
                    {
                        "sentence": "오늘은 좋은 일이 있었다.",
                        "emotion": "행복",
                        "activityType": "긍정적 경험"
                    }
                ],
                "positiveFeedback": "오늘은 특별한 하루였네요! 긍정적인 경험이 많았던 것 같습니다.",
                "activityRecommendations": [
                    {
                        "activity": "명상",
                        "reason": "불안한 감정을 완화하는데 도움이 될 것입니다."
                    }
                ]
            }
            """.formatted(diaryContent);
    }

    private String callOpenAI(String prompt) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("model", model);
            request.put("messages", List.of(
                    Map.of("role", "system", "content", "당신은 감정 분석 전문가입니다."),
                    Map.of("role", "user", "content", prompt)
            ));
            request.put("temperature", 0.7);
            request.put("max_tokens", 1000);

            String response = webClient.post()
                    .uri("/v1/chat/completions")
                    .header("Authorization", "Bearer " + openaiApiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> responseMap = mapper.readValue(response, Map.class);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");

            if (choices != null && !choices.isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return (String) message.get("content");
            }

            throw new RuntimeException("OpenAI 응답이 비어있습니다");

        } catch (Exception e) {
            log.error("OpenAI API 호출 실패: ", e);
            throw new RuntimeException("OpenAI API 호출에 실패했습니다", e);
        }
    }

    private EmotionAnalysisResult parseOpenAIResponse(String response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(response, EmotionAnalysisResult.class);
        } catch (Exception e) {
            log.error("OpenAI 응답 파싱 실패: ", e);
            throw new RuntimeException("감정 분석 결과 파싱에 실패했습니다", e);
        }
    }

    private void saveEmotionAnalysis(Long diaryId, EmotionAnalysisResult result) {
        // 전체 감정 분석 결과 저장
        for (EmotionPercentage ep : result.getOverallEmotions()) {
            Emotion emotion = emotionRepository.findByEmotionName(ep.getEmotion())
                    .orElseThrow(() -> new RuntimeException("감정을 찾을 수 없습니다: " + ep.getEmotion()));

            DiaryEmotion diaryEmotion = DiaryEmotion.builder()
                    .diary(Diary.builder().id(diaryId).build())
                    .emotion(emotion)
                    .emotionPercentage(ep.getPercentage())
                    .isPrimaryEmotion(ep.isPrimary())
                    .build();

            diaryEmotionRepository.save(diaryEmotion);
        }

        // 문장별 감정 분석 결과 저장
        for (SentenceAnalysis sa : result.getSentenceAnalysis()) {
            Emotion emotion = emotionRepository.findByEmotionName(sa.getEmotion())
                    .orElseThrow(() -> new RuntimeException("감정을 찾을 수 없습니다: " + sa.getEmotion()));

            SentenceEmotion sentenceEmotion = SentenceEmotion.builder()
                    .diary(Diary.builder().id(diaryId).build())
                    .sentenceText(sa.getSentence())
                    .emotion(emotion)
                    .activityType(sa.getActivityType())
                    .build();

            sentenceEmotionRepository.save(sentenceEmotion);
        }
    }

    private EmotionAnalysisDTO convertToDTO(EmotionAnalysisResult result) {
        // 대표 감정 찾기
        EmotionPercentage primaryEmotion = result.getOverallEmotions().stream()
                .max(Comparator.comparing(EmotionPercentage::getPercentage))
                .orElse(null);

        // 감정 리스트 변환
        List<EmotionPercentageDTO> emotions = result.getOverallEmotions().stream()
                .map(this::toEmotionPercentageDTO)
                .collect(Collectors.toList());

        // 문장별 감정 변환
        List<SentenceEmotionDTO> sentenceEmotions = result.getSentenceAnalysis().stream()
                .map(this::toSentenceEmotionDTO)
                .collect(Collectors.toList());

        // 긍정적 피드백 DTO 생성
        PositiveFeedbackDTO feedback = PositiveFeedbackDTO.builder()
                .message(result.getPositiveFeedback())
                .build();

        // 활동 추천 DTO 생성
        List<ActivityRecommendationDTO> recommendations = result.getActivityRecommendations().stream()
                .map(this::toActivityRecommendationDTO)
                .collect(Collectors.toList());

        return EmotionAnalysisDTO.builder()
                .primaryEmotion(primaryEmotion != null ? toEmotionDTO(primaryEmotion) : null)
                .emotions(emotions)
                .sentenceEmotions(sentenceEmotions)
                .positiveFeedback(feedback)
                .activityRecommendations(recommendations)
                .build();
    }

    // Helper 메서드들...
    private EmotionPercentageDTO toEmotionPercentageDTO(EmotionPercentage ep) {
        Emotion emotion = emotionRepository.findByEmotionName(ep.getEmotion()).orElse(null);

        return EmotionPercentageDTO.builder()
                .emotionId(emotion != null ? emotion.getId() : null)
                .emotionName(ep.getEmotion())
                .percentage(ep.getPercentage())
                .emotionColor(emotion != null ? emotion.getEmotionColor() : null)
                .emotionEmoji(emotion != null ? emotion.getEmotionEmoji() : null)
                .isPrimary(ep.isPrimary())
                .build();
    }

    private SentenceEmotionDTO toSentenceEmotionDTO(SentenceAnalysis sa) {
        return SentenceEmotionDTO.builder()
                .sentence(sa.getSentence())
                .emotion(EmotionDTO.builder().emotionName(sa.getEmotion()).build())
                .activityType(sa.getActivityType())
                .build();
    }

    private EmotionDTO toEmotionDTO(EmotionPercentage ep) {
        Emotion emotion = emotionRepository.findByEmotionName(ep.getEmotion()).orElse(null);

        return EmotionDTO.builder()
                .id(emotion != null ? emotion.getId() : null)
                .emotionName(ep.getEmotion())
                .emotionColor(emotion != null ? emotion.getEmotionColor() : null)
                .emotionEmoji(emotion != null ? emotion.getEmotionEmoji() : null)
                .build();
    }

    private ActivityRecommendationDTO toActivityRecommendationDTO(ActivityRecommendation ar) {
        return ActivityRecommendationDTO.builder()
                .activity(ar.getActivity())
                .reason(ar.getReason())
                .emotionChangeScore(calculateEmotionChangeScore(ar.getActivity()))
                .build();
    }

    private Double calculateEmotionChangeScore(String activity) {
        // 실제로는 데이터베이스에서 이 활동의 평균 효과성을 조회
        return activityEffectRepository.findAverageChangeScore(activity).orElse(0.0);
    }
}
