// EmotionTrendService.java
package com.test.feelio.service;

import com.test.feelio.dto.EmotionTrendResponse;
import com.test.feelio.dto.PeriodType;
import com.test.feelio.entity.UserEmotionScore;
import com.test.feelio.repository.UserEmotionScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmotionTrendService {

    private final UserEmotionScoreRepository scoreRepository;

    public EmotionTrendResponse getTrend(Long userId, PeriodType period) {
        return getTrend(userId, period, null);
    }

    public EmotionTrendResponse getTrend(Long userId, PeriodType period, LocalDate startDate) {
        List<UserEmotionScore> scores = scoreRepository.findAll().stream()
                .filter(s -> s.getUser().getId().equals(userId))
                .sorted(Comparator.comparing(UserEmotionScore::getScoreDate))
                .collect(Collectors.toList());

        Map<String, Double> grouped = new LinkedHashMap<>();

        if (period == PeriodType.WEEKLY) {
            // startDate 기준으로 해당 주(월~일) 7일 범위 설정
            LocalDate monday = startDate != null ? startDate : LocalDate.now().with(java.time.DayOfWeek.MONDAY);
            LocalDate sunday = monday.plusDays(6);

            scores = scores.stream()
                    .filter(s -> !s.getScoreDate().isBefore(monday) && !s.getScoreDate().isAfter(sunday))
                    .collect(Collectors.toList());

            // 총합 계산
            double total = scores.stream().mapToDouble(UserEmotionScore::getScore).sum();

            // 날짜별 점수 누적
            for (UserEmotionScore score : scores) {
                String date = score.getScoreDate().toString();
                double percent = total > 0 ? (score.getScore() / total * 100.0) : 0.0;
                grouped.merge(date, percent, Double::sum);
            }



            // 빈 날짜도 0점으로 채움 (보여주기 위함)
            for (int i = 0; i < 7; i++) {
                LocalDate d = monday.plusDays(i);
                grouped.putIfAbsent(d.toString(), 0.0);
            }
        } else {
            // 최근 5개월 기준 월별 집계 (yyyy-MM 형식)
            scores = scores.stream()
                    .filter(s -> s.getScoreDate().isAfter(LocalDate.now().minusMonths(5)))
                    .collect(Collectors.toList());

            for (UserEmotionScore score : scores) {
                String month = score.getScoreDate().getYear() + "-" + String.format("%02d", score.getScoreDate().getMonthValue());
                grouped.merge(month, score.getScore(), Double::sum);
            }

            // 🔽 라벨 정렬 (yyyy-MM 오름차순)
            grouped = grouped.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (oldValue, newValue) -> oldValue,
                            LinkedHashMap::new
                    ));
        }

        List<String> labels = new ArrayList<>(grouped.keySet());
        List<Double> values = new ArrayList<>(grouped.values());

        return new EmotionTrendResponse(labels, values);

    }
}
