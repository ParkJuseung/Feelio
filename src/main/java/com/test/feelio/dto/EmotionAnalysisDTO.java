package com.test.feelio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmotionAnalysisDTO {
    private EmotionDTO primaryEmotion;
    private List<EmotionPercentageDTO> emotions;
    private List<SentenceEmotionDTO> sentenceEmotions;
    private String positiveFeedback;
    private List<ActivityRecommendationDTO> activityRecommendations;
}