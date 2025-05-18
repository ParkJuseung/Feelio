package com.test.feelio.repository;

import com.test.feelio.dto.DiaryGalleryDTO;
import com.test.feelio.entity.DiaryPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GalleryRepository extends JpaRepository<DiaryPhoto,Long> {
    @Query(value = """
        SELECT 
            TO_CHAR(d.DIARY_DATE, 'YYYY-MM') AS yearMonth,
            p.PHOTO_URL AS photoUrl,
            d.DIARY_ID AS diaryId
        FROM DIARIES d
        JOIN DIARY_PHOTOS p ON d.DIARY_ID = p.DIARY_ID
        WHERE d.USER_ID = :userId
        ORDER BY yearMonth DESC
        """, nativeQuery = true)
    List<DiaryGalleryDTO> findGalleryPhotosByUser(@Param("userId") Long userId);
}
