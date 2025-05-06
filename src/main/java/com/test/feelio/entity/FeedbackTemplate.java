package com.test.feelio.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "FEEDBACK_TEMPLATES")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackTemplate {
    @Id
    @Column(name = "FEEDBACK_ID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "FEEDBACK_TEMPLATE_SEQ_GENERATOR")
    @SequenceGenerator(name = "FEEDBACK_TEMPLATE_SEQ_GENERATOR", sequenceName = "FEEDBACK_TEMPLATE_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EMOTION_ID", nullable = false)
    private Emotion emotion;

    @Lob
    @Column(name = "FEEDBACK_TEXT", nullable = false)
    private String feedbackText;

    @Column(name = "IS_POSITIVE")
    private boolean isPositive = true;

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