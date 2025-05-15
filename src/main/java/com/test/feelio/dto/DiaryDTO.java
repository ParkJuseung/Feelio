package com.test.feelio.dto;

import com.test.feelio.entity.Diary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiaryDTO {
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private LocalDate diaryDate;
    private String weather;
    private boolean isBookmarked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 파일 업로드용
    private List<MultipartFile> photos;

    // 조회시 포함될 정보
    private List<PhotoDTO> existingPhotos;
    private EmotionAnalysisDTO emotionAnalysis;
    private MusicDTO recommendedMusic;

    public static DiaryDTO fromEntity(Diary diary) {
        return DiaryDTO.builder()
                .id(diary.getId())
                .userId(diary.getUser().getId())
                .title(diary.getTitle())
                .content(diary.getContent())
                .diaryDate(diary.getDiaryDate())
                .weather(diary.getWeather())
                .isBookmarked(diary.isBookmarked())
                .createdAt(diary.getCreatedAt())
                .updatedAt(diary.getUpdatedAt())
                .build();
    }
}