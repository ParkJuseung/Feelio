package com.test.feelio.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "USER_ACTIVITY_RECORDS")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActivityRecord {
    @Id
    @Column(name = "RECORD_ID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "USER_ACTIVITY_RECORD_SEQ_GENERATOR")
    @SequenceGenerator(name = "USER_ACTIVITY_RECORD_SEQ_GENERATOR", sequenceName = "USER_ACTIVITY_RECORD_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;

    @Column(name = "ACTIVITY_NAME", nullable = false, length = 100)
    private String activityName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EMOTION_BEFORE_ID")
    private Emotion emotionBefore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EMOTION_AFTER_ID")
    private Emotion emotionAfter;

    @Column(name = "EMOTION_CHANGE_SCORE")
    private Double emotionChangeScore;

    @Column(name = "ACTIVITY_DATE")
    private LocalDate activityDate;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // JPA 이벤트 리스너
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.activityDate == null) {
            this.activityDate = LocalDate.now();
        }
    }
}