package com.test.feelio.repository;

import com.test.feelio.entity.Diary;
import com.test.feelio.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiaryRepository extends JpaRepository<Diary, Long> {


    // 사용자별 일기 목록 조회 (최신순)
    List<Diary> findByUserOrderByDiaryDateDesc(User user);

    // 날짜 조회
    Optional<Diary> findByUser_IdAndDiaryDate(Long userId, LocalDate date);

    // 사용자별 북마크된 일기 조회
    @Query("SELECT d FROM Diary d WHERE d.user = :user AND d.isBookmarked = true ORDER BY d.diaryDate DESC")
    List<Diary> findBookmarkedDiaries(@Param("user") User user);

    // 특정 날짜의 일기 조회
    List<Diary> findByUserAndDiaryDate(User user, LocalDate date);

    // 사용자별 최근 N개 일기 조회
    @Query(value = "SELECT * FROM DIARIES WHERE USER_ID = :userId ORDER BY DIARY_DATE DESC FETCH FIRST :limit ROWS ONLY", nativeQuery = true)
    List<Diary> findRecentDiaries(@Param("userId") Long userId, @Param("limit") int limit);

    // 해당 유저의 특정 달 전체 일기
    @Query("SELECT d FROM Diary d WHERE d.user.id = :userId AND FUNCTION('TO_CHAR', d.diaryDate, 'YYYY-MM') = :yearMonth")
    List<Diary> findAllByUserIdAndYearMonth(Long userId, String yearMonth);

    // 해당 유저의 특정 연도 전체 일기
    @Query("SELECT d FROM Diary d WHERE d.user.id = :userId AND FUNCTION('TO_CHAR', d.diaryDate, 'YYYY') = :year")
    List<Diary> findAllByUserIdAndYear(Long userId, String year);

    Diary findByUserIdAndDiaryDate(Long userId, LocalDate date);

    @Query("SELECT d FROM Diary d WHERE d.user.id = :userId AND d.diaryDate = :date")
    Optional<Diary> findByUserIdAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);


    @Query("SELECT d FROM Diary d WHERE d.user.id = :userId AND d.diaryDate BETWEEN :start AND :end")
    Optional<Diary> findDiaryOnDate(@Param("userId") Long userId,
                                    @Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end);


}