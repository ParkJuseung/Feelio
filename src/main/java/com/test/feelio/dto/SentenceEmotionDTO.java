package com.test.feelio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SentenceEmotionDTO {
    private String sentence;
    private EmotionDTO emotion;
    private String activityType;
}