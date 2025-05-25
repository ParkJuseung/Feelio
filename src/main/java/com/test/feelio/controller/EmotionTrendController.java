package com.test.feelio.controller;

import com.test.feelio.dto.EmotionTrendResponse;
import com.test.feelio.dto.PeriodType;
import com.test.feelio.service.EmotionTrendService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
public class EmotionTrendController {

    private final EmotionTrendService trendService;

    // HTML 페이지 반환
    @GetMapping("/chart")
    public String showChartPage() {
        return "pages/chart";
    }

    // JSON 데이터 반환 (주간/월간)
    @GetMapping("/api/emotion-trend")
    @ResponseBody
    public EmotionTrendResponse getEmotionTrend(
            @RequestParam Long userId,
            @RequestParam PeriodType period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate
    ) {
        return trendService.getTrend(userId, period, startDate);
    }
}
