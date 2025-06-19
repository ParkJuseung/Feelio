package com.test.feelio.repository;

import com.test.feelio.entity.Diary;
import com.test.feelio.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DiaryRepository extends JpaRepository<Diary, Long> {

    // 사용자별 일기 목록 조회 (최신순)
    List<Diary> findByUserOrderByDiaryDateDesc(User user);

    // 사용자별 북마크된 일기 조회
    @Query("SELECT d FROM Diary d WHERE d.user = :user AND d.isBookmarked = true ORDER BY d.diaryDate DESC")
    List<Diary> findBookmarkedDiaries(@Param("user") User user);

    // 특정 날짜의 일기 조회
    List<Diary> findByUserAndDiaryDate(User user, LocalDate date);

    // 사용자별 최근 N개 일기 조회
    @Query(value = "SELECT * FROM DIARIES WHERE USER_ID = :userId ORDER BY DIARY_DATE DESC FETCH FIRST :limit ROWS ONLY", nativeQuery = true)
    List<Diary> findRecentDiaries(@Param("userId") Long userId, @Param("limit") int limit);
}