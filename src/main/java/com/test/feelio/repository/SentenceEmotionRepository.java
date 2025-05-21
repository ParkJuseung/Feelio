package com.test.feelio.repository;

import com.test.feelio.entity.SentenceEmotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SentenceEmotionRepository extends JpaRepository<SentenceEmotion, Long> {
    List<SentenceEmotion> findByDiaryId(Long diaryId);
}