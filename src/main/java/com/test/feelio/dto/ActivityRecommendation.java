package com.test.feelio.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityRecommendation {

    @JsonProperty("activity")
    private String activity;

    @JsonProperty("reason")
    private String reason;
}