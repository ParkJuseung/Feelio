package com.test.feelio.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "SENTENCE_EMOTIONS")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SentenceEmotion {
    @Id
    @Column(name = "SENTENCE_EMOTION_ID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SENTENCE_EMOTION_SEQ_GENERATOR")
    @SequenceGenerator(name = "SENTENCE_EMOTION_SEQ_GENERATOR", sequenceName = "SENTENCE_EMOTION_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DIARY_ID", nullable = false)
    private Diary diary;

    @Lob
    @Column(name = "SENTENCE_TEXT", nullable = false)
    private String sentenceText;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EMOTION_ID", nullable = false)
    private Emotion emotion;

    @Column(name = "ACTIVITY_TYPE", length = 50)
    private String activityType;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // JPA 이벤트 리스너
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}