package com.test.feelio.repository;

import com.test.feelio.entity.UserEmotionScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserEmotionScoreRepository extends JpaRepository<UserEmotionScore, Long> {
    List<UserEmotionScore> findByUser_IdOrderByScoreDateAsc(Long userId);


}
