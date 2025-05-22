package com.test.feelio.controller;

import com.test.feelio.dto.DiaryGalleryDTO;
import com.test.feelio.entity.User;
import com.test.feelio.service.CustomUserDetailsService;
import com.test.feelio.service.GalleryService;
import com.test.feelio.service.UserService;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;

@Controller
public class MypageController {

    private final GalleryService galleryService;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    public MypageController(GalleryService galleryService,
                            PasswordEncoder passwordEncoder,
                            UserService userService) {
        this.galleryService = galleryService;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
    }




    @GetMapping("/mypage")
    public String mypage(@AuthenticationPrincipal CustomUserDetailsService.CustomUserPrincipal userDetails, Model model) {
        if (userDetails == null) return "redirect:/login";

        //  최신 DB 상태로 강제 갱신
        User freshUser = userService.findById(userDetails.getUserId());
        model.addAttribute("user", freshUser);
        return "pages/mypage";
    }

    @GetMapping("/mypage/profile")
    public String showProfile(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserPrincipal userDetails,
            Model model) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        //  최신 사용자 정보 다시 조회
        User user = userService.findById(userDetails.getUserId());
        model.addAttribute("user", user);

        if (user.getPassword() == null) {
            return "redirect:/profile-oauth"; // 소셜 로그인 사용자
        }

        return "pages/profile";
    }


    @PostMapping("/mypage/profile")
    public String updateProfile(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserPrincipal userDetails,
            @RequestParam String nickname,
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes
    ) {
        if (userDetails == null) return "redirect:/login";

        User user = userService.findById(userDetails.getUserId());

        boolean nicknameChanged = !nickname.equals(user.getNickname());
        boolean passwordChanged = false;

        // 비밀번호 검증 및 변경
        if (!newPassword.isBlank() || !confirmPassword.isBlank() || !currentPassword.isBlank()) {

            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                redirectAttributes.addFlashAttribute("error", "현재 비밀번호가 일치하지 않습니다.");
                return "redirect:/mypage/profile";
            }

            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "새 비밀번호가 일치하지 않습니다.");
                return "redirect:/mypage/profile";
            }

            user.setPassword(passwordEncoder.encode(newPassword));
            passwordChanged = true;
        }

        if (nicknameChanged) {
            user.setNickname(nickname);
        }


        if (nicknameChanged && passwordChanged) {
            redirectAttributes.addFlashAttribute("message", "닉네임과 비밀번호가 변경되었습니다.");
        } else if (nicknameChanged) {
            redirectAttributes.addFlashAttribute("message", "닉네임이 변경되었습니다.");
        } else if (passwordChanged) {
            redirectAttributes.addFlashAttribute("message", "비밀번호가 변경되었습니다.");
        } else {
            redirectAttributes.addFlashAttribute("message", "변경된 사항이 없습니다.");
        }

        userService.updateUser(user);

        return "redirect:/mypage/profile";
    }




    @PostMapping("/mypage/profile-oauth")
    public String updateNickname(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserPrincipal userDetails,
            @RequestParam String nickname
    ) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        Long userId = userDetails.getUserId();

        userService.updateNickname(userId, nickname);

        return "redirect:/mypage/profile";
    }



    @GetMapping("/mypage/gallery")
    public String showGallery(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserPrincipal userDetails,
            Model model
    ) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        User user = userDetails.getUser(); // 진짜 Users 엔티티
        model.addAttribute("user", user);  // Thymeleaf에서 ${user.nickname} 등 사용 가능

        List<DiaryGalleryDTO> photoList = galleryService.getGalleryPhotosByUser(user.getId());

        Map<String, List<DiaryGalleryDTO>> photoMap = photoList.stream()
                .collect(Collectors.groupingBy(
                        DiaryGalleryDTO::getYearMonth,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        model.addAttribute("photoMap", photoMap);

        return "pages/gallery";
    }



    @GetMapping("/auth-check")
    @ResponseBody
    public String checkAuth() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("현재 인증 객체: " + auth);
        return auth != null ? auth.getName() : "비로그인 상태";
    }

}
