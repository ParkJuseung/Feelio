package com.test.feelio.controller;

import com.test.feelio.dto.*;
import com.test.feelio.entity.User;
import com.test.feelio.repository.UserRepository;
import com.test.feelio.service.DiaryService;
import com.test.feelio.service.MusicRecommendationService;
import com.test.feelio.service.CustomUserDetailsService.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;
    private final MusicRecommendationService musicRecommendationService;
    private final UserRepository userRepository;

    // 일기 목록 페이지
    @GetMapping({"/", "/home"})
    public String home(@AuthenticationPrincipal Object principal, Model model) {
        System.out.println("===== HOME 컨트롤러 호출 =====");
        System.out.println("Principal: " + (principal != null ? principal.getClass().getSimpleName() : "null"));

        User user = getCurrentUser(principal);
        if (user == null) {
            System.out.println("사용자 정보를 찾을 수 없어 로그인 페이지로 리디렉션");
            return "redirect:/login";
        }

        System.out.println("사용자 정보: " + user.getEmail() + " (enabled: " + user.isEnabled() + ")");

        // 일기 목록 조회
        List<DiaryDTO> diaries = diaryService.getDiaryList(user.getId(), false);
        System.out.println("일기 개수: " + diaries.size());

        model.addAttribute("diaries", diaries);
        model.addAttribute("user", user);
        System.out.println("home.html 템플릿으로 이동");
        return "pages/home";
    }

    // 일기 작성 페이지
    @GetMapping("/diary/write")
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
    @GetMapping("/diary/edit/{id}")
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
    @GetMapping("/diary/{id}")
    public String diaryDetail(@PathVariable("id") Long diaryId,
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

        model.addAttribute("diary", diary);
        return "pages/diary-read";
    }

    // AI 분석 결과 페이지
    @GetMapping("/diary/{id}/analysis")
    public String analysisPage(@PathVariable("id") Long diaryId,
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

        EmotionAnalysisDTO analysis = diary.getEmotionAnalysis();

        // 분석 결과 로그 출력
        System.out.println("===== AI 분석 결과 시작 =====");
        System.out.println("일기 ID: " + diaryId);
        if (analysis != null) {
            System.out.println("긍정적 피드백: " + analysis.getPositiveFeedback());
            System.out.println("주요 감정: " + (analysis.getPrimaryEmotion() != null ?
                    analysis.getPrimaryEmotion().getEmotionName() : "없음"));

            if (analysis.getEmotions() != null) {
                System.out.println("감정 목록 (" + analysis.getEmotions().size() + "개):");
                for (EmotionPercentageDTO emotion : analysis.getEmotions()) {
                    System.out.println("  - " + emotion.getEmotionName() + ": " +
                            emotion.getPercentage() + "% (주요감정: " + emotion.isPrimary() + ")");
                }
            } else {
                System.out.println("감정 목록: 없음");
            }

            if (analysis.getActivityRecommendations() != null) {
                System.out.println("추천 활동 (" + analysis.getActivityRecommendations().size() + "개):");
                for (ActivityRecommendationDTO activity : analysis.getActivityRecommendations()) {
                    System.out.println("  - " + activity.getActivity() + ": " + activity.getReason());
                }
            } else {
                System.out.println("추천 활동: 없음");
            }
        } else {
            System.out.println("분석 결과가 없습니다.");
        }
        System.out.println("===== AI 분석 결과 종료 =====");

        // 음악 추천 조회
        if (analysis != null && analysis.getPrimaryEmotion() != null) {
            List<MusicDTO> recommendedMusic = musicRecommendationService
                    .getRecommendedMusic(analysis.getPrimaryEmotion().getId());
            model.addAttribute("recommendedMusic", recommendedMusic);
        }

        model.addAttribute("diary", diary);
        model.addAttribute("analysis", analysis);
        return "pages/ai-result";
    }

    /**
     * 현재 인증된 사용자 정보를 가져오는 헬퍼 메서드
     */
    private User getCurrentUser(Object principal) {
        System.out.println("===== getCurrentUser 호출 =====");
        if (principal == null) {
            System.out.println("Principal is null");
            return null;
        }

        System.out.println("Principal 타입: " + principal.getClass().getSimpleName());

        // CustomUserPrincipal (일반 로그인)
        if (principal instanceof CustomUserPrincipal) {
            System.out.println("CustomUserPrincipal로 사용자 인증됨");
            return ((CustomUserPrincipal) principal).getUser();
        }

        // OAuth2User (소셜 로그인) - DefaultOAuth2User 포함
        if (principal instanceof OAuth2User) {
            System.out.println("OAuth2User로 사용자 인증됨");
            OAuth2User oauth2User = (OAuth2User) principal;
            String email = null;
            String provider = null;
            String providerId = null;

            // 디버깅 정보 출력
            System.out.println("OAuth2User name: " + oauth2User.getName());
            System.out.println("OAuth2User attributes: " + oauth2User.getAttributes());

            // Google
            if (oauth2User.getAttributes().containsKey("email")) {
                email = oauth2User.getAttribute("email");
                provider = "google";
                providerId = oauth2User.getAttribute("sub");
                System.out.println("Google 사용자 감지: " + email);
            }

            // 이메일이 존재하는 경우 먼저 이메일로 검색
            if (email != null) {
                try {
                    User user = userRepository.findByEmail(email).orElse(null);
                    if (user != null) {
                        System.out.println("이메일로 사용자 찾음: " + user.getEmail());
                        return user;
                    }
                } catch (Exception e) {
                    System.out.println("이메일로 사용자 검색 실패: " + e.getMessage());
                }
            }

            // 이메일로 찾지 못한 경우 provider + providerId로 검색
            if (provider != null && providerId != null) {
                try {
                    User user = userRepository.findByProviderAndProviderId(provider, providerId).orElse(null);
                    if (user != null) {
                        System.out.println("Provider/ProviderId로 사용자 찾음: " + user.getEmail());
                        return user;
                    }
                } catch (Exception e) {
                    System.out.println("Provider/ProviderId로 사용자 검색 실패: " + e.getMessage());
                }
            }
        }

        System.out.println("알 수 없는 principal 타입 또는 사용자를 찾을 수 없음: " + principal.getClass().getName());
        return null;
    }
}