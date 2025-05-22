package com.test.feelio.controller;

import com.test.feelio.dto.DiaryDTO;
import com.test.feelio.dto.MusicDTO;
import com.test.feelio.entity.User;
import com.test.feelio.repository.UserRepository;
import com.test.feelio.service.DiaryService;
import com.test.feelio.service.MusicRecommendationService;
import com.test.feelio.service.CustomUserDetailsService.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/diary")
@RequiredArgsConstructor
@Slf4j
public class DiaryRestController {

    private final DiaryService diaryService;
    private final MusicRecommendationService musicRecommendationService;
    private final UserRepository userRepository;

    // 북마크 토글
    @PostMapping("/{id}/bookmark")
    public ResponseEntity<Void> toggleBookmark(
            @PathVariable("id") Long diaryId,
            @AuthenticationPrincipal Object principal) {

        try {
            User user = getCurrentUser(principal);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // 권한 체크
            DiaryDTO existing = diaryService.getDiary(diaryId);
            if (!existing.getUserId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            diaryService.toggleBookmark(diaryId);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("북마크 토글 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 일기 목록 조회 (AJAX용)
    @GetMapping("/list")
    public ResponseEntity<List<DiaryDTO>> getDiaryList(
            @AuthenticationPrincipal Object principal,
            @RequestParam(required = false, defaultValue = "false") boolean bookmark) {

        try {
            User user = getCurrentUser(principal);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            List<DiaryDTO> diaries = diaryService.getDiaryList(user.getId(), bookmark);
            return ResponseEntity.ok(diaries);
        } catch (Exception e) {
            log.error("일기 목록 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 일기 저장
    @PostMapping
    public ResponseEntity<DiaryDTO> createDiary(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam("diaryDate") String diaryDate,
            @RequestParam("weather") String weather,
            @RequestParam(value = "photos", required = false) List<MultipartFile> photos,
            @AuthenticationPrincipal Object principal) {

        try {
            User user = getCurrentUser(principal);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            log.info("일기 작성 - 사용자: {}", user.getEmail());
            log.info("제목: {}, 내용 길이: {}", title, content.length());

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
            log.error("일기 저장 중 에러: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 일기 수정
    @PutMapping("/{id}")
    public ResponseEntity<DiaryDTO> updateDiary(
            @PathVariable("id") Long diaryId,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam("weather") String weather,
            @RequestParam(value = "photos", required = false) List<MultipartFile> photos,
            @AuthenticationPrincipal Object principal) {

        try {
            User user = getCurrentUser(principal);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

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
            log.error("일기 수정 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 일기 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDiary(
            @PathVariable("id") Long diaryId,
            @AuthenticationPrincipal Object principal) {

        try {
            User user = getCurrentUser(principal);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // 권한 체크
            DiaryDTO existing = diaryService.getDiary(diaryId);
            if (!existing.getUserId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            diaryService.deleteDiary(diaryId);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("일기 삭제 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 음악 추천 조회
    @GetMapping("/music/{emotionId}")
    public ResponseEntity<List<MusicDTO>> getRecommendedMusic(
            @PathVariable("emotionId") Long emotionId) {

        try {
            List<MusicDTO> music = musicRecommendationService.getRecommendedMusic(emotionId);
            return ResponseEntity.ok(music);
        } catch (Exception e) {
            log.error("음악 추천 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 현재 인증된 사용자 정보를 가져오는 헬퍼 메서드
     */
    private User getCurrentUser(Object principal) {
        log.debug("===== getCurrentUser 호출 (REST API) =====");
        if (principal == null) {
            log.debug("Principal is null");
            return null;
        }

        log.debug("Principal 타입: {}", principal.getClass().getSimpleName());

        // CustomUserPrincipal (일반 로그인)
        if (principal instanceof CustomUserPrincipal) {
            log.debug("CustomUserPrincipal로 사용자 인증됨");
            return ((CustomUserPrincipal) principal).getUser();
        }

        // OAuth2User (소셜 로그인) - DefaultOAuth2User 포함
        if (principal instanceof OAuth2User) {
            log.debug("OAuth2User로 사용자 인증됨");
            OAuth2User oauth2User = (OAuth2User) principal;
            String email = null;
            String provider = null;
            String providerId = null;

            // 디버깅 정보 출력
            log.debug("OAuth2User name: {}", oauth2User.getName());
            log.debug("OAuth2User attributes: {}", oauth2User.getAttributes());

            // Google
            if (oauth2User.getAttributes().containsKey("email")) {
                email = oauth2User.getAttribute("email");
                provider = "google";
                providerId = oauth2User.getAttribute("sub");
                log.debug("Google 사용자 감지: {}", email);
            }

            // 이메일이 존재하는 경우 먼저 이메일로 검색
            if (email != null) {
                try {
                    User user = userRepository.findByEmail(email).orElse(null);
                    if (user != null) {
                        log.debug("이메일로 사용자 찾음: {}", user.getEmail());
                        return user;
                    }
                } catch (Exception e) {
                    log.error("이메일로 사용자 검색 실패: {}", e.getMessage());
                }
            }

            // 이메일로 찾지 못한 경우 provider + providerId로 검색
            if (provider != null && providerId != null) {
                try {
                    User user = userRepository.findByProviderAndProviderId(provider, providerId).orElse(null);
                    if (user != null) {
                        log.debug("Provider/ProviderId로 사용자 찾음: {}", user.getEmail());
                        return user;
                    }
                } catch (Exception e) {
                    log.error("Provider/ProviderId로 사용자 검색 실패: {}", e.getMessage());
                }
            }
        }

        log.warn("알 수 없는 principal 타입 또는 사용자를 찾을 수 없음: {}", principal.getClass().getName());
        return null;
    }
}