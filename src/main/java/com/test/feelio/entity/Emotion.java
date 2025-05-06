package com.test.feelio.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "EMOTIONS")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Emotion {
    @Id
    @Column(name = "EMOTION_ID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "EMOTION_SEQ_GENERATOR")
    @SequenceGenerator(name = "EMOTION_SEQ_GENERATOR", sequenceName = "EMOTION_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "EMOTION_NAME", nullable = false, length = 30)
    private String emotionName;

    @Column(name = "EMOTION_COLOR", length = 7)
    private String emotionColor;

    @Column(name = "EMOTION_EMOJI", length = 1000)
    private String emotionEmoji;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 연관 관계 매핑
    @OneToMany(mappedBy = "emotion", cascade = CascadeType.ALL)
    private List<DiaryEmotion> diaryEmotions = new ArrayList<>();

    @OneToMany(mappedBy = "emotion", cascade = CascadeType.ALL)
    private List<SentenceEmotion> sentenceEmotions = new ArrayList<>();

    @OneToMany(mappedBy = "emotion", cascade = CascadeType.ALL)
    private List<UserEmotionScore> userEmotionScores = new ArrayList<>();

    @OneToMany(mappedBy = "emotionBefore", cascade = CascadeType.ALL)
    private List<UserActivityRecord> activityRecordsBefore = new ArrayList<>();

    @OneToMany(mappedBy = "emotionAfter", cascade = CascadeType.ALL)
    private List<UserActivityRecord> activityRecordsAfter = new ArrayList<>();

    @OneToMany(mappedBy = "emotion", cascade = CascadeType.ALL)
    private List<ActivityEffect> activityEffects = new ArrayList<>();

    @OneToMany(mappedBy = "emotion", cascade = CascadeType.ALL)
    private List<FeedbackTemplate> feedbackTemplates = new ArrayList<>();

    @OneToMany(mappedBy = "emotion", cascade = CascadeType.ALL)
    private List<RecommendedMusic> recommendedMusics = new ArrayList<>();

    // JPA 이벤트 리스너
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}