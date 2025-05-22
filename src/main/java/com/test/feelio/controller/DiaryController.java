package com.test.feelio.controller;

import com.test.feelio.dto.*;
import com.test.feelio.entity.User;
import com.test.feelio.repository.UserRepository;
import com.test.feelio.service.DiaryService;
import com.test.feelio.service.MusicRecommendationService;
import com.test.feelio.service.CustomUserDetailsService.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/diary")
@RequiredArgsConstructor
@Slf4j
public class DiaryController {

    private final DiaryService diaryService;
    private final MusicRecommendationService musicRecommendationService;
    private final UserRepository userRepository;

    // 일기 작성 페이지
    @GetMapping("/write")
    public String writePage(@AuthenticationPrincipal Object principal, Model model) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("isEdit", false);
        model.addAttribute("diary", new DiaryDTO());
        return "pages/diary-form";
    }

    // 일기 수정 페이지
    @GetMapping("/edit/{id}")
    public String editPage(@PathVariable("id") Long diaryId,
                           @AuthenticationPrincipal Object principal,
                           Model model) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        DiaryDTO diary = diaryService.getDiary(diaryId);

        // 권한 체크
        if (!diary.getUserId().equals(user.getId())) {
            return "redirect:/home?error=unauthorized";
        }

        model.addAttribute("isEdit", true);
        model.addAttribute("diary", diary);
        return "pages/diary-form";
    }

    // 일기 상세 페이지
    @GetMapping("/{id}")
    public String diaryDetail(@PathVariable("id") Long diaryId,
                              @AuthenticationPrincipal Object principal,
                              Model model) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        try {
            DiaryDTO diary = diaryService.getDiary(diaryId);

            // 권한 체크
            if (!diary.getUserId().equals(user.getId())) {
                return "redirect:/home?error=unauthorized";
            }

            model.addAttribute("diary", diary);
            return "pages/diary-read";
        } catch (RuntimeException e) {
            log.error("일기 조회 중 오류 발생: {}", e.getMessage());
            return "redirect:/home?error=notfound";
        }
    }

    // AI 분석 결과 페이지
    @GetMapping("/{id}/analysis")
    public String analysisPage(@PathVariable("id") Long diaryId,
                               @AuthenticationPrincipal Object principal,
                               Model model) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        try {
            DiaryDTO diary = diaryService.getDiary(diaryId);

            // 권한 체크
            if (!diary.getUserId().equals(user.getId())) {
                return "redirect:/home?error=unauthorized";
            }

            EmotionAnalysisDTO analysis = diary.getEmotionAnalysis();

            // 분석 결과 로그 출력
            log.info("===== AI 분석 결과 시작 =====");
            log.info("일기 ID: {}", diaryId);
            if (analysis != null) {
                log.info("긍정적 피드백: {}", analysis.getPositiveFeedback());
                log.info("주요 감정: {}", analysis.getPrimaryEmotion() != null ?
                        analysis.getPrimaryEmotion().getEmotionName() : "없음");

                if (analysis.getEmotions() != null) {
                    log.info("감정 목록 ({}개):", analysis.getEmotions().size());
                    for (EmotionPercentageDTO emotion : analysis.getEmotions()) {
                        log.info("  - {}: {}% (주요감정: {})", emotion.getEmotionName(),
                                emotion.getPercentage(), emotion.isPrimary());
                    }
                } else {
                    log.info("감정 목록: 없음");
                }

                if (analysis.getActivityRecommendations() != null) {
                    log.info("추천 활동 ({}개):", analysis.getActivityRecommendations().size());
                    for (ActivityRecommendationDTO activity : analysis.getActivityRecommendations()) {
                        log.info("  - {}: {}", activity.getActivity(), activity.getReason());
                    }
                } else {
                    log.info("추천 활동: 없음");
                }
            } else {
                log.info("분석 결과가 없습니다.");
            }
            log.info("===== AI 분석 결과 종료 =====");

            // 음악 추천 조회
            if (analysis != null && analysis.getPrimaryEmotion() != null) {
                List<MusicDTO> recommendedMusic = musicRecommendationService
                        .getRecommendedMusic(analysis.getPrimaryEmotion().getId());
                model.addAttribute("recommendedMusic", recommendedMusic);
            }

            model.addAttribute("diary", diary);
            model.addAttribute("analysis", analysis);
            return "pages/ai-result";
        } catch (RuntimeException e) {
            log.error("AI 분석 결과 조회 중 오류 발생: {}", e.getMessage());
            return "redirect:/home?error=notfound";
        }
    }

    /**
     * 현재 인증된 사용자 정보를 가져오는 헬퍼 메서드
     */
    private User getCurrentUser(Object principal) {
        log.debug("===== getCurrentUser 호출 =====");
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