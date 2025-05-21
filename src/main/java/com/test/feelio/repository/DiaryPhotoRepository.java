package com.test.feelio.repository;

import com.test.feelio.entity.DiaryPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiaryPhotoRepository extends JpaRepository<DiaryPhoto, Long> {
    List<DiaryPhoto> findByDiaryId(Long diaryId);
}