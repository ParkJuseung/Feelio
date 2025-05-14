package com.test.feelio.repository;

import com.test.feelio.entity.RecommendedMusic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecommendedMusicRepository extends JpaRepository<RecommendedMusic, Long> {

    List<RecommendedMusic> findByEmotionId(Long emotionId);

    @Query(value = "SELECT * FROM RECOMMENDED_MUSIC WHERE EMOTION_ID = :emotionId ORDER BY DBMS_RANDOM.VALUE FETCH FIRST :limit ROWS ONLY",
            nativeQuery = true)
    List<RecommendedMusic> findRandomMusicByEmotion(@Param("emotionId") Long emotionId, @Param("limit") int limit);
}