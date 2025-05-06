package com.test.feelio.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "RECOMMENDED_MUSIC")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendedMusic {
    @Id
    @Column(name = "MUSIC_ID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "RECOMMENDED_MUSIC_SEQ_GENERATOR")
    @SequenceGenerator(name = "RECOMMENDED_MUSIC_SEQ_GENERATOR", sequenceName = "RECOMMENDED_MUSIC_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EMOTION_ID", nullable = false)
    private Emotion emotion;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 255)
    private String artist;

    @Column(name = "SPOTIFY_LINK", length = 1000)
    private String spotifyLink;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // JPA 이벤트 리스너
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}