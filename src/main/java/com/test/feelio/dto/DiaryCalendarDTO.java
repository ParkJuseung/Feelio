package com.test.feelio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DiaryCalendarDTO {
    private Long diaryId;
    private LocalDate diaryDate;
    private String emotionEmoji; // 이모티콘 이미지 경로
}
