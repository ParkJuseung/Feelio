package com.test.feelio.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmotionPercentage {

    @JsonProperty("emotion")
    private String emotion;

    @JsonProperty("percentage")
    private Double percentage;

    public boolean isPrimary() {
        // 가장 높은 비율의 감정이 대표 감정
        return percentage > 30.0; // 임의의 기준값
    }
}