package com.test.feelio.controller;

import com.test.feelio.dto.EmotionTrendResponse;
import com.test.feelio.dto.PeriodType;
import com.test.feelio.entity.Diary;
import com.test.feelio.repository.DiaryRepository;
import com.test.feelio.service.EmotionTrendService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class EmotionTrendController {

    private static final Logger log = LoggerFactory.getLogger(EmotionTrendController.class);

    private final EmotionTrendService trendService;
    private final DiaryRepository diaryRepository;

    // 감정 통계 페이지 반환
    @GetMapping("/chart")
    public String showChartPage() {
        return "pages/chart";
    }

    // 감정 트렌드 JSON API
    @GetMapping("/api/emotion-trend")
    @ResponseBody
    public EmotionTrendResponse getEmotionTrend(
            @RequestParam Long userId,
            @RequestParam PeriodType period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate
    ) {
        return trendService.getTrend(userId, period, startDate);
    }

    // 일기 상세 API
    @GetMapping("/api/diary-detail")
    public ResponseEntity<?> getDiaryByDate(@RequestParam Long userId, @RequestParam String date) {
        try {
            Optional<Diary> diaryOpt = diaryRepository.findByUserIdAndDate(userId, LocalDate.parse(date));

            if (diaryOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Diary diary = diaryOpt.get();
            return ResponseEntity.ok(diary);

        } catch (Exception e) {
            log.error("일기 조회 실패: userId={}, date={}, error={}", userId, date, e.getMessage());
            return ResponseEntity.status(500).body("서버 오류로 인해 일기를 불러올 수 없습니다.");
        }
    }
}
