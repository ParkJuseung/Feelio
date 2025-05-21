package com.test.feelio.repository;

import com.test.feelio.entity.FeedbackTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FeedbackTemplateRepository extends JpaRepository<FeedbackTemplate, Long> {
    // 1. JPA에서 지원하는 방식으로 랜덤 정렬
    @Query("SELECT ft FROM FeedbackTemplate ft WHERE ft.emotion.id = :emotionId AND ft.isPositive = true ORDER BY FUNCTION('RAND')")
    Optional<FeedbackTemplate> findRandomPositiveFeedbackByEmotion(@Param("emotionId") Long emotionId);

    boolean existsByFeedbackTextAndEmotionId(String feedbackText, Long emotionId);

    @Query("SELECT ft FROM FeedbackTemplate ft WHERE ft.diary.id = :diaryId")
    Optional<FeedbackTemplate> findByDiaryId(@Param("diaryId") Long diaryId);
}