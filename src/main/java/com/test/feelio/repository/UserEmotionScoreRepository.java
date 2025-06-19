package com.test.feelio.repository;

import com.test.feelio.entity.UserEmotionScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserEmotionScoreRepository extends JpaRepository<UserEmotionScore, Long> {

    // 사용자별, 감정별 최신 점수 조회
    Optional<UserEmotionScore> findTopByUserIdAndEmotionIdOrderByScoreDateDesc(
            @Param("userId") Long userId,
            @Param("emotionId") Long emotionId);

    // 특정 날짜의 점수 조회
    Optional<UserEmotionScore> findByUserIdAndEmotionIdAndScoreDate(
            @Param("userId") Long userId,
            @Param("emotionId") Long emotionId,
            @Param("scoreDate") LocalDate scoreDate);

    // 기간별 점수 조회
    List<UserEmotionScore> findByUserIdAndEmotionIdAndScoreDateBetweenOrderByScoreDate(
            @Param("userId") Long userId,
            @Param("emotionId") Long emotionId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // 날짜별 모든 감정 점수 조회
    List<UserEmotionScore> findByUserIdAndScoreDateOrderByEmotionId(
            @Param("userId") Long userId,
            @Param("scoreDate") LocalDate scoreDate);
}