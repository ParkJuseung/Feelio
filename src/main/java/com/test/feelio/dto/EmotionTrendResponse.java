// EmotionTrendResponse.java
package com.test.feelio.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class EmotionTrendResponse {
    private List<String> labels;
    private List<Double> values;

    public EmotionTrendResponse(List<String> labels, List<Double> values) {
        this.labels = labels;
        this.values = values;
    }

}
