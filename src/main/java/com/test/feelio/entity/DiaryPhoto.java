package com.test.feelio.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "DIARY_PHOTOS")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiaryPhoto {
    @Id
    @Column(name = "PHOTO_ID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "DIARY_PHOTO_SEQ_GENERATOR")
    @SequenceGenerator(name = "DIARY_PHOTO_SEQ_GENERATOR", sequenceName = "DIARY_PHOTO_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DIARY_ID", nullable = false)
    private Diary diary;

    @Column(name = "PHOTO_URL", nullable = false, length = 255)
    private String photoUrl;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // JPA 이벤트 리스너
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}