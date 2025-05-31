package com.test.feelio.controller;

import com.test.feelio.entity.Diary;
import com.test.feelio.entity.DiaryPhoto;
import com.test.feelio.entity.User;
import com.test.feelio.repository.DiaryEmotionRepository;
import com.test.feelio.repository.DiaryRepository;
import com.test.feelio.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class CalendarController {

    private final DiaryEmotionRepository diaryEmotionRepository;
    private final UserService userService; // 이메일로 유저 조회용
    private final DiaryRepository diaryRepository;

    @GetMapping("/calendar")
    public String calendarView(@RequestParam(required = false, defaultValue = "monthly") String mode,
                               @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth,
                               Model model) {

        if (yearMonth == null) {
            yearMonth = YearMonth.now();
        }

        // SecurityContext에서 인증된 사용자 정보 가져오기
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return "redirect:/login?error=sessionExpired";
        }

        String email = auth.getName();
        User user = userService.findByEmail(email);
        if (user == null) {
            return "redirect:/login?error=userNotFound";
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

            for (Object[] row : emotionData) {
                Long diaryId = ((Number) row[0]).longValue();
                LocalDate diaryDate;
                if (row[1] instanceof java.sql.Date) {
                    diaryDate = ((java.sql.Date) row[1]).toLocalDate();
                } else if (row[1] instanceof java.sql.Timestamp) {
                    diaryDate = ((java.sql.Timestamp) row[1]).toLocalDateTime().toLocalDate();
                } else {
                    diaryDate = (LocalDate) row[1];
                }
                String emotionEmoji = (String) row[2];
                String key = diaryDate.format(keyFormatter);

                if (!emotionMap.containsKey(key)) {
                    String fileName = emotionEmoji.contains("/") ?
                            emotionEmoji.substring(emotionEmoji.lastIndexOf("/") + 1) : emotionEmoji;
                    emotionMap.put(key, fileName);
                    diaryIdMap.put(key, diaryId);
                }
            }

            LocalDate firstDay = yearMonth.atDay(1);
            int firstDayOfWeek = firstDay.getDayOfWeek().getValue() % 7;
            int lastDay = yearMonth.lengthOfMonth();

            model.addAttribute("firstDayOfWeek", firstDayOfWeek);
            model.addAttribute("lastDay", lastDay);
            model.addAttribute("emotionMap", emotionMap);
            model.addAttribute("diaryIdMap", diaryIdMap);

        } else if ("yearly".equals(mode)) {
            List<Object[]> yearlyData = diaryEmotionRepository.findYearlyEmotions(userId, String.valueOf(yearMonth.getYear()));

            Map<String, String> yearlyEmotionMap = new HashMap<>();
            DateTimeFormatter keyFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            for (Object[] row : yearlyData) {
                LocalDate date = (LocalDate) row[0];
                String emotionEmoji = (String) row[1];
                String fileName = emotionEmoji.contains("/") ?
                        emotionEmoji.substring(emotionEmoji.lastIndexOf("/") + 1) : emotionEmoji;
                yearlyEmotionMap.put(date.format(keyFormatter), fileName);
            }

            model.addAttribute("yearlyEmotionMap", yearlyEmotionMap);
        }

        return "pages/calendar";
    }

    @GetMapping("/api/calendar/yearly/{year}")
    @ResponseBody
    public Map<String, Object> getYearlyEmotionData(@PathVariable int year) {
        Map<String, Object> response = new HashMap<>();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            response.put("error", "사용자 인증 필요");
            return response;
        }

        String email = auth.getName();
        User user = userService.findByEmail(email);
        if (user == null) {
            response.put("error", "사용자 정보 없음");
            return response;
        }

        Long userId = user.getId();
        List<Object[]> results = diaryEmotionRepository.findYearlyEmotionsWithDiaryId(userId, String.valueOf(year));

        Map<String, Object> emotionMap = new HashMap<>();
        for (Object[] row : results) {
            Long diaryId = ((Number) row[0]).longValue();
            LocalDate date = (LocalDate) row[1];
            String emojiPath = (String) row[2];

            String fileName = emojiPath.contains("/") ?
                    emojiPath.substring(emojiPath.lastIndexOf("/") + 1) : emojiPath;

            Map<String, Object> emotionData = new HashMap<>();
            emotionData.put("emoji", fileName);
            emotionData.put("diaryId", diaryId);

            emotionMap.put(date.toString(), emotionData);
        }

        return emotionMap;
    }
    @GetMapping("/api/diary/{diaryId}")
    @ResponseBody
    public ResponseEntity<?> getDiaryDetail(@PathVariable Long diaryId) {
        Optional<Diary> diaryOpt = diaryRepository.findById(diaryId);
        if (diaryOpt.isEmpty()) {
            return ResponseEntity.status(404).body("일기를 찾을 수 없습니다.");
        }

        Diary diary = diaryOpt.get();

        Map<String, Object> result = new HashMap<>();
        result.put("title", diary.getTitle());
        result.put("content", diary.getContent());

        //  1. 사진 URL 목록
        List<String> photoUrls = diary.getPhotos().stream()
                .map(DiaryPhoto::getPhotoUrl)
                .toList();
        result.put("photos", photoUrls);

        // 2. 태그용 문장 감정 키워드 추출 (예시: 감정명 + 문장 일부)
        List<String> tags = diary.getSentenceEmotions().stream()
                .map(se -> {
                    String sentence = se.getSentenceText();
                    String shortSentence = sentence.length() > 10 ? sentence.substring(0, 10) + "..." : sentence;
                    return se.getEmotion().getEmotionName() + ": " + shortSentence;
                })
                .toList();
        result.put("tags", tags);

        //  3. 오디오 필드 (임시 null 반환 중)
        // result.put("audioUrl", diary.getAudioUrl());

        return ResponseEntity.ok(result);
    }


}
