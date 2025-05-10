package com.test.feelio.controller;

import com.test.feelio.dto.DiaryDTO;
import com.test.feelio.dto.MusicDTO;
import com.test.feelio.entity.User;
import com.test.feelio.repository.UserRepository;
import com.test.feelio.service.DiaryService;
import com.test.feelio.service.MusicRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/diary")
@RequiredArgsConstructor
public class DiaryRestController {

    private final DiaryService diaryService;
    private final MusicRecommendationService musicRecommendationService;
    private final UserRepository userRepository;

    // 일기 목록 조회 (AJAX용)
    @GetMapping("/list")
    public ResponseEntity<List<DiaryDTO>> getDiaryList(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false, defaultValue = "false") boolean bookmark) {

        List<DiaryDTO> diaries = diaryService.getDiaryList(user.getId(), bookmark);
        return ResponseEntity.ok(diaries);
    }

    // 일기 저장
    @PostMapping
    public ResponseEntity<DiaryDTO> createDiary(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam("diaryDate") String diaryDate,
            @RequestParam("weather") String weather,
            @RequestParam(value = "photos", required = false) List<MultipartFile> photos) {

        try {
            // 인증 정보 가져오기
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() ||
                    "anonymousUser".equals(authentication.getPrincipal())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // 디버그 로그
            System.out.println("Create diary request. Authentication: " + authentication);

            // 사용자 조회
            User user = getUserFromAuthentication(authentication);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // 디버그 로그
            System.out.println("일기 작성 - 사용자: " + user.getEmail());
            System.out.println("제목: " + title + ", 내용 길이: " + content.length());

            DiaryDTO diaryDTO = DiaryDTO.builder()
                    .title(title)
                    .content(content)
                    .diaryDate(java.time.LocalDate.parse(diaryDate))
                    .weather(weather)
                    .photos(photos)
                    .build();

            DiaryDTO created = diaryService.createDiary(diaryDTO, user.getId());
            return ResponseEntity.ok(created);

        } catch (Exception e) {
            System.err.println("일기 저장 중 에러: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 인증 객체에서 사용자 정보 추출하는 유틸리티 메서드
    private User getUserFromAuthentication(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        // OAuth2User 타입인 경우
        if (principal instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) principal;

            // 디버그 로그
            System.out.println("OAuth2User attributes: " + oauth2User.getAttributes());

            // OAuth 제공자에 따라 다른 필드 사용
            String email = null;

            // Google
            if (oauth2User.getAttributes().containsKey("email")) {
                email = oauth2User.getAttribute("email");

                try {
                    return userRepository.findByEmail(email).orElse(null);
                } catch (Exception e) {
                    System.out.println("이메일로 사용자 검색 실패: " + e.getMessage());
                }
            }
        }
        // UserDetails 타입인 경우 (일반 로그인)
        else if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            try {
                return userRepository.findByEmail(username).orElse(null);
            } catch (Exception e) {
                System.out.println("Username으로 사용자 검색 실패: " + e.getMessage());
            }
        }

        return null;
    }

    // 일기 수정
    @PutMapping("/{id}")
    public ResponseEntity<DiaryDTO> updateDiary(
            @PathVariable("id") Long diaryId,
            @AuthenticationPrincipal User user,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam("weather") String weather,
            @RequestParam(value = "photos", required = false) List<MultipartFile> photos) {

        try {
            // 권한 체크
            DiaryDTO existing = diaryService.getDiary(diaryId);
            if (!existing.getUserId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            DiaryDTO diaryDTO = DiaryDTO.builder()
                    .title(title)
                    .content(content)
                    .weather(weather)
                    .photos(photos)
                    .build();

            DiaryDTO updated = diaryService.updateDiary(diaryId, diaryDTO);
            return ResponseEntity.ok(updated);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 일기 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDiary(
            @PathVariable("id") Long diaryId,
            @AuthenticationPrincipal User user) {

        try {
            // 권한 체크
            DiaryDTO existing = diaryService.getDiary(diaryId);
            if (!existing.getUserId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            diaryService.deleteDiary(diaryId);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 북마크 토글
    @PostMapping("/{id}/bookmark")
    public ResponseEntity<Void> toggleBookmark(
            @PathVariable("id") Long diaryId,
            @AuthenticationPrincipal User user) {

        try {
            // 권한 체크
            DiaryDTO existing = diaryService.getDiary(diaryId);
            if (!existing.getUserId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            diaryService.toggleBookmark(diaryId);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 음악 추천 조회
    @GetMapping("/music/{emotionId}")
    public ResponseEntity<List<MusicDTO>> getRecommendedMusic(
            @PathVariable("emotionId") Long emotionId) {

        List<MusicDTO> music = musicRecommendationService.getRecommendedMusic(emotionId);
        return ResponseEntity.ok(music);
    }
}