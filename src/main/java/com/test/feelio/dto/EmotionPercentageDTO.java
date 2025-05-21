package com.test.feelio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmotionPercentageDTO {
    private Long emotionId;
    private String emotionName;
    private Double percentage;
    private String emotionColor;
    private String emotionEmoji;
    private boolean isPrimary;
}