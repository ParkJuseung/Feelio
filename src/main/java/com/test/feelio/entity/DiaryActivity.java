package com.test.feelio.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "DIARY_ACTIVITIES")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiaryActivity {
    @Id
    @Column(name = "DIARY_ACTIVITY_ID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "DIARY_ACTIVITY_SEQ_GENERATOR")
    @SequenceGenerator(name = "DIARY_ACTIVITY_SEQ_GENERATOR", sequenceName = "DIARY_ACTIVITY_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DIARY_ID", nullable = false)
    private Diary diary;

    @Column(name = "ACTIVITY_NAME", nullable = false, length = 100)
    private String activityName;

    @Column(name = "ACTIVITY_SENTIMENT", precision = 3, scale = 2)
    private Double activitySentiment;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // JPA 이벤트 리스너
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}