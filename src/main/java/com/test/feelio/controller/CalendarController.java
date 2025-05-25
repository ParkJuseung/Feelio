package com.test.feelio.controller;

import com.test.feelio.entity.User;
import com.test.feelio.repository.DiaryEmotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class CalendarController {

    private final DiaryEmotionRepository diaryEmotionRepository;

    @GetMapping("/calendar")
    public String calendarView(HttpSession session,
                               @RequestParam(required = false, defaultValue = "monthly") String mode,
                               @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth,
                               Model model) {

        // 현재 월 없으면 오늘 기준
        if (yearMonth == null) {
            yearMonth = YearMonth.now();
        }

        // 세션에서 유저 정보 꺼내기
        User user = (User) session.getAttribute("user");
        if (user == null) {
            throw new RuntimeException("사용자 없음"); // 로그인 안 된 상태 처리
        }

        Long userId = user.getId();

        model.addAttribute("year", yearMonth.getYear());
        model.addAttribute("month", yearMonth.getMonthValue());
        model.addAttribute("mode", mode);

        if ("monthly".equals(mode)) {
            LocalDate startDate = yearMonth.atDay(1);
            LocalDate endDate = yearMonth.atEndOfMonth();

            List<Object[]> emotionData = diaryEmotionRepository.findCalendarEmotionsByDateRange(userId, startDate, endDate);

            Map<String, String> emotionMap = new HashMap<>();
            Map<String, Long> diaryIdMap = new HashMap<>();
            DateTimeFormatter keyFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            System.out.println("===== 월간 감정 데이터 =====");
            System.out.println("사용자 ID: " + userId);
            System.out.println("조회 범위: " + startDate + " ~ " + endDate);
            System.out.println("조회된 감정 수: " + emotionData.size());

            for (Object[] row : emotionData) {
                Long diaryId = ((Number) row[0]).longValue();
                // java.sql.Date를 LocalDate로 변환
                LocalDate diaryDate;
                if (row[1] instanceof java.sql.Date) {
                    diaryDate = ((java.sql.Date) row[1]).toLocalDate();
                } else if (row[1] instanceof java.sql.Timestamp) {
                    diaryDate = ((java.sql.Timestamp) row[1]).toLocalDateTime().toLocalDate();
                } else {
                    // 이미 LocalDate인 경우
                    diaryDate = (LocalDate) row[1];
                }
                String emotionEmoji = (String) row[2];

                String key = diaryDate.format(keyFormatter);
                if (!emotionMap.containsKey(key)) {
                    // emotionEmoji가 full path인지 확인하고, 파일명만 추출
                    String fileName = emotionEmoji;
                    if (emotionEmoji.contains("/")) {
                        fileName = emotionEmoji.substring(emotionEmoji.lastIndexOf("/") + 1);
                    }
                    emotionMap.put(key, fileName);
                    diaryIdMap.put(key, diaryId);


                    System.out.println("🧩 " + key + " → " + fileName + " (diaryId: " + diaryId + ")");
                    System.out.println("🧩 날짜: " + key + ", 이모지: " + emotionEmoji);
                }
            }

            // 달력 렌더링용 정보
            LocalDate firstDay = yearMonth.atDay(1);
            int firstDayOfWeek = firstDay.getDayOfWeek().getValue() % 7;
            int lastDay = yearMonth.lengthOfMonth();

            model.addAttribute("firstDayOfWeek", firstDayOfWeek);
            model.addAttribute("lastDay", lastDay);
            model.addAttribute("emotionMap", emotionMap);
            model.addAttribute("diaryIdMap", diaryIdMap);

        } else if ("yearly".equals(mode)) {
            LocalDate startDate = LocalDate.of(yearMonth.getYear(), 1, 1);
            LocalDate endDate = LocalDate.of(yearMonth.getYear(), 12, 31);

            List<Object[]> yearlyData = diaryEmotionRepository.findYearlyEmotions(userId, String.valueOf(yearMonth.getYear()));

            Map<String, String> yearlyEmotionMap = new HashMap<>();
            DateTimeFormatter keyFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            for (Object[] row : yearlyData) {
                LocalDate date = (LocalDate) row[0];
                String emotionEmoji = (String) row[1];

                String key = date.format(keyFormatter);
                String fileName = emotionEmoji;
                if (emotionEmoji.contains("/")) {
                    fileName = emotionEmoji.substring(emotionEmoji.lastIndexOf("/") + 1);
                }
                yearlyEmotionMap.put(key, fileName);
            }

            model.addAttribute("yearlyEmotionMap", yearlyEmotionMap);
        }

        return "pages/calendar";
    }

    @GetMapping("/api/calendar/yearly/{year}")
    @ResponseBody
    public Map<String, Object> getYearlyEmotionData(@PathVariable int year, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            System.out.println("❗ 세션에 사용자 없음");
            throw new RuntimeException("사용자 없음");
        }

        Long userId = user.getId();

        // 연간 데이터에도 diaryId를 포함하도록 쿼리 수정 필요
        List<Object[]> results = diaryEmotionRepository.findYearlyEmotionsWithDiaryId(userId, String.valueOf(year));

        Map<String, Object> emotionMap = new HashMap<>();
        for (Object[] row : results) {
            Long diaryId = ((Number) row[0]).longValue();
            LocalDate date = (LocalDate) row[1];
            String emojiPath = (String) row[2];

            String fileName = emojiPath;
            if (emojiPath.contains("/")) {
                fileName = emojiPath.substring(emojiPath.lastIndexOf("/") + 1);
            }

            Map<String, Object> emotionData = new HashMap<>();
            emotionData.put("emoji", fileName);
            emotionData.put("diaryId", diaryId);

            emotionMap.put(date.toString(), emotionData);
        }

        return emotionMap;
    }
}