package com.test.feelio.service;

import com.test.feelio.dto.*;
import com.test.feelio.entity.*;
import com.test.feelio.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DiaryService {

    private final DiaryRepository diaryRepository;
    private final DiaryPhotoRepository diaryPhotoRepository;
    private final DiaryEmotionRepository diaryEmotionRepository;
    private final RecommendedMusicRepository recommendedMusicRepository;
    private final UserRepository userRepository;
    private final EmotionAnalysisService emotionAnalysisService;
    private final FileStorageService fileStorageService;

    // 일기 목록 조회
    public List<DiaryDTO> getDiaryList(Long userId, boolean bookmarkOnly) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        List<Diary> diaries;
        if (bookmarkOnly) {
            diaries = diaryRepository.findBookmarkedDiaries(user);
        } else {
            diaries = diaryRepository.findByUserOrderByDiaryDateDesc(user);
        }

        return diaries.stream()
                .map(this::convertToDiaryDTO)
                .collect(Collectors.toList());
    }

    // 일기 상세 조회
    public DiaryDTO getDiary(Long diaryId) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new RuntimeException("일기를 찾을 수 없습니다"));

        DiaryDTO diaryDTO = convertToDiaryDTO(diary);

        // 감정 분석 결과 조회
        List<DiaryEmotion> emotions = diaryEmotionRepository.findByDiaryId(diaryId);
        if (!emotions.isEmpty()) {
            EmotionAnalysisDTO analysisDTO = createEmotionAnalysisDTO(emotions);
            diaryDTO.setEmotionAnalysis(analysisDTO);

            // 대표 감정에 따른 음악 추천
            DiaryEmotion primaryEmotion = diaryEmotionRepository
                    .findByDiaryIdAndIsPrimaryEmotion(diaryId, true)
                    .orElse(null);

            if (primaryEmotion != null) {
                List<RecommendedMusic> musics = recommendedMusicRepository
                        .findRandomMusicByEmotion(primaryEmotion.getEmotion().getId(), 1);

                if (!musics.isEmpty()) {
                    diaryDTO.setRecommendedMusic(convertToMusicDTO(musics.get(0)));
                }
            }
        }

        // 사진 조회
        List<DiaryPhoto> photos = diaryPhotoRepository.findByDiaryId(diaryId);
        List<PhotoDTO> photoDTOs = photos.stream()
                .map(this::convertToPhotoDTO)
                .collect(Collectors.toList());
        diaryDTO.setExistingPhotos(photoDTOs);

        return diaryDTO;
    }

    // 일기 작성
    public DiaryDTO createDiary(DiaryDTO diaryDTO, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        // 일기 저장
        Diary diary = Diary.builder()
                .user(user)
                .title(diaryDTO.getTitle())
                .content(diaryDTO.getContent())
                .diaryDate(diaryDTO.getDiaryDate() != null ? diaryDTO.getDiaryDate() : LocalDate.now())
                .weather(diaryDTO.getWeather())
                .isBookmarked(false)
                .build();

        diary = diaryRepository.save(diary);

        // 사진 업로드 처리
        if (diaryDTO.getPhotos() != null && !diaryDTO.getPhotos().isEmpty()) {
            for (MultipartFile photo : diaryDTO.getPhotos()) {
                if (!photo.isEmpty()) {
                    String fileName = fileStorageService.storeFile(photo);

                    DiaryPhoto diaryPhoto = DiaryPhoto.builder()
                            .diary(diary)
                            .photoUrl("/images/diary/" + fileName)
                            .build();

                    diaryPhotoRepository.save(diaryPhoto);
                }
            }
        }

        // AI 감정 분석 수행
        EmotionAnalysisDTO analysisResult = emotionAnalysisService.analyzeEmotion(diary.getContent(), diary.getId());

        // DTO로 변환 후 분석 결과 포함하여 반환
        DiaryDTO result = convertToDiaryDTO(diary);
        result.setEmotionAnalysis(analysisResult);

        return result;
    }

    // 일기 수정
    public DiaryDTO updateDiary(Long diaryId, DiaryDTO diaryDTO) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new RuntimeException("일기를 찾을 수 없습니다"));

        // 기본 정보 업데이트
        diary.setTitle(diaryDTO.getTitle());
        diary.setContent(diaryDTO.getContent());
        diary.setWeather(diaryDTO.getWeather());

        // 사진 업데이트 처리
        if (diaryDTO.getPhotos() != null) {
            // 기존 사진 삭제
            List<DiaryPhoto> existingPhotos = diaryPhotoRepository.findByDiaryId(diaryId);
            for (DiaryPhoto photo : existingPhotos) {
                fileStorageService.deleteFile(extractFileName(photo.getPhotoUrl()));
                diaryPhotoRepository.delete(photo);
            }

            // 새 사진 저장
            for (MultipartFile photo : diaryDTO.getPhotos()) {
                if (!photo.isEmpty()) {
                    String fileName = fileStorageService.storeFile(photo);

                    DiaryPhoto diaryPhoto = DiaryPhoto.builder()
                            .diary(diary)
                            .photoUrl("/images/diary/" + fileName)
                            .build();

                    diaryPhotoRepository.save(diaryPhoto);
                }
            }
        }

        diary = diaryRepository.save(diary);

        // 내용이 변경된 경우 감정 재분석
        if (!diary.getContent().equals(diaryDTO.getContent())) {
            // 기존 감정 분석 데이터 삭제
            diaryEmotionRepository.deleteByDiaryId(diaryId);

            // 재분석 수행
            EmotionAnalysisDTO analysisResult = emotionAnalysisService.analyzeEmotion(diary.getContent(), diary.getId());

            DiaryDTO result = convertToDiaryDTO(diary);
            result.setEmotionAnalysis(analysisResult);
            return result;
        }

        return convertToDiaryDTO(diary);
    }

    // 일기 삭제
    public void deleteDiary(Long diaryId) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new RuntimeException("일기를 찾을 수 없습니다"));

        // 관련 사진 파일 삭제
        List<DiaryPhoto> photos = diaryPhotoRepository.findByDiaryId(diaryId);
        for (DiaryPhoto photo : photos) {
            fileStorageService.deleteFile(extractFileName(photo.getPhotoUrl()));
        }

        // DB에서 삭제 (CASCADE로 관련 데이터 자동 삭제)
        diaryRepository.delete(diary);
    }

    // 북마크 토글
    public void toggleBookmark(Long diaryId) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new RuntimeException("일기를 찾을 수 없습니다"));

        diary.setBookmarked(!diary.isBookmarked());
        diaryRepository.save(diary);
    }

    // Helper 메서드들
    private DiaryDTO convertToDiaryDTO(Diary diary) {
        return DiaryDTO.builder()
                .id(diary.getId())
                .userId(diary.getUser().getId())
                .title(diary.getTitle())
                .content(diary.getContent())
                .diaryDate(diary.getDiaryDate())
                .weather(diary.getWeather())
                .isBookmarked(diary.isBookmarked())
                .createdAt(diary.getCreatedAt())
                .updatedAt(diary.getUpdatedAt())
                .build();
    }

    private PhotoDTO convertToPhotoDTO(DiaryPhoto photo) {
        return PhotoDTO.builder()
                .id(photo.getId())
                .photoUrl(photo.getPhotoUrl())
                .build();
    }

    private MusicDTO convertToMusicDTO(RecommendedMusic music) {
        return MusicDTO.builder()
                .id(music.getId())
                .title(music.getTitle())
                .artist(music.getArtist())
                .spotifyLink(music.getSpotifyLink())
                .build();
    }

    private EmotionAnalysisDTO createEmotionAnalysisDTO(List<DiaryEmotion> emotions) {
        // 감정 비율 데이터 변환
        List<EmotionPercentageDTO> emotionPercentages = emotions.stream()
                .map(emotion -> EmotionPercentageDTO.builder()
                        .emotionId(emotion.getEmotion().getId())
                        .emotionName(emotion.getEmotion().getEmotionName())
                        .percentage(emotion.getEmotionPercentage())
                        .emotionColor(emotion.getEmotion().getEmotionColor())
                        .emotionEmoji(emotion.getEmotion().getEmotionEmoji())
                        .isPrimary(emotion.isPrimaryEmotion())
                        .build())
                .collect(Collectors.toList());

        // 대표 감정 찾기
        EmotionPercentageDTO primary = emotionPercentages.stream()
                .filter(EmotionPercentageDTO::isPrimary)
                .findFirst()
                .orElse(null);

        return EmotionAnalysisDTO.builder()
                .primaryEmotion(primary != null ?
                        EmotionDTO.builder()
                                .id(primary.getEmotionId())
                                .emotionName(primary.getEmotionName())
                                .emotionColor(primary.getEmotionColor())
                                .emotionEmoji(primary.getEmotionEmoji())
                                .build() : null)
                .emotions(emotionPercentages)
                .build();
    }

    private String extractFileName(String photoUrl) {
        // "/images/diary/filename.jpg" -> "filename.jpg"
        return photoUrl.substring(photoUrl.lastIndexOf('/') + 1);
    }
}