package com.test.feelio.controller;

import com.test.feelio.dto.*;
import com.test.feelio.entity.User;
import com.test.feelio.repository.UserRepository;
import com.test.feelio.service.DiaryService;
import com.test.feelio.service.MusicRecommendationService;
import com.test.feelio.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;
    private final MusicRecommendationService musicRecommendationService;
    //private final UserService userService;
    private final UserRepository userRepository;

    // 일기 목록 페이지
    @GetMapping({"/", "/home"})
    public String home(Model model) {
        // SecurityContext에서 인증 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/login";
        }

        // 디버그 로그 추가
        Object principal = authentication.getPrincipal();
        System.out.println("Principal class: " + principal.getClass().getName());
        System.out.println("Principal: " + principal);
        System.out.println("Authorities: " + authentication.getAuthorities());

        User user = null;

        // OAuth2User 타입인 경우
        if (principal instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) principal;

            // 디버그 로그 추가
            System.out.println("OAuth2User attributes: " + oauth2User.getAttributes());

            // OAuth 제공자에 따라 다른 필드 사용
            String email = null;
            String provider = null;
            String providerId = null;

            // Google
            if (oauth2User.getAttributes().containsKey("email")) {
                email = oauth2User.getAttribute("email");
                provider = "google";
                providerId = oauth2User.getAttribute("sub");
            }
            // 다른 제공자 추가 가능

            // 이메일이 존재하는 경우 먼저 이메일로 검색
            if (email != null) {
                try {
                    user = userRepository.findByEmail(email).orElse(null);
                } catch (Exception e) {
                    System.out.println("이메일로 사용자 검색 실패: " + e.getMessage());
                }
            }

            // 이메일로 찾지 못한 경우 provider + providerId로 검색
            if (user == null && provider != null && providerId != null) {
                try {
                    user = userRepository.findByProviderAndProviderId(provider, providerId).orElse(null);
                } catch (Exception e) {
                    System.out.println("Provider/ProviderId로 사용자 검색 실패: " + e.getMessage());
                }
            }
        }
        // UserDetails 타입인 경우 (일반 로그인)
        else if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            try {
                user = userRepository.findByEmail(username).orElse(null);
            } catch (Exception e) {
                System.out.println("Username으로 사용자 검색 실패: " + e.getMessage());
            }
        }
        // String 타입인 경우 (기본 인증)
        else if (principal instanceof String) {
            try {
                user = userRepository.findByEmail((String) principal).orElse(null);
            } catch (Exception e) {
                System.out.println("Username(String)으로 사용자 검색 실패: " + e.getMessage());
            }
        }

        // 사용자를 찾지 못한 경우
        if (user == null) {
            System.out.println("인증된 사용자를 찾을 수 없습니다.");
            return "redirect:/login?error=user_not_found";
        }

        // 일기 목록 조회
        List<DiaryDTO> diaries = diaryService.getDiaryList(user.getId(), false);

        model.addAttribute("diaries", diaries);
        model.addAttribute("user", user);
        return "pages/home";
    }

    // 일기 작성 페이지
    @GetMapping("/diary/write")
    public String writePage(Model model) {
        // 현재 인증된 사용자 확인
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/login";
        }

        // 디버그 로그
        System.out.println("Write page requested. Authentication: " + authentication);

        model.addAttribute("isEdit", false);
        model.addAttribute("diary", new DiaryDTO());
        return "pages/diary-form";  // diary-form.html 또는 write.html
    }

    // 일기 수정 페이지
    @GetMapping("/diary/edit/{id}")
    public String editPage(@PathVariable("id") Long diaryId, Model model) {
        // SecurityContext에서 인증 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/login";
        }

        // 인증된 사용자 정보 가져오기
        Object principal = authentication.getPrincipal();
        User user = null;

        // OAuth2User 타입인 경우
        if (principal instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) principal;
            String email = null;
            String provider = null;
            String providerId = null;

            // Google
            if (oauth2User.getAttributes().containsKey("email")) {
                email = oauth2User.getAttribute("email");
                provider = "google";
                providerId = oauth2User.getAttribute("sub");
            }

            // 이메일이 존재하는 경우 먼저 이메일로 검색
            if (email != null) {
                try {
                    user = userRepository.findByEmail(email).orElse(null);
                } catch (Exception e) {
                    System.out.println("이메일로 사용자 검색 실패: " + e.getMessage());
                }
            }

            // 이메일로 찾지 못한 경우 provider + providerId로 검색
            if (user == null && provider != null && providerId != null) {
                try {
                    user = userRepository.findByProviderAndProviderId(provider, providerId).orElse(null);
                } catch (Exception e) {
                    System.out.println("Provider/ProviderId로 사용자 검색 실패: " + e.getMessage());
                }
            }
        }
        // UserDetails 타입인 경우 (일반 로그인)
        else if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            try {
                user = userRepository.findByEmail(username).orElse(null);
            } catch (Exception e) {
                System.out.println("Username으로 사용자 검색 실패: " + e.getMessage());
            }
        }
        // String 타입인 경우 (기본 인증)
        else if (principal instanceof String) {
            try {
                user = userRepository.findByEmail((String) principal).orElse(null);
            } catch (Exception e) {
                System.out.println("Username(String)으로 사용자 검색 실패: " + e.getMessage());
            }
        }

        // 사용자를 찾지 못한 경우
        if (user == null) {
            System.out.println("인증된 사용자를 찾을 수 없습니다.");
            return "redirect:/login?error=user_not_found";
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
    public String diaryDetail(@PathVariable("id") Long diaryId, Model model) {
        // SecurityContext에서 인증 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/login";
        }

        // 인증된 사용자 정보 가져오기
        Object principal = authentication.getPrincipal();
        User user = null;

        // OAuth2User 타입인 경우
        if (principal instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) principal;
            String email = null;
            String provider = null;
            String providerId = null;

            // Google
            if (oauth2User.getAttributes().containsKey("email")) {
                email = oauth2User.getAttribute("email");
                provider = "google";
                providerId = oauth2User.getAttribute("sub");
            }

            // 이메일이 존재하는 경우 먼저 이메일로 검색
            if (email != null) {
                try {
                    user = userRepository.findByEmail(email).orElse(null);
                } catch (Exception e) {
                    System.out.println("이메일로 사용자 검색 실패: " + e.getMessage());
                }
            }

            // 이메일로 찾지 못한 경우 provider + providerId로 검색
            if (user == null && provider != null && providerId != null) {
                try {
                    user = userRepository.findByProviderAndProviderId(provider, providerId).orElse(null);
                } catch (Exception e) {
                    System.out.println("Provider/ProviderId로 사용자 검색 실패: " + e.getMessage());
                }
            }
        }
        // UserDetails 타입인 경우 (일반 로그인)
        else if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            try {
                user = userRepository.findByEmail(username).orElse(null);
            } catch (Exception e) {
                System.out.println("Username으로 사용자 검색 실패: " + e.getMessage());
            }
        }
        // String 타입인 경우 (기본 인증)
        else if (principal instanceof String) {
            try {
                user = userRepository.findByEmail((String) principal).orElse(null);
            } catch (Exception e) {
                System.out.println("Username(String)으로 사용자 검색 실패: " + e.getMessage());
            }
        }

        // 사용자를 찾지 못한 경우
        if (user == null) {
            System.out.println("인증된 사용자를 찾을 수 없습니다.");
            return "redirect:/login?error=user_not_found";
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
    public String analysisPage(@PathVariable("id") Long diaryId, Model model) {
        // SecurityContext에서 인증 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/login";
        }

        // 인증된 사용자 정보 가져오기
        Object principal = authentication.getPrincipal();
        User user = null;

        // OAuth2User 타입인 경우
        if (principal instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) principal;
            String email = null;
            String provider = null;
            String providerId = null;

            // Google
            if (oauth2User.getAttributes().containsKey("email")) {
                email = oauth2User.getAttribute("email");
                provider = "google";
                providerId = oauth2User.getAttribute("sub");
            }

            // 이메일이 존재하는 경우 먼저 이메일로 검색
            if (email != null) {
                try {
                    user = userRepository.findByEmail(email).orElse(null);
                } catch (Exception e) {
                    System.out.println("이메일로 사용자 검색 실패: " + e.getMessage());
                }
            }

            // 이메일로 찾지 못한 경우 provider + providerId로 검색
            if (user == null && provider != null && providerId != null) {
                try {
                    user = userRepository.findByProviderAndProviderId(provider, providerId).orElse(null);
                } catch (Exception e) {
                    System.out.println("Provider/ProviderId로 사용자 검색 실패: " + e.getMessage());
                }
            }
        }
        // UserDetails 타입인 경우 (일반 로그인)
        else if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            try {
                user = userRepository.findByEmail(username).orElse(null);
            } catch (Exception e) {
                System.out.println("Username으로 사용자 검색 실패: " + e.getMessage());
            }
        }
        // String 타입인 경우 (기본 인증)
        else if (principal instanceof String) {
            try {
                user = userRepository.findByEmail((String) principal).orElse(null);
            } catch (Exception e) {
                System.out.println("Username(String)으로 사용자 검색 실패: " + e.getMessage());
            }
        }

        // 사용자를 찾지 못한 경우
        if (user == null) {
            System.out.println("인증된 사용자를 찾을 수 없습니다.");
            return "redirect:/login?error=user_not_found";
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

}