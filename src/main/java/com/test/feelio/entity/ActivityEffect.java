package com.test.feelio.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ACTIVITY_EFFECTS")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityEffect {
    @Id
    @Column(name = "EFFECT_ID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ACTIVITY_EFFECT_SEQ_GENERATOR")
    @SequenceGenerator(name = "ACTIVITY_EFFECT_SEQ_GENERATOR", sequenceName = "ACTIVITY_EFFECT_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EMOTION_ID", nullable = false)
    private Emotion emotion;

    @Column(name = "ACTIVITY_NAME", nullable = false, length = 100)
    private String activityName;

    @Column(name = "AVERAGE_CHANGE_SCORE")
    private Double averageChangeScore;

    @Column(name = "USAGE_COUNT")
    private Integer usageCount = 0;

    @Column(name = "POSITIVE_EFFECT_COUNT")
    private Integer positiveEffectCount = 0;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    // JPA 이벤트 리스너
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}