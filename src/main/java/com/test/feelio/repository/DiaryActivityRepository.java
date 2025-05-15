package com.test.feelio.repository;

import com.test.feelio.entity.DiaryActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiaryActivityRepository extends JpaRepository<DiaryActivity, Long> {
    List<DiaryActivity> findByDiaryId(Long diaryId);
}