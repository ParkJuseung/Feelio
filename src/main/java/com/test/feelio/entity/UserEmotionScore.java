package com.test.feelio.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "USER_EMOTION_SCORES")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEmotionScore {
    @Id
    @Column(name = "SCORE_ID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "USER_EMOTION_SCORE_SEQ_GENERATOR")
    @SequenceGenerator(name = "USER_EMOTION_SCORE_SEQ_GENERATOR", sequenceName = "USER_EMOTION_SCORE_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EMOTION_ID", nullable = false)
    private Emotion emotion;

    @Column(nullable = false, precision = 5, scale = 2)
    private Double score;

    @Column(name = "SCORE_DATE", nullable = false)
    private LocalDate scoreDate;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // JPA 이벤트 리스너
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.scoreDate == null) {
            this.scoreDate = LocalDate.now();
        }
    }
}