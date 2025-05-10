package com.test.feelio.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SentenceAnalysis {

    @JsonProperty("sentence")
    private String sentence;

    @JsonProperty("emotion")
    private String emotion;

    @JsonProperty("activityType")
    private String activityType;
}