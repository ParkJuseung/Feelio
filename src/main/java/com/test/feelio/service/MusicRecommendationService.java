package com.test.feelio.service;

import com.test.feelio.dto.MusicDTO;
import com.test.feelio.entity.RecommendedMusic;
import com.test.feelio.repository.RecommendedMusicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MusicRecommendationService {

    private final RecommendedMusicRepository recommendedMusicRepository;

    // 감정별 음악 추천 (랜덤 10곡)
    public List<MusicDTO> getRecommendedMusic(Long emotionId) {
        List<RecommendedMusic> musics = recommendedMusicRepository
                .findRandomMusicByEmotion(emotionId, 10);

        return musics.stream()
                .map(this::convertToMusicDTO)
                .collect(Collectors.toList());
    }

    private MusicDTO convertToMusicDTO(RecommendedMusic music) {
        return MusicDTO.builder()
                .id(music.getId())
                .title(music.getTitle())
                .artist(music.getArtist())
                .spotifyLink(music.getSpotifyLink())
                .build();
    }
}