package com.test.feelio.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.test.feelio.dto.ActivityRecommendation;
import com.test.feelio.dto.EmotionPercentage;
import com.test.feelio.dto.SentenceAnalysis;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmotionAnalysisResult {

    @JsonProperty("overallEmotions")
    private List<EmotionPercentage> overallEmotions;

    @JsonProperty("sentenceAnalysis")
    private List<SentenceAnalysis> sentenceAnalysis;

    @JsonProperty("userActivities")
    private List<UserActivity> userActivities;

    @JsonProperty("positiveFeedback")
    private String positiveFeedback;

    @JsonProperty("activityRecommendations")
    private List<ActivityRecommendation> activityRecommendations;
}