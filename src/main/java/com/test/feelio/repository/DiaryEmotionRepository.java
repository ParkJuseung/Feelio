package com.test.feelio.repository;

import com.test.feelio.entity.DiaryEmotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

    // DiaryEmotionRepository.java에 다음 메서드 추가
    void deleteByDiaryId(Long diaryId);
}