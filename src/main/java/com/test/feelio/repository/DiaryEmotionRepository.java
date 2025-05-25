package com.test.feelio.repository;

import com.test.feelio.dto.DiaryCalendarDTO;
import com.test.feelio.entity.DiaryEmotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiaryEmotionRepository extends JpaRepository<DiaryEmotion, Long> {

    List<DiaryEmotion> findByDiaryId(Long diaryId);

    Optional<DiaryEmotion> findByDiaryIdAndIsPrimaryEmotion(Long diaryId, boolean isPrimary);

    @Query("SELECT de FROM DiaryEmotion de WHERE de.diary.user.id = :userId AND de.diary.diaryDate BETWEEN :startDate AND :endDate")
    List<DiaryEmotion> findEmotionsByUserAndDateRange(@Param("userId") Long userId,
                                                      @Param("startDate") java.time.LocalDate startDate,
                                                      @Param("endDate") java.time.LocalDate endDate);

    @Query(value = "SELECT d.diary_id AS diaryId, d.diary_date AS diaryDate, e.emotion_emoji AS emotionEmoji " +
            "FROM diary_emotions de " +
            "JOIN diaries d ON de.diary_id = d.diary_id " +
            "JOIN emotions e ON de.emotion_id = e.emotion_id " +
            "WHERE d.user_id = :userId " +
            "AND de.is_primary_emotion = 1 " +
            "AND d.diary_date BETWEEN :startDate AND :endDate", nativeQuery = true)
    List<Object[]> findRawEmotions(@Param("userId") Long userId,
                                   @Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate);

    @Query("SELECT new com.test.feelio.dto.DiaryCalendarDTO(d.id, d.diaryDate, e.emotionEmoji) " +
            "FROM DiaryEmotion de " +
            "JOIN de.diary d " +
            "JOIN de.emotion e " +
            "WHERE d.user.id = :userId " +
            "AND de.isPrimaryEmotion = true " +
            "AND FUNCTION('TO_CHAR', d.diaryDate, 'YYYY-MM') = :yearMonth")
    List<DiaryCalendarDTO> findCalendarEmotions(@Param("userId") Long userId,
                                                @Param("yearMonth") String yearMonth);

    @Query("SELECT d.diaryDate, e.emotionEmoji " +
            "FROM DiaryEmotion de " +
            "JOIN de.diary d " +
            "JOIN de.emotion e " +
            "WHERE d.user.id = :userId " +
            "AND de.isPrimaryEmotion = true " +
            "AND FUNCTION('TO_CHAR', d.diaryDate, 'YYYY') = :year")
    List<Object[]> findYearlyEmotions(@Param("userId") Long userId,
                                      @Param("year") String year);

    // 연간 데이터용 - diaryId 포함
    @Query("SELECT d.id, d.diaryDate, e.emotionEmoji " +
            "FROM DiaryEmotion de " +
            "JOIN de.diary d " +
            "JOIN de.emotion e " +
            "WHERE d.user.id = :userId " +
            "AND de.isPrimaryEmotion = true " +
            "AND FUNCTION('TO_CHAR', d.diaryDate, 'YYYY') = :year")
    List<Object[]> findYearlyEmotionsWithDiaryId(@Param("userId") Long userId,
                                                 @Param("year") String year);

    @Query(value = """
    SELECT d.DIARY_ID, d.DIARY_DATE, e.EMOTION_EMOJI
    FROM DIARIES d 
    JOIN DIARY_EMOTIONS de ON d.DIARY_ID = de.DIARY_ID 
    JOIN EMOTIONS e ON de.EMOTION_ID = e.EMOTION_ID 
    WHERE d.USER_ID = :userId 
    AND d.DIARY_DATE >= :startDate 
    AND d.DIARY_DATE <= :endDate
    AND de.IS_PRIMARY_EMOTION = 1
    ORDER BY d.DIARY_DATE ASC
    """, nativeQuery = true)
    List<Object[]> findCalendarEmotionsByDateRange(@Param("userId") Long userId,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);

    // DiaryEmotionRepository.java에 다음 메서드 추가
    void deleteByDiaryId(Long diaryId);
}