package com.test.feelio.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "DIARY_EMOTIONS")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiaryEmotion {
    @Id
    @Column(name = "DIARY_EMOTION_ID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "DIARY_EMOTION_SEQ_GENERATOR")
    @SequenceGenerator(name = "DIARY_EMOTION_SEQ_GENERATOR", sequenceName = "DIARY_EMOTION_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DIARY_ID", nullable = false)
    private Diary diary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EMOTION_ID", nullable = false)
    private Emotion emotion;

    @Column(name = "EMOTION_PERCENTAGE", nullable = false)
    private Double emotionPercentage;

    @Column(name = "IS_PRIMARY_EMOTION")
    private boolean isPrimaryEmotion;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // JPA 이벤트 리스너
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }


}