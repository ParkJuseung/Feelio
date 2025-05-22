package com.test.feelio.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.feelio.dto.*;
import com.test.feelio.entity.*;
import com.test.feelio.repository.*;
import lombok.RequiredArgsConstructor;

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
    private final DiaryActivityRepository diaryActivityRepository; // 추가

    //@Value("${openai.api.key}")
    private String openaiApiKey;

    //@Value("${openai.model}")
    private String model;

    public EmotionAnalysisDTO analyzeEmotion(String diaryContent, Long diaryId, Long userId) {
        try {
            // OpenAI API 호출
            String prompt = createAnalysisPrompt(diaryContent);
            String response = callOpenAI(prompt);

            // 응답 파싱
            EmotionAnalysisResult result = parseOpenAIResponse(response);

            // 데이터베이스에 저장
            saveEmotionAnalysis(diaryId, userId, result);

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
        
        2. 문장별 감정 분석: 각 문장의 감정과 활동 유형 분석
        
        3. 사용자 활동 추출: 일기 내용에서 사용자가 실제로 수행한 활동(예: 산책, 독서, 대화, 운동 등)을 구체적으로 추출
        
        4. 긍정적 피드백 메시지 생성: 일기의 내용과 관련된 구체적인 긍정적 피드백 생성
        
        5. 감정-행동 기반 활동 추천: 사용자의 현재 감정 상태를 개선하기 위해 3개 이상의 구체적인 활동 추천
        
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
            "userActivities": [
                {
                    "activity": "친구와 대화",
                    "relatedEmotion": "행복"
                },
                {
                    "activity": "공원 산책",
                    "relatedEmotion": "평온"
                }
            ],
            "positiveFeedback": "오늘은 특별한 하루였네요! 긍정적인 경험이 많았던 것 같습니다.",
            "activityRecommendations": [
                {
                    "activity": "명상",
                    "reason": "불안한 감정을 완화하는데 도움이 될 것입니다."
                },
                {
                    "activity": "가벼운 조깅",
                    "reason": "운동을 통해 스트레스를 해소하고 기분을 전환할 수 있습니다."
                },
                {
                    "activity": "친구와 대화",
                    "reason": "감정을 공유하고 소통함으로써 감정 해소에 도움이 됩니다."
                }
            ]
        }
        """.formatted(diaryContent);
    }

    // DIARY_ACTIVITIES 테이블에 데이터 저장하는 코드 추가
    private void saveDiaryActivities(Long diaryId, EmotionAnalysisResult result) {
        // userActivities 필드 활용
        if (result.getUserActivities() != null && !result.getUserActivities().isEmpty()) {
            log.info("사용자 활동 추출: {} 개, 일기 ID: {}", result.getUserActivities().size(), diaryId);

            for (UserActivity userActivity : result.getUserActivities()) {
                try {
                    // 관련 감정 ID 찾기
                    Emotion emotion = null;
                    if (userActivity.getRelatedEmotion() != null) {
                        emotion = emotionRepository.findByEmotionName(userActivity.getRelatedEmotion())
                                .orElse(null);
                    }

                    // 일기 활동 저장
                    DiaryActivity activity = DiaryActivity.builder()
                            .diary(Diary.builder().id(diaryId).build())
                            .activityName(userActivity.getActivity())
                            .activitySentiment(emotion != null ? 0.0 : null) // 감정 관련 점수
                            .build();

                    diaryActivityRepository.save(activity);
                    log.info("사용자 활동 저장: {}, 관련 감정: {}, 일기 ID: {}",
                            userActivity.getActivity(),
                            userActivity.getRelatedEmotion(),
                            diaryId);
                } catch (Exception e) {
                    log.error("사용자 활동 저장 실패: {}, 일기 ID: {}, 오류: {}",
                            userActivity.getActivity(), diaryId, e.getMessage());
                }
            }
        }
        // userActivities가 없으면 대체 방법으로 sentenceAnalysis에서 활동 유형 추출
        else if (result.getSentenceAnalysis() != null && !result.getSentenceAnalysis().isEmpty()) {
            log.info("문장 분석에서 활동 유형 추출: {} 개 문장, 일기 ID: {}",
                    result.getSentenceAnalysis().size(), diaryId);

            Set<String> activityTypes = result.getSentenceAnalysis().stream()
                    .map(SentenceAnalysis::getActivityType)
                    .filter(type -> type != null && !type.trim().isEmpty())
                    .collect(Collectors.toSet());

            for (String activityType : activityTypes) {
                try {
                    DiaryActivity activity = DiaryActivity.builder()
                            .diary(Diary.builder().id(diaryId).build())
                            .activityName(activityType)
                            .activitySentiment(0.0) // 기본값
                            .build();

                    diaryActivityRepository.save(activity);
                    log.info("활동 유형 저장: {}, 일기 ID: {}", activityType, diaryId);
                } catch (Exception e) {
                    log.error("활동 유형 저장 실패: {}, 일기 ID: {}, 오류: {}",
                            activityType, diaryId, e.getMessage());
                }
            }
        } else {
            log.warn("사용자 활동을 추출할 수 없습니다. 일기 ID: {}", diaryId);
        }
    }

    // 추천 활동 저장 메서드 - ACTIVITY_EFFECTS 테이블에 초기 데이터 생성
    private void saveActivityRecommendations(Long diaryId, Long userId, EmotionAnalysisResult result) {
        if (result.getActivityRecommendations() == null || result.getActivityRecommendations().isEmpty()) {
            log.warn("추천 활동이 없습니다. 일기 ID: {}", diaryId);
            return;
        }

        // 주요 감정 ID 찾기
        EmotionPercentage primaryEmotion = result.getOverallEmotions().stream()
                .max(Comparator.comparing(EmotionPercentage::getPercentage))
                .orElse(null);

        if (primaryEmotion == null) {
            log.warn("주요 감정을 찾을 수 없습니다. 일기 ID: {}", diaryId);
            return;
        }

        String primaryEmotionName = primaryEmotion.getEmotion();
        Emotion emotion = emotionRepository.findByEmotionName(primaryEmotionName)
                .orElse(null);

        if (emotion == null) {
            log.warn("주요 감정을 DB에서 찾을 수 없습니다: {}, 일기 ID: {}", primaryEmotionName, diaryId);
            return;
        }

        // 각 추천 활동에 대해 ACTIVITY_EFFECTS 테이블에 초기 데이터 생성
        for (ActivityRecommendation rec : result.getActivityRecommendations()) {
            try {
                // 이미 존재하는지 확인
                Optional<ActivityEffect> existingEffect =
                        activityEffectRepository.findByEmotionIdAndActivityName(emotion.getId(), rec.getActivity());

                if (existingEffect.isPresent()) {
                    // 이미 존재하면 카운트만 증가
                    ActivityEffect effect = existingEffect.get();
                    effect.setUsageCount(effect.getUsageCount() + 1);
                    activityEffectRepository.save(effect);
                    log.info("활동 효과 업데이트: {}, 감정: {}, 사용 횟수: {}",
                            rec.getActivity(), emotion.getEmotionName(), effect.getUsageCount());
                } else {
                    // 새로 생성
                    ActivityEffect effect = ActivityEffect.builder()
                            .emotion(emotion)
                            .activityName(rec.getActivity())
                            .averageChangeScore(0.0) // 초기값
                            .usageCount(1)
                            .positiveEffectCount(0)
                            .build();

                    activityEffectRepository.save(effect);
                    log.info("활동 효과 생성: {}, 감정: {}", rec.getActivity(), emotion.getEmotionName());
                }
            } catch (Exception e) {
                log.error("활동 효과 저장 실패: {}, 감정: {}, 오류: {}",
                        rec.getActivity(), emotion.getEmotionName(), e.getMessage());
            }
        }
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
            log.debug("OpenAI 원본 응답: {}", response);
            ObjectMapper mapper = new ObjectMapper();
            EmotionAnalysisResult result = mapper.readValue(response, EmotionAnalysisResult.class);

            // 파싱 결과 확인 - 상세 로그 추가
            log.info("감정 분석 결과:");
            log.info("- 감정: {} 개", result.getOverallEmotions() != null ? result.getOverallEmotions().size() : 0);

            if (result.getOverallEmotions() != null) {
                for (EmotionPercentage ep : result.getOverallEmotions()) {
                    log.info("  * {}: {}%", ep.getEmotion(), ep.getPercentage());
                }
            }

            log.info("- 문장 분석: {} 개", result.getSentenceAnalysis() != null ? result.getSentenceAnalysis().size() : 0);
            log.info("- 긍정적 피드백: {}", result.getPositiveFeedback());
            log.info("- 활동 추천: {} 개", result.getActivityRecommendations() != null ? result.getActivityRecommendations().size() : 0);

            if (result.getActivityRecommendations() != null) {
                for (ActivityRecommendation rec : result.getActivityRecommendations()) {
                    log.info("  * 활동: {}, 이유: {}", rec.getActivity(), rec.getReason());
                }
            }

            // null 체크 및 기본값 설정
            if (result.getPositiveFeedback() == null) {
                log.warn("긍정적 피드백이 null입니다. 기본값을 설정합니다.");
                result.setPositiveFeedback("오늘 하루도 수고하셨습니다. 내일은 더 좋은 날이 될 거예요.");
            }

            if (result.getActivityRecommendations() == null || result.getActivityRecommendations().isEmpty()) {
                log.warn("활동 추천이 null 또는 비어있습니다. 기본 추천을 설정합니다.");
                List<ActivityRecommendation> defaultRecs = new ArrayList<>();
                defaultRecs.add(new ActivityRecommendation("가벼운 산책", "신선한 공기를 마시며 생각을 정리하는데 도움이 됩니다."));
                defaultRecs.add(new ActivityRecommendation("좋아하는 음악 듣기", "마음을 편안하게 하고 감정을 표현하는데 도움이 됩니다."));
                defaultRecs.add(new ActivityRecommendation("일기 쓰기", "감정을 정리하고 자신을 되돌아보는데 도움이 됩니다."));
                result.setActivityRecommendations(defaultRecs);
            }

            return result;
        } catch (Exception e) {
            log.error("OpenAI 응답 파싱 실패: ", e);
            throw new RuntimeException("감정 분석 결과 파싱에 실패했습니다", e);
        }
    }

    private void saveEmotionAnalysis(Long diaryId, Long userId, EmotionAnalysisResult result) {
        // 지원하는 기본 감정 목록
        List<String> supportedEmotions = List.of("행복", "만족", "설렘", "평온", "불안", "슬픔", "분노");
        String defaultEmotion = "평온";  // 기본 대체 감정

        // 대표 감정 설정 확인
        EmotionPercentage primaryEmotion = null;
        if (result.getOverallEmotions() != null && !result.getOverallEmotions().isEmpty()) {
            primaryEmotion = result.getOverallEmotions().stream()
                    .max(Comparator.comparing(EmotionPercentage::getPercentage))
                    .orElse(null);
        }

        Emotion primaryEmotionEntity = null;
        if (primaryEmotion != null) {
            primaryEmotionEntity = emotionRepository.findByEmotionName(primaryEmotion.getEmotion()).orElse(null);
        }

        // 전체 감정 분석 결과 저장
        for (EmotionPercentage ep : result.getOverallEmotions()) {
            // 감정이 지원되는 감정 목록에 없는 경우 대체 감정 사용
            String emotionName = supportedEmotions.contains(ep.getEmotion()) ?
                    ep.getEmotion() : defaultEmotion;

            try {
                Emotion emotion = emotionRepository.findByEmotionName(emotionName)
                        .orElseGet(() -> {
                            // 로그 남기기
                            log.warn("감정을 찾을 수 없어 기본 감정으로 대체합니다. 원본: {}, 대체: {}",
                                    ep.getEmotion(), defaultEmotion);
                            return emotionRepository.findByEmotionName(defaultEmotion)
                                    .orElseThrow(() -> new RuntimeException("기본 감정도 찾을 수 없습니다: " + defaultEmotion));
                        });

                // 대표 감정 여부 결정 - primaryEmotion과 동일한 객체인지 확인
                boolean isPrimaryEmotion = (primaryEmotion != null &&
                        primaryEmotion.getEmotion().equals(ep.getEmotion()) &&
                        Math.abs(primaryEmotion.getPercentage() - ep.getPercentage()) < 0.001);

                DiaryEmotion diaryEmotion = DiaryEmotion.builder()
                        .diary(Diary.builder().id(diaryId).build())
                        .emotion(emotion)
                        .emotionPercentage(ep.getPercentage())
                        .isPrimaryEmotion(isPrimaryEmotion) // 대표 감정 여부 확인
                        .build();

                diaryEmotionRepository.save(diaryEmotion);
                log.info("감정 저장 완료: {}, 비율: {}%, 대표 감정: {}",
                        emotion.getEmotionName(), ep.getPercentage(), isPrimaryEmotion);
            } catch (Exception e) {
                log.error("감정 저장 중 오류 발생: {}", e.getMessage());
                // 한 감정 저장 실패가 전체 프로세스를 중단시키지 않도록 예외 처리
            }
        }

        // 문장별 감정 분석 결과 저장
        for (SentenceAnalysis sa : result.getSentenceAnalysis()) {
            // 감정이 지원되는 감정 목록에 없는 경우 기본 감정 사용
            String emotionName = supportedEmotions.contains(sa.getEmotion()) ?
                    sa.getEmotion() : defaultEmotion;

            try {
                Emotion emotion = emotionRepository.findByEmotionName(emotionName)
                        .orElseGet(() -> {
                            // 로그 남기기
                            log.warn("문장 감정을 찾을 수 없어 기본 감정으로 대체합니다. 원본: {}, 대체: {}",
                                    sa.getEmotion(), defaultEmotion);
                            return emotionRepository.findByEmotionName(defaultEmotion)
                                    .orElseThrow(() -> new RuntimeException("기본 감정도 찾을 수 없습니다: " + defaultEmotion));
                        });

                SentenceEmotion sentenceEmotion = SentenceEmotion.builder()
                        .diary(Diary.builder().id(diaryId).build())
                        .sentenceText(sa.getSentence())
                        .emotion(emotion)
                        .activityType(sa.getActivityType())
                        .build();

                sentenceEmotionRepository.save(sentenceEmotion);
            } catch (Exception e) {
                log.error("문장 감정 저장 중 오류 발생: {}", e.getMessage());
                // 한 문장 감정 저장 실패가 전체 프로세스를 중단시키지 않도록 예외 처리
            }
        }

        // 피드백 템플릿 저장 (일기 ID도 함께 전달)
        if (primaryEmotionEntity != null && result.getPositiveFeedback() != null) {
            saveFeedbackTemplate(result.getPositiveFeedback(), primaryEmotionEntity.getId(), diaryId);
        }

        // 일기 활동 데이터 저장 (일기에서 추출한 활동)
        saveDiaryActivities(diaryId, result);

        // 활동 추천 데이터 저장 (AI가 추천하는 활동)
        saveActivityRecommendations(diaryId, userId, result);
    }


    private List<EmotionPercentageDTO> normalizeEmotionPercentages(List<EmotionPercentageDTO> emotions) {
        // 총합 계산
        double total = emotions.stream()
                .mapToDouble(EmotionPercentageDTO::getPercentage)
                .sum();

        // 총합이 0이거나 100에 가까우면 조정하지 않음
        if (total == 0 || Math.abs(total - 100.0) < 1.0) {
            return emotions;
        }

        // 비율에 맞게 조정
        return emotions.stream()
                .map(emotion -> {
                    double normalizedPercentage = (emotion.getPercentage() / total) * 100.0;
                    // 소수점 첫째자리까지 반올림
                    normalizedPercentage = Math.round(normalizedPercentage * 10) / 10.0;

                    return EmotionPercentageDTO.builder()
                            .emotionId(emotion.getEmotionId())
                            .emotionName(emotion.getEmotionName())
                            .percentage(normalizedPercentage)
                            .emotionColor(emotion.getEmotionColor())
                            .emotionEmoji(emotion.getEmotionEmoji())
                            .isPrimary(emotion.isPrimary())
                            .build();
                })
                .collect(Collectors.toList());
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

        // 감정 백분율 정규화
        emotions = normalizeEmotionPercentages(emotions);

        // 문장별 감정 변환
        List<SentenceEmotionDTO> sentenceEmotions = new ArrayList<>();
        if (result.getSentenceAnalysis() != null) {
            sentenceEmotions = result.getSentenceAnalysis().stream()
                    .map(this::toSentenceEmotionDTO)
                    .collect(Collectors.toList());
        }

        // 긍정적 피드백 문자열 직접 사용
        String feedbackText = result.getPositiveFeedback();
        log.info("OpenAI 피드백 원본: {}", feedbackText);

        // 피드백이 null이면 기본값 설정
        if (feedbackText == null || feedbackText.trim().isEmpty()) {
            feedbackText = "오늘 하루도 고생 많으셨습니다. 내일은 더 좋은 하루가 될 거예요.";
            log.info("피드백 기본값 설정: {}", feedbackText);
        }

        // 활동 추천 DTO 생성
        List<ActivityRecommendationDTO> recommendations = new ArrayList<>();
        if (result.getActivityRecommendations() != null) {
            recommendations = result.getActivityRecommendations().stream()
                    .map(this::toActivityRecommendationDTO)
                    .collect(Collectors.toList());

            log.info("활동 추천 수: {} 개", recommendations.size());
        }

        // DTO 생성 및 반환
        EmotionAnalysisDTO dto = EmotionAnalysisDTO.builder()
                .primaryEmotion(primaryEmotion != null ? toEmotionDTO(primaryEmotion) : null)
                .emotions(emotions)
                .sentenceEmotions(sentenceEmotions)
                .positiveFeedback(feedbackText)  // 문자열 직접 사용
                .activityRecommendations(recommendations)
                .build();

        return dto;
    }

    private void saveFeedbackTemplate(String feedback, Long emotionId, Long diaryId) {
        if (feedback == null || feedback.trim().isEmpty() || emotionId == null) {
            log.warn("피드백 또는 감정 ID가 null이어서 템플릿을 저장할 수 없습니다.");
            return;
        }

        try {
            // 동일한 피드백이 이미 존재하는지 확인
            boolean exists = feedbackTemplateRepository.existsByFeedbackTextAndEmotionId(feedback, emotionId);

            if (!exists) {
                FeedbackTemplate template = FeedbackTemplate.builder()
                        .emotion(Emotion.builder().id(emotionId).build())
                        .feedbackText(feedback)
                        .isPositive(true)
                        .diary(Diary.builder().id(diaryId).build())  // 일기 ID 설정
                        .build();

                feedbackTemplateRepository.save(template);
                // ... 로그 등
            }
        } catch (Exception e) {
            log.error("피드백 템플릿 저장 실패: '{}', 감정 ID: {}, 오류: {}",
                    feedback, emotionId, e.getMessage());
        }
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
