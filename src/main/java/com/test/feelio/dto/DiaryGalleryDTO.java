package com.test.feelio.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class DiaryGalleryDTO {
    private Long diaryId;
    private String yearMonth;
    private String photoUrl;

    public DiaryGalleryDTO(String yearMonth, String photoUrl, Long diaryId) {
        this.yearMonth = yearMonth;
        this.photoUrl = photoUrl;
        this.diaryId = diaryId;
    }

}
