package com.test.feelio.controller;

import com.test.feelio.dto.DiaryDTO;
import com.test.feelio.entity.User;
import com.test.feelio.repository.UserRepository;
import com.test.feelio.service.DiaryService;
import com.test.feelio.service.CustomUserDetailsService.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class HomeController {

    private final DiaryService diaryService;
    private final UserRepository userRepository;

    @GetMapping({"/", "/home"})
    public String home(@AuthenticationPrincipal Object principal,
                       @RequestParam(value = "bookmarkOnly", defaultValue = "false") boolean bookmarkOnly,
                       Model model) {
        log.info("===== HOME 컨트롤러 호출 =====");
        log.info("Principal: {}", principal != null ? principal.getClass().getSimpleName() : "null");

        User user = getCurrentUser(principal);
        if (user == null) {
            log.info("사용자 정보를 찾을 수 없어 로그인 페이지로 리디렉션");
            return "redirect:/login";
        }

        log.info("사용자 정보: {} (enabled: {})", user.getEmail(), user.isEnabled());

        // 일기 목록 조회 (북마크 필터 적용)
        List<DiaryDTO> diaries = diaryService.getDiaryList(user.getId(), bookmarkOnly);
        log.info("일기 개수: {} (북마크 필터: {})", diaries.size(), bookmarkOnly);

        model.addAttribute("diaries", diaries);
        model.addAttribute("user", user);
        model.addAttribute("bookmarkOnly", bookmarkOnly);

        log.info("home.html 템플릿으로 이동");
        return "pages/home";
    }

    /**
     * 현재 인증된 사용자 정보를 가져오는 헬퍼 메서드
     */
    private User getCurrentUser(Object principal) {
        log.info("===== getCurrentUser 호출 =====");
        if (principal == null) {
            log.info("Principal is null");
            return null;
        }

        log.info("Principal 타입: {}", principal.getClass().getSimpleName());

        // CustomUserPrincipal (일반 로그인)
        if (principal instanceof CustomUserPrincipal) {
            log.info("CustomUserPrincipal로 사용자 인증됨");
            return ((CustomUserPrincipal) principal).getUser();
        }

        // OAuth2User (소셜 로그인) - DefaultOAuth2User 포함
        if (principal instanceof OAuth2User) {
            log.info("OAuth2User로 사용자 인증됨");
            OAuth2User oauth2User = (OAuth2User) principal;
            String email = null;
            String provider = null;
            String providerId = null;

            // 디버깅 정보 출력
            log.info("OAuth2User name: {}", oauth2User.getName());
            log.info("OAuth2User attributes: {}", oauth2User.getAttributes());

            // Google
            if (oauth2User.getAttributes().containsKey("email")) {
                email = oauth2User.getAttribute("email");
                provider = "google";
                providerId = oauth2User.getAttribute("sub");
                log.info("Google 사용자 감지: {}", email);
            }

            // 이메일이 존재하는 경우 먼저 이메일로 검색
            if (email != null) {
                try {
                    User user = userRepository.findByEmail(email).orElse(null);
                    if (user != null) {
                        log.info("이메일로 사용자 찾음: {}", user.getEmail());
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
                        log.info("Provider/ProviderId로 사용자 찾음: {}", user.getEmail());
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