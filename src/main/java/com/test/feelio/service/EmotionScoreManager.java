package com.test.feelio.service;

import com.test.feelio.entity.Emotion;
import com.test.feelio.entity.UserEmotionScore;
import com.test.feelio.entity.User;
import com.test.feelio.repository.EmotionRepository;
import com.test.feelio.repository.UserEmotionScoreRepository;
import com.test.feelio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmotionScoreManager {

    private final UserEmotionScoreRepository userEmotionScoreRepository;
    private final EmotionRepository emotionRepository;
    private final UserRepository userRepository;

    // 감정별 점수 매핑 (고정값)
    private static final Map<String, Double> EMOTION_SCORES = new HashMap<>();

    static {
        EMOTION_SCORES.put("행복", 5.0);
        EMOTION_SCORES.put("만족", 3.0);
        EMOTION_SCORES.put("설렘", 1.0);
        EMOTION_SCORES.put("평온", 0.0);
        EMOTION_SCORES.put("불안", -1.0);
        EMOTION_SCORES.put("슬픔", -3.0);
        EMOTION_SCORES.put("분노", -5.0);
    }

    /**
     * 일기 분석 후 주요 감정에 따라 감정 점수 업데이트
     * @param userId 사용자 ID
     * @param emotionName 감정 이름 (행복, 만족, 설렘, 평온, 불안, 슬픔, 분노)
     */
    @Transactional
    public void updateEmotionScore(Long userId, String emotionName) {
        log.info("감정 점수 업데이트 - 사용자: {}, 감정: {}", userId, emotionName);

        if (!EMOTION_SCORES.containsKey(emotionName)) {
            log.warn("지원하지 않는 감정: {}", emotionName);
            return;
        }

        try {
            // 감정 점수 계산
            double scoreChange = EMOTION_SCORES.get(emotionName);

            // 감정 엔티티 조회
            Emotion emotion = emotionRepository.findByEmotionName(emotionName)
                    .orElseThrow(() -> new RuntimeException("감정을 찾을 수 없습니다: " + emotionName));

            // 사용자 조회
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));

            // 오늘 날짜
            LocalDate today = LocalDate.now();

            // 현재 감정 점수 조회
            Optional<UserEmotionScore> existingScore = userEmotionScoreRepository
                    .findByUserIdAndEmotionIdAndScoreDate(userId, emotion.getId(), today);

            if (existingScore.isPresent()) {
                // 기존 점수가 있으면 덮어쓰기
                UserEmotionScore score = existingScore.get();
                double oldScore = score.getScore();
                score.setScore(scoreChange); // 기존 점수를 변화량으로 대체
                userEmotionScoreRepository.save(score);
                log.info("감정 점수 덮어쓰기: {} -> {}, 감정: {}, 사용자: {}",
                        oldScore, scoreChange, emotionName, userId);
            } else {
                // 새 점수 생성 (이전 점수 무시)
                UserEmotionScore newScoreEntity = UserEmotionScore.builder()
                        .user(user)
                        .emotion(emotion)
                        .score(scoreChange) // 직접 변화량만 저장
                        .scoreDate(today)
                        .build();

                userEmotionScoreRepository.save(newScoreEntity);
                log.info("새 감정 점수 생성: {}, 감정: {}, 사용자: {}",
                        scoreChange, emotionName, userId);
            }
        } catch (Exception e) {
            log.error("감정 점수 업데이트 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("감정 점수 업데이트에 실패했습니다", e);
        }
    }

    /**
     * 모든 감정에 대한 점수를 업데이트
     * @param userId 사용자 ID
     */
    @Transactional
    public void updateAllEmotionScores(Long userId) {
        log.info("모든 감정 점수 초기화 - 사용자: {}", userId);

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));

            LocalDate today = LocalDate.now();

            // 모든 감정에 대해 점수 생성/업데이트
            for (Map.Entry<String, Double> entry : EMOTION_SCORES.entrySet()) {
                String emotionName = entry.getKey();

                Emotion emotion = emotionRepository.findByEmotionName(emotionName)
                        .orElseThrow(() -> new RuntimeException("감정을 찾을 수 없습니다: " + emotionName));

                Optional<UserEmotionScore> existingScore = userEmotionScoreRepository
                        .findByUserIdAndEmotionIdAndScoreDate(userId, emotion.getId(), today);

                if (!existingScore.isPresent()) {
                    // 이전 최신 점수 조회
                    double initialScore = 0.0;
                    Optional<UserEmotionScore> latestScore = userEmotionScoreRepository
                            .findTopByUserIdAndEmotionIdOrderByScoreDateDesc(userId, emotion.getId());

                    if (latestScore.isPresent()) {
                        initialScore = latestScore.get().getScore();
                    }

                    // 새 점수 생성 (기존 점수 유지)
                    UserEmotionScore newScoreEntity = UserEmotionScore.builder()
                            .user(user)
                            .emotion(emotion)
                            .score(initialScore)  // 기존 점수 그대로 유지
                            .scoreDate(today)
                            .build();

                    userEmotionScoreRepository.save(newScoreEntity);
                    log.info("감정 점수 복사: {}, 감정: {}, 사용자: {}", initialScore, emotionName, userId);
                }
            }
        } catch (Exception e) {
            log.error("모든 감정 점수 초기화 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("감정 점수 초기화에 실패했습니다", e);
        }
    }
}