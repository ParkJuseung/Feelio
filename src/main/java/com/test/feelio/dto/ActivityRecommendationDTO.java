package com.test.feelio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityRecommendationDTO {
    private String activity;
    private String reason;
    private Double emotionChangeScore;
}