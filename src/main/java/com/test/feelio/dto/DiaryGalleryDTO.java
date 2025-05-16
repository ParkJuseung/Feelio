package com.test.feelio.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Builder
public class DiaryGalleryDTO {
    private String yearMonth;     // 예: "2024-01"
    private String photoUrl;      // 예: "/asset/images/2026.jpg"
    private String emotionName;   // 예: "기쁨"
}
