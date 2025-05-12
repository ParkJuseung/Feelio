package com.test.feelio.repository;

import com.test.feelio.entity.Emotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmotionRepository extends JpaRepository<Emotion, Long> {
    Optional<Emotion> findByEmotionName(String emotionName);
}
